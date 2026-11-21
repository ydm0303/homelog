// 通用类型定义
export interface User {
  id: number
  username: string
  email: string
  nickName: string
  avatar: string
  role?: number
  spaceId?: number
  createdAt: string
  updatedAt: string
}

export interface Space {
  id: number
  name: string
  ownerId: number
  /** 本人在该空间的角色：1=owner 2=admin 3=member 4=readonly */
  role?: number
  /** 在籍成员数 */
  memberCount?: number
  createdAt: string
}

export interface SpaceMember {
  id: number
  spaceId: number
  userId: number
  /** 1=owner 2=admin 3=member 4=readonly（语义见 utils/role） */
  role: number
  /** 1=active 2=left 3=removed */
  status?: number
  nickname?: string
  receiveMail: number
  createdAt: string
  // 关联用户信息
  username?: string
  nickName?: string
  email?: string
}

export interface FieldVO {
  id: number
  fieldKey: string
  fieldLabel: string
  fieldType: string
  isRequired: number
  isPrivate: number
  optionsJson: string
  sortOrder: number
}

export interface ModuleMetaVO {
  id: number
  moduleKey: string
  name: string
  moduleType: number
  isEnable: number
  showInHome: number
  sortOrder: number
  configJson: string
  fields: FieldVO[]
}

export interface RecordVO {
  id: number
  spaceId: number
  moduleId: number
  moduleKey: string
  moduleName: string
  title: string
  creatorId: number
  creatorName: string
  isPrivate: number
  tags: string
  remindDate: string
  dataJson: Record<string, any>
  imagesJson: string[]
  createdAt: string
  updatedAt: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface MessageVO {
  id: number
  spaceId: number
  userId: number
  recordId: number
  content: string
  isRead: number
  createdAt: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  data: T[]
  total: number
  page: number
  size: number
}

export interface DashboardModuleStat {
  moduleId: number
  moduleKey: string
  name: string
  showInHome: number
  count: number
}

export interface DashboardSummary {
  modules: DashboardModuleStat[]
  todos: RecordVO[]
  expiring: RecordVO[]
  recent: RecordVO[]
}