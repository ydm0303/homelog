package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息中心表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_message")
public class Message {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("space_id")
    private Long spaceId;

    @TableField("user_id")
    private Long userId;

    @TableField("record_id")
    private Long recordId;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Integer isRead;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}