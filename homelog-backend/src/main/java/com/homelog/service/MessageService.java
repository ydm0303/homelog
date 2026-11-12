package com.homelog.service;

import com.homelog.dto.MessageQueryRequest;
import com.homelog.entity.Message;
import com.homelog.entity.Record;
import com.homelog.entity.Module;
import com.homelog.mapper.MessageMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息中心服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageMapper messageMapper;
    private final RecordMapper recordMapper;
    private final ModuleMapper moduleMapper;

    /**
     * 获取当前空间ID（MVP 单空间）
     */
    private Long getCurrentSpaceId() {
        return 1L;
    }

    /**
     * 获取当前用户ID（简化：从 SecurityContext 获取，实际应查用户表）
     */
    private Long getCurrentUserId() {
        // 实际项目中应从 JWT 或 SecurityContext 获取用户名，再查用户表得到 ID
        // 这里简化返回 1L
        return 1L;
    }

    /**
     * 消息列表查询（分页、筛选）
     */
    public List<MessageVO> list(MessageQueryRequest request) {
        Long spaceId = getCurrentSpaceId();
        Long userId = getCurrentUserId();

        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getSize();

        // 查询消息
        List<Message> messages = messageMapper.selectByUserIdWithFilters(
                userId, request.getIsRead(), request.getKeyword(),
                request.getStartDate(), request.getEndDate(),
                offset, request.getSize()
        );

        // 转换为 VO，补充关联记录信息
        return messages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取消息总数（用于分页）
     */
    public long count(MessageQueryRequest request) {
        Long userId = getCurrentUserId();
        return messageMapper.countByUserIdWithFilters(
                userId, request.getIsRead(), request.getKeyword(),
                request.getStartDate(), request.getEndDate()
        );
    }

    /**
     * 标记单条已读
     */
    @Transactional
    public void markRead(Long messageId) {
        Long userId = getCurrentUserId();
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            log.warn("Mark message read failed, message not found: messageId={}", messageId);
            return;
        }
        if (!message.getUserId().equals(userId)) {
            log.warn("Mark message read denied, not the owner: messageId={}, userId={}, ownerId={}",
                    messageId, userId, message.getUserId());
            return;
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
        log.info("消息已标记为已读：消息ID={}，用户ID={}", messageId, userId);
    }

    /**
     * 标记全部已读
     */
    @Transactional
    public void markAllRead() {
        Long userId = getCurrentUserId();
        messageMapper.updateAllReadByUserId(userId);
        log.info("用户的全部消息已标记为已读：用户ID={}", userId);
    }

    /**
     * 获取未读数量
     */
    public int getUnreadCount() {
        Long userId = getCurrentUserId();
        return messageMapper.countUnreadByUserId(userId);
    }

    /**
     * 创建消息（供定时任务/其他服务调用）
     */
    @Transactional
    public void createMessage(Long userId, Long recordId, String content, String type) {
        Message message = new Message()
                .setSpaceId(getCurrentSpaceId())
                .setUserId(userId)
                .setRecordId(recordId)
                .setContent(content)
                .setIsRead(0)
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        log.info("消息创建成功：消息ID={}，用户ID={}，记录ID={}，类型={}",
                message.getId(), userId, recordId, type);
    }

    /**
     * 批量创建消息（给空间内所有符合条件的成员）
     */
    @Transactional
    public void createMessagesForSpace(List<Long> userIds, Long recordId, String content, String type) {
        log.info("批量创建消息：记录ID={}，类型={}，接收人数={}", recordId, type, userIds.size());
        for (Long userId : userIds) {
            createMessage(userId, recordId, content, type);
        }
    }

    /**
     * 转换为 VO，补充模块/记录信息用于前端跳转
     */
    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO()
                .setId(message.getId())
                .setSpaceId(message.getSpaceId())
                .setUserId(message.getUserId())
                .setRecordId(message.getRecordId())
                .setContent(message.getContent())
                .setIsRead(message.getIsRead())
                .setCreatedAt(message.getCreatedAt())
                .setType(inferType(message.getContent()));

        // 补充关联记录信息
        if (message.getRecordId() != null) {
            Record record = recordMapper.selectById(message.getRecordId());
            if (record != null) {
                vo.setRecordTitle(record.getTitle());
                Module module = moduleMapper.selectById(record.getModuleId());
                if (module != null) {
                    vo.setModuleKey(module.getModuleKey());
                    vo.setModuleName(module.getName());
                }
            }
        }

        return vo;
    }

    /**
     * 根据内容推断消息类型
     */
    private String inferType(String content) {
        if (content == null) return "system";
        if (content.contains("到期") || content.contains("截止") || content.contains("提醒")) {
            return "remind";
        }
        if (content.contains("@")) {
            return "mention";
        }
        return "system";
    }
}