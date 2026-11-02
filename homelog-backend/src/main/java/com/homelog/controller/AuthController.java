package com.homelog.controller;

import com.homelog.dto.LoginRequest;
import com.homelog.dto.PasswordUpdateRequest;
import com.homelog.dto.RegisterRequest;
import com.homelog.entity.User;
import com.homelog.entity.Space;
import com.homelog.entity.SpaceMember;
import com.homelog.mapper.UserMapper;
import com.homelog.mapper.SpaceMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;

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
    private final UserMapper userMapper;
    private final SpaceMapper spaceMapper;
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
        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册（自动创建默认空间，用户为管理员）
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User register: {}", request.getUsername());

        // 检查用户名是否存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().build();
        }

        // 检查邮箱是否存在
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            return ResponseEntity.badRequest().build();
        }

        // 创建用户
        User user = new User()
                .setUsername(request.getUsername())
                .setPassword(passwordEncoder.encode(request.getPassword()))
                .setEmail(request.getEmail())
                .setNickName(request.getNickName())
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        // 创建默认空间
        Space space = new Space()
                .setName("我的家庭")
                .setOwnerId(user.getId())
                .setCreatedAt(LocalDateTime.now());
        spaceMapper.insert(space);

        // 创建空间成员（管理员）
        SpaceMember member = new SpaceMember()
                .setSpaceId(space.getId())
                .setUserId(user.getId())
                .setRole(1) // 管理员
                .setReceiveMail(1)
                .setCreatedAt(LocalDateTime.now());
        spaceMemberMapper.insert(member);

        // 登录生成 Token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getUsername());

        TokenResponse response = new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpire());
        return ResponseEntity.ok(response);
    }

    /**
     * 刷新 Access Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String refreshToken = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, Collections.emptyList()
        );

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        TokenResponse response = new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpire());
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
        SecurityContextHolder.clearContext();
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
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().build();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return ResponseEntity.ok().build();
    }
}