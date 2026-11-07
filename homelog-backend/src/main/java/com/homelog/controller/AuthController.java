package com.homelog.controller;

import com.homelog.dto.LoginRequest;
import com.homelog.dto.PasswordUpdateRequest;
import com.homelog.dto.RegisterRequest;
import com.homelog.entity.User;
import com.homelog.entity.SpaceMember;
import com.homelog.mapper.UserMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.security.JwtTokenProvider;
import com.homelog.vo.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * 认证控制器
 * 规范：只使用 GET/POST，参数通过 query 或 body 传递
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {

    /** MVP 单空间：默认空间 ID，与 UserDetailsServiceImpl / MetaController / RecordService 保持一致 */
    private static final long DEFAULT_SPACE_ID = 1L;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final SpaceMemberMapper spaceMemberMapper;

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());

        // 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        TokenResponse response = new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpire());
        log.info("User login success: {}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册（加入默认空间，用户为管理员）
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User register: {}", request.getUsername());
        log.info("注册用户开始");

        // 检查用户名是否存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            log.warn("Register failed, username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("该用户名已被注册");
        }

        // 检查邮箱是否存在
        log.info("检查邮箱是否存在");
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            log.warn("Register failed, email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("该邮箱已被注册");
        }

        // 创建用户
        log.info("创建用户");
        User user = new User()
                .setUsername(request.getUsername())
                .setPassword(passwordEncoder.encode(request.getPassword()))
                .setEmail(request.getEmail())
                .setNickName(request.getNickName())
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        log.info("User created: userId={}, username={}", user.getId(), user.getUsername());

        // 加入默认空间（MVP 单空间）：不再为每个新用户新建空间。
        // 内置模块、记录等都挂在默认空间下，若注册时另建空间，会导致
        // 用户所属空间(如 id=2)与模块/数据所在空间(id=1)不一致，菜单与数据全部落空。
        log.info("加入默认空间");
        SpaceMember member = new SpaceMember()
                .setSpaceId(DEFAULT_SPACE_ID)
                .setUserId(user.getId())
                .setRole(1) // 管理员
                .setReceiveMail(1)
                .setCreatedAt(LocalDateTime.now());
        spaceMemberMapper.insert(member);
        log.info("Space member created: spaceId={}, userId={}, role=ADMIN", DEFAULT_SPACE_ID, user.getId());
        log.info("注册用户结束");

        // 登录生成 Token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        TokenResponse response = new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpire());
        log.info("User register success: userId={}, username={}", user.getId(), user.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 刷新 Access Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Refresh failed, missing or malformed Authorization header");
            return ResponseEntity.badRequest().build();
        }

        String refreshToken = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("Refresh failed, token is invalid or not a refresh token");
            return ResponseEntity.status(401).build();
        }

        String username = jwtTokenProvider.getUsername(refreshToken);

        // 重新加载完整用户信息（含权限），避免用 String 作为 principal 导致 generateAccessToken 强转 UserDetails 失败
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.warn("Refresh failed, user not found: {}", username);
            return ResponseEntity.status(401).build();
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        TokenResponse response = new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpire());
        log.info("Token refreshed for user: {}", username);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/detail
     */
    @GetMapping("/detail")
    public ResponseEntity<User> detail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("Get current user detail failed, user not found: {}", username);
            return ResponseEntity.status(404).build();
        }

        // 清除密码
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    /**
     * 登出（客户端清除 Token 即可，服务端无状态）
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "anonymous";
        SecurityContextHolder.clearContext();
        log.info("User logout: {}", username);
        return ResponseEntity.ok().build();
    }

    /**
     * 修改密码
     * POST /api/auth/password/update
     */
    @PostMapping("/password/update")
    public ResponseEntity<Void> updatePassword(@RequestBody PasswordUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        log.info("Password update request for user: {}", username);

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("Password update failed, user not found: {}", username);
            return ResponseEntity.status(404).build();
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Password update failed, old password mismatch for user: {}", username);
            return ResponseEntity.badRequest().build();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("Password updated for user: {}", username);
        return ResponseEntity.ok().build();
    }
}