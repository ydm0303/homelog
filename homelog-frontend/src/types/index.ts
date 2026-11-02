// 通用类型定义
export interface User {
  id: number
  username: string
  email: string
  nickName: string
  avatar: string
  createdAt: string
  updatedAt: string
}

export interface Space {
  id: number
  name: string
  ownerId: number
  createdAt: string
}

export interface SpaceMember {
  id: number
  spaceId: number
  userId: number
  role: number // 1=admin, 2=edit, 3=readonly
  receiveMail: number
  createdAt: string
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