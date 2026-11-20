package com.homelog.security;

import com.homelog.entity.SpaceMember;
import com.homelog.mapper.SpaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成员关系读取器：取某用户的<b>在籍</b>（status=active）成员记录，并在单次请求内缓存。
 *
 * <p>为什么要缓存：一条请求里 {@code getCurrentSpaceId()} / {@code getCurrentRole()} /
 * {@code isAdmin()} 会被反复调用，早先每次都触发一次 {@code selectByUserId}。
 * 缓存挂在 request attribute 上，随请求生命周期结束自动失效——
 * 绝不能用单例字段缓存，那会把 A 用户的空间泄露给 B 用户。
 *
 * <p>缓存不可用（非 Web 线程，如定时任务）时退化为直接查库，行为正确只是少了缓存。
 */
@Component
@RequiredArgsConstructor
public class SpaceMembershipReader {

    /** 在籍 */
    public static final int STATUS_ACTIVE = 1;

    private static final String CACHE_KEY = "homelog.activeMemberships";

    private final SpaceMemberMapper spaceMemberMapper;

    /**
     * 该用户当前在籍的全部空间成员关系。
     */
    @SuppressWarnings("unchecked")
    public List<SpaceMember> findActiveMemberships(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        HttpServletRequest request = currentRequest();
        Object cached = request != null ? request.getAttribute(CACHE_KEY) : null;
        if (cached instanceof List) {
            return (List<SpaceMember>) cached;
        }

        List<SpaceMember> active = loadFromDb(userId);
        if (request != null) {
            request.setAttribute(CACHE_KEY, active);
        }
        return active;
    }

    private List<SpaceMember> loadFromDb(Long userId) {
        List<SpaceMember> memberships = spaceMemberMapper.selectByUserId(userId);
        if (memberships == null || memberships.isEmpty()) {
            return new ArrayList<>();
        }
        return memberships.stream()
                .filter(m -> m.getSpaceId() != null)
                .filter(m -> isActive(m))
                .collect(Collectors.toList());
    }

    /**
     * 成员关系是否在籍。
     * <p>{@code status} 列为 null 时视为在籍：V3 迁移给历史数据补的是默认值 1，
     * 但任何在本迁移之前插入、且未显式写 status 的行都会是 1；
     * 这里容错是为了不因脏数据把人锁在自己的家门外。
     */
    public static boolean isActive(SpaceMember member) {
        return member.getStatus() == null || member.getStatus() == STATUS_ACTIVE;
    }

    private static HttpServletRequest currentRequest() {
        Object attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
            return ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }
}
