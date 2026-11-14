// 认证状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const user = ref<User | null>(null)
  const spaceId = ref<number>(1)
  const role = ref<number>(3) // 1=admin, 2=edit, 3=readonly
  const initialized = ref(false)
  let initPromise: Promise<void> | null = null

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
  async function refreshAccessToken() {
    if (!refreshToken.value) {
      logout()
      throw new Error('No refresh token')
    }

    try {
      const response = await authApi.refresh()

      token.value = response.data.accessToken
      refreshToken.value = response.data.refreshToken

      localStorage.setItem('access_token', token.value!)
      localStorage.setItem('refresh_token', refreshToken.value!)

      const api = (await import('@/api')).default
      api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
    } catch {
      logout()
      throw new Error('Token refresh failed')
    }
  }

  // 登出
  async function logout() {
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