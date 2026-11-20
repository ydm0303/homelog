package com.homelog.security;

/**
 * 空间角色语义层（设计文档 v1.2 §4.4 / 落地路线步 0 + 步 1）。
 *
 * <p>本类是「角色编号 → 权限语义」的唯一权威来源。业务代码一律调用这里的
 * 语义方法，<b>禁止</b>再出现 {@code role == 1} / {@code role != 1} /
 * {@code role <= 2} 这类裸数字比较——编号若要再调整，只改本类。
 *
 * <p><b>步 1 已启用新四档编号</b>（决策 D5+D6）：
 * <pre>
 *   1 = OWNER     所有者   每空间唯一，取 jj_space.owner_id
 *   2 = ADMIN     管理员   原「管理员」中非 owner 的部分
 *   3 = MEMBER    普通成员 原「可编辑」(2)
 *   4 = READONLY  只读成员 原「只读」(3)
 * </pre>
 * 与数据库需配合 {@code db/migration/V3__space_role.sql} 一并上线，
 * 单独跑代码或单独跑 SQL 都会造成权限判定静默错位。
 *
 * <p>权限语义按决策 D3（member 沿用现状：可新增、可改自己创建的、不能删任何东西）：
 * <ul>
 *   <li>{@link #canManageMembers}  owner / admin</li>
 *   <li>{@link #canManageModule}   owner / admin</li>
 *   <li>{@link #canSeePrivate}     owner / admin</li>
 *   <li>{@link #canWrite}          owner / admin / member</li>
 *   <li>{@link #ownContentOnly}    member（只能改自己创建的）</li>
 *   <li>{@link #canDelete}         owner / admin</li>
 *   <li>{@link #isOwner}           owner（解散空间、指派 admin 等专属）</li>
 * </ul>
 */
public final class Role {

    private Role() {
    }

    /** 所有者：空间创建者，唯一，不可被移除 / 不可退出，仅可解散空间 */
    public static final int OWNER = 1;

    /** 管理员：协助管理成员与内容，不能指派或移除 admin */
    public static final int ADMIN = 2;

    /** 普通成员：可查看、可新增、可改自己创建的，无删除权与管理权 */
    public static final int MEMBER = 3;

    /** 只读成员：仅查看 */
    public static final int READONLY = 4;

    /**
     * 未知 / 无成员关系时的兜底档位。
     * <p>取最保守的 {@link #READONLY}：宁可少给权限，不可默认放行。
     */
    public static final int DEFAULT = READONLY;

    /**
     * 规范化的角色档位：{@code null} 一律按 {@link #DEFAULT}（只读）兜底。
     */
    public static int of(Integer role) {
        return role == null ? DEFAULT : role;
    }

    // ------------------------------------------------------------------
    // 档位识别：仅用于映射 / 展示 / 互斥判断，不要用作权限判断
    // 权限一律走下面的 canXxx 语义方法
    // ------------------------------------------------------------------

    /** 是否所有者档 */
    public static boolean isOwner(Integer role) {
        return of(role) == OWNER;
    }

    /** 是否管理员档（不含 owner） */
    public static boolean isAdminTier(Integer role) {
        return of(role) == ADMIN;
    }

    /** 是否普通成员档 */
    public static boolean isMemberTier(Integer role) {
        return of(role) == MEMBER;
    }

    /** 是否只读档 */
    public static boolean isReadonlyTier(Integer role) {
        return of(role) == READONLY;
    }

    /**
     * 是否「有管理权」（owner 或 admin）。
     * <p>前端 {@code isAdmin} 与路由 {@code requiresAdmin} 走这一档——
     * 老代码里 {@code role==1} 同时承担「管理员」与「有管理权」两义，
     * 拆成 owner/admin 后必须区分：迁移后原管理员变成 2，
     * 若仍按 {@code ==OWNER} 判会静默失去全部管理功能。
     */
    public static boolean isAdmin(Integer role) {
        int r = of(role);
        return r == OWNER || r == ADMIN;
    }

    // ------------------------------------------------------------------
    // 权限语义
    // ------------------------------------------------------------------

    /** 能否管理空间成员：增删改成员、发起邀请 */
    public static boolean canManageMembers(Integer role) {
        return isAdmin(role);
    }

    /** 能否指派 / 移除管理员档成员——<b>仅 owner</b>（决策：任何角色不能授予高于自身的角色） */
    public static boolean canAppointAdmin(Integer role) {
        return isOwner(role);
    }

    /** 能否管理模块与字段配置（启用/禁用、别名、排序、增删自定义模块） */
    public static boolean canManageModule(Integer role) {
        return isAdmin(role);
    }

    /** 能否看到私密内容：私密记录本体、私密字段值、privateOnly 筛选 */
    public static boolean canSeePrivate(Integer role) {
        return isAdmin(role);
    }

    /** 能否新增 / 修改内容（owner / admin / member；只读不可） */
    public static boolean canWrite(Integer role) {
        return of(role) <= MEMBER;
    }

    /**
     * 是否只能操作自己创建的内容（member）。
     * <p>owner / admin 可改任何人的，readonly 由 {@link #canWrite} 先拦住。
     */
    public static boolean ownContentOnly(Integer role) {
        return isMemberTier(role);
    }

    /** 能否删除记录（owner / admin；member 无删除权，决策 D3） */
    public static boolean canDelete(Integer role) {
        return isAdmin(role);
    }

    /** 能否解散空间 / 变更 owner——仅 owner */
    public static boolean canDissolve(Integer role) {
        return isOwner(role);
    }

    /** 该角色是否可被他人移除：owner 不可被移除 */
    public static boolean canBeRemoved(Integer targetRole) {
        return !isOwner(targetRole);
    }

    /**
     * 操作人能否<b>管理某个已存在的成员</b>（改其角色 / 移除他）。
     * <p>核心规则：只能管理严格低于自己的一档。由于档位数值本身就是
     * owner(1) &lt; admin(2) &lt; member(3) &lt; readonly(4)，一条比较即可覆盖
     * 权限矩阵中的三条约束：
     * <ul>
     *   <li>{@code owner} 可管 admin / member / readonly；</li>
     *   <li>{@code admin} <b>不能</b>动同为 admin 的成员（2 &lt; 2 不成立），
     *       否则 admin 可把同级降为 member 架空对方 —— 矩阵里「修改/移除管理员：仅 owner」；</li>
     *   <li>任何人都不能动 owner（1 &lt; 1 不成立），owner 不可被移除、角色不可变更。</li>
     * </ul>
     * 注：与 {@link #canGrantRole} 分工不同 —— 本方法管「能不能碰这个人」，
     * canGrantRole 管「能不能把人放到那个档位」。两处需<b>同时</b>满足。
     */
    public static boolean canManageTargetRole(Integer operatorRole, Integer targetRole) {
        return of(operatorRole) < of(targetRole);
    }

    /**
     * 发起邀请 / 指派角色时，操作人能否授予目标角色。
     * <p>规则（设计文档 v1.2 §4.2）：
     * <ul>
     *   <li>OWNER 永不通过指派产生（由创建空间固定，不支持转让）；</li>
     *   <li>ADMIN 档<b>只有 OWNER</b> 能指派 —— 数值相等并不够，
     *       admin(2) 指派 admin(2) 必须拒绝，否则「任命管理员仅 owner」形同虚设；</li>
     *   <li>MEMBER / READONLY 档：owner 与 admin 皆可，即目标档位须严格低于操作人。</li>
     * </ul>
     */
    public static boolean canGrantRole(Integer inviterRole, Integer targetRole) {
        int inviter = of(inviterRole);
        int target = of(targetRole);
        if (target == OWNER) {
            return false;
        }
        if (target == ADMIN) {
            return inviter == OWNER;
        }
        return target > inviter;
    }

    /** 角色显示名（日志 / 接口回显用；前端文案另有 utils/role.ts） */
    public static String label(Integer role) {
        switch (of(role)) {
            case OWNER:
                return "所有者";
            case ADMIN:
                return "管理员";
            case MEMBER:
                return "普通成员";
            default:
                return "只读";
        }
    }
}
