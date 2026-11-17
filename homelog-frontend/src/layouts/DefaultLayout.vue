<template>
  <div class="default-layout">
    <aside v-show="!isMobile" class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="brand-block">
          <div class="brand-mark">
            <el-icon><HomeFilled /></el-icon>
          </div>
          <div v-if="!sidebarCollapsed" class="brand-text">
            <h1 class="logo">家记</h1>
          </div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" exact-active-class="active">
          <el-icon><HomeFilled /></el-icon>
          <span v-if="!sidebarCollapsed">首页</span>
        </router-link>

        <router-link
          v-for="module in enabledModules"
          :key="module.id"
          :to="`/module/${module.moduleKey}`"
          class="nav-item"
          active-class="active"
        >
          <el-icon><component :is="getModuleIcon(module.moduleKey)" /></el-icon>
          <span v-if="!sidebarCollapsed">{{ module.name }}</span>
        </router-link>

        <router-link v-if="isAdmin" to="/settings" class="nav-item" exact-active-class="active">
          <el-icon><Setting /></el-icon>
          <span v-if="!sidebarCollapsed">设置中心</span>
        </router-link>
      </nav>

      <div v-if="!sidebarCollapsed" class="sidebar-footer">
        <p class="footer-note">
          <span class="footer-note-heart" aria-hidden="true">♥</span>
          <span class="footer-note-text">
            <span>把平凡日子</span>
            <span>过成温柔的纪念</span>
          </span>
        </p>
        <div class="user-info">
          <el-avatar :size="36" :src="authStore.user?.avatar" />
          <div class="user-detail">
            <span class="user-name">{{ displayName }}</span>
            <span class="user-role">{{ getRoleLabel }}</span>
          </div>
        </div>
      </div>
    </aside>

    <el-drawer
      v-model="mobileDrawer"
      direction="ltr"
      size="86%"
      class="mobile-drawer"
      modal-class="mobile-drawer-overlay"
      :before-close="handleDrawerClose"
    >
      <nav class="mobile-nav">
        <router-link to="/" class="mobile-nav-item" exact-active-class="active" @click="mobileDrawer = false">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </router-link>

        <router-link
          v-for="module in enabledModules"
          :key="module.id"
          :to="`/module/${module.moduleKey}`"
          class="mobile-nav-item"
          active-class="active"
          @click="mobileDrawer = false"
        >
          <el-icon><component :is="getModuleIcon(module.moduleKey)" /></el-icon>
          <span>{{ module.name }}</span>
        </router-link>

        <router-link v-if="isAdmin" to="/settings" class="mobile-nav-item" exact-active-class="active" @click="mobileDrawer = false">
          <el-icon><Setting /></el-icon>
          <span>设置中心</span>
        </router-link>

        <div class="mobile-user-box">
          <el-avatar :size="42" :src="authStore.user?.avatar" />
          <div>
            <p>{{ displayName }}</p>
            <span>{{ getRoleLabel }}</span>
          </div>
        </div>
      </nav>
    </el-drawer>

    <div class="main-content" :class="{ expanded: sidebarCollapsed && !isMobile }">
      <header class="top-bar">
        <div class="top-bar-left">
          <el-button v-if="!isMobile" class="toggle-btn" :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" @click="sidebarCollapsed = !sidebarCollapsed" circle size="small">
            <el-icon><Menu /></el-icon>
          </el-button>

          <el-button v-if="isMobile" class="menu-btn" aria-label="打开菜单" @click="mobileDrawer = true" circle size="small">
            <el-icon><Menu /></el-icon>
          </el-button>
        </div>

        <div class="top-bar-center" v-show="!isMobile">
          <el-input v-model="searchKeyword" placeholder="搜索记录、标签、内容..." :prefix-icon="Search" clearable size="small" class="top-search-input" @keyup.enter="handleSearch" />
        </div>

        <div class="top-bar-right">
          <el-button v-if="isMobile" class="search-btn" aria-label="打开全局搜索" @click="goToSearch" circle size="small">
            <el-icon><Search /></el-icon>
          </el-button>

          <el-dropdown>
            <span class="dropdown-link" role="button" tabindex="0" aria-label="消息中心">
              <el-badge is-dot :hidden="unreadCount === 0" class="notification-badge">
                <el-icon class="notification-icon"><Bell /></el-icon>
              </el-badge>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled class="dropdown-header">消息中心</el-dropdown-item>
                <el-dropdown-item @click="goToMessages">查看全部消息</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown ref="userDropdownRef" trigger="click" class="user-drop" popper-class="user-menu-popper">
            <div class="user-trigger">
              <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-trigger-name">{{ displayName }}</span>
              <el-icon class="user-trigger-caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <div class="user-menu-card">
                <div class="user-menu-header">
                  <span class="user-menu-name">{{ displayName }}</span>
                  <span class="user-menu-role">{{ getRoleLabel }}·家庭空间</span>
                </div>
                <div class="user-menu-divider"></div>
                <button class="user-menu-item user-menu-item-primary" @click="goToSettings">
                  <el-icon><Setting /></el-icon>
                  <span>个人设置</span>
                </button>
                <button class="user-menu-item" @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </button>
              </div>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="page-content">
        <div class="page-container">
          <router-view v-slot="{ Component, route: currentRoute }">
            <transition name="page-fade" mode="out-in" @after-enter="restoreScroll">
              <component :is="Component" :key="currentRoute.path" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>

    <button class="global-fab" @click="handleQuickCreate" aria-label="新增记录">
      <span class="fab-plus">＋</span>
      <span class="fab-label">新增记录</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Setting, Menu, Bell, Search, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { messageApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getModuleIcon } from '@/utils/module'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 记录每个路由的滚动位置，返回时恢复（滚动容器是 .page-content，不是 window）
const scrollPositions = new Map<string, number>()
let unregisterScrollGuard: (() => void) | null = null

// 响应式状态
const sidebarCollapsed = ref(false)
const mobileDrawer = ref(false)
const unreadCount = ref(0)
const searchKeyword = ref('')

// 断点检测
const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth < 1024
  if (isMobile.value) {
    sidebarCollapsed.value = false
  }
}

let unreadTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)

  unregisterScrollGuard = router.beforeEach((_to, from) => {
    const el = document.querySelector<HTMLElement>('.page-content')
    if (el && from.fullPath) {
      scrollPositions.set(from.fullPath, el.scrollTop)
    }
  })

  // 加载启用模块，用于动态渲染侧边菜单（直接进入非模块页时也要有菜单）
  moduleStore.fetchModules().catch((e) => {
    console.error('Failed to load modules for sidebar:', e)
  })

  // 恢复长辈模式与高对比度设置（设置页负责写入，刷新后由布局恢复全局样式）
  restoreElderlySettings()

  // 获取初始未读数并轮询
  fetchUnreadCount()
  unreadTimer = setInterval(() => {
    if (!document.hidden) fetchUnreadCount()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (unregisterScrollGuard) unregisterScrollGuard()
  if (unreadTimer) clearInterval(unreadTimer)
})

// 页面转场结束后恢复滚动位置
function restoreScroll() {
  const el = document.querySelector<HTMLElement>('.page-content')
  if (el) {
    el.scrollTop = scrollPositions.get(route.fullPath) ?? 0
  }
}

// 恢复适老化 CSS 变量与高对比度标记
function restoreElderlySettings() {
  const body = document.body
  type ElderlySettings = {
    fontScale?: number
    buttonScale?: number
    spacingScale?: number
    highContrast?: boolean
  }

  let settings: ElderlySettings = {}
  try {
    settings = JSON.parse(localStorage.getItem('elderlySettings') || '{}') as ElderlySettings
  } catch {
    settings = {}
  }

  body.style.setProperty('--elderly-font-scale', String(settings.fontScale ?? 1.3))
  body.style.setProperty('--elderly-button-scale', String(settings.buttonScale ?? 1.2))
  body.style.setProperty('--elderly-spacing-scale', String(settings.spacingScale ?? 1.2))
  body.classList.toggle('high-contrast', Boolean(settings.highContrast))
  body.dataset.highContrast = String(Boolean(settings.highContrast))

  if (localStorage.getItem('elderlyMode') === 'true') {
    body.classList.add('elderly-mode')
    body.dataset.elderly = 'true'
  }
}

async function fetchUnreadCount() {
  try {
    const response = await messageApi.getUnreadCount()
    unreadCount.value = response.data?.count || 0
  } catch (e) {
    console.error('Failed to fetch unread count:', e)
  }
}

// 启用的模块
const enabledModules = computed(() => moduleStore.enabledModules)

// 管理员判断
const isAdmin = computed(() => authStore.isAdmin)

// 登录人昵称（优先昵称，其次用户名）
const displayName = computed(() => authStore.user?.nickName || authStore.user?.username || '家人')

// 头像占位首字
const avatarText = computed(() => displayName.value.charAt(0) || '家')

// 用户下拉实例（用于手动关闭）
const userDropdownRef = ref<InstanceType<typeof import('element-plus').ElDropdown> | null>(null)

// 角色标签文本
const getRoleLabel = computed(() => {
  switch (authStore.role) {
    case 1: return '管理员'
    case 2: return '可编辑'
    default: return '只读'
  }
})

function handleQuickCreate() {
  const firstModule = moduleStore.enabledModules?.[0]
  if (firstModule?.moduleKey) {
    router.push(`/module/${firstModule.moduleKey}/create`)
    return
  }
  router.push('/settings')
}

// 抽屉关闭前
function handleDrawerClose() {
  return true
}

// 退出登录
function handleLogout() {
  userDropdownRef.value?.handleClose()
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 跳转个人设置
function goToSettings() {
  userDropdownRef.value?.handleClose()
  router.push('/settings')
}

// 跳转消息中心
function goToMessages() {
  router.push('/messages')
}

function goToSearch() {
  router.push('/search')
}

// 搜索处理
function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { q: searchKeyword.value.trim() } })
    searchKeyword.value = ''
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.default-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background: $bg-color;
}

// 侧边栏
.sidebar {
  width: 240px;
  min-width: 240px;
  height: 100%;
  background: $sidebar-bg;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: $z-index-sidebar;
  overflow: hidden;
  box-shadow: inset -1px 0 0 rgba($primary-color, 0.06);

  &.collapsed {
    width: 64px;
    min-width: 64px;

    .nav-item {
      justify-content: center;
      padding-left: 0;
      padding-right: 0;
      gap: 0;

      span {
        display: none;
      }
    }
  }
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid $border-color;
  background: $sidebar-header-bg;

  .brand-block {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;

    .brand-mark {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: $border-radius-lg;
      background: $primary-color;
      color: $card-bg;
      font-size: 20px;
      flex-shrink: 0;
    }

    .brand-text {
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-width: 0;

      .logo {
        font-size: 20px;
        font-weight: 800;
        color: $primary-text;
        margin: 0;
        white-space: nowrap;
        letter-spacing: 0.04em;
        line-height: 1;
      }
    }
  }
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
  background: transparent;

  .nav-item {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 10px;
    min-height: 44px;
    padding: 10px 12px;
    border-radius: 14px;
    background: transparent;
    color: $nav-text;
    text-decoration: none;
    transition: all 0.2s ease;
    white-space: nowrap;
    font-weight: 500;
    border: none;
    box-shadow: none;

    &:hover {
      background-color: $nav-hover-bg;
      color: $primary-text;
      transform: translateX(1px);
    }

    &.active {
      background: $hover-bg;
      color: $primary-text;
      font-weight: 700;
      box-shadow: inset 0 0 0 1px rgba($primary-color, 0.05);
    }

    span {
      display: inline-flex;
      align-items: center;
      line-height: 1.2;
    }

    el-icon {
      width: 18px;
      height: 18px;
      font-size: 18px;
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: $nav-text;
      transition: color 0.2s ease;
    }

    &:hover el-icon,
    &.active el-icon {
      color: $primary-text;
    }

    &:hover el-icon {
      color: $primary-hover;
    }
  }

  .nav-item.active,
  .nav-item:hover {
    border: none;
  }
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid $border-color;

  .footer-note {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 12px;
    padding: 12px 14px;
    text-align: left;
    font-size: 12px;
    line-height: 1.6;
    letter-spacing: 0.08em;
    color: $text-tertiary;
    background: $surface-warm;
    border: 1px solid $border-color-light;
    border-radius: $border-radius-lg;

    .footer-note-heart {
      flex-shrink: 0;
      color: $brand-400;
      font-size: 14px;
      line-height: 1;
    }

    .footer-note-text {
      display: flex;
      flex-direction: column;
      gap: 3px;

      span {
        display: block;
        white-space: nowrap;
      }
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;

    .user-detail {
      display: flex;
      flex-direction: column;
      gap: 2px;
      min-width: 0;

      .user-name {
        font-size: 13px;
        font-weight: 600;
        color: $text-primary;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .user-role {
        font-size: 11px;
        color: $text-placeholder;
        white-space: nowrap;
      }
    }
  }
}

// 主内容区
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  transition: margin-left 0.3s ease;

  &.expanded {
    // 展开状态下无需额外样式，侧边栏已收缩
  }
}

// 顶部栏
.top-bar {
  height: calc(64px + env(safe-area-inset-top));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: env(safe-area-inset-top) 24px 0;
  background: $topbar-bg;
  border-bottom: 1px solid $border-color;
  position: sticky;
  top: 0;
  z-index: $z-index-topbar;
  backdrop-filter: blur(10px);

  .top-bar-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .top-bar-center {
    flex: 1;
    display: flex;
    justify-content: flex-start;
    max-width: 560px;
    margin: 0 20px 0 16px;

    .top-search-input {
      width: 100%;
      max-width: 420px;

      :deep(.el-input__wrapper) {
        height: 36px;
        padding: 0 14px;
        border-radius: 999px;
        background: $field-bg;
        border: 1px solid transparent;
        box-shadow: none;
        transition: all 0.2s ease;

        &:hover {
          background: $card-bg;
          border-color: $border-color;
        }

        &.is-focus {
          background: $card-bg;
          border-color: $primary-color;
          box-shadow: 0 0 0 3px rgba($primary-color, 0.12);
        }
      }

      :deep(.el-input__inner) {
        height: 34px;
        font-size: 14px;
        color: $text-primary;
      }

      :deep(.el-input__prefix) {
        color: $text-placeholder;
      }
    }
  }

  .top-bar-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .toggle-btn,
  .menu-btn,
  .search-btn {
    color: $text-tertiary;
    background: transparent;
    border: none;
    transition: color 0.2s ease;

    &:hover,
    &:focus {
      color: $primary-color;
      background: transparent;
      border: none;
    }
  }

  .notification-icon {
    font-size: 20px;
    color: $text-tertiary;
    cursor: pointer;
    position: relative;
    transition: color 0.2s ease;

    &:hover {
      color: $primary-color;
    }
  }

  // 未读点用主色，替换 Element 默认的红色
  .notification-badge {
    :deep(.el-badge__content.is-dot) {
      background-color: $primary-color;
      width: 7px;
      height: 7px;
    }
  }
}

// 顶栏用户入口
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 6px;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background-color: $hover-bg;

    .user-trigger-name {
      color: $primary-color;
    }

    .user-trigger-caret {
      color: $primary-color;
    }
  }

  .user-avatar {
    background: $primary-light-bg;
    color: $primary-text;
    font-size: 14px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .user-trigger-name {
    max-width: 120px;
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.2s ease;
  }

  .user-trigger-caret {
    font-size: 12px;
    color: $text-placeholder;
    transition: color 0.2s ease;
  }
}

// 页面内容
.page-content {
  flex: 1;
  padding: 24px clamp(24px, 5vw, 88px) calc(88px + env(safe-area-inset-bottom));
  overflow-y: auto;
  background: $bg-color;
}

.page-container {
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
}

.global-fab {
  position: fixed;
  right: 28px;
  bottom: calc(28px + env(safe-area-inset-bottom));
  z-index: $z-index-fab;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px 0 14px;
  height: 58px;
  border: 0;
  border-radius: 999px;
  background: $gradient-primary;
  color: $card-bg;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
  box-shadow: 0 16px 26px rgba($primary-color, 0.28);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px) scale(1.01);
    box-shadow: 0 18px 30px rgba($primary-color, 0.32);
  }

  &:active {
    transform: translateY(0);
  }
}

.fab-plus {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  font-size: 22px;
  line-height: 1;
}

.fab-label {
  white-space: nowrap;
}

// 移动端抽屉
.mobile-nav {
  display: flex;
  flex-direction: column;
  padding: 24px 16px calc(24px + env(safe-area-inset-bottom));
  gap: 8px;
  background: transparent;

  .mobile-nav-item {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 10px;
    min-height: 44px;
    padding: 10px 12px;
    border-radius: 14px;
    background: transparent;
    color: $nav-text;
    text-decoration: none;
    font-size: 16px;
    border: none;

    &:hover {
      background-color: $nav-hover-bg;
      color: $primary-text;
    }

    &.active {
      background-color: $hover-bg;
      color: $primary-text;
    }

    span {
      display: inline-flex;
      align-items: center;
      line-height: 1.2;
    }

    el-icon {
      width: 18px;
      height: 18px;
      font-size: 18px;
      color: $nav-text;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    &:hover el-icon,
    &.active el-icon {
      color: $primary-text;
    }
  }
}

// 响应式
@media (max-width: 1023px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: $z-index-mobile-drawer;
    box-shadow: 4px 0 20px rgba($text-primary, 0.12);
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &.mobile-open {
      transform: translateX(0);
    }
  }

  .main-content {
    margin-left: 0;
  }

  .top-bar {
    padding: 0 16px;
  }

  .top-bar-center {
    display: none;
  }

  .page-content {
    padding: 16px 16px calc(84px + env(safe-area-inset-bottom));
  }

  .page-container {
    max-width: none;
  }

  .global-fab {
    right: 16px;
    bottom: calc(20px + env(safe-area-inset-bottom));
    width: 52px;
    height: 52px;
    padding: 0;
    justify-content: center;
  }

  .fab-label {
    display: none;
  }

  .fab-plus {
    width: auto;
    height: auto;
    background: transparent;
    font-size: 24px;
  }

  .search-btn {
    display: flex;
  }
}

@media (min-width: 1024px) {
  .search-btn {
    display: none;
  }
}
</style>

<style lang="scss">
@use '@/styles/variables' as *;

// 用户下拉卡片（teleported 到 body，需全局样式覆盖 Element 默认）
.el-popper.is-light.user-menu-popper {
  border: 1px solid $border-color-light;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-lg;
  background: $card-bg;
  padding: 0;

  .el-popper__arrow {
    display: none;
  }

  .user-menu-card {
    width: 224px;
    padding: 8px;
  }

  .user-menu-header {
    display: flex;
    flex-direction: column;
    gap: 3px;
    padding: 6px 12px 12px;
  }

  .user-menu-name {
    font-size: 14px;
    font-weight: 700;
    color: $text-primary;
  }

  .user-menu-role {
    font-size: 12px;
    color: $text-tertiary;
  }

  .user-menu-divider {
    height: 1px;
    margin: 0 8px 8px;
    background: $border-color-light;
  }

  .user-menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 9px 12px;
    border: none;
    background: transparent;
    border-radius: $border-radius-base;
    font-size: 13px;
    color: $text-primary;
    cursor: pointer;
    text-align: left;
    transition: background-color 0.15s ease, color 0.15s ease;

    .el-icon {
      font-size: 16px;
      flex-shrink: 0;
    }

    &:hover {
      background: $hover-bg;
    }
  }

  .user-menu-item-primary {
    color: $primary-text;
    font-weight: 600;

    &:hover {
      color: $primary-hover;
    }
  }
}
</style>
