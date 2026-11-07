package com.homelog.security;

import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户认证信息加载服务
 * 从 jj_user 读取账号密码，从 jj_space_member 读取空间角色并映射为 ROLE_* 权限。
 * 权限约定：1=ADMIN 2=EDIT 3=READ，与 RecordService/SpaceController/MetaController 中的角色解析保持一致。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    /** MVP 单空间：默认空间 ID */
    private static final long DEFAULT_SPACE_ID = 1L;

    private final UserMapper userMapper;
    private final SpaceMemberMapper spaceMemberMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("User details load failed, user not found: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<GrantedAuthority> authorities = resolveAuthorities(user.getId());
        log.debug("User details loaded: username={}, userId={}, authorities={}", username, user.getId(), authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    /**
     * 将空间成员角色映射为 Spring Security 权限（ROLE_ADMIN/ROLE_EDIT/ROLE_READ）
     * 优先取默认空间(1)的成员关系；新注册用户空间 id 非 1，则回退为其任一成员关系。
     */
    private List<GrantedAuthority> resolveAuthorities(Long userId) {
        List<SpaceMember> memberships = spaceMemberMapper.selectByUserId(userId);
        if (memberships == null || memberships.isEmpty()) {
            return Collections.emptyList();
        }

        SpaceMember member = memberships.stream()
                .filter(m -> m.getSpaceId() != null && m.getSpaceId() == DEFAULT_SPACE_ID)
                .findFirst()
                .orElse(memberships.get(0));

        if (member.getRole() == null) {
            return Collections.emptyList();
        }

        String role;
        switch (member.getRole()) {
            case 1:
                role = "ROLE_ADMIN";
                break;
            case 2:
                role = "ROLE_EDIT";
                break;
            default:
                role = "ROLE_READ";
                break;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}
