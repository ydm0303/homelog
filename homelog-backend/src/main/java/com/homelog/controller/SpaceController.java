package com.homelog.controller;

import com.homelog.dto.SpaceMemberCreateRequest;
import com.homelog.dto.SpaceMemberUpdateRequest;
import com.homelog.dto.SpaceMemberDeleteRequest;
import com.homelog.entity.SpaceMember;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.Role;
import com.homelog.security.SpaceMembershipReader;
import com.homelog.vo.SpaceMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间成员控制器
 * 规范：只使用 GET/POST，参数通过 query 或 body 传递
 */
@RestController
@RequestMapping("/api/space")
@RequiredArgsConstructor
@Slf4j
public class SpaceController {

    private final SpaceMemberMapper spaceMemberMapper;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    /**
     * 获取成员列表
     * GET /api/space/members/list
     *
     * <p>步 2：去掉 {@code spaceId} 请求参数。原实现允许管理员通过 {@code ?spaceId=}
     * 读取<b>任意</b>空间的成员名单，单空间时无害，多空间开放后即是 IDOR
     * （可枚举他人家庭结构）—— 设计文档 v1.2 §15 安全洞 S1。
     * 现在空间一律由服务端从当前上下文解析，而解析本身已强制校验在籍身份，
     * 因此不存在「读到别人家」的路径。
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
                .map(member -> {
                    var user = userMapper.selectById(member.getUserId());
                    return SpaceMemberVO.from(member, user);
                })
                .collect(Collectors.toList());
    }

    /**
     * 添加成员
     * POST /api/space/members/create
     */
    @PostMapping("/members/create")
    public SpaceMemberVO createMember(@RequestBody SpaceMemberCreateRequest request) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("添加空间成员：空间ID={}，邮箱={}，角色={}", spaceId, request.getEmail(), request.getRole());

        // 只有管理员可添加成员
        if (!Role.canManageMembers(role)) {
            log.warn("Add member denied, not an admin: spaceId={}, role={}", spaceId, role);
            throw new RuntimeException("权限不足，仅管理员可添加成员");
        }

        // 检查用户是否存在
        var user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            log.warn("Add member failed, user not found: email={}", request.getEmail());
            throw new RuntimeException("用户不存在");
        }

        // 检查是否已经是成员
        SpaceMember existing = spaceMemberMapper.selectBySpaceIdAndUserId(spaceId, user.getId());
        if (existing != null) {
            log.warn("Add member failed, already a member: spaceId={}, userId={}", spaceId, user.getId());
            throw new RuntimeException("该用户已经是成员");
        }

        // owner 由创建空间产生，不接受指派；且任何角色不能授予高于自身的角色
        if (!Role.canGrantRole(role, request.getRole())) {
            log.warn("Add member denied, cannot grant role {}: spaceId={}, operatorRole={}",
                    request.getRole(), spaceId, role);
            throw new RuntimeException("权限不足，不能指派该角色");
        }

        SpaceMember member = new SpaceMember()
                .setSpaceId(spaceId)
                .setUserId(user.getId())
                .setRole(request.getRole())
                .setReceiveMail(request.getReceiveMail())
                .setCreatedAt(java.time.LocalDateTime.now());

        spaceMemberMapper.insert(member);
        log.info("空间成员添加成功：空间ID={}，用户ID={}，角色={}", spaceId, user.getId(), request.getRole());

        return SpaceMemberVO.from(member, user);
    }

    /**
     * 更新成员
     * POST /api/space/members/update
     */
    @PostMapping("/members/update")
    public SpaceMemberVO updateMember(@RequestBody SpaceMemberUpdateRequest request) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("更新空间成员：空间ID={}，用户ID={}，角色={}", spaceId, request.getUserId(), request.getRole());

        // 只有管理员可更新成员
        if (!Role.canManageMembers(role)) {
            log.warn("Update member denied, not an admin: spaceId={}, role={}", spaceId, role);
            throw new RuntimeException("权限不足，仅管理员可更新成员");
        }

        SpaceMember member = spaceMemberMapper.selectBySpaceIdAndUserId(spaceId, request.getUserId());
        if (member == null) {
            log.warn("Update member failed, member not found: spaceId={}, userId={}", spaceId, request.getUserId());
            throw new RuntimeException("成员不存在");
        }

        // 不能修改自己的角色
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equals(userMapper.selectById(request.getUserId()).getUsername())) {
            if (request.getRole() != null && !request.getRole().equals(member.getRole())) {
                log.warn("Update member denied, cannot change own role: spaceId={}, userId={}", spaceId, request.getUserId());
                throw new RuntimeException("不能修改自己的角色");
            }
        }

        if (request.getRole() != null && !request.getRole().equals(member.getRole())) {
            // 只能管理严格低于自己的成员：挡住「admin 把同级 admin 降为 member 架空对方」
            // 以及「任何人改 owner」两类越权
            if (!Role.canManageTargetRole(role, member.getRole())) {
                log.warn("Update member denied, cannot manage same-or-higher tier: spaceId={}, operatorRole={}, targetRole={}",
                        spaceId, role, member.getRole());
                throw new RuntimeException("权限不足，不能变更该成员的角色");
            }
            // 目标档位也必须是自己能授予的（不能把人放到等于或高于自己的档）
            if (!Role.canGrantRole(role, request.getRole())) {
                log.warn("Update member denied, cannot grant role {}: spaceId={}, operatorRole={}",
                        request.getRole(), spaceId, role);
                throw new RuntimeException("权限不足，不能指派该角色");
            }
            member.setRole(request.getRole());
        }
        if (request.getReceiveMail() != null) member.setReceiveMail(request.getReceiveMail());

        spaceMemberMapper.updateById(member);

        var user = userMapper.selectById(request.getUserId());
        log.info("空间成员更新成功：空间ID={}，用户ID={}", spaceId, request.getUserId());
        return SpaceMemberVO.from(member, user);
    }

    /**
     * 删除成员
     * POST /api/space/members/delete
     */
    @PostMapping("/members/delete")
    public void deleteMember(@RequestBody SpaceMemberDeleteRequest request) {
        Long spaceId = currentUserService.requireCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("删除空间成员：空间ID={}，用户ID={}", spaceId, request.getUserId());

        // 只有管理员可删除成员
        if (!Role.canManageMembers(role)) {
            log.warn("Delete member denied, not an admin: spaceId={}, role={}", spaceId, role);
            throw new RuntimeException("权限不足，仅管理员可删除成员");
        }

        SpaceMember member = spaceMemberMapper.selectBySpaceIdAndUserId(spaceId, request.getUserId());
        if (member == null) {
            log.warn("Delete member failed, member not found: spaceId={}, userId={}", spaceId, request.getUserId());
            throw new RuntimeException("成员不存在");
        }

        // 不能删除自己
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equals(userMapper.selectById(request.getUserId()).getUsername())) {
            log.warn("Delete member denied, cannot delete self: spaceId={}, userId={}", spaceId, request.getUserId());
            throw new RuntimeException("不能删除自己");
        }

        // 只能移除严格低于自己的成员。等价覆盖两条矩阵规则：
        //   · owner 不可被任何人移除（含其本人），如需离开请解散空间
        //   · admin 不可移除同为 admin 的成员 —— 仅 owner 可
        if (!Role.canManageTargetRole(role, member.getRole())) {
            if (Role.isOwner(member.getRole())) {
                log.warn("Delete member denied, owner cannot be removed: spaceId={}, userId={}", spaceId, request.getUserId());
                throw new RuntimeException("所有者不可被移除，如需离开请解散空间");
            }
            log.warn("Delete member denied, cannot remove same-or-higher tier: spaceId={}, operatorRole={}, targetRole={}",
                    spaceId, role, member.getRole());
            throw new RuntimeException("权限不足，仅所有者可移除管理员");
        }

        spaceMemberMapper.deleteById(member.getId());
        log.info("空间成员删除成功：空间ID={}，用户ID={}", spaceId, request.getUserId());
    }
}