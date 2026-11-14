package com.homelog.security;

import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 当前登录用户/空间上下文。
 *
 * 统一从 SecurityContext 的用户名反查 jj_user 与 jj_space_member，
 * 避免各 Service/Controller 里继续写死 userId=1、spaceId=1。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    /** MVP 单空间：默认空间 ID */
    public static final long DEFAULT_SPACE_ID = 1L;

    private final UserMapper userMapper;
    private final SpaceMemberMapper spaceMemberMapper;

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.trim().isEmpty() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userMapper.selectByUsername(username);
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 当前用户在当前空间的成员关系；优先默认空间，找不到则取任一成员关系。
     */
    public SpaceMember getCurrentMembership() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        List<SpaceMember> memberships = spaceMemberMapper.selectByUserId(user.getId());
        if (memberships == null || memberships.isEmpty()) {
            return null;
        }
        return memberships.stream()
                .filter(m -> m.getSpaceId() != null && m.getSpaceId() == DEFAULT_SPACE_ID)
                .findFirst()
                .orElse(memberships.get(0));
    }

    public Long getCurrentSpaceId() {
        SpaceMember member = getCurrentMembership();
        return member != null && member.getSpaceId() != null ? member.getSpaceId() : DEFAULT_SPACE_ID;
    }

    /**
     * 当前用户在空间内的角色：1=管理员 2=可编辑 3=只读。
     * 优先使用成员表角色，回退到 Spring Security 的 ROLE_* 权限。
     */
    public Integer getCurrentRole() {
        SpaceMember member = getCurrentMembership();
        if (member != null && member.getRole() != null) {
            return member.getRole();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return 3;
        }
        return auth.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(role -> {
                    switch (role) {
                        case "ADMIN":
                            return 1;
                        case "EDIT":
                            return 2;
                        default:
                            return 3;
                    }
                })
                .findFirst()
                .orElse(3);
    }

    public boolean isAdmin() {
        return getCurrentRole() == 1;
    }
}
