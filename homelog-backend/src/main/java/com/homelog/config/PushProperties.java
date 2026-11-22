package com.homelog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Web Push（VAPID）配置。
 *
 * <p>对应 yml：
 * <pre>
 * vapid:
 *   public-key:  &lt;base64url P-256 公钥&gt;
 *   private-key: &lt;base64url 私钥&gt;
 *   subject:     mailto:admin@example.com
 * </pre>
 *
 * <p>subject 须为 {@code mailto:} 邮箱或 {@code https:} 域名形式（VAPID 规范）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "vapid")
public class PushProperties {

    /** VAPID 公钥（base64url），下发给前端用于 subscribe */
    private String publicKey;

    /** VAPID 私钥（base64url），仅服务端持有，用于签发 JWT */
    private String privateKey;

    /** VAPID subject，形如 mailto:you@example.com */
    private String subject;
}
