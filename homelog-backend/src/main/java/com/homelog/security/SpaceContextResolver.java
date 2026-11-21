package com.homelog.security;

import com.homelog.entity.SpaceMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 当前请求的空间上下文解析（设计文档 v1.2 §6.5 / §13 步 2）。
 *
 * <p>职责单一：把「客户端声明的当前空间」翻译成一个<b>已校验</b>的成员关系。
 * 校验顺序不可简化——文档 §15 S1 记录的越权正是「相信客户端传来的 spaceId」造成的。
 *
 * <p>解析规则：
 * <ol>
 *   <li>读 {@code X-Space-Id} 请求头；缺失则按兼容策略回落（见下）；</li>
 *   <li>值非法（非正整数）→ 400 语义的拒绝，不放行；</li>
 *   <li>查 {@code jj_space_member(space_id, user_id)}，要求 {@code status=active}
 *       ——<b>非成员一律 403</b>，这是多空间开放后的核心闸门；</li>
 *   <li>命中即返回，调用方据此得出当前空间与角色。</li>
 * </ol>
 *
 * <p>兼容策略（老客户端不带 header）：
 * 用户只有唯一空间时直接用它；有多个空间时取 {@code space_id} 最小的一个并告警。
 * 取最小值是为了<b>确定性</b>——同一用户同一请求树内不会忽而 A 家忽而 B 家。
 * 步 5 前端切换器上线后，这条回落路径就不再被命中。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpaceContextResolver {

    /** 当前空间标识请求头 */
    public static final String SPACE_HEADER = "X-Space-Id";

    /** 在籍状态 */
    static final int STATUS_ACTIVE = 1;

    private final SpaceMembershipReader membershipReader;

    /**
     * 解析当前请求应作用于哪个空间，并校验当前用户是该空间的在籍成员。
     *
     * @param userId 当前登录用户 ID，可为 null（未认证）
     * @return 已校验的成员关系；用户无任何在籍空间时返回 null
     * @throws AccessDeniedException 客户端指定的空间不属于该用户
     */
    public SpaceMember resolve(Long userId) {
        if (userId == null) {
            return null;
        }

        List<SpaceMember> active = membershipReader.findActiveMemberships(userId);
        if (active.isEmpty()) {
            return null;
        }

        Long requested = readRequestedSpaceId();
        if (requested == null) {
            return fallback(active);
        }

        for (SpaceMember member : active) {
            if (requested.equals(member.getSpaceId())) {
                return member;
            }
        }

        // 指定了不属于自己的空间：拒绝，绝不静默切换到别的空间继续执行
        log.warn("Space access denied, not an active member: userId={}, requestedSpaceId={}, ownedSpaceIds={}",
                userId, requested, spaceIdsOf(active));
        throw new AccessDeniedException("无权访问该家庭空间");
    }

    /**
     * 无 header 时的回落空间。
     */
    private SpaceMember fallback(List<SpaceMember> active) {
        if (active.size() == 1) {
            return active.get(0);
        }
        return active.stream()
                .filter(m -> m.getSpaceId() != null)
                .min((a, b) -> Long.compare(a.getSpaceId(), b.getSpaceId()))
                .map(m -> {
                    log.warn("Multiple spaces but no {} header, falling back to spaceId={}; "
                                    + "client should send the header explicitly", SPACE_HEADER, m.getSpaceId());
                    return m;
                })
                .orElse(active.get(0));
    }

    /**
     * 读取并校验 {@code X-Space-Id}。返回 null 表示「未提供或格式不可用」。
     */
    private Long readRequestedSpaceId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String raw = request.getHeader(SPACE_HEADER);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value <= 0) {
                log.warn("Invalid {} header value (non-positive): {}", SPACE_HEADER, raw);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            log.warn("Invalid {} header value (not a number): {}", SPACE_HEADER, raw);
            return null;
        }
    }

    private static HttpServletRequest currentRequest() {
        Object attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    private static List<Long> spaceIdsOf(List<SpaceMember> members) {
        return members.stream().map(SpaceMember::getSpaceId).collect(java.util.stream.Collectors.toList());
    }
}
