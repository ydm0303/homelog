package com.homelog.service;

import com.homelog.entity.Invitation;
import com.homelog.entity.Space;
import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.InvitationMapper;
import com.homelog.mapper.SpaceMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.Role;
import com.homelog.security.SpaceMembershipReader;
import com.homelog.vo.InvitationVO;
import com.homelog.vo.InviteeCheckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间邀请服务（设计文档 v1.2 §6.2-6.4 / §10 / 落地路线步 4）。
 *
 * <p>决策 D2 的落地形态：<b>没有 token、没有链接、没有过期时间</b>。
 * 邀请是一条服务端记录，被邀请人从站内通知里凭 invitationId 接受或拒绝，
 * 全程校验 {@code invitee_id == 当前登录用户}。这比 token 链接的不变面更小——
 * 链接会泄露，而站内通知受账号鉴权保护。
 *
 * <p>并发与幂等：接受/拒绝走 {@code UPDATE ... WHERE status = pending} 原子条件更新，
 * 影响行数 0 即表示已被处理，无需唯一约束也绝不会产生重复成员关系；
 * 插入成员关系前按 (space_id, user_id) 查旧行，命中则 UPDATE 复用（支持 §9「被移除后重新邀请」）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    /** 成员状态：在籍 */
    private static final int MEMBER_STATUS_ACTIVE = 1;
    /** 邀请状态 */
    private static final int PENDING = Invitation.STATUS_PENDING;

    private final InvitationMapper invitationMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final SpaceMapper spaceMapper;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final MessageService messageService;

    /**
     * 发起前真实性预检：判断标识（账号或邮箱）是否对应已注册账号，并返回脱敏信息供前端确认。
     *
     * <p>与登录/邀请一致的多标识解析规则见 {@link #resolveInvitee}。本方法<b>不抛异常</b>，
     * 仅以 {@code exists=false} 反馈「未注册」，让对话框可在失焦时就地提示而不打断输入流。
     * 仍要求调用方具备成员管理权，避免无权限者借预检枚举账号。
     */
    public InviteeCheckVO check(String identifier) {
        Integer inviterRole = currentUserService.getCurrentRole();
        if (!Role.canManageMembers(inviterRole)) {
            throw new AccessDeniedException("权限不足，仅所有者与管理员可邀请成员");
        }
        User invitee = resolveInvitee(identifier);
        if (invitee == null) {
            return new InviteeCheckVO().setExists(false);
        }
        return new InviteeCheckVO()
                .setExists(true)
                .setNickname(displayOf(invitee.getId()))
                .setEmail(maskEmail(invitee.getEmail()));
    }

    /**
     * 发起邀请（当前空间内，邀请人须有成员管理权）。
     *
     * @param identifier 被邀请人的账号或邮箱，必须是系统内已存在账号（决策：不支持匿名/未注册邀请）。
     *                   与登录一致支持多标识：含 {@code @} 视为邮箱，否则先按用户名查、再按邮箱兜底。
     * @param role       授予角色
     * @return 新建或复用的待处理邀请
     */
    @Transactional
    public InvitationVO invite(String identifier, Integer role) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer inviterRole = currentUserService.getCurrentRole();
        Long inviterId = currentUserService.getCurrentUserId();

        if (!Role.canManageMembers(inviterRole)) {
            log.warn("Invite denied, cannot manage members: spaceId={}, role={}", spaceId, inviterRole);
            throw new AccessDeniedException("权限不足，仅所有者与管理员可邀请成员");
        }
        if (role == null) {
            throw new IllegalArgumentException("请选择邀请角色");
        }
        if (!Role.canGrantRole(inviterRole, role)) {
            log.warn("Invite denied, cannot grant role: spaceId={}, inviterRole={}, targetRole={}",
                    spaceId, inviterRole, role);
            throw new AccessDeniedException("权限不足，不能邀请该角色");
        }

        // 被邀请账号必须已存在：账号或邮箱任一命中即可（多标识，与登录保持一致）
        User invitee = resolveInvitee(identifier);
        if (invitee == null) {
            log.warn("Invite failed, invitee not registered: spaceId={}, identifier={}", spaceId, identifier);
            throw new IllegalArgumentException("该账号或邮箱尚未注册，请确认对方用户名/邮箱是否正确，或让对方先注册");
        }

        // 已在籍则拒绝（§9）
        SpaceMember existing = spaceMemberMapper.selectBySpaceIdAndUserId(spaceId, invitee.getId());
        if (existing != null && SpaceMembershipReader.isActive(existing)) {
            log.warn("Invite failed, already an active member: spaceId={}, inviteeId={}", spaceId, invitee.getId());
            throw new IllegalArgumentException("该用户已经是本家庭成员");
        }

        // 待处理邀请去重：幂等返回原邀请，不重复插
        Invitation pending = invitationMapper.selectPendingBySpaceAndInvitee(spaceId, invitee.getId());
        if (pending != null) {
            log.info("Invite deduplicated, reuse pending invitation: id={}, spaceId={}, inviteeId={}",
                    pending.getId(), spaceId, invitee.getId());
            return toVO(pending);
        }

        Invitation invitation = new Invitation()
                .setSpaceId(spaceId)
                .setInviterId(inviterId)
                .setInviteeId(invitee.getId())
                .setRole(role)
                .setStatus(PENDING);
        invitationMapper.insert(invitation);

        // 站内通知给被邀请人：ref_id 指回邀请，前端据此渲染接受/拒绝按钮
        Space space = spaceMapper.selectById(spaceId);
        String spaceName = space != null ? space.getName() : "家庭空间";
        String content = String.format("🏠 %s 想邀请你一起经营「%s」，成为家里的%s。点下方按钮即可同意或婉拒～",
                displayOf(inviterId), spaceName, Role.label(role));
        messageService.createNotification(spaceId, invitee.getId(), invitation.getId(),
                content, MessageType.FAMILY_INVITATION);

        log.info("邀请已发起：invitationId={}, spaceId={}, inviterId={}, inviteeId={}, role={}",
                invitation.getId(), spaceId, inviterId, invitee.getId(), role);
        return toVO(invitation);
    }

    /**
     * 被邀请人接受邀请。
     *
     * <p>核心保证：不改动被邀请人在<b>自己</b>家庭空间的 owner 身份（§6.3 第 4 点，
     * 也是本次重设计要解决的原始问题 —— 角色不再挂在账号上）。
     */
    @Transactional
    public InvitationVO accept(Long invitationId) {
        Long inviteeId = currentUserService.getCurrentUserId();
        if (inviteeId == null) {
            throw new AccessDeniedException("请先登录");
        }

        Invitation invitation = invitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new IllegalArgumentException("邀请不存在");
        }
        if (!inviteeId.equals(invitation.getInviteeId())) {
            log.warn("Accept denied, not the invitee: invitationId={}, inviteeId={}, currentUserId={}",
                    invitationId, invitation.getInviteeId(), inviteeId);
            throw new AccessDeniedException("无权处理该邀请");
        }
        if (invitation.getStatus() == null || invitation.getStatus() != PENDING) {
            throw new IllegalArgumentException("该邀请已处理或已失效");
        }

        // 目标空间必须仍正常存在（§9）
        Space space = spaceMapper.selectById(invitation.getSpaceId());
        if (space == null || (space.getStatus() != null && space.getStatus() != 1)) {
            invitation.setStatus(Invitation.STATUS_CANCELLED);
            invitation.setRespondedAt(LocalDateTime.now());
            invitationMapper.updateById(invitation);
            log.warn("Accept failed, space not available: invitationId={}, spaceId={}",
                    invitationId, invitation.getSpaceId());
            throw new IllegalArgumentException("该家庭空间已不存在或已解散");
        }

        // 原子接受：并发下只有一人成功
        int updated = invitationMapper.acceptAtomically(invitationId, inviteeId, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("该邀请已被处理");
        }

        // 成员关系：有历史行则复用（§9 被移除后重新加入），否则新建
        SpaceMember existing = spaceMemberMapper.selectBySpaceIdAndUserId(invitation.getSpaceId(), inviteeId);
        if (existing != null) {
            existing.setRole(invitation.getRole())
                    .setStatus(MEMBER_STATUS_ACTIVE);
            spaceMemberMapper.updateById(existing);
        } else {
            SpaceMember member = new SpaceMember()
                    .setSpaceId(invitation.getSpaceId())
                    .setUserId(inviteeId)
                    .setRole(invitation.getRole())
                    .setStatus(MEMBER_STATUS_ACTIVE)
                    .setReceiveMail(1);
            spaceMemberMapper.insert(member);
        }

        // 回执通知给邀请人（§8）
        String content = String.format("🎉 好消息！%s 接受了你的邀请，已经加入「%s」，快一起把家里的事记起来吧～",
                displayOf(inviteeId), space.getName());
        messageService.createNotification(invitation.getSpaceId(), invitation.getInviterId(),
                invitationId, content, MessageType.FAMILY_RESULT);

        invitation.setStatus(Invitation.STATUS_ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        log.info("邀请已接受：invitationId={}, spaceId={}, inviteeId={}, role={}",
                invitationId, invitation.getSpaceId(), inviteeId, invitation.getRole());
        return toVO(invitation);
    }

    /**
     * 被邀请人拒绝邀请。不产生成员关系。
     */
    @Transactional
    public InvitationVO reject(Long invitationId) {
        Long inviteeId = currentUserService.getCurrentUserId();
        if (inviteeId == null) {
            throw new AccessDeniedException("请先登录");
        }

        Invitation invitation = invitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new IllegalArgumentException("邀请不存在");
        }
        if (!inviteeId.equals(invitation.getInviteeId())) {
            throw new AccessDeniedException("无权处理该邀请");
        }
        if (invitation.getStatus() == null || invitation.getStatus() != PENDING) {
            throw new IllegalArgumentException("该邀请已处理或已失效");
        }

        int updated = invitationMapper.rejectAtomically(invitationId, inviteeId, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("该邀请已被处理");
        }

        String content = String.format("%s 这次先婉拒了你的家庭邀请，之后随时欢迎再邀请～", displayOf(inviteeId));
        messageService.createNotification(invitation.getSpaceId(), invitation.getInviterId(),
                invitationId, content, MessageType.FAMILY_RESULT);

        invitation.setStatus(Invitation.STATUS_REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        log.info("邀请已拒绝：invitationId={}, spaceId={}, inviteeId={}",
                invitationId, invitation.getSpaceId(), inviteeId);
        return toVO(invitation);
    }

    /**
     * 邀请人撤销自己发出的待处理邀请。
     */
    @Transactional
    public void cancel(Long invitationId) {
        Long inviterId = currentUserService.getCurrentUserId();
        if (inviterId == null) {
            throw new AccessDeniedException("请先登录");
        }
        int updated = invitationMapper.cancelAtomically(invitationId, inviterId, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("邀请不存在或已处理");
        }
        log.info("邀请已撤销：invitationId={}, inviterId={}", invitationId, inviterId);
    }

    /**
     * 我收到的邀请（站内通知中心 / 邀请列表用）。
     *
     * @param pendingOnly 仅待处理
     */
    public List<InvitationVO> listReceived(boolean pendingOnly) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return new ArrayList<>();
        }
        List<Invitation> list = invitationMapper.selectReceived(userId);
        if (pendingOnly) {
            list = list.stream()
                    .filter(i -> i.getStatus() != null && i.getStatus() == PENDING)
                    .collect(Collectors.toList());
        }
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 我在当前空间发出的邀请，供邀请方查看对方是否已同意/拒绝（§8）。
     */
    public List<InvitationVO> listSent(boolean pendingOnly) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Long userId = currentUserService.getCurrentUserId();
        if (spaceId == null || userId == null) {
            return new ArrayList<>();
        }
        List<Invitation> list = invitationMapper.selectSent(spaceId, userId);
        if (pendingOnly) {
            list = list.stream()
                    .filter(i -> i.getStatus() != null && i.getStatus() == PENDING)
                    .collect(Collectors.toList());
        }
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 级联撤销某邀请人在某空间的全部待处理邀请。
     * <p>供步 6「移除成员」调用：邀请人中途被移除或降权，其发出的邀请不应再生效（§9）。
     */
    @Transactional
    public int cancelPendingByInviter(Long spaceId, Long inviterId) {
        int cancelled = invitationMapper.cancelPendingByInviter(spaceId, inviterId, LocalDateTime.now());
        if (cancelled > 0) {
            log.info("级联撤销待处理邀请：spaceId={}, inviterId={}, 数量={}", spaceId, inviterId, cancelled);
        }
        return cancelled;
    }

    private InvitationVO toVO(Invitation invitation) {
        InvitationVO vo = new InvitationVO()
                .setId(invitation.getId())
                .setSpaceId(invitation.getSpaceId())
                .setInviterId(invitation.getInviterId())
                .setInviteeId(invitation.getInviteeId())
                .setRole(invitation.getRole())
                .setRoleLabel(Role.label(invitation.getRole()))
                .setStatus(invitation.getStatus())
                .setStatusLabel(statusLabel(invitation.getStatus()))
                .setCreatedAt(invitation.getCreatedAt())
                .setRespondedAt(invitation.getRespondedAt());

        Space space = spaceMapper.selectById(invitation.getSpaceId());
        vo.setSpaceName(space != null ? space.getName() : null);
        vo.setInviterNickname(displayOf(invitation.getInviterId()));
        vo.setInviteeNickname(displayOf(invitation.getInviteeId()));
        return vo;
    }

    /**
     * 按标识解析被邀请人：含 @ 视为邮箱；否则先按用户名查，未命中再按邮箱兜底
     * （兼容用户误把邮箱填进"账号"框、或用户名恰好含特殊字符的情况）。
     */
    private User resolveInvitee(String identifier) {
        String key = identifier == null ? null : identifier.trim();
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.contains("@")) {
            return userMapper.selectByEmail(key);
        }
        User byName = userMapper.selectByUsername(key);
        return byName != null ? byName : userMapper.selectByEmail(key);
    }

    /**
     * 邮箱脱敏：保留前缀前若干位与完整域名，中间以 *** 替代，供邀请人核对是否本人。
     * 例：abcdef@qq.com → abc***@qq.com；过短则仅留首字符。
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email.charAt(0) + "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        int keep = Math.min(3, Math.max(1, local.length() - 2));
        return local.substring(0, keep) + "***" + domain;
    }

    private String displayOf(Long userId) {
        if (userId == null) {
            return "家庭成员";
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "家庭成员";
        }
        return user.getNickName() != null && !user.getNickName().trim().isEmpty()
                ? user.getNickName() : user.getUsername();
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Invitation.STATUS_PENDING:
                return "待处理";
            case Invitation.STATUS_ACCEPTED:
                return "已接受";
            case Invitation.STATUS_REJECTED:
                return "已拒绝";
            case Invitation.STATUS_CANCELLED:
                return "已撤销";
            default:
                return "未知";
        }
    }
}
