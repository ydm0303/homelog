package com.homelog.security;

import com.homelog.entity.User;
import com.homelog.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 多标识登录（用户名或邮箱）核心解析逻辑单元测试。
 *
 * <p>关注点：无论用 username 还是 email 进来，{@code loadUserByUsername} 都必须归一到
 * 同一条 jj_user 的真实 username，下游 JWT/refresh/改密码才能保持一致身份。
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SpaceMembershipReader spaceMembershipReader;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User sampleUser() {
        return new User().setId(42L).setUsername("dongming").setEmail("dm@example.com").setPassword("hash");
    }

    @Test
    // 用户名登录：走 selectByUsername，返回真实 username，不触碰邮箱查询
    void loadUserByUsername_withUsername_resolvesViaSelectByUsernameAndReturnsRealUsername() {
        when(userMapper.selectByUsername("dongming")).thenReturn(sampleUser());
        when(spaceMembershipReader.findActiveMemberships(anyLong())).thenReturn(Collections.emptyList());

        UserDetails details = userDetailsService.loadUserByUsername("dongming");

        assertThat(details.getUsername()).isEqualTo("dongming");
        verify(userMapper, never()).selectByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    // 邮箱登录：归一到真实 username 而非邮箱，确保 JWT subject 不含邮箱
    void loadUserByUsername_withEmail_normalizesToRealUsernameNotEmail() {
        when(userMapper.selectByEmail("dm@example.com")).thenReturn(sampleUser());
        when(spaceMembershipReader.findActiveMemberships(anyLong())).thenReturn(Collections.emptyList());

        UserDetails details = userDetailsService.loadUserByUsername("dm@example.com");

        // 关键断言：token subject 用的是真实 username，不会把邮箱写进 JWT
        assertThat(details.getUsername()).isEqualTo("dongming");
        verify(userMapper, never()).selectByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    // 两种入口（用户名/邮箱）解析到同一条账号，身份保持一致
    void loadUserByUsername_bothIdentifiers_resolveToSameAccount() {
        when(userMapper.selectByUsername("dongming")).thenReturn(sampleUser());
        when(userMapper.selectByEmail("dm@example.com")).thenReturn(sampleUser());
        when(spaceMembershipReader.findActiveMemberships(anyLong())).thenReturn(Collections.emptyList());

        UserDetails byName = userDetailsService.loadUserByUsername("dongming");
        UserDetails byEmail = userDetailsService.loadUserByUsername("dm@example.com");

        assertThat(byName.getUsername()).isEqualTo(byEmail.getUsername());
    }

    @Test
    // 邮箱不存在时抛 UsernameNotFoundException
    void loadUserByUsername_emailNotFound_throwsUsernameNotFound() {
        when(userMapper.selectByEmail("nope@example.com")).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nope@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
