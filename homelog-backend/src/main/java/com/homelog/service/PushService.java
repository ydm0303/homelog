package com.homelog.service;

import com.homelog.config.PushProperties;
import com.homelog.dto.PushSubscriptionRequest;
import com.homelog.entity.PushSubscription;
import com.homelog.mapper.PushSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web Push 推送服务（VAPID）。
 *
 * <p>依赖 {@code nl.martijndwars:web-push:3.1.1}（Apache HttpClient 线，兼容 Java 8）。
 *
 * <p>可靠性约定：
 * <ul>
 *   <li>单个订阅发送失败绝不影响其它订阅与调用方；</li>
 *   <li>订阅失效（推送网关返回 404/410 Gone）时删除该订阅行，其余异常仅告警不删；</li>
 *   <li>{@link #pushToUser} 整体不抛异常——提醒任务循环可对每个用户安全调用。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    /** 推送网关判定订阅已失效的 HTTP 状态码 */
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_GONE = 410;

    private final PushSubscriptionMapper pushSubscriptionMapper;
    private final PushProperties pushProperties;

    /** web-push 客户端，懒加载并缓存；密钥缺失/非法时保持 null，推送静默禁用 */
    private volatile nl.martijndwars.webpush.PushService client;
    private volatile boolean clientInitFailed;

    // ========================================================================
    // 订阅管理
    // ========================================================================

    /**
     * 按 (userId, endpoint) upsert 订阅。
     */
    @Transactional
    public void saveOrUpdate(Long userId, PushSubscriptionRequest request) {
        if (userId == null || request == null || !StringUtils.hasText(request.getEndpoint())) {
            log.warn("保存 Web Push 订阅被忽略：参数不完整 userId={}", userId);
            return;
        }
        PushSubscription existing =
                pushSubscriptionMapper.selectByUserIdAndEndpoint(userId, request.getEndpoint());
        if (existing != null) {
            existing.setP256dh(request.getP256dh())
                    .setAuth(request.getAuth())
                    .setPlatform(request.getPlatform());
            pushSubscriptionMapper.updateById(existing);
            log.info("Web Push 订阅已更新：userId={}, subscriptionId={}", userId, existing.getId());
            return;
        }
        PushSubscription entity = new PushSubscription()
                .setUserId(userId)
                .setEndpoint(request.getEndpoint())
                .setP256dh(request.getP256dh())
                .setAuth(request.getAuth())
                .setPlatform(request.getPlatform());
        pushSubscriptionMapper.insert(entity);
        log.info("Web Push 订阅已新增：userId={}, subscriptionId={}", userId, entity.getId());
    }

    /**
     * 删除当前用户的某个订阅（按 endpoint）。
     */
    @Transactional
    public void removeByEndpoint(Long userId, String endpoint) {
        if (userId == null || !StringUtils.hasText(endpoint)) {
            return;
        }
        int n = pushSubscriptionMapper.deleteByUserIdAndEndpoint(userId, endpoint);
        log.info("Web Push 订阅删除：userId={}, endpoint={}, 影响行数={}", userId, endpoint, n);
    }

    /**
     * 删除某用户的全部订阅（如退出空间/注销时清理）。
     */
    @Transactional
    public void removeByUser(Long userId) {
        if (userId == null) {
            return;
        }
        int n = pushSubscriptionMapper.deleteByUserId(userId);
        log.info("Web Push 订阅全量删除：userId={}, 影响行数={}", userId, n);
    }

    // ========================================================================
    // 推送
    // ========================================================================

    /**
     * 向某用户的全部订阅推送一条通知。
     *
     * <p>载荷为 JSON 字符串 {@code {"title":...,"body":...,"url":...}}。
     * <p>本方法不抛异常；调用方（提醒任务）即便不额外 try-catch 也不会被推送失败中断，
     * 但调用方仍应自行按用户兜底，以免 DB 查询等其它异常冒泡。
     *
     * @param userId 目标用户
     * @param title  通知标题
     * @param body   通知正文
     * @param url    点击跳转链接
     */
    public void pushToUser(Long userId, String title, String body, String url) {
        if (userId == null) {
            return;
        }
        nl.martijndwars.webpush.PushService pushClient = obtainClient();
        if (pushClient == null) {
            // VAPID 未配置或密钥非法：静默禁用，不影响邮件等主流程
            return;
        }
        List<PushSubscription> subscriptions = pushSubscriptionMapper.selectByUserId(userId);
        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }

        byte[] payload = buildPayload(title, body, url).getBytes(StandardCharsets.UTF_8);
        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = buildNotification(sub, payload);

                // web-push 3.1.1 send() 返回 org.apache.http.HttpResponse，链式取状态码，免显式 import
                int status = pushClient.send(notification).getStatusLine().getStatusCode();

                if (status == HTTP_NOT_FOUND || status == HTTP_GONE) {
                    pushSubscriptionMapper.deleteByUserIdAndEndpoint(userId, sub.getEndpoint());
                    log.info("Web Push 订阅已失效并移除：userId={}, subscriptionId={}, status={}",
                            userId, sub.getId(), status);
                } else if (status < 200 || status >= 300) {
                    log.warn("Web Push 网关返回非 2xx：userId={}, subscriptionId={}, status={}",
                            userId, sub.getId(), status);
                } else {
                    log.debug("Web Push 发送成功：userId={}, subscriptionId={}", userId, sub.getId());
                }
            } catch (Exception e) {
                // 单订阅失败仅告警，继续后续订阅，绝不向上抛
                log.warn("Web Push 单订阅发送失败（忽略，不影响其它订阅/主流程）：userId={}, subscriptionId={}, err={}",
                        userId, sub.getId(), e.toString());
            }
        }
    }

    // ========================================================================
    // 内部
    // ========================================================================

    /**
     * 把库表订阅行装配成 web-push 库的 {@link Notification}。
     *
     * <p><b>本方法是全项目唯一装配 web-push 对象（Notification）的地方。</b>
     * 3.1.1 提供 {@code Notification(endpoint, p256dh, auth, payload)} 四参构造器，
     * 内部用 {@code Utils.base64Decode}（自动识别 base64url）解析 ECDH 公钥与 auth，
     * 故无需手工构造 {@code Subscription}/{@code Keys}（后者在 3.x 是非静态内部类）。
     * 除此之外不要在本类之外新增对库对象模型的引用。
     */
    private Notification buildNotification(PushSubscription sub, byte[] payload) throws Exception {
        return new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
    }

    /**
     * 懒加载 VAPID 客户端。密钥缺失或非法时只尝试初始化一次，之后保持禁用并返回 null。
     *
     * <p>用 {@code catch (Exception)}：web-push 构造器在密钥非法时会抛 checked 异常，
     * 但具体声明随版本而异；统一兜底避免拖垮应用启动与调用方。
     */
    private nl.martijndwars.webpush.PushService obtainClient() {
        if (client != null || clientInitFailed) {
            return client;
        }
        synchronized (this) {
            if (client != null || clientInitFailed) {
                return client;
            }
            if (!StringUtils.hasText(pushProperties.getPublicKey())
                    || !StringUtils.hasText(pushProperties.getPrivateKey())) {
                clientInitFailed = true;
                log.warn("未配置 vapid.public-key/private-key，Web Push 推送禁用");
                return null;
            }
            String subject = StringUtils.hasText(pushProperties.getSubject())
                    ? pushProperties.getSubject() : "mailto:admin@example.com";
            try {
                client = new nl.martijndwars.webpush.PushService(
                        pushProperties.getPublicKey(), pushProperties.getPrivateKey(), subject);
                log.info("Web Push VAPID 客户端初始化完成");
            } catch (Exception e) {
                clientInitFailed = true;
                log.error("Web Push VAPID 客户端初始化失败（密钥非法？），推送禁用：{}", e.toString());
            }
            return client;
        }
    }

    /**
     * 手工拼装载荷 JSON：{@code {"title":...,"body":...,"url":...}}。
     * 不引入额外序列化依赖，做最小转义即可。
     */
    private String buildPayload(String title, String body, String url) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{')
                .append("\"title\":\"").append(escape(title)).append("\",")
                .append("\"body\":\"").append(escape(body)).append("\",")
                .append("\"url\":\"").append(escape(url)).append('"')
                .append('}');
        return sb.toString();
    }

    private String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
