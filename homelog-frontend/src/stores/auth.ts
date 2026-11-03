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

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => role.value === 1)
  const isEditor = computed(() => role.value <= 2)
  const isReadonly = computed(() => role.value === 3)

  // 初始化认证状态
  async function initAuth() {
    const savedToken = localStorage.getItem('access_token')
    const savedRefreshToken = localStorage.getItem('refresh_token')
    const savedUser = localStorage.getItem('user')
    const savedRole = localStorage.getItem('role')

    if (savedToken && savedUser) {
      token.value = savedToken
      refreshToken.value = savedRefreshToken
      user.value = JSON.parse(savedUser)
      role.value = savedRole ? parseInt(savedRole) : 3

      // 设置 axios 默认头
      const api = (await import('@/api')).default
      api.defaults.headers.common['Authorization'] = `Bearer ${token.value}`

      // 验证 token 有效性
      try {
        await fetchMe()
      } catch {
        // token 无效，尝试刷新
        await refreshAccessToken()
      }
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

  // 获取当前用户信息
  async function fetchMe() {
    const response = await authApi.detail()
    user.value = response.data
    localStorage.setItem('user', JSON.stringify(response.data))

    // 从后端获取角色（简化：这里用固定值，实际应从用户信息或单独接口获取）
    // 实际项目中可通过 /api/meta/modules/all 判断是否为管理员
    role.value = 1 // 暂时默认管理员，后续接入真实权限
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