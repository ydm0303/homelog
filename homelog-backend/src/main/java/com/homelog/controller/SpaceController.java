package com.homelog.controller;

import com.homelog.dto.SpaceDissolveRequest;
import com.homelog.dto.SpaceUpdateRequest;
import com.homelog.dto.SpaceMemberDeleteRequest;
import com.homelog.dto.SpaceMemberUpdateRequest;
import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.Role;
import com.homelog.security.SpaceMembershipReader;
import com.homelog.service.SpaceService;
import com.homelog.vo.SpaceMemberVO;
import com.homelog.vo.SpaceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间控制器（设计文档 v1.2 §10 / 落地路线步 5 + 步 6）。
 * 规范：只使用 GET/POST，参数通过 query 或 body 传递。
 *
 * <p>空间一律由服务端从当前上下文（{@code X-Space-Id} + 在籍校验）解析，
 * 不再接受客户端 {@code spaceId} 入参（安全洞 S1）。
 * 破坏性生命周期操作（解散 / 退出 / 移除）委托 {@link SpaceService} 完成。
 */
@RestController
@RequestMapping("/api/space")
@RequiredArgsConstructor
@Slf4j
public class SpaceController {

    private final SpaceMemberMapper spaceMemberMapper;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final SpaceService spaceService;

    /**
     * 我的空间列表（空间切换器数据源）
     * GET /api/space/list
     */
    @GetMapping("/list")
    public List<SpaceVO> list() {
        return spaceService.listMySpaces();
    }

    /**
     * 修改当前空间名称
     * POST /api/space/update
     */
    @PostMapping("/update")
    public SpaceVO update(@RequestBody SpaceUpdateRequest request) {
        return spaceService.rename(request.getName());
    }

    /**
     * 解散当前空间（仅 owner，逻辑标记 + 撤销邀请）
     * POST /api/space/dissolve
     */
    @PostMapping("/dissolve")
    public void dissolve(@RequestBody SpaceDissolveRequest request) {
        spaceService.dissolve(request.getConfirm(), request.getName());
    }

    /**
     * 获取成员列表
     * GET /api/space/members/list
     *
     * <p>空间由服务端解析并已校验在籍身份，因此不存在「读到别人家」的路径。
     */
    @GetMapping("/members/list")
    public List<SpaceMemberVO> getMembers() {
        Long spaceId = currentUserService.getCurrentSpaceId();
        if (spaceId == null) {
            log.warn("List members denied, user has no active space");
            return new ArrayList<>();
        }
        log.info("查询空间成员：空间ID={}", spaceId);

        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        return members.stream()
                .filter(SpaceMembershipReader::isActive)
                .map(member -> SpaceMemberVO.from(member, userMapper.selectById(member.getUserId())))
                .collect(Collectors.toList());
    }

    /**
     * 更新成员（改角色 / 邮件开关）
     * POST /api/space/members/update
     */
    @PostMapping("/members/update")
    public SpaceMemberVO updateMember(@RequestBody SpaceMemberUpdateRequest request) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();
        log.info("更新空间成员：空间ID={}，用户ID={}，角色={}", spaceId, request.getUserId(), request.getRole());

        if (!Role.canManageMembers(role)) {
            log.warn("Update member denied, not an admin: spaceId={}, role={}", spaceId, role);
            throw new AccessDeniedException("权限不足，仅管理员可更新成员");
        }

        SpaceMember member = spaceMemberMapper.selectBySpaceIdAndUserId(spaceId, request.getUserId());
        if (member == null || !SpaceMembershipReader.isActive(member)) {
            log.warn("Update member failed, member not found: spaceId={}, userId={}", spaceId, request.getUserId());
            throw new IllegalArgumentException("成员不存在");
        }

        // 不能修改自己的角色（邮件开关可改）
        if (request.getUserId().equals(currentUserId) && request.getRole() != null
                && !request.getRole().equals(member.getRole())) {
            log.warn("Update member denied, cannot change own role: spaceId={}, userId={}", spaceId, request.getUserId());
            throw new IllegalArgumentException("不能修改自己的角色");
        }

        if (request.getRole() != null && !request.getRole().equals(member.getRole())) {
            // 只能管理严格低于自己的成员；目标档位也须是自己能授予的
            if (!Role.canManageTargetRole(role, member.getRole())) {
                log.warn("Update member denied, cannot manage same-or-higher tier: spaceId={}, operatorRole={}, targetRole={}",
                        spaceId, role, member.getRole());
                throw new AccessDeniedException("权限不足，不能变更该成员的角色");
            }
            if (!Role.canGrantRole(role, request.getRole())) {
                log.warn("Update member denied, cannot grant role {}: spaceId={}, operatorRole={}",
                        request.getRole(), spaceId, role);
                throw new AccessDeniedException("权限不足，不能指派该角色");
            }
            member.setRole(request.getRole());
        }
        if (request.getReceiveMail() != null) {
            member.setReceiveMail(request.getReceiveMail());
        }

        spaceMemberMapper.updateById(member);

        User user = userMapper.selectById(request.getUserId());
        log.info("空间成员更新成功：空间ID={}，用户ID={}", spaceId, request.getUserId());
        return SpaceMemberVO.from(member, user);
    }

    /**
     * 移除成员（连带删除其在本空间的内容，需 confirm=true）
     * POST /api/space/members/delete
     */
    @PostMapping("/members/delete")
    public void deleteMember(@RequestBody SpaceMemberDeleteRequest request) {
        log.info("移除空间成员：userId={}", request.getUserId());
        spaceService.removeMember(request.getUserId(), request.getConfirm());
    }

    /**
     * 主动退出当前空间（非 owner）
     * POST /api/space/members/leave
     */
    @PostMapping("/members/leave")
    public void leave() {
        log.info("成员主动退出当前空间");
        spaceService.leave();
    }
}
