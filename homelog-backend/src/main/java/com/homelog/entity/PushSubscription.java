package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 浏览器 Web Push 订阅表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_push_subscription")
public class PushSubscription {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 推送服务端签发的订阅 endpoint（全局唯一标识一个订阅） */
    @TableField("endpoint")
    private String endpoint;

    /** 订阅端 ECDH 公钥 keys.p256dh（base64url） */
    @TableField("p256dh")
    private String p256dh;

    /** 订阅端鉴权密钥 keys.auth（base64url） */
    @TableField("auth")
    private String auth;

    /** 来源平台：ios/android/desktop，仅展示与排查用 */
    @TableField("platform")
    private String platform;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
