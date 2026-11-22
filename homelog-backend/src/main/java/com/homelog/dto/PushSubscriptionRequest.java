package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Web Push 订阅注册/更新请求
 * <p>对应前端 PushSubscription 的 endpoint + keys(p256dh, auth)。
 */
@Data
public class PushSubscriptionRequest {

    @NotBlank(message = "endpoint 不能为空")
    private String endpoint;

    @NotBlank(message = "p256dh 不能为空")
    private String p256dh;

    @NotBlank(message = "auth 不能为空")
    private String auth;

    /** 来源平台：ios/android/desktop，可选 */
    private String platform;
}
