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
      size="clamp(260px, 78%, 320px)"
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

          <el-dropdown
            v-if="spaces.length > 1"
            trigger="click"
            class="space-switch"
            popper-class="space-switch-popper"
            @command="handleSwitchSpace"
          >
            <span class="space-switcher" role="button" tabindex="0" aria-label="切换家庭空间">
              <el-icon><House /></el-icon>
              <span class="space-switcher-name">{{ currentSpaceName }}</span>
              <el-icon class="space-switcher-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled class="dropdown-header">切换家庭空间</el-dropdown-item>
                <el-dropdown-item
                  v-for="s in spaces"
                  :key="s.id"
                  :command="s.id"
                  :disabled="s.id === authStore.spaceId"
                >
                  <span class="space-option">
                    <span class="space-option-name">{{ s.name }}</span>
                    <el-tag :type="roleTagType(s.role)" size="small" effect="plain">{{ roleLabel(s.role) }}</el-tag>
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

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
                <button v-if="!authStore.isOwner" class="user-menu-item" @click="handleLeaveSpace">
                  <el-icon><Back /></el-icon>
                  <span>退出当前家庭</span>
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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Setting, Menu, Bell, Search, ArrowDown, SwitchButton, House, Back } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { messageApi, spaceApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModuleIcon } from '@/utils/module'
import { roleLabel, roleTagType } from '@/utils/role'
import { useIsTabletDown } from '@/composables/useMediaQuery'
import { haptic } from '@/composables/useHaptics'

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

// 断点检测：<1024px 走移动端布局（抽屉菜单）
const isMobile = useIsTabletDown()
watch(isMobile, (mobile) => {
  if (mobile) sidebarCollapsed.value = false
}, { immediate: true })

let unreadTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
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

// 我可切换的空间列表与当前空间名（切换器数据源，设计文档 v1.2 §6.5）
const spaces = computed(() => authStore.spaces)
const currentSpaceName = computed(() => authStore.currentSpace?.name || '我的家庭')

// 管理员判断
const isAdmin = computed(() => authStore.isAdmin)

// 登录人昵称（优先昵称，其次用户名）
const displayName = computed(() => authStore.user?.nickName || authStore.user?.username || '家人')

// 头像占位首字
const avatarText = computed(() => displayName.value.charAt(0) || '家')

// 用户下拉实例（用于手动关闭）
const userDropdownRef = ref<InstanceType<typeof import('element-plus').ElDropdown> | null>(null)

// 角色标签文本（档位语义统一来自 @/utils/role）
const getRoleLabel = computed(() => roleLabel(authStore.role))

function handleQuickCreate() {
  haptic('light')
  const firstModule = moduleStore.enabledModules?.[0]
  if (firstModule?.moduleKey) {
    router.push(`/module/${firstModule.moduleKey}/create`)
    return
  }
  router.push('/settings')
}

// 抽屉关闭前：Element Plus 的 before-close 不会看返回值，
// 必须调用 done() 才真正关闭（X 按钮 / 遮罩 / ESC 都走这里）
function handleDrawerClose(done: () => void) {
  done()
}

// 退出登录
function handleLogout() {
  userDropdownRef.value?.handleClose()
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 切换家庭空间（设计文档 v1.2 §6.5）：切换后整体重新加载，确保以新 X-Space-Id 重新取数
async function handleSwitchSpace(targetId: number) {
  if (!targetId || targetId === authStore.spaceId) return
  try {
    await authStore.switchSpace(targetId)
    ElMessage.success(`已切换到「${authStore.currentSpace?.name || '新空间'}」`)
    window.location.assign('/')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '切换空间失败')
  }
}

// 主动退出当前家庭空间（非 owner）
async function handleLeaveSpace() {
  userDropdownRef.value?.handleClose()
  try {
    await ElMessageBox.confirm(
      '退出后你将被移出当前家庭空间，且不再能查看其内容。是否继续？',
      '退出家庭',
      { confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    await spaceApi.leave()
    // 当前空间已失效：先清掉 header，再用剩余在籍空间重新定位，避免请求被 403
    localStorage.removeItem('current_space_id')
    const list = await authStore.fetchSpaces()
    if (list.length > 0) {
      await authStore.switchSpace(list[0].id)
      ElMessage.success('已退出当前家庭空间')
      window.location.assign('/')
    } else {
      ElMessage.success('已退出全部家庭空间')
      authStore.logout()
      router.push('/login')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '退出失败')
  }
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
@use '@/styles/breakpoints' as *;

.default-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background: $bg-color;
}

// 侧边栏
.sidebar {
  width: $layout-sidebar-width;
  min-width: $layout-sidebar-width;
  height: 100%;
  background: $sidebar-bg;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  transition: width $transition-slow $ease-out;
  z-index: $z-index-sidebar;
  overflow: hidden;
  box-shadow: inset -1px 0 0 rgba($primary-color, 0.06);

  &.collapsed {
    width: $layout-sidebar-collapsed-width;
    min-width: $layout-sidebar-collapsed-width;

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
  height: $layout-header-height;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 $spacing-md;
  border-bottom: 1px solid $border-color;
  // 与顶栏搜索区同一背景（$topbar-bg + 毛玻璃），保证顶部一行视觉连贯
  background: $topbar-bg;
  backdrop-filter: blur(10px);

  .brand-block {
    display: flex;
    align-items: center;
    gap: $spacing-md-soft;
    min-width: 0;

    .brand-mark {
      display: flex;
      align-items: center;
      justify-content: center;
      width: $icon-tile-size-sm;
      height: $icon-tile-size-sm;
      border-radius: $border-radius-lg;
      background: $primary-color;
      color: $card-bg;
      font-size: $icon-size-xl;
      flex-shrink: 0;
    }

    .brand-text {
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-width: 0;

      .logo {
        font-size: $text-heading;
        font-weight: $font-weight-bold;
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
  padding: $spacing-md-soft $spacing-sm;
  overflow-y: auto;
  background: transparent;

  .nav-item {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: $spacing-md-soft;
    min-height: $touch-target-min;
    padding: $spacing-sm $spacing-md-soft;
    // 原先 14px：卡片 12 / 导航 14 两种圆角混用，是「同一层级不同长相」的典型
    border-radius: $border-radius-lg;
    background: transparent;
    color: $nav-text;
    text-decoration: none;
    font-size: $text-body;
    @include transition-interactive;
    white-space: nowrap;
    font-weight: $font-weight-medium;
    border: none;
    box-shadow: none;

    &:hover {
      background-color: $nav-hover-bg;
      color: $primary-text;
      transform: translateX($shift-on-hover);
    }

    &.active {
      background: $hover-bg;
      color: $primary-text;
      font-weight: $font-weight-bold;
      box-shadow: inset 0 0 0 1px rgba($primary-color, 0.05);
    }

    span {
      display: inline-flex;
      align-items: center;
      line-height: 1.2;
    }

    el-icon {
      width: $icon-size-base;
      height: $icon-size-base;
      font-size: $icon-size-base;
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: $nav-text;
      @include transition-interactive;
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
  padding: $spacing-md-soft;
  border-top: 1px solid $border-color;

  .footer-note {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin: 0 0 $spacing-md-soft;
    padding: $spacing-md-soft $spacing-lg-soft;
    text-align: left;
    font-size: $text-caption;
    line-height: $line-height-base;
    letter-spacing: 0.08em;
    color: $text-tertiary;
    background: $surface-warm;
    border: 1px solid $border-color-light;
    border-radius: $border-radius-lg;

    .footer-note-heart {
      flex-shrink: 0;
      color: $brand-400;
      font-size: $text-body;
      line-height: 1;
    }

    .footer-note-text {
      display: flex;
      flex-direction: column;
      gap: $spacing-micro;

      span {
        display: block;
        white-space: nowrap;
      }
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: $spacing-md-soft;

    .user-detail {
      display: flex;
      flex-direction: column;
      gap: $spacing-micro;
      min-width: 0;

      .user-name {
        font-size: $text-label;
        font-weight: $font-weight-semibold;
        color: $text-primary;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .user-role {
        // 原先 11px 低于可读下限，且与说明文字 12px 只差 1px，两档并存无意义
        font-size: $text-caption;
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
  transition: margin-left $transition-slow $ease-out;
}

// 顶部栏
.top-bar {
  height: calc(#{$layout-header-height} + env(safe-area-inset-top));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: env(safe-area-inset-top) $spacing-lg 0;
  background: $topbar-bg;
  border-bottom: 1px solid $border-color;
  position: sticky;
  top: 0;
  z-index: $z-index-topbar;
  backdrop-filter: blur(10px);

  .top-bar-left {
    display: flex;
    align-items: center;
    gap: $spacing-md;
  }

  .top-bar-center {
    flex: 1;
    display: flex;
    justify-content: flex-start;
    max-width: $layout-search-slot-width;
    margin: 0 $spacing-lg-soft 0 $spacing-md;

    .top-search-input {
      width: 100%;
      max-width: $layout-search-input-width;

      :deep(.el-input__wrapper) {
        height: $control-height-md;
        padding: 0 $spacing-lg-soft;
        border-radius: $border-radius-pill;
        background: $field-bg;
        border: 1px solid transparent;
        box-shadow: none;
        @include transition-interactive;

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
        // 内层比外壳少 2px：外壳有 1px 上下边框，写满会撑高外壳
        height: calc(#{$control-height-md} - 2px);
        font-size: $text-body;
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
    gap: $spacing-md;
  }

  .toggle-btn,
  .menu-btn,
  .search-btn {
    color: $text-tertiary;
    background: transparent;
    border: none;
    @include transition-interactive;

    &:hover,
    &:focus {
      color: $primary-color;
      background: transparent;
      border: none;
    }
  }

  .notification-icon {
    font-size: $icon-size-lg;
    color: $text-tertiary;
    cursor: pointer;
    position: relative;
    @include transition-interactive;

    &:hover {
      color: $primary-color;
    }
  }

  // 未读点用主色，替换 Element 默认的红色
  .notification-badge {
    :deep(.el-badge__content.is-dot) {
      background-color: $primary-color;
      width: $badge-dot-size;
      height: $badge-dot-size;
    }
  }
}

// 空间切换器
.space-switcher {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  height: $control-height-sm;
  padding: 0 $spacing-md-soft;
  border-radius: $border-radius-pill;
  cursor: pointer;
  color: $text-secondary;
  font-size: $text-label;
  font-weight: $font-weight-semibold;
  @include transition-interactive;

  &:hover {
    background: $hover-bg;
    color: $primary-color;
  }

  .space-switcher-name {
    max-width: $layout-name-max-width;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .space-switcher-caret {
    font-size: $text-caption;
    color: $text-placeholder;
  }
}

// 顶栏用户入口
.user-trigger {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-xs $spacing-md-soft $spacing-xs $spacing-sm;
  border-radius: $border-radius-pill;
  cursor: pointer;
  @include transition-interactive;

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
    font-size: $text-body;
    font-weight: $font-weight-semibold;
    flex-shrink: 0;
  }

  .user-trigger-name {
    max-width: $layout-name-max-width;
    font-size: $text-label;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    @include transition-interactive;
  }

  .user-trigger-caret {
    font-size: $text-caption;
    color: $text-placeholder;
    @include transition-interactive;
  }
}

// 页面内容
.page-content {
  flex: 1;
  padding: $spacing-lg clamp($spacing-lg, 5vw, #{$layout-content-pad-x-max})
    calc(#{$layout-content-pad-bottom} + env(safe-area-inset-bottom));
  overflow-y: auto;
  overscroll-behavior: contain;
  background: $bg-color;
}

.page-container {
  width: 100%;
  max-width: $layout-content-max-width;
  margin: 0 auto;
}

.global-fab {
  position: fixed;
  right: $layout-fab-inset;
  bottom: calc(#{$layout-fab-inset} + env(safe-area-inset-bottom));
  z-index: $z-index-fab;
  touch-action: manipulation;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: 0 $spacing-lg-soft 0 $spacing-md-soft;
  height: $layout-fab-size;
  border: 0;
  border-radius: $border-radius-pill;
  background: $gradient-primary;
  color: $card-bg;
  font-size: $text-body-lg;
  font-weight: $font-weight-bold;
  letter-spacing: 0.02em;
  box-shadow: $shadow-fab;
  cursor: pointer;
  transition: transform $transition-base $ease-out, box-shadow $transition-base $ease-out;

  &:hover {
    transform: translateY(-$lift-on-hover) scale(1.01);
    box-shadow: $shadow-fab-hover;
  }

  &:active {
    transform: translateY(0);
  }
}

.fab-plus {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: $icon-button-size;
  height: $icon-button-size;
  border-radius: $border-radius-pill;
  background: rgba(255, 255, 255, 0.2);
  font-size: $icon-size-xl;
  line-height: 1;
}

.fab-label {
  white-space: nowrap;
}

// 移动端抽屉
.mobile-nav {
  display: flex;
  flex-direction: column;
  padding: $spacing-lg $spacing-md calc(#{$spacing-lg} + env(safe-area-inset-bottom));
  gap: $spacing-sm;
  background: transparent;

  .mobile-nav-item {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: $spacing-md-soft;
    min-height: $touch-target-min;
    padding: $spacing-sm $spacing-md-soft;
    border-radius: $border-radius-lg;
    background: transparent;
    color: $nav-text;
    text-decoration: none;
    font-size: $text-title;
    border: none;
    @include transition-interactive;

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
      width: $icon-size-base;
      height: $icon-size-base;
      font-size: $icon-size-base;
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
@include down(lg) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    height: 100dvh;
    z-index: $z-index-mobile-drawer;
    box-shadow: $shadow-drawer;
    transform: translateX(-100%);
    transition: transform $transition-slow $ease-out;

    &.mobile-open {
      transform: translateX(0);
    }
  }

  .main-content {
    margin-left: 0;
  }

  .top-bar {
    padding: 0 $spacing-md;
  }

  .top-bar-center {
    display: none;
  }

  .page-content {
    padding: $spacing-md $spacing-md calc(#{$layout-content-pad-bottom-mobile} + env(safe-area-inset-bottom));
  }

  .page-container {
    max-width: none;
  }

  .global-fab {
    right: $layout-fab-inset-mobile;
    bottom: calc(#{$layout-fab-inset-mobile} + env(safe-area-inset-bottom));
    width: $layout-fab-size-mobile;
    height: $layout-fab-size-mobile;
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
    font-size: $icon-size-xl;
  }

  .search-btn {
    display: flex;
  }
}

@include up(lg) {
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
    width: $popper-menu-width;
    padding: $spacing-sm;
  }

  .user-menu-header {
    display: flex;
    flex-direction: column;
    gap: $spacing-xs;
    padding: $spacing-sm $spacing-md-soft $spacing-md-soft;
  }

  .user-menu-name {
    font-size: $text-body;
    font-weight: $font-weight-bold;
    color: $text-primary;
  }

  .user-menu-role {
    font-size: $text-caption;
    color: $text-tertiary;
  }

  .user-menu-divider {
    height: 1px;
    margin: 0 $spacing-sm $spacing-sm;
    background: $border-color-light;
  }

  .user-menu-item {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    width: 100%;
    padding: $spacing-sm $spacing-md-soft;
    border: none;
    background: transparent;
    border-radius: $border-radius-base;
    font-size: $text-label;
    color: $text-primary;
    cursor: pointer;
    text-align: left;
    @include transition-interactive;

    .el-icon {
      font-size: $icon-size-sm;
      flex-shrink: 0;
    }

    &:hover {
      background: $hover-bg;
    }
  }

  .user-menu-item-primary {
    color: $primary-text;
    font-weight: $font-weight-semibold;

    &:hover {
      color: $primary-hover;
    }
  }
}

// 空间切换下拉
.el-popper.is-light.space-switch-popper {
  .space-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: $spacing-md;
    min-width: $popper-menu-min-width;
  }

  .space-option-name {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
