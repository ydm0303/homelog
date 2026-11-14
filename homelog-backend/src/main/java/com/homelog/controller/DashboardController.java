package com.homelog.controller;

import com.homelog.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页聚合接口：一次返回模块计数、今日待办、即将到期、最近记录，
 * 避免前端按模块 N+1 请求并自行估算总数。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final RecordService recordService;

    /**
     * 首页聚合数据
     * GET /api/dashboard/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        Map<String, Object> summary = recordService.getDashboardSummary();
        log.info("首页聚合数据查询完成：模块数={}", ((java.util.List<?>) summary.get("modules")).size());
        return ResponseEntity.ok(summary);
    }
}
