package com.homelog.controller;

import com.homelog.dto.LoginRequest;
import com.homelog.dto.PasswordUpdateRequest;
import com.homelog.dto.RegisterRequest;
import com.homelog.entity.User;
import com.homelog.mapper.UserMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.JwtTokenProvider;
import com.homelog.security.Role;
import com.homelog.service.SpaceCreationService;
import com.homelog.vo.TokenResponse;
import com.homelog.vo.UserProfileVO;
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
import org.springframework.transaction.annotation.Transactional;
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

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final SpaceCreationService spaceCreationService;

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录尝试：{}", request.getUsername());

        // 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        TokenResponse response = new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpire());
        log.info("用户登录成功：{}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册（加入默认空间，用户为管理员）
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册：{}", request.getUsername());
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
        log.info("用户创建成功：用户ID={}，用户名={}", user.getId(), user.getUsername());

        // 注册即建自己的私有家庭空间，并按模板实例化内置模块与字段（设计文档 v1.2 §6.1 / 步 3）。
        // 用户在自己的家是 OWNER(1) —— 这是 owner 唯一的产生途径，不接受邀请或指派。
        //
        // 本方法已标 @Transactional：建号 / 建家 / owner 成员关系 / 模块字段实例化同事务，
        // 任一失败整体回滚，不会留下「已注册但无家」或「有家但菜单全空」的半态。
        // （旧实现把新用户塞进 space_id=1 当管理员，那是在单空间硬编码下的权宜之计。）
        log.info("创建私有家庭空间");
        Long spaceId = spaceCreationService.createFamilySpaceWithPresets(user.getId(), null);
        log.info("私有家庭空间就绪：spaceId={}，用户ID={}，角色={}", spaceId, user.getId(), Role.label(Role.OWNER));
        log.info("注册用户结束");

        // 登录生成 Token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        TokenResponse response = new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpire());
        log.info("用户注册成功：用户ID={}，用户名={}", user.getId(), user.getUsername());
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
        log.info("用户令牌刷新成功：{}", username);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户信息（含空间角色，前端据此控制菜单与操作按钮）
     * GET /api/auth/detail
     */
    @GetMapping("/detail")
    public ResponseEntity<UserProfileVO> detail() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            log.warn("Get current user detail failed, user not found");
            return ResponseEntity.status(401).build();
        }

        UserProfileVO profile = UserProfileVO.from(user, currentUserService.getCurrentMembership());
        return ResponseEntity.ok(profile);
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
        log.info("用户退出登录：{}", username);
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
        log.info("用户密码修改请求：{}", username);

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

        log.info("用户密码修改成功：{}", username);
        return ResponseEntity.ok().build();
    }
}