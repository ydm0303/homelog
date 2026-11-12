import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export function getApiErrorMessage(error: any, fallback: string): string {
  const payload = error?.response?.data

  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim()
  }

  if (payload && typeof payload === 'object') {
    if (typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message.trim()
    }

    if (typeof payload.error === 'string' && payload.error.trim()) {
      return payload.error.trim()
    }

    const details = payload.details
    if (details && typeof details === 'object') {
      const firstDetail = Object.values(details as Record<string, unknown>)[0]
      if (typeof firstDetail === 'string' && firstDetail.trim()) {
        return firstDetail.trim()
      }
    }
  }

  if (error?.message) {
    return error.message
  }

  return fallback
}

export function getFriendlyAuthErrorMessage(error: any, fallback: string): string {
  const message = getApiErrorMessage(error, fallback)
  const normalized = message.toLowerCase()

  if (
    normalized.includes('bad credentials') ||
    normalized.includes('invalid credentials') ||
    normalized.includes('用户名或密码') ||
    normalized.includes('password') ||
    normalized.includes('username')
  ) {
    return '用户名或密码错误'
  }

  if (normalized.includes('already exists') || normalized.includes('已被注册')) {
    return '该用户名已被注册'
  }

  return message
}

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    // 刷新接口会自带 refresh token，此处不能再用 access token 覆盖
    if (authStore.token && config.headers && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & {
      _retry?: boolean
      _skipAuthRefresh?: boolean
      _skipErrorToast?: boolean
    }
    const authStore = useAuthStore()

    const isAuthRequest = /\/auth\/(login|register|refresh)/.test(originalRequest.url || '')

    // 登录/注册/刷新请求本身的 401 不应走 token 刷新逻辑，避免覆盖真实鉴权错误
    if (
      error.response?.status === 401 &&
      !originalRequest._skipAuthRefresh &&
      !originalRequest._retry &&
      !isAuthRequest
    ) {
      originalRequest._retry = true

      try {
        await authStore.refreshAccessToken()

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${authStore.token}`
        }

        return api(originalRequest)
      } catch {
        authStore.logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }
    }

    // 统一错误提示
    const message = getApiErrorMessage(error, '请求失败')
    if (error.response?.status !== 401 && !originalRequest._skipErrorToast) {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

export default api

// ============ 认证相关 API ============
export const authApi = {
  /** 登录 POST /api/auth/login（错误提示由登录页统一处理，跳过拦截器提示和 token 刷新） */
  login: (username: string, password: string) => {
    const config = { _skipErrorToast: true, _skipAuthRefresh: true }
    return api.post('/auth/login', { username, password }, config)
  },

  /** 注册 POST /api/auth/register（错误提示由注册页统一处理，跳过拦截器提示和 token 刷新） */
  register: (data: { username: string; password: string; email: string; nickName?: string }) => {
    const config = { _skipErrorToast: true, _skipAuthRefresh: true }
    return api.post('/auth/register', data, config)
  },

  /** 刷新 Token POST /api/auth/refresh（后端从 Authorization 头读取 refresh token） */
  refresh: () => {
    const config = {
      headers: { Authorization: `Bearer ${localStorage.getItem('refresh_token') || ''}` },
      _skipAuthRefresh: true
    }
    return api.post('/auth/refresh', null, config)
  },

  /** 获取当前用户 GET /api/auth/detail */
  detail: () =>
    api.get('/auth/detail'),

  /** 登出 POST /api/auth/logout */
  logout: () =>
    api.post('/auth/logout'),

  /** 修改密码 POST /api/auth/password/update */
  updatePassword: (oldPassword: string, newPassword: string) =>
    api.post('/auth/password/update', { oldPassword, newPassword })
}

// ============ 记录相关 API ============
export const recordApi = {
  /** 列表查询 GET /api/records/list?moduleId=xxx&page=1&size=20&keyword=&tag= */
  list: (params: { moduleId: number; page?: number; size?: number; keyword?: string; tag?: string }) =>
    api.get('/records/list', { params }),

  /** 详情查询 GET /api/records/detail?recordId=xxx&moduleId=xxx */
  detail: (recordId: number, moduleId: number) =>
    api.get('/records/detail', { params: { recordId, moduleId } }),

  /** 创建记录 POST /api/records/create */
  create: (data: {
    moduleId: number
    title?: string
    dataJson: Record<string, any>
    imagesJson?: string[]
    tags?: string
    remindDate?: string
  }) =>
    api.post('/records/create', data),

  /** 更新记录 POST /api/records/update */
  update: (data: {
    recordId: number
    moduleId: number
    title?: string
    dataJson?: Record<string, any>
    imagesJson?: string[]
    tags?: string
    remindDate?: string
  }) =>
    api.post('/records/update', data),

  /** 删除记录 POST /api/records/delete */
  delete: (recordId: number, moduleId: number) =>
    api.post('/records/delete', { recordId, moduleId })
}

// ============ 元数据相关 API ============
export const metaApi = {
  /** 获取启用的模块列表 GET /api/meta/modules/list */
  getModules: () =>
    api.get('/meta/modules/list'),

  /** 获取所有模块（管理员）GET /api/meta/modules/all */
  getAllModules: () =>
    api.get('/meta/modules/all'),

  /** 获取单个模块详情 GET /api/meta/modules/detail?moduleId=xxx */
  getModule: (moduleId: number) =>
    api.get('/meta/modules/detail', { params: { moduleId } }),

  /** 更新模块 POST /api/meta/modules/update */
  updateModule: (data: {
    moduleId: number
    name?: string
    isEnable?: number
    showInHome?: number
    sortOrder?: number
    configJson?: string
  }) =>
    api.post('/meta/modules/update', data),

  /** 创建自定义模块 POST /api/meta/modules/create */
  createModule: (data: { name: string; moduleKey: string; sortOrder?: number }) =>
    api.post('/meta/modules/create', data),

  /** 删除自定义模块 POST /api/meta/modules/delete?moduleId=xxx */
  deleteModule: (moduleId: number) =>
    api.post('/meta/modules/delete', null, { params: { moduleId } })
}

// ============ 空间/成员相关 API ============
export const spaceApi = {
  /** 获取成员列表 GET /api/space/members/list?spaceId=1 */
  getMembers: (spaceId: number = 1) =>
    api.get('/space/members/list', { params: { spaceId } }),

  /** 添加成员 POST /api/space/members/create */
  createMember: (data: { spaceId: number; email: string; role: number; receiveMail: number }) =>
    api.post('/space/members/create', data),

  /** 更新成员 POST /api/space/members/update */
  updateMember: (data: { spaceId: number; userId: number; role?: number; receiveMail?: number }) =>
    api.post('/space/members/update', data),

  /** 删除成员 POST /api/space/members/delete */
  deleteMember: (spaceId: number, userId: number) =>
    api.post('/space/members/delete', { spaceId, userId })
}

// ============ 导出/导入相关 API ============
export const exportApi = {
  /** 导出数据 GET /api/export/all?scope=module&moduleId=xxx */
  exportAll: (params: { scope: 'module' | 'all'; moduleId?: number }) =>
    api.get('/export/all', { params, responseType: 'blob' }),

  /** 药品 CSV 导入 POST /api/import/drug/csv */
  importDrugCsv: (formData: FormData) =>
    api.post('/import/drug/csv', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ============ 文件上传 API ============
export const fileApi = {
  /** 上传文件 POST /api/files/upload */
  upload: (formData: FormData) =>
    api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ============ 消息中心 API ============
export const messageApi = {
  /** 消息列表 GET /api/messages?page=1&size=20&isRead= */
  list: (params: { page?: number; size?: number; isRead?: number }) =>
    api.get('/messages', { params }),

  /** 标记单条已读 PUT /api/messages/:id/read */
  markRead: (id: number) =>
    api.put(`/messages/${id}/read`),

  /** 标记全部已读 PUT /api/messages/read-all */
  markAllRead: () =>
    api.put('/messages/read-all'),

  /** 获取未读数量 GET /api/messages/unread-count */
  getUnreadCount: () =>
    api.get('/messages/unread-count')
}

// ============ 全局搜索 API ============
export const searchApi = {
  /** 全局搜索 GET /api/search?q=xxx&moduleId=&tag=&page=1&size=20 */
  global: (params: {
    q: string
    moduleId?: number
    tag?: string
    page?: number
    size?: number
    startDate?: string
    endDate?: string
  }) =>
    api.get('/search', { params })
}