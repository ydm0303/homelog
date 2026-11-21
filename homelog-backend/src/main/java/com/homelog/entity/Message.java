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

    /** 类型：record=记录相关 remind=提醒 family_invitation=入家邀请 family_result=邀请回执 */
    @TableField("type")
    private String type;

    @TableField("record_id")
    private Long recordId;

    /** 业务引用ID：family_invitation/family_result 类消息指向 jj_invitation.id */
    @TableField("ref_id")
    private Long refId;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Integer isRead;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}