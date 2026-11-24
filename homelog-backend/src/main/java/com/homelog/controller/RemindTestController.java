package com.homelog.controller;

import com.homelog.entity.User;
import com.homelog.security.CurrentUserService;
import com.homelog.service.MailService;
import com.homelog.task.RemindTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 邮件提醒测试接口 —— 仅用于本地/联调验证 SMTP 配置与到期扫描逻辑，不等定时任务早上 8 点触发。
 *
 * <p>受 Spring Security 保护（需登录带 JWT），不放行匿名访问，避免被外部滥用群发。
 */
@RestController
@RequestMapping("/api/remind-test")
@RequiredArgsConstructor
@Slf4j
public class RemindTestController {

    private final RemindTask remindTask;
    private final MailService mailService;
    private final CurrentUserService currentUserService;

    /**
     * 立即执行一次"全量到期扫描 + 发邮件"，同步返回统计报告。
     * POST /api/remind-test/scan
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan() {
        log.info("收到手动到期扫描测试请求：operator={}", currentUserService.getCurrentUsername());
        Map<String, Object> report = remindTask.triggerScanForTest();
        report.put("triggeredAt", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return ResponseEntity.ok(report);
    }

    /**
     * 给指定邮箱（默认当前登录用户邮箱）发一封测试邮件，用于单独验证 SMTP 连通性/认证。
     * POST /api/remind-test/send?to=someone@example.com
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(@RequestParam(required = false) String to) {
        User me = currentUserService.getCurrentUser();
        String target = (to != null && !to.trim().isEmpty())
                ? to.trim()
                : (me != null ? me.getEmail() : null);

        Map<String, Object> result = new LinkedHashMap<>();
        if (target == null || target.isEmpty()) {
            result.put("success", false);
            result.put("error", "未指定收件邮箱，且当前用户无邮箱");
            return ResponseEntity.badRequest().body(result);
        }

        String subject = "【家记·邮件测试】SMTP 配置验证";
        String body = "<p>这是一封来自家记的测试邮件。</p>"
                + "<p>如果你收到了，说明 spring.mail 的 host/port/username/password 配置正确。</p>"
                + "<p style='color:#999'>发送时间：" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>";

        try {
            // 用同步版本：失败时把真实异常抛出，便于定位认证/网络根因
            mailService.sendEmailSync(target, subject, body);
            result.put("success", true);
            result.put("to", target);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试邮件发送失败：to={}", target, e);
            result.put("success", false);
            result.put("to", target);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
