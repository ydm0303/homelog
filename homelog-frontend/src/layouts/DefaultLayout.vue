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
            <span class="brand-subtitle">家庭空间</span>
          </div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" exact-active-class="active">
          <el-icon><HomeFilled /></el-icon>
          <span v-if="!sidebarCollapsed">首页</span>
        </router-link>

        <router-link to="/module/drug" class="nav-item" exact-active-class="active">
          <el-icon><FirstAidKit /></el-icon>
          <span v-if="!sidebarCollapsed">药品档案</span>
        </router-link>

        <router-link to="/module/todo" class="nav-item" exact-active-class="active">
          <el-icon><Checked /></el-icon>
          <span v-if="!sidebarCollapsed">家庭待办</span>
        </router-link>

        <router-link to="/module/note" class="nav-item" exact-active-class="active">
          <el-icon><Picture /></el-icon>
          <span v-if="!sidebarCollapsed">生活记事相册</span>
        </router-link>

        <router-link to="/module/idoc" class="nav-item" exact-active-class="active">
          <el-icon><CreditCard /></el-icon>
          <span v-if="!sidebarCollapsed">证件档案</span>
        </router-link>

        <router-link v-if="isAdmin" to="/settings" class="nav-item" exact-active-class="active">
          <el-icon><Setting /></el-icon>
          <span v-if="!sidebarCollapsed">设置中心</span>
        </router-link>
      </nav>

      <div v-if="!sidebarCollapsed" class="sidebar-footer">
        <p class="footer-note">把平凡日子 过成温柔的纪念</p>
        <div class="user-info">
          <el-avatar :size="36" :src="authStore.user?.avatar" />
          <div class="user-detail">
            <span class="user-name">{{ authStore.user?.nickName || '杨东铭' }}</span>
            <span class="user-role">管理员</span>
          </div>
        </div>
      </div>
    </aside>

    <el-drawer v-model="mobileDrawer" direction="ltr" size="78%" :before-close="handleDrawerClose">
      <nav class="mobile-nav">
        <router-link to="/" class="mobile-nav-item" @click="mobileDrawer = false">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </router-link>
        <router-link to="/module/drug" class="mobile-nav-item" @click="mobileDrawer = false">
          <el-icon><FirstAidKit /></el-icon>
          <span>药品档案</span>
        </router-link>
        <router-link to="/module/todo" class="mobile-nav-item" @click="mobileDrawer = false">
          <el-icon><Checked /></el-icon>
          <span>家庭待办</span>
        </router-link>
        <router-link to="/module/note" class="mobile-nav-item" @click="mobileDrawer = false">
          <el-icon><Picture /></el-icon>
          <span>生活记事相册</span>
        </router-link>
        <router-link to="/module/idoc" class="mobile-nav-item" @click="mobileDrawer = false">
          <el-icon><CreditCard /></el-icon>
          <span>证件档案</span>
        </router-link>
        <router-link v-if="isAdmin" to="/settings" class="mobile-nav-item" @click="mobileDrawer = false">
          <el-icon><Setting /></el-icon>
          <span>设置中心</span>
        </router-link>

        <div class="mobile-user-box">
          <el-avatar :size="42" :src="authStore.user?.avatar" />
          <div>
            <p>{{ authStore.user?.nickName || '杨东铭' }}</p>
            <span>管理员</span>
          </div>
        </div>
      </nav>
    </el-drawer>

    <div class="main-content" :class="{ expanded: sidebarCollapsed && !isMobile }">
      <header class="top-bar">
        <div class="top-bar-left">
          <el-button v-if="!isMobile" class="toggle-btn" @click="sidebarCollapsed = !sidebarCollapsed" circle size="small">
            <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
          </el-button>

          <el-button v-if="isMobile" class="menu-btn" @click="mobileDrawer = true" circle size="small">
            <el-icon><Menu /></el-icon>
          </el-button>
        </div>

        <div class="top-bar-center" v-show="!isMobile">
          <el-input v-model="searchKeyword" placeholder="搜索记录、标签、内容..." :prefix-icon="Search" clearable size="small" class="top-search-input" @keyup.enter="handleSearch" @focus="showSearchDrawer = true" @blur="setTimeout(() => showSearchDrawer = false, 200)" />
        </div>

        <div class="top-bar-right">
          <el-button v-if="isMobile" class="search-btn" @click="showSearchDrawer = true" circle size="small">
            <el-icon><Search /></el-icon>
          </el-button>

          <el-dropdown>
            <span class="dropdown-link">
              <el-icon class="notification-icon"><Bell /></el-icon>
              <el-badge v-if="unreadCount > 0" :value="unreadCount" max="99" class="notification-badge" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled class="dropdown-header">消息中心</el-dropdown-item>
                <el-dropdown-item @click="goToMessages">查看全部消息</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click" class="user-drop">
            <div class="user-trigger">
              <el-avatar :size="32" :src="authStore.user?.avatar" />
              <span class="user-trigger-name">{{ authStore.user?.nickName || '杨东铭' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="page-content">
        <router-view />
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
import {
  HomeFilled, Setting, Fold, Expand, Menu, Bell, Download, Search,
  FirstAidKit, Checked, Picture, CreditCard
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { messageApi } from '@/api'
import { registerSW } from 'virtual:pwa-register'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 响应式状态
const sidebarCollapsed = ref(false)
const mobileDrawer = ref(false)
const elderlyMode = ref(false)
const showInstallPrompt = ref(false)
const unreadCount = ref(0)
const showSearchDrawer = ref(false)
const searchKeyword = ref('')

// 断点检测
const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth < 1024
  if (isMobile.value) {
    sidebarCollapsed.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)

  // PWA 安装提示
  registerSW({
    onNeedRefresh() {
      showInstallPrompt.value = true
    },
    onOfflineReady() {
      console.log('App ready for offline use')
    }
  })

  // 恢复长辈模式设置
  const savedElderly = localStorage.getItem('elderlyMode')
  if (savedElderly === 'true') {
    elderlyMode.value = true
    document.body.classList.add('elderly-mode')
  }

  // 获取初始未读数
  fetchUnreadCount()

  // 启动未读数轮询
  startUnreadPolling()
})

function startUnreadPolling() {
  const timer = setInterval(async () => {
    if (!document.hidden) {
      await fetchUnreadCount()
    }
  }, 30000)

  onUnmounted(() => clearInterval(timer))
}

async function fetchUnreadCount() {
  try {
    const response = await messageApi.getUnreadCount()
    unreadCount.value = response.data?.count || 0
  } catch (e) {
    console.error('Failed to fetch unread count:', e)
  }
}

// 页面标题
const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    Dashboard: '首页仪表盘',
    ModuleList: '模块列表',
    RecordCreate: '新增记录',
    RecordDetail: '记录详情',
    RecordEdit: '编辑记录',
    Settings: '设置中心',
    Messages: '消息中心',
    GlobalSearch: '全局搜索'
  }
  return titles[route.name as string] || '家记'
})

// 启用的模块
const enabledModules = computed(() => moduleStore.enabledModules)

// 管理员判断
const isAdmin = computed(() => authStore.isAdmin)

// 模块图标映射
function getModuleIcon(moduleKey: string) {
  const icons: Record<string, string> = {
    drug: 'FirstAidKit',
    todo: 'Checked',
    note: 'Picture',
    idoc: 'CreditCard'
  }
  return icons[moduleKey] || 'Grid'
}

// 角色标签类型
const getRoleTagType = computed(() => {
  switch (authStore.role) {
    case 1: return 'danger'
    case 2: return 'success'
    default: return 'info'
  }
})

// 角色标签文本
const getRoleLabel = computed(() => {
  switch (authStore.role) {
    case 1: return '管理员'
    case 2: return '可编辑'
    default: return '只读'
  }
})

// 切换长辈模式
function toggleElderlyMode() {
  localStorage.setItem('elderlyMode', elderlyMode.value.toString())
  if (elderlyMode.value) {
    document.body.classList.add('elderly-mode')
    ElMessage.success('已开启长辈模式')
  } else {
    document.body.classList.remove('elderly-mode')
    ElMessage.success('已关闭长辈模式')
  }
}

// 处理安装
function handleInstall() {
  ElMessage.success('正在更新应用...')
  location.reload()
}

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
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 跳转消息中心
function goToMessages() {
  router.push('/messages')
}

// 搜索处理
function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { q: searchKeyword.value.trim() } })
    searchKeyword.value = ''
    showSearchDrawer.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.default-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #f7f3ed 0%, #f4efe9 100%);
}

// 侧边栏
.sidebar {
  width: 240px;
  min-width: 240px;
  height: 100%;
  background: linear-gradient(180deg, rgba(255, 250, 245, 0.95), rgba(255, 250, 245, 0.98));
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: 100;
  overflow: hidden;
  box-shadow: inset -1px 0 0 rgba(221, 112, 78, 0.06);

  &.collapsed {
    width: 64px;
    min-width: 64px;
  }
}

.sidebar-header {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid $border-color;
  background: linear-gradient(180deg, rgba(245, 239, 232, 0.9), rgba(245, 239, 232, 0.76));

  .logo {
    font-size: 20px;
    font-weight: 800;
    color: $primary-color;
    margin: 0;
    white-space: nowrap;
    letter-spacing: 0.04em;
  }

  .logo-mini {
    font-size: 18px;
    font-weight: 800;
    color: $primary-color;
    width: 34px;
    height: 34px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(221, 112, 78, 0.1);
  }
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;

  .nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 14px;
    color: $text-primary;
    text-decoration: none;
    transition: all 0.2s ease;
    white-space: nowrap;
    font-weight: 500;

    &:hover {
      background-color: $hover-bg;
      color: $primary-color;
      transform: translateX(1px);
    }

    &.active {
      background: linear-gradient(135deg, rgba(221, 112, 78, 0.12), rgba(221, 112, 78, 0.06));
      color: $primary-color;
      box-shadow: inset 0 0 0 1px rgba(221, 112, 78, 0.08);
      font-weight: 700;
    }

    el-icon {
      font-size: 18px;
      flex-shrink: 0;
    }
  }
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid $border-color;

  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .user-detail {
      display: flex;
      flex-direction: column;
      min-width: 0;

      .user-name {
        font-size: 13px;
        font-weight: 500;
        color: $text-primary;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .logout-btn {
    width: 100%;
    justify-content: center;
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
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 253, 251, 0.8);
  border-bottom: 1px solid $border-color;
  position: sticky;
  top: 0;
  z-index: 50;
  backdrop-filter: blur(10px);

  .top-bar-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .page-title {
      font-size: 18px;
      font-weight: 700;
      color: $text-primary;
      margin: 0;
      letter-spacing: -0.02em;
    }
  }

  .top-bar-center {
    flex: 1;
    display: flex;
    justify-content: center;
    max-width: 600px;
    margin: 0 24px;

    .top-search-input {
      width: 100%;
      max-width: 480px;

      :deep(.el-input__inner) {
        height: 36px;
        padding: 0 12px 0 36px;
        font-size: 14px;

        &:hover {
          border-color: $primary-color;
        }

        &:focus {
          border-color: $primary-color;
          box-shadow: 0 0 0 2px $primary-light-bg;
        }
      }

      :deep(.el-input__prefix) {
        color: $text-placeholder;
        font-size: 16px;
      }

      :deep(.el-input__suffix) {
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
    color: $text-secondary;
  }

  .notification-icon {
    font-size: 20px;
    color: $text-secondary;
    cursor: pointer;
    position: relative;
  }

  .install-btn {
    color: $primary-color;
  }
}

// 面包屑
// 页面内容
.page-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, rgba(247, 243, 237, 0.7), rgba(247, 243, 237, 0.92));
}

.global-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px 0 14px;
  height: 58px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #dd704e 0%, #d9633d 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
  box-shadow: 0 16px 26px rgba(221, 112, 78, 0.28);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px) scale(1.01);
    box-shadow: 0 18px 30px rgba(221, 112, 78, 0.32);
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
  padding: 24px 16px;
  gap: 8px;

  .mobile-nav-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 12px;
    border-radius: $border-radius-base;
    color: $text-primary;
    text-decoration: none;
    font-size: 16px;

    &:hover {
      background-color: $hover-bg;
      color: $primary-color;
    }

    &.active {
      background-color: $primary-light-bg;
      color: $primary-color;
    }

    el-icon {
      font-size: 20px;
    }
  }
}

.mobile-divider {
  margin: 16px 0;
}

.mobile-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;

  div {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

.mobile-logout {
  margin-top: 16px;
}

// 长辈模式全局样式
.elderly-mode {
  font-size: 18px !important;

  .el-button,
  .el-input,
  .el-select,
  .el-tag,
  .el-breadcrumb {
    font-size: 18px !important;
  }

  .el-button {
    padding: 12px 24px !important;
    height: auto !important;
  }

  .el-input__inner {
    padding: 12px 16px !important;
    height: 48px !important;
  }

  .sidebar {
    width: 280px !important;
    min-width: 280px !important;
  }

  .nav-item {
    padding: 14px 16px !important;
  }

  .page-content {
    padding: 32px !important;
  }
}

// 响应式
@media (max-width: 1023px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 200;
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);
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
    display: none !important;
  }

  .breadcrumb {
    padding: 8px 16px;
  }

  .page-content {
    padding: 16px;
  }

  .search-btn {
    display: flex !important;
  }
}

@media (min-width: 1024px) {
  .search-btn {
    display: none;
  }
}
</style>