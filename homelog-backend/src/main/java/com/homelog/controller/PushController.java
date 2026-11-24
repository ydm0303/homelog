package com.homelog.controller;

import com.homelog.config.PushProperties;
import com.homelog.dto.PushSubscriptionRequest;
import com.homelog.security.CurrentUserService;
import com.homelog.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Web Push 订阅控制器。
 *
 * <p>全部走默认 JWT 鉴权（{@code anyRequest().authenticated()}），
 * userId 取自当前登录态，不接受客户端传入。
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PushController {

    private final PushService pushService;
    private final CurrentUserService currentUserService;
    private final PushProperties pushProperties;

    /**
     * 注册/更新当前用户的浏览器订阅（按 endpoint upsert）
     * PUT /api/push/subscription
     */
    @PutMapping("/subscription")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        log.info("注册 Web Push 订阅：userId={}, platform={}", userId, request.getPlatform());
        pushService.saveOrUpdate(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除当前用户的某个订阅
     * DELETE /api/push/subscription?endpoint=...
     */
    @DeleteMapping("/subscription")
    public ResponseEntity<Void> unsubscribe(@RequestParam("endpoint") String endpoint) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        log.info("注销 Web Push 订阅：userId={}", userId);
        pushService.removeByEndpoint(userId, endpoint);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取 VAPID 公钥，供前端 pushManager.subscribe 使用。
     * GET /api/push/vapid-public-key
     *
     * <p>需登录态（保持最小暴露，未加白名单）。
     */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, Object>> vapidPublicKey() {
        Map<String, Object> response = new HashMap<>();
        response.put("publicKey", pushProperties.getPublicKey());
        return ResponseEntity.ok(response);
    }
}
