package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息中心视图对象
 */
@Data
@Accessors(chain = true)
public class MessageVO {

    private Long id;
    private Long spaceId;
    private Long userId;
    private Long recordId;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;

    // 关联记录信息（用于跳转）
    private String moduleKey;
    private String moduleName;
    private String recordTitle;

    // 消息类型：remind(到期提醒)、system(系统通知)、mention(@提及)
    private String type;
}