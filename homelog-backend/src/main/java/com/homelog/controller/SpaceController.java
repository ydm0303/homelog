package com.homelog.controller;

import com.homelog.dto.SpaceMemberCreateRequest;
import com.homelog.dto.SpaceMemberUpdateRequest;
import com.homelog.dto.SpaceMemberDeleteRequest;
import com.homelog.entity.SpaceMember;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.JwtTokenProvider;
import com.homelog.vo.SpaceMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 获取当前空间ID（MVP 单空间，默认 space_id=1）
     */
    private Long getCurrentSpaceId() {
        return 1L;
    }

    /**
     * 获取当前用户角色
     */
    private Integer getCurrentUserRole(Long spaceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return 3;

        return auth.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(role -> {
                    switch (role) {
                        case "ADMIN": return 1;
                        case "EDIT": return 2;
                        default: return 3;
                    }
                })
                .findFirst()
                .orElse(3);
    }

    /**
     * 获取成员列表
     * GET /api/space/members/list?spaceId=1
     */
    @GetMapping("/members/list")
    public List<SpaceMemberVO> getMembers(@RequestParam Long spaceId) {
        log.info("查询空间成员：空间ID={}", spaceId);

        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        return members.stream()
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
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole(spaceId);
        log.info("添加空间成员：空间ID={}，邮箱={}，角色={}", spaceId, request.getEmail(), request.getRole());

        // 只有管理员可添加成员
        if (role != 1) {
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
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole(spaceId);
        log.info("更新空间成员：空间ID={}，用户ID={}，角色={}", spaceId, request.getUserId(), request.getRole());

        // 只有管理员可更新成员
        if (role != 1) {
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

        if (request.getRole() != null) member.setRole(request.getRole());
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
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole(spaceId);
        log.info("删除空间成员：空间ID={}，用户ID={}", spaceId, request.getUserId());

        // 只有管理员可删除成员
        if (role != 1) {
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

        spaceMemberMapper.deleteById(member.getId());
        log.info("空间成员删除成功：空间ID={}，用户ID={}", spaceId, request.getUserId());
    }
}