package com.homelog.controller;

import com.homelog.dto.MessageQueryRequest;
import com.homelog.service.MessageService;
import com.homelog.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心控制器
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MessageController {

    private final MessageService messageService;

    /**
     * 消息列表查询
     * GET /api/messages?page=1&size=20&isRead=0&keyword=&startDate=&endDate=
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@Valid MessageQueryRequest request) {
        log.info("收到消息列表查询请求：{}", request);

        List<MessageVO> messages = messageService.list(request);
        long total = messageService.count(request);

        Map<String, Object> response = new HashMap<>();
        response.put("data", messages);
        response.put("total", total);
        response.put("page", request.getPage());
        response.put("size", request.getSize());

        log.info("消息列表查询完成：总数={}，返回数={}", total, messages.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取未读消息数量
     * GET /api/messages/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> unreadCount() {
        int count = messageService.getUnreadCount();
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        log.debug("Unread message count: {}", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 标记单条消息已读
     * PUT /api/messages/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        log.info("标记消息为已读：{}", id);
        messageService.markRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 标记全部已读
     * PUT /api/messages/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        log.info("标记全部消息为已读");
        messageService.markAllRead();
        return ResponseEntity.ok().build();
    }
}