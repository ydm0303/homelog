package com.homelog.service;

import com.homelog.entity.Module;
import com.homelog.entity.Record;
import com.homelog.entity.Space;
import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.InvitationMapper;
import com.homelog.mapper.MessageMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.SpaceMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.Role;
import com.homelog.security.SpaceMembershipReader;
import com.homelog.vo.SpaceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 家庭空间生命周期服务（设计文档 v1.2 §6.5-6.8 / 落地路线步 5 + 步 6）。
 *
 * <p>覆盖：我的空间列表、改名、解散、主动退出、移除成员（连带清理）。
 * 所有方法以 {@link CurrentUserService} 解析的「当前空间 + 当前角色」为准，
 * 不接受客户端传入的 spaceId，角色一律走 {@link Role} 语义方法。
 *
 * <p><b>解散按评审决策采用「逻辑标记 + 延迟清理」</b>：只把 {@code jj_space.status}
 * 置 2、在籍成员置 left、撤销该空间的 pending 邀请，不物理删除模块/记录/图片。
 * 这样可回滚、避免长事务锁表，留待后续异步清理。
 *
 * <p><b>移除成员采用「完整连带删除」</b>（§6.7）：成员置 removed，并删除其在本空间
 * 创建的全部记录、记录图片目录、名下消息、以及其发起的 pending 邀请。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceService {

    /** 空间状态：正常 */
    private static final int SPACE_STATUS_ACTIVE = 1;
    /** 空间状态：已解散 */
    private static final int SPACE_STATUS_DISSOLVED = 2;

    /** 成员状态：在籍 */
    private static final int MEMBER_STATUS_ACTIVE = 1;
    /** 成员状态：已退出 */
    private static final int MEMBER_STATUS_LEFT = 2;
    /** 成员状态：已移除 */
    private static final int MEMBER_STATUS_REMOVED = 3;

    private final SpaceMapper spaceMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final RecordMapper recordMapper;
    private final MessageMapper messageMapper;
    private final ModuleMapper moduleMapper;
    private final InvitationMapper invitationMapper;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final MessageService messageService;

    /**
     * 当前用户全部在籍空间（切换器数据源）。已解散空间不返回。
     */
    public List<SpaceVO> listMySpaces() {
        List<SpaceMember> memberships = currentUserService.getMyActiveMemberships();
        List<SpaceVO> result = new ArrayList<>();
        for (SpaceMember membership : memberships) {
            Space space = spaceMapper.selectById(membership.getSpaceId());
            if (space == null || !isSpaceActive(space)) {
                continue;
            }
            result.add(SpaceVO.from(space, membership.getRole(), countActiveMembers(space.getId())));
        }
        return result;
    }

    /**
     * 修改当前空间名称（owner / admin）。
     */
    @Transactional
    public SpaceVO rename(String name) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        if (!Role.canManageMembers(role)) {
            throw new AccessDeniedException("权限不足，仅所有者与管理员可修改空间名称");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("空间名称不能为空");
        }
        Space space = requireActiveSpace(spaceId);
        space.setName(name.trim());
        spaceMapper.updateById(space);
        log.info("空间已改名：spaceId={}, name={}", spaceId, space.getName());
        return SpaceVO.from(space, role, countActiveMembers(spaceId));
    }

    /**
     * 解散当前空间（仅 owner）。逻辑标记 + 撤销邀请，数据保留待延迟清理。
     */
    @Transactional
    public void dissolve(Boolean confirm, String name) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        if (!Role.canDissolve(role)) {
            throw new AccessDeniedException("权限不足，仅所有者可解散空间");
        }
        Space space = requireActiveSpace(spaceId);
        if (!Boolean.TRUE.equals(confirm)) {
            throw new IllegalArgumentException("解散空间属于危险操作，请确认后再执行");
        }
        if (name == null || !name.trim().equals(space.getName())) {
            throw new IllegalArgumentException("请输入正确的空间名称以确认解散");
        }

        // 1. 逻辑标记空间
        markSpaceDissolved(spaceId);

        // 2. 在籍成员全部置为已退出（保留成员行以便追溯）
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        for (SpaceMember member : members) {
            if (SpaceMembershipReader.isActive(member)) {
                member.setStatus(MEMBER_STATUS_LEFT);
                spaceMemberMapper.updateById(member);
            }
        }

        // 3. 撤销该空间全部待处理邀请
        int cancelled = invitationMapper.cancelPendingBySpace(spaceId, LocalDateTime.now());

        log.info("空间已解散（逻辑标记）：spaceId={}, 退出成员数={}, 撤销邀请数={}",
                spaceId, members.size(), cancelled);
    }

    /**
     * 主动退出当前空间（非 owner）。owner 不支持退出，只能解散。
     */
    @Transactional
    public void leave() {
        SpaceMember membership = currentUserService.getCurrentMembership();
        if (membership == null) {
            throw new AccessDeniedException("当前账号不在任何家庭空间中");
        }
        if (Role.isOwner(membership.getRole())) {
            throw new IllegalArgumentException("所有者不可退出家庭空间，如需离开请解散空间");
        }

        membership.setStatus(MEMBER_STATUS_LEFT);
        spaceMemberMapper.updateById(membership);

        // 其在本空间发起的待处理邀请不再有效（§9 邀请人被移除/降权 → 级联撤销）
        int cancelled = invitationMapper.cancelPendingByInviter(
                membership.getSpaceId(), membership.getUserId(), LocalDateTime.now());

        // 告知空间所有者（仅通知，无需操作）
        notifyOwnerOnLeave(membership);

        log.info("成员主动退出空间：spaceId={}, userId={}, 撤销邀请数={}",
                membership.getSpaceId(), membership.getUserId(), cancelled);
    }

    /**
     * 成员主动退出后，给空间所有者发一条告知型站内通知。
     * 所有者不可退出，故正常不会出现 owner 通知自己的情况；此处仍做防御。
     */
    private void notifyOwnerOnLeave(SpaceMember membership) {
        Space space = spaceMapper.selectById(membership.getSpaceId());
        if (space == null || space.getOwnerId() == null) {
            return;
        }
        if (space.getOwnerId().equals(membership.getUserId())) {
            return;
        }

        User leaver = userMapper.selectById(membership.getUserId());
        String leaverName;
        if (leaver == null) {
            leaverName = "某成员";
        } else if (leaver.getNickName() != null && !leaver.getNickName().trim().isEmpty()) {
            leaverName = leaver.getNickName();
        } else {
            leaverName = leaver.getUsername();
        }

        String content = String.format("%s 已退出【%s】", leaverName, space.getName());
        messageService.createNotification(space.getId(), space.getOwnerId(), null, content,
                MessageType.FAMILY_MEMBER_LEAVE);
        log.info("已通知空间所有者成员退出：spaceId={}, ownerId={}, leaverId={}",
                space.getId(), space.getOwnerId(), membership.getUserId());
    }

    /**
     * 移除成员并连带清理其内容（owner / admin；owner 不可被移除）。
     *
     * @param userId  被移除成员的用户 ID
     * @param confirm 必须为 true；缺失即拒绝，避免误触破坏性操作
     */
    @Transactional
    public void removeMember(Long userId, Boolean confirm) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer operatorRole = currentUserService.getCurrentRole();
        Long operatorId = currentUserService.getCurrentUserId();

        if (!Role.canManageMembers(operatorRole)) {
            throw new AccessDeniedException("权限不足，仅所有者与管理员可移除成员");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (userId.equals(operatorId)) {
            throw new IllegalArgumentException("不能移除自己，如需离开请使用退出功能");
        }
        if (!Boolean.TRUE.equals(confirm)) {
            throw new IllegalArgumentException("移除成员会连带删除其内容，属危险操作，请确认后再执行");
        }

        SpaceMember target = spaceMemberMapper.selectBySpaceIdAndUserId(spaceId, userId);
        if (target == null || !SpaceMembershipReader.isActive(target)) {
            throw new IllegalArgumentException("成员不存在或已不在该空间");
        }
        // owner 不可被移除；admin 不能移除同级 admin（仅 owner 可）
        if (!Role.canManageTargetRole(operatorRole, target.getRole())) {
            if (Role.isOwner(target.getRole())) {
                throw new AccessDeniedException("所有者不可被移除");
            }
            throw new AccessDeniedException("权限不足，仅所有者可移除管理员");
        }

        // 1. 成员置为已移除（保留行，支持未来重新邀请）
        target.setStatus(MEMBER_STATUS_REMOVED);
        spaceMemberMapper.updateById(target);

        // 2. 删除其在本空间创建的记录与图片目录
        List<Record> records = recordMapper.selectBySpaceAndCreator(spaceId, userId);
        for (Record record : records) {
            Module module = moduleMapper.selectById(record.getModuleId());
            if (module != null) {
                fileStorageService.deleteRecordDirectory(module.getModuleKey(), record.getId());
            }
            recordMapper.deleteById(record.getId());
        }

        // 3. 清理其名下消息
        int messages = messageMapper.deleteByUserAndSpace(userId, spaceId);

        // 4. 级联撤销其发起的 pending 邀请
        int cancelled = invitationMapper.cancelPendingByInviter(spaceId, userId, LocalDateTime.now());

        log.info("成员已移除并连带清理：spaceId={}, userId={}, 删除记录数={}, 删除消息数={}, 撤销邀请数={}",
                spaceId, userId, records.size(), messages, cancelled);
    }

    private Space requireActiveSpace(Long spaceId) {
        Space space = spaceMapper.selectById(spaceId);
        if (space == null || !isSpaceActive(space)) {
            throw new IllegalArgumentException("家庭空间不存在或已解散");
        }
        return space;
    }

    private void markSpaceDissolved(Long spaceId) {
        Space space = new Space();
        space.setId(spaceId);
        space.setStatus(SPACE_STATUS_DISSOLVED);
        spaceMapper.updateById(space);
    }

    private boolean isSpaceActive(Space space) {
        return space.getStatus() == null || space.getStatus() == SPACE_STATUS_ACTIVE;
    }

    private int countActiveMembers(Long spaceId) {
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        int count = 0;
        for (SpaceMember member : members) {
            if (SpaceMembershipReader.isActive(member)) {
                count++;
            }
        }
        return count;
    }
}
