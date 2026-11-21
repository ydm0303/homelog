package com.homelog.service;

import com.homelog.dto.MessageQueryRequest;
import com.homelog.entity.Message;
import com.homelog.entity.Record;
import com.homelog.entity.Module;
import com.homelog.mapper.MessageMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.security.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    /**
     * 消息列表查询（分页、筛选）
     */
    public List<MessageVO> list(MessageQueryRequest request) {
        Long userId = currentUserService.getCurrentUserId();

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
        Long userId = currentUserService.getCurrentUserId();
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
        Long userId = currentUserService.getCurrentUserId();
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            log.warn("Mark message read failed, message not found: messageId={}", messageId);
            return;
        }
        if (userId == null || !userId.equals(message.getUserId())) {
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
        Long userId = currentUserService.getCurrentUserId();
        messageMapper.updateAllReadByUserId(userId);
        log.info("用户的全部消息已标记为已读：用户ID={}", userId);
    }

    /**
     * 删除单条消息（仅限本人）
     */
    @Transactional
    public void deleteMessage(Long messageId) {
        Long userId = currentUserService.getCurrentUserId();
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            log.warn("Delete message failed, message not found: messageId={}", messageId);
            throw new RuntimeException("消息不存在");
        }
        if (userId == null || !userId.equals(message.getUserId())) {
            log.warn("Delete message denied, not the owner: messageId={}, userId={}, ownerId={}",
                    messageId, userId, message.getUserId());
            throw new RuntimeException("无权删除该消息");
        }
        messageMapper.deleteById(messageId);
        log.info("消息删除成功：消息ID={}，用户ID={}", messageId, userId);
    }

    /**
     * 获取未读数量
     */
    public int getUnreadCount() {
        Long userId = currentUserService.getCurrentUserId();
        return messageMapper.countUnreadByUserId(userId);
    }

    /**
     * 创建消息（供定时任务/其他服务调用）
     *
     * <p>M1 修复：早先 {@code type} 形参只进了日志、未落库（V3 才加的 type 列），
     * 导致通知无法按类型筛选。现在真正写入。{@code spaceId} 取当前请求空间——
     * 调用方（提醒任务、业务事件）都在目标空间上下文内执行，语义正确。
     */
    @Transactional
    public void createMessage(Long userId, Long recordId, String content, String type) {
        insertMessage(currentUserService.getCurrentSpaceId(), userId, recordId, null, content, type);
    }

    /**
     * 创建一条带业务引用的通知（邀请类消息用）。
     *
     * <p>与 {@link #createMessage} 的区别：显式传入 {@code spaceId} 与 {@code refId}，
     * 不依赖当前请求上下文。邀请通知要在「被邀请人尚未加入的目标空间」语境下记录归属，
     * 且需通过 {@code refId} 指回 {@code jj_invitation.id} 供前端跳转接受页。
     *
     * @param spaceId  消息归属空间（邀请=目标空间）
     * @param userId   接收人
     * @param refId    业务引用 ID（邀请类=invitationId），可空
     * @param content  文案
     * @param type     类型常量，见 {@code MessageType}
     */
    @Transactional
    public void createNotification(Long spaceId, Long userId, Long refId, String content, String type) {
        insertMessage(spaceId, userId, null, refId, content, type);
    }

    private void insertMessage(Long spaceId, Long userId, Long recordId, Long refId, String content, String type) {
        Message message = new Message()
                .setSpaceId(spaceId)
                .setUserId(userId)
                .setRecordId(recordId)
                .setRefId(refId)
                .setType(type)
                .setContent(content)
                .setIsRead(0)
                .setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        log.info("消息创建成功：消息ID={}，用户ID={}，记录ID={}，引用ID={}，类型={}",
                message.getId(), userId, recordId, refId, type);
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
                // M1 修复：直接取落库的 type，不再靠猜文案关键字。
                // 旧实现用 inferType(content) 嗅探「到期/提醒/@」，任何文案改动或新增
                // 业务类型（如 family_invitation）都会让分类静默出错。
                // 老数据在 V3 迁移里已回填为默认值 'record'。
                .setType(message.getType() != null ? message.getType() : MessageType.RECORD)
                .setRefId(message.getRefId());

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
}