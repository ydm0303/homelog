package com.homelog.security;

import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
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
 * 用户认证信息加载服务。
 *
 * <p>从 jj_user 读取账号密码；并为用户生成一个<b>代表性的</b> ROLE_* 权限写入 JWT。
 * 注意该权限只是元数据：真正的鉴权由 {@link CurrentUserService} 按请求实时解析
 * 「当前空间 + 该空间内的角色」完成，档位语义统一见 {@link Role}
 * （1=owner 2=admin 3=member 4=readonly）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final SpaceMembershipReader spaceMembershipReader;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 多标识登录：输入含 @ 视为邮箱，否则按用户名查。两者都全表唯一且指向同一条 jj_user，
        // 最终归一到真实的 user.getUsername()，下游 JWT/refresh/改密码全部以 username 为准。
        User user = identifier != null && identifier.contains("@")
                ? userMapper.selectByEmail(identifier.trim())
                : userMapper.selectByUsername(identifier);
        if (user == null) {
            log.warn("User details load failed, user not found: {}", identifier);
            throw new UsernameNotFoundException("用户不存在: " + identifier);
        }

        List<GrantedAuthority> authorities = resolveAuthorities(user.getId());
        log.debug("User details loaded: identifier={}, username={}, userId={}, authorities={}",
                identifier, user.getUsername(), user.getId(), authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    /**
     * 为 JWT 生成一个代表性的 Spring Security 权限声明。
     *
     * <p><b>现状说明（步 2）：</b>本仓库没有任何 {@code @PreAuthorize}/{@code hasRole}
     * 注解消费这些权限，真正的鉴权全部走 {@link CurrentUserService#getCurrentRole()}
     * 按请求实时解析当前空间，因此这里的权限<b>只是元数据</b>，不是授权依据。
     * 早期实现把它建立在写死的 {@code DEFAULT_SPACE_ID=1} 上，多空间下已无意义，
     * 改为取「space_id 最小的在籍空间」作为代表档，纯粹为了稳定可重现。
     *
     * <p>不要据这些权限做安全判断，也不要在新增鉴权时复用它们——
     * 它们无法表达「在哪个家的什么身份」。若将来要做注解层鉴权，应改为
     * 自定义 {@code PermissionEvaluator} 内部调 {@link Role}，而非扩展权限名。
     */
    private List<GrantedAuthority> resolveAuthorities(Long userId) {
        List<SpaceMember> active = spaceMembershipReader.findActiveMemberships(userId);
        if (active.isEmpty()) {
            return Collections.emptyList();
        }

        SpaceMember representative = active.stream()
                .filter(m -> m.getSpaceId() != null)
                .min((a, b) -> Long.compare(a.getSpaceId(), b.getSpaceId()))
                .orElse(active.get(0));

        if (representative.getRole() == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new SimpleGrantedAuthority(roleToAuthority(representative.getRole())));
    }

    /**
     * 角色档位 → Spring Security 权限名（仅元数据，见 {@link #resolveAuthorities} 说明）。
     * <p>owner 单列 ROLE_OWNER，便于将来在注解层区分「仅所有者」的操作。
     */
    static String roleToAuthority(Integer role) {
        if (Role.isOwner(role)) {
            return "ROLE_OWNER";
        }
        if (Role.isAdminTier(role)) {
            return "ROLE_ADMIN";
        }
        if (Role.isMemberTier(role)) {
            return "ROLE_EDIT";
        }
        return "ROLE_READ";
    }
}
