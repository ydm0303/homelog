package com.homelog.security;

import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 当前登录用户 / 空间上下文（设计文档 v1.2 §6.5 / §13 步 2）。
 *
 * <p>本类是「我是谁 + 我在哪个家 + 我在这个家是什么身份」的唯一入口。
 * 各 Service / Controller 不得再自行推断空间，也不得写死 spaceId。
 *
 * <p>步 2 的关键变化：
 * <ul>
 *   <li>删除 {@code DEFAULT_SPACE_ID} 硬编码，当前空间改由
 *       {@link SpaceContextResolver} 从 {@code X-Space-Id} 解析并<b>强制校验在籍身份</b>；</li>
 *   <li>无权限时不再回退成默认空间 1（那是文档 §15 S1 的同类问题：
 *       任何登录用户都能借「默认空间」读到 space 1 的数据）；</li>
 *   <li>{@link #getCurrentRole()} 不再从 JWT 的 {@code ROLE_*} 兜底。
 *       旧实现里，一个<b>没有任何成员关系</b>的用户会拿到 JWT 缓存的 ROLE_ADMIN
 *       并被换算成管理员档 —— 相当于无家可归的人默认是管理员，属越权。
 *       现在统一回落到最保守的 {@link Role#READONLY}。</li>
 * </ul>
 *
 * <p>兼容性：老客户端不带 {@code X-Space-Id} 时，唯一空间的用户照常工作（走回落路径），
 * 因此步 2 可以早于步 5（前端切换器）上线。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserMapper userMapper;
    private final SpaceContextResolver spaceContextResolver;
    private final SpaceMembershipReader spaceMembershipReader;

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.trim().isEmpty() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userMapper.selectByUsername(username);
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 当前用户在<b>当前空间</b>的成员关系（已校验在籍）。
     *
     * @return 无登录用户 / 无任何在籍空间时为 {@code null}
     */
    public SpaceMember getCurrentMembership() {
        return spaceContextResolver.resolve(getCurrentUserId());
    }

    /**
     * 当前空间 ID。
     *
     * @return 无在籍空间时为 {@code null}。调用方据此返回空结果或拒绝，
     *         <b>不得</b>回落到任何默认空间。
     */
    public Long getCurrentSpaceId() {
        SpaceMember member = getCurrentMembership();
        return member != null ? member.getSpaceId() : null;
    }

    /**
     * 写路径专用的当前空间 ID。
     *
     * <p>{@code jj_record.space_id} / {@code jj_module.space_id} 等列都是 NOT NULL，
     * 拿 null 去 insert 会撞成 500 数据库异常，既难排查又会污染错误语义。
     * 写操作前应调用本方法，把「没有可写入的空间」表达成明确的 403。
     *
     * @throws AccessDeniedException 当前用户没有任何在籍空间
     */
    public Long requireCurrentSpaceId() {
        Long spaceId = getCurrentSpaceId();
        if (spaceId == null) {
            String username = getCurrentUsername();
            log.warn("Write denied, user has no active space: username={}", username);
            throw new AccessDeniedException("当前账号未加入任何家庭空间，无法执行该操作");
        }
        return spaceId;
    }

    /**
     * 当前用户在当前空间的角色档位。
     * <p>语义判断请用 {@code Role.canXxx(role)}，不要比较裸数字。
     *
     * @return 无成员关系时为 {@link Role#READONLY}（最保守档）
     */
    public Integer getCurrentRole() {
        SpaceMember member = getCurrentMembership();
        if (member == null || member.getRole() == null) {
            return Role.READONLY;
        }
        return member.getRole();
    }

    /**
     * 当前用户是否具备管理权（owner 或 admin）。
     * <p>这是<b>档位</b>判断；需要权限语义时用
     * {@link Role#canManageMembers(Integer)} / {@link Role#canManageModule(Integer)} 等。
     */
    public boolean isAdmin() {
        return Role.isAdmin(getCurrentRole());
    }

    /**
     * 当前用户在所有在籍空间的成员关系，供「我的空间列表」/ 空间切换器使用。
     */
    public List<SpaceMember> getMyActiveMemberships() {
        return spaceMembershipReader.findActiveMemberships(getCurrentUserId());
    }
}
