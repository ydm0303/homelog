/**
 * 角色档位语义层（前端侧唯一权威来源）。
 *
 * 与后端 `com.homelog.security.Role` 一一对应（设计文档 v1.2 §4.4）。
 * 业务代码禁止再出现 `role === 1` / `role <= 2` 这类裸数字比较——
 * 编号若要再调整，只改本文件，所有调用方行为自动跟变。
 *
 * 步 1 已启用新四档（决策 D5+D6，需配合 db/migration/V3__space_role.sql）：
 *   1 = OWNER    所有者   每空间唯一
 *   2 = ADMIN    管理员
 *   3 = MEMBER   普通成员  原「可编辑」
 *   4 = READONLY 只读成员  原「只读」
 *
 * ⚠️ isAdmin 的语义变化：旧代码 role===1 既是「管理员档」也隐含「有管理权」，
 *    拆成 owner/admin 两档后，本文件的 isAdmin 取「有管理权 = owner || admin」。
 *    老管理员迁移后变成 2，若按 role===1 判会静默失去 Settings 入口、删除权、
 *    私密可见等全部管理功能。
 */

/** 角色档位常量（新四档） */
export const ROLE = {
  /** 所有者 */
  OWNER: 1,
  /** 管理员 */
  ADMIN: 2,
  /** 普通成员 */
  MEMBER: 3,
  /** 只读成员 */
  READONLY: 4,
} as const

/** 无角色信息时的兜底档（与后端 Role.DEFAULT 一致，取最保守的只读） */
export const ROLE_DEFAULT = ROLE.READONLY

function norm(role: number | null | undefined): number {
  return role ?? ROLE_DEFAULT
}

// ---------------------------------------------------------------------------
// 档位识别：仅用于展示 / 互斥判断，权限一律走下面的 canXxx
// ---------------------------------------------------------------------------

/** 是否所有者档 */
export function isOwner(role: number | null | undefined): boolean {
  return norm(role) === ROLE.OWNER
}

/** 是否管理员档（不含 owner） */
export function isAdminTier(role: number | null | undefined): boolean {
  return norm(role) === ROLE.ADMIN
}

/** 是否普通成员档 */
export function isMemberTier(role: number | null | undefined): boolean {
  return norm(role) === ROLE.MEMBER
}

/** 是否只读档 */
export function isReadonlyTier(role: number | null | undefined): boolean {
  return norm(role) === ROLE.READONLY
}

/** 是否有管理权（owner 或 admin） */
export function isAdmin(role: number | null | undefined): boolean {
  const r = norm(role)
  return r === ROLE.OWNER || r === ROLE.ADMIN
}

// ---------------------------------------------------------------------------
// 权限语义
// ---------------------------------------------------------------------------

/** 可管理空间成员（增删改成员、发起邀请） */
export function canManageMembers(role: number | null | undefined): boolean {
  return isAdmin(role)
}

/** 可指派 / 移除管理员档——仅 owner */
export function canAppointAdmin(role: number | null | undefined): boolean {
  return isOwner(role)
}

/** 可管理模块与字段配置 */
export function canManageModule(role: number | null | undefined): boolean {
  return isAdmin(role)
}

/** 可见私密内容（私密记录 / 私密字段 / privateOnly 筛选） */
export function canSeePrivate(role: number | null | undefined): boolean {
  return isAdmin(role)
}

/** 可新增/修改内容（owner/admin/member，只读不可） */
export function canWrite(role: number | null | undefined): boolean {
  return norm(role) <= ROLE.MEMBER
}

/** 只能操作自己创建的内容（member） */
export function ownContentOnly(role: number | null | undefined): boolean {
  return isMemberTier(role)
}

/** 可删除记录（owner/admin；member 无删除权，决策 D3） */
export function canDelete(role: number | null | undefined): boolean {
  return isAdmin(role)
}

/** 可解散空间 / 变更 owner——仅 owner */
export function canDissolve(role: number | null | undefined): boolean {
  return isOwner(role)
}

/**
 * 邀请人 / 操作人能否把成员指派到目标角色。须与后端 Role.canGrantRole 完全一致：
 *   · OWNER 永不通过指派产生；
 *   · ADMIN 档只有 OWNER 能指派（admin 指派 admin 必须拒绝）；
 *   · MEMBER / READONLY 档：目标档位须严格低于操作人（target > inviter）。
 */
export function canGrantRole(
  inviterRole: number | null | undefined,
  targetRole: number | null | undefined
): boolean {
  const inviter = norm(inviterRole)
  const target = norm(targetRole)
  if (target === ROLE.OWNER) return false
  if (target === ROLE.ADMIN) return inviter === ROLE.OWNER
  return target > inviter
}

/**
 * 操作人能否管理某个已存在的成员（改角色 / 移除）。须与后端 Role.canManageTargetRole 一致：
 * 只能管理严格低于自己的一档（数值更小 = 权限更高）。一条比较覆盖
 * 「admin 不能动同级 admin」「任何人不能动 owner」两类越权。
 * 与 canGrantRole 分工不同：本方法管「能不能碰这个人」，canGrantRole 管「能不能把他放到那个档」。
 */
export function canManageTargetRole(
  operatorRole: number | null | undefined,
  targetRole: number | null | undefined
): boolean {
  return norm(operatorRole) < norm(targetRole)
}

/** 角色显示文案 */
export function roleLabel(role: number | null | undefined): string {
  switch (norm(role)) {
    case ROLE.OWNER:
      return '所有者'
    case ROLE.ADMIN:
      return '管理员'
    case ROLE.MEMBER:
      return '普通成员'
    default:
      return '只读'
  }
}

/** 角色标签颜色（el-tag type） */
export function roleTagType(role: number | null | undefined): 'danger' | 'warning' | 'success' | 'info' {
  switch (norm(role)) {
    case ROLE.OWNER:
      return 'danger'
    case ROLE.ADMIN:
      return 'warning'
    case ROLE.MEMBER:
      return 'success'
    default:
      return 'info'
  }
}

/** 可指派的档位（owner 不在其中：由创建空间产生，不支持转让与指派） */
export const ASSIGNABLE_ROLES: ReadonlyArray<{ value: number; label: string }> = [
  { value: ROLE.ADMIN, label: '管理员' },
  { value: ROLE.MEMBER, label: '普通成员' },
  { value: ROLE.READONLY, label: '只读' },
]
