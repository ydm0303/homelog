// 认证状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import type { User } from '@/types'

/**
 * 从 JWT 载荷里解析过期时间（毫秒时间戳），解析失败返回 0。
 * 用 token 自身推导而非依赖服务端 expiresIn，这样刷新/恢复登录态都无需额外存储。
 */
function getTokenExpiryMs(token: string | null): number {
  if (!token) return 0
  try {
    const payload = token.split('.')[1]
    if (!payload) return 0
    // base64url → base64，并补齐 '=' padding（atob 要求长度是 4 的倍数）
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const json = JSON.parse(atob(padded))
    return typeof json.exp === 'number' ? json.exp * 1000 : 0
  } catch {
    return 0
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const user = ref<User | null>(null)
  const spaceId = ref<number>(1)
  const role = ref<number>(3) // 1=admin, 2=edit, 3=readonly
  const initialized = ref(false)
  let initPromise: Promise<void> | null = null
  // 并发的 401 共用同一次刷新，避免多个请求同时刷新导致后到的刷新用旧 refresh token 失败
  let refreshPromise: Promise<void> | null = null
  // 主动刷新定时器：在 access token 过期前提前续期，避免用户操作时撞上过期
  let refreshTimer: ReturnType<typeof setTimeout> | null = null
  // 标签页可见性监听只绑定一次
  let visibilityBound = false

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => role.value === 1)
  const isEditor = computed(() => role.value <= 2)
  const isReadonly = computed(() => role.value === 3)

  // 初始化认证状态（页面刷新时先恢复登录态，路由守卫/布局都要等它完成）
  async function initAuth() {
    if (initialized.value) return
    if (initPromise) return initPromise

    initPromise = doInitAuth().finally(() => {
      initialized.value = true
      initPromise = null
    })

    return initPromise
  }

  async function doInitAuth() {
    const savedToken = localStorage.getItem('access_token')
    const savedRefreshToken = localStorage.getItem('refresh_token')
    const savedUser = localStorage.getItem('user')
    const savedRole = localStorage.getItem('role')

    if (!savedToken) return

    token.value = savedToken
    refreshToken.value = savedRefreshToken
    role.value = savedRole ? parseInt(savedRole) : 3

    // 先用本地缓存渲染，避免刷新瞬间显示默认占位名
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        localStorage.removeItem('user')
      }
    }

    // 设置 axios 默认头
    const api = (await import('@/api')).default
    api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`

    // 验证 token 有效性；401 由 axios 拦截器自动刷新并重试
    try {
      await fetchMe()
      // 恢复登录态后安排主动续期，避免停留过久后 token 过期
      scheduleProactiveRefresh()
    } catch {
      // 走到这里说明刷新也已失败，清理登录态
      await logout()
    }
  }

  // 登录
  async function login(username: string, password: string) {
    const response = await authApi.login(username, password)
    const { accessToken, refreshToken: newRefreshToken } = response.data

    token.value = accessToken
    refreshToken.value = newRefreshToken

    localStorage.setItem('access_token', accessToken)
    localStorage.setItem('refresh_token', newRefreshToken)

    const api = (await import('@/api')).default
    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`

    await fetchMe()
    scheduleProactiveRefresh()
  }

  // 注册
  async function register(data: { username: string; password: string; email: string; nickName?: string }) {
    const response = await authApi.register(data)
    const { accessToken, refreshToken: newRefreshToken } = response.data

    token.value = accessToken
    refreshToken.value = newRefreshToken

    localStorage.setItem('access_token', accessToken)
    localStorage.setItem('refresh_token', newRefreshToken)

    const api = (await import('@/api')).default
    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`

    await fetchMe()
    scheduleProactiveRefresh()
  }

  // 获取当前用户信息（含空间角色）
  async function fetchMe() {
    const response = await authApi.detail()
    const profile = response.data
    user.value = profile
    localStorage.setItem('user', JSON.stringify(profile))

    // 后端 /auth/detail 返回当前空间角色，前端不再硬编码管理员
    role.value = profile.role ?? 3
    spaceId.value = profile.spaceId ?? 1
    localStorage.setItem('role', role.value.toString())
  }

  // 刷新 Access Token
  async function refreshAccessToken(): Promise<void> {
    // 已有刷新在进行中：复用同一个 Promise，避免并发刷新互相覆盖 refresh token
    if (refreshPromise) return refreshPromise

    if (!refreshToken.value) {
      logout()
      throw new Error('No refresh token')
    }

    refreshPromise = doRefreshAccessToken().finally(() => {
      refreshPromise = null
    })

    return refreshPromise
  }

  async function doRefreshAccessToken(): Promise<void> {
    try {
      const response = await authApi.refresh()

      token.value = response.data.accessToken
      refreshToken.value = response.data.refreshToken

      localStorage.setItem('access_token', token.value!)
      localStorage.setItem('refresh_token', refreshToken.value!)

      const api = (await import('@/api')).default
      api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`

      scheduleProactiveRefresh()
    } catch {
      logout()
      throw new Error('Token refresh failed')
    }
  }

  /**
   * 在 access token 过期前 60 秒左右提前刷新，避免用户停留过久后操作时撞上过期。
   * 用 JWT 自身的 exp 计算，不依赖服务端返回值。
   */
  function scheduleProactiveRefresh() {
    clearProactiveRefresh()
    bindVisibilityWatcher()

    const expiresAt = getTokenExpiryMs(token.value)
    if (!expiresAt) return

    const ttl = expiresAt - Date.now()
    if (ttl <= 0) return

    // 提前量：不超过 60s、不低于 5s，且不超过剩余时间的 1/3（避免刚签发就触发新一轮刷新）
    const lead = Math.min(60_000, Math.max(5_000, Math.floor(ttl * 0.2)), Math.floor(ttl / 3))
    const delay = Math.max(1_000, ttl - lead)

    refreshTimer = setTimeout(() => {
      // 主动刷新失败不打断用户：交给后续 401 的被动刷新兜底
      refreshAccessToken().catch(() => {})
    }, delay)
  }

  function clearProactiveRefresh() {
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  /**
   * 标签页重新可见时检查 token：浏览器会冻结/节流后台标签的定时器，
   * 导致“页面开久了”续期没按时触发。切回来时若已临近或超过过期时间，立即续期。
   */
  function handleVisibilityChange() {
    if (document.visibilityState !== 'visible' || !token.value) return
    const expiresAt = getTokenExpiryMs(token.value)
    if (expiresAt && expiresAt - Date.now() < 60_000) {
      refreshAccessToken().catch(() => {})
    } else {
      // 仍有效：重新对齐一次定时器（后台期间可能已被节流打乱）
      scheduleProactiveRefresh()
    }
  }

  function bindVisibilityWatcher() {
    if (visibilityBound) return
    visibilityBound = true
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }

  // 登出
  async function logout() {
    clearProactiveRefresh()
    if (visibilityBound) {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      visibilityBound = false
    }
    token.value = null
    refreshToken.value = null
    user.value = null
    role.value = 3

    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user')
    localStorage.removeItem('role')

    const api = (await import('@/api')).default
    delete api.defaults.headers.common['Authorization']
  }

  // 修改密码
  async function updatePassword(oldPassword: string, newPassword: string) {
    await authApi.updatePassword(oldPassword, newPassword)
  }

  return {
    token,
    refreshToken,
    user,
    spaceId,
    role,
    isLoggedIn,
    isAdmin,
    isEditor,
    isReadonly,
    initAuth,
    login,
    register,
    fetchMe,
    refreshAccessToken,
    logout,
    updatePassword
  }
})