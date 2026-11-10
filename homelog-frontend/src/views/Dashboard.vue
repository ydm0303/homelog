<template>
  <div class="dashboard-page">
    <!-- 核心指标卡片 -->
    <el-row :gutter="24" class="metrics-row">
      <el-col :xs="24" :sm="12" :md="6" :lg="3" v-for="metric in metrics" :key="metric.key">
        <el-card class="metric-card" :class="metric.class">
          <div class="metric-content">
            <div class="metric-icon" :class="metric.iconClass">
              <el-icon><component :is="metric.icon" /></el-icon>
            </div>
            <div class="metric-info">
              <p class="metric-value">{{ metric.value }}</p>
              <p class="metric-label">{{ metric.label }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主要内容区：两列布局 -->
    <el-row :gutter="24" class="content-row">
      <!-- 左侧：待办 + 到期提醒 -->
      <el-col :xs="24" :lg="16">
        <el-row :gutter="24">
          <!-- 今日待办 -->
          <el-col :xs="24" :md="12">
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <h3>
                    <el-icon><Checked /></el-icon>
                    今日待办
                  </h3>
                  <el-button
                    v-if="todoModule"
                    size="small"
                    type="text"
                    @click="goToModule('todo')"
                  >
                    查看全部
                  </el-button>
                </div>
              </template>

              <div v-if="todoList.length === 0" class="empty-state">
                <el-icon class="empty-icon"><Document /></el-icon>
                <p>暂无待办事项</p>
                <el-button
                  v-if="todoModule"
                  size="small"
                  type="primary"
                  @click="goToModule('todo')"
                >
                  添加第一个待办
                </el-button>
              </div>

              <ul v-else class="item-list">
                <li v-for="item in todoList" :key="item.id" class="list-item">
                  <el-checkbox
                    v-model="item.checked"
                    @change="toggleTodo(item)"
                    :disabled="isReadonly"
                  />
                  <div class="item-content">
                    <p class="item-title">{{ item.title || item.dataJson?.title }}</p>
                    <p class="item-meta" v-if="item.dataJson?.dueDate">
                      <el-icon><Calendar /></el-icon>
                      {{ formatDate(item.dataJson.dueDate) }}
                    </p>
                  </div>
                  <el-tag :type="getPriorityType(item.dataJson?.priority)" size="small">
                    {{ getPriorityLabel(item.dataJson?.priority) }}
                  </el-tag>
                </li>
              </ul>
            </el-card>
          </el-col>

          <!-- 即将到期药品/证件 -->
          <el-col :xs="24" :md="12">
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <h3>
                    <el-icon><AlarmClock /></el-icon>
                    即将到期
                  </h3>
                  <el-button
                    v-if="drugModule || idocModule"
                    size="small"
                    type="text"
                    @click="goToModule('drug')"
                  >
                    查看全部
                  </el-button>
                </div>
              </template>

              <div v-if="expiringList.length === 0" class="empty-state">
                <el-icon class="empty-icon"><FirstAidKit /></el-icon>
                <p>暂无即将到期项目</p>
              </div>

              <ul v-else class="item-list">
                <li v-for="item in expiringList" :key="item.id" class="list-item expiring-item">
                  <div class="item-icon" :class="item.moduleKey">
                    <el-icon>
                      <component :is="item.moduleKey === 'drug' ? 'FirstAidKit' : 'CreditCard'" />
                    </el-icon>
                  </div>
                  <div class="item-content">
                    <p class="item-title">{{ item.title }}</p>
                    <p class="item-meta">
                      <el-icon><Calendar /></el-icon>
                      {{ formatDate(item.remindDate) }} 到期
                      <span v-if="item.moduleKey === 'drug'"> · {{ item.dataJson?.drug_name || item.dataJson?.doc_type }}</span>
                    </p>
                  </div>
                  <el-tag :type="getExpiringType(item.remindDate)" size="small">
                    {{ getExpiringLabel(item.remindDate) }}
                  </el-tag>
                </li>
              </ul>
            </el-card>
          </el-col>
        </el-row>

        <!-- 最近记录 -->
        <el-card class="section-card recent-section">
          <template #header>
            <div class="card-header">
              <h3>
                <el-icon><Clock /></el-icon>
                最近记录
              </h3>
              <el-select
                v-model="recentModuleFilter"
                placeholder="全部模块"
                size="small"
                style="width: 160px"
                @change="fetchRecentRecords"
              >
                <el-option
                  label="全部模块"
                  value=""
                />
                <el-option
                  v-for="module in enabledModules"
                  :key="module.id"
                  :label="module.name"
                  :value="module.moduleKey"
                />
              </el-select>
            </div>
          </template>

          <div v-if="recentRecords.length === 0" class="empty-state">
            <el-icon class="empty-icon"><Document /></el-icon>
            <p>暂无最近记录</p>
          </div>

          <div v-else class="recent-grid">
            <div
              v-for="record in recentRecords"
              :key="record.id"
              class="recent-card"
              @click="goToDetail(record)"
            >
              <div class="recent-card-header">
                <el-tag size="small" :type="getModuleTagType(record.moduleKey)">
                  {{ record.moduleName }}
                </el-tag>
                <span class="recent-time">{{ formatDateTime(record.createdAt) }}</span>
              </div>
              <p class="recent-title">{{ record.title || getFirstFieldValue(record) }}</p>
              <div class="recent-tags" v-if="record.tags">
                <el-tag
                  v-for="tag in record.tags.split(',')"
                  :key="tag"
                  size="small"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：快捷入口 -->
      <el-col :xs="24" :lg="8">
        <el-card class="section-card shortcuts-card">
          <template #header>
            <h3>
              <el-icon><Grid /></el-icon>
              快捷入口
            </h3>
          </template>

          <div class="shortcuts-grid">
            <a
              v-for="module in homeModules"
              :key="module.id"
              :href="`#/module/${module.moduleKey}`"
              class="shortcut-item"
              @click.prevent="goToModule(module.moduleKey)"
            >
              <div class="shortcut-icon" :class="module.moduleKey">
                <el-icon>
                  <component :is="getModuleIcon(module.moduleKey)" />
                </el-icon>
              </div>
              <span class="shortcut-name">{{ module.name }}</span>
              <span class="shortcut-count" v-if="getModuleCount(module.moduleKey) > 0">
                {{ getModuleCount(module.moduleKey) }}
              </span>
            </a>

            <!-- 通用新增按钮 -->
            <a
              v-if="enabledModules.length > 0"
              class="shortcut-item add-shortcut"
              @click.prevent="showCreateDialog = true"
            >
              <el-icon class="add-icon"><Plus /></el-icon>
              <span>新增记录</span>
            </a>
          </div>
        </el-card>

        <!-- PWA 安装提示 -->
        <el-card
          v-if="showInstallPrompt && isMobile"
          class="section-card pwa-install-card"
        >
          <template #header>
            <h3>
              <el-icon><Download /></el-icon>
              安装应用
            </h3>
          </template>
          <p class="pwa-install-text">
            点击浏览器菜单选择「添加到主屏幕」，像原生 APP 一样使用家记
          </p>
          <el-button
            type="primary"
            block
            @click="handleInstall"
          >
            立即安装
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增记录对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="选择要新增的记录类型"
      width="500px"
      :before-close="handleCreateDialogClose"
    >
      <div class="module-select-grid">
        <div
          v-for="module in enabledModules"
          :key="module.id"
          class="module-select-item"
          :class="{ selected: createDialogModule === module.moduleKey }"
          @click="createDialogModule = module.moduleKey"
        >
          <div class="module-select-icon" :class="module.moduleKey">
            <el-icon>
              <component :is="getModuleIcon(module.moduleKey)" />
            </el-icon>
          </div>
          <span>{{ module.name }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { recordApi } from '@/api'
import {
  Checked, AlarmClock, Clock, Grid, Plus, Download,
  Document, Calendar, FirstAidKit
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 响应式状态
const metrics = ref([
  { key: 'todo', label: '待办总数', value: 0, icon: 'Checked', iconClass: 'icon-todo', class: 'metric-todo' },
  { key: 'drug', label: '药品数量', value: 0, icon: 'FirstAidKit', iconClass: 'icon-drug', class: 'metric-drug' },
  { key: 'note', label: '记事条数', value: 0, icon: 'Picture', iconClass: 'icon-note', class: 'metric-note' },
  { key: 'idoc', label: '证件数量', value: 0, icon: 'CreditCard', iconClass: 'icon-idoc', class: 'metric-idoc' }
])

const todoList = ref<any[]>([])
const expiringList = ref<any[]>([])
const recentRecords = ref<any[]>([])
const recentModuleFilter = ref('')

const showCreateDialog = ref(false)
const createDialogModule = ref('')
const showInstallPrompt = ref(false)
const isMobile = ref(false)

const todoModule = computed(() => moduleStore.getModuleByKey('todo'))
const drugModule = computed(() => moduleStore.getModuleByKey('drug'))
const idocModule = computed(() => moduleStore.getModuleByKey('idoc'))

const enabledModules = computed(() => moduleStore.enabledModules)
const homeModules = computed(() => moduleStore.homeModules)
const isReadonly = computed(() => authStore.isReadonly)

const moduleCounts = ref<Record<string, number>>({})

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)

  await moduleStore.fetchModules()
  await fetchDashboardData()

  // PWA 安装提示
  if ('serviceWorker' in navigator) {
    const registration = await navigator.serviceWorker.ready
    if (registration.active?.state === 'activated') {
      // 检查是否已安装
      const isStandalone = window.matchMedia('(display-mode: standalone)').matches
      const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
      if (!isStandalone && !isIOS && isMobile.value) {
        showInstallPrompt.value = true
      }
    }
  }
})

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

async function fetchDashboardData() {
  try {
    // 并行获取各模块数据
    const promises = enabledModules.value.map(async (module) => {
      if (!module.id) return
      try {
        const response = await recordApi.list({
          moduleId: module.id,
          page: 1,
          size: 100
        })
        const records = response.data
        moduleCounts.value[module.moduleKey] = records.length

        // 收集待办
        if (module.moduleKey === 'todo') {
          todoList.value = records
            .filter((r: any) => !r.dataJson?.checked)
            .slice(0, 5)
            .map((r: any) => ({ ...r, checked: r.dataJson?.checked || false }))
        }

        // 收集到期
        if (['drug', 'idoc'].includes(module.moduleKey)) {
          const now = dayjs()
          const expiring = records.filter((r: any) => {
            if (!r.remindDate) return false
            const diff = dayjs(r.remindDate).diff(now, 'day')
            return diff >= 0 && diff <= 30
          })
          expiringList.value.push(...expiring.map((r: any) => ({ ...r, moduleKey: module.moduleKey })))
        }

        // 收集最近记录
        recentRecords.value.push(...records.slice(0, 3).map((r: any) => ({ ...r, moduleKey: module.moduleKey, moduleName: module.name })))
      } catch (e) {
        console.error(`Failed to fetch ${module.moduleKey}:`, e)
      }
    })

    await Promise.all(promises)

    // 排序到期列表
    expiringList.value.sort((a, b) => dayjs(a.remindDate).diff(dayjs(b.remindDate)))
    expiringList.value = expiringList.value.slice(0, 5)

    // 排序最近记录
    recentRecords.value.sort((a, b) => dayjs(b.createdAt).diff(dayjs(a.createdAt)))
    recentRecords.value = recentRecords.value.slice(0, 8)

    // 更新指标卡片
    metrics.value.forEach(m => {
      m.value = moduleCounts.value[m.key] || 0
    })
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
  }
}

async function fetchRecentRecords() {
  recentRecords.value = []
  for (const module of enabledModules.value) {
    if (recentModuleFilter.value && module.moduleKey !== recentModuleFilter.value) continue
    try {
      const response = await recordApi.list({
        moduleId: module.id,
        page: 1,
        size: 3
      })
      recentRecords.value.push(...response.data.map((r: any) => ({ ...r, moduleKey: module.moduleKey, moduleName: module.name })))
    } catch (e) {
      console.error(e)
    }
  }
  recentRecords.value.sort((a, b) => dayjs(b.createdAt).diff(dayjs(a.createdAt)))
  recentRecords.value = recentRecords.value.slice(0, 8)
}

function getModuleCount(moduleKey: string) {
  return moduleCounts.value[moduleKey] || 0
}

function getModuleIcon(moduleKey: string) {
  const icons: Record<string, string> = {
    drug: 'FirstAidKit',
    todo: 'Checked',
    note: 'Picture',
    idoc: 'CreditCard'
  }
  return icons[moduleKey] || 'Grid'
}

function goToModule(moduleKey: string) {
  router.push(`/module/${moduleKey}`)
}

function goToDetail(record: any) {
  router.push(`/module/${record.moduleKey}/${record.id}`)
}

async function toggleTodo(item: any) {
  if (isReadonly.value) return
  try {
    await recordApi.update({
      recordId: item.id,
      moduleId: todoModule.value?.id,
      dataJson: { ...item.dataJson, checked: item.checked }
    })
    ElMessage.success(item.checked ? '已完成' : '已标记未完成')
  } catch {
    item.checked = !item.checked
    ElMessage.error('操作失败')
  }
}

function getPriorityType(priority: string) {
  switch (priority) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    default: return 'success'
  }
}

function getPriorityLabel(priority: string) {
  switch (priority) {
    case 'high': return '高'
    case 'medium': return '中'
    default: return '低'
  }
}

function getExpiringType(remindDate: string) {
  const diff = dayjs(remindDate).diff(dayjs(), 'day')
  if (diff <= 0) return 'danger'
  if (diff <= 3) return 'warning'
  return 'success'
}

function getExpiringLabel(remindDate: string) {
  const diff = dayjs(remindDate).diff(dayjs(), 'day')
  if (diff <= 0) return '已过期'
  if (diff === 1) return '明天到期'
  return `${diff}天后到期`
}

function getModuleTagType(moduleKey: string) {
  const types: Record<string, string> = {
    drug: 'primary',
    todo: 'success',
    note: 'info',
    idoc: 'warning'
  }
  return types[moduleKey] || 'info'
}

function formatDate(dateStr: string) {
  return dayjs(dateStr).format('MM-DD')
}

function formatDateTime(dateStr: string) {
  return dayjs(dateStr).format('MM-DD HH:mm')
}

function getFirstFieldValue(record: any) {
  if (!record.dataJson) return ''
  const keys = Object.keys(record.dataJson)
  if (keys.length === 0) return ''
  const val = record.dataJson[keys[0]]
  return typeof val === 'string' ? val.substring(0, 30) : String(val).substring(0, 30)
}

function handleCreateDialogClose(done: () => void) {
  createDialogModule.value = ''
  done()
}

function confirmCreate() {
  if (!createDialogModule.value) {
    ElMessage.warning('请选择记录类型')
    return
  }
  showCreateDialog.value = false
  router.push(`/module/${createDialogModule.value}/create`)
}

// 浏览器在应用可安装时抛出该事件，保存下来才能在点击时调起安装
let deferredInstallPrompt: any = null

function onBeforeInstallPrompt(e: Event) {
  e.preventDefault()
  deferredInstallPrompt = e
  showInstallPrompt.value = true
}

async function handleInstall() {
  if (!deferredInstallPrompt) {
    ElMessage.info('请点击浏览器菜单选择「添加到主屏幕」')
    return
  }

  deferredInstallPrompt.prompt()
  const { outcome } = await deferredInstallPrompt.userChoice
  if (outcome === 'accepted') {
    showInstallPrompt.value = false
    ElMessage.success('已添加到桌面')
  }
  deferredInstallPrompt = null
}

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.dashboard-page {
  .metrics-row {
    margin-bottom: 8px;
  }

  .metric-card {
    height: 100%;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-lg;
    }
  }

  .metric-content {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 8px 0;
  }

  .metric-icon {
    width: 48px;
    height: 48px;
    border-radius: $border-radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;

    &.icon-todo { background: #e1f3d8; color: #67c23a; }
    &.icon-drug { background: #fef0f0; color: #f56c6c; }
    &.icon-note { background: #e0f3ff; color: #409eff; }
    &.icon-idoc { background: #fdf6ec; color: #e6a23c; }
  }

  .metric-info {
    flex: 1;
    min-width: 0;
  }

  .metric-value {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin: 0 0 4px;
    line-height: 1;
  }

  .metric-label {
    font-size: 13px;
    color: $text-secondary;
    margin: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.content-row {
  .section-card {
    height: 100%;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;

      el-icon {
        font-size: 16px;
        color: $primary-color;
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    text-align: center;

    .empty-icon {
      font-size: 40px;
      color: $text-placeholder;
      margin-bottom: 12px;
    }

    p {
      color: $text-secondary;
      margin: 0 0 16px;
    }
  }

  .item-list {
    .list-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid $border-color-light;

      &:last-child {
        border-bottom: none;
      }

      .item-content {
        flex: 1;
        min-width: 0;
      }

      .item-title {
        font-size: 14px;
        font-weight: 500;
        color: $text-primary;
        margin: 0 0 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .item-meta {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: $text-secondary;
        margin: 0;

        el-icon {
          font-size: 12px;
        }
      }
    }

    .expiring-item {
      .item-icon {
        width: 36px;
        height: 36px;
        border-radius: $border-radius-base;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        flex-shrink: 0;

        &.drug { background: #fef0f0; color: #f56c6c; }
        &.idoc { background: #fdf6ec; color: #e6a23c; }
      }
    }
  }
}

.recent-section {
  .recent-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }

  .recent-card {
    padding: 16px;
    background: #fff;
    border: 1px solid $border-color;
    border-radius: $border-radius-base;
    transition: all 0.2s ease;
    cursor: pointer;

    &:hover {
      border-color: $primary-color;
      box-shadow: $shadow-base;
      transform: translateY(-2px);
    }

    .recent-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;

      .recent-time {
        font-size: 12px;
        color: $text-placeholder;
      }
    }

    .recent-title {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
      margin: 0 0 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .recent-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }
}

.shortcuts-card {
  .shortcuts-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .shortcut-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 20px 12px;
    background: #fff;
    border: 1px solid $border-color;
    border-radius: $border-radius-base;
    text-decoration: none;
    color: $text-primary;
    transition: all 0.2s ease;
    position: relative;

    &:hover {
      border-color: $primary-color;
      color: $primary-color;
      transform: translateY(-2px);
      box-shadow: $shadow-base;
    }

    .shortcut-icon {
      width: 48px;
      height: 48px;
      border-radius: $border-radius-base;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;

      &.drug { background: #fef0f0; color: #f56c6c; }
      &.todo { background: #e1f3d8; color: #67c23a; }
      &.note { background: #e0f3ff; color: #409eff; }
      &.idoc { background: #fdf6ec; color: #e6a23c; }
    }

    .shortcut-name {
      font-size: 14px;
      font-weight: 500;
      text-align: center;
    }

    .shortcut-count {
      position: absolute;
      top: 8px;
      right: 8px;
      min-width: 18px;
      height: 18px;
      border-radius: 9px;
      background: $primary-color;
      color: #fff;
      font-size: 11px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 5px;
    }
  }

  .add-shortcut {
    border-style: dashed;
    border-color: $border-color;

    &:hover {
      border-color: $primary-color;
      background: $primary-light-bg;
    }

    .add-icon {
      font-size: 28px;
      color: $text-placeholder;

      &:hover {
        color: $primary-color;
      }
    }
  }
}

.pwa-install-card {
  background: $primary-light-bg;
  border-color: $primary-light-border;

  .pwa-install-text {
    font-size: 13px;
    color: $text-secondary;
    margin: 0 0 16px;
    line-height: 1.6;
  }
}

.module-select-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.module-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: #fff;
  border: 2px solid $border-color;
  border-radius: $border-radius-base;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: $primary-color;
  }

  &.selected {
    border-color: $primary-color;
    background: $primary-light-bg;
  }

  .module-select-icon {
    width: 44px;
    height: 44px;
    border-radius: $border-radius-base;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;

    &.drug { background: #fef0f0; color: #f56c6c; }
    &.todo { background: #e1f3d8; color: #67c23a; }
    &.note { background: #e0f3ff; color: #409eff; }
    &.idoc { background: #fdf6ec; color: #e6a23c; }
  }

  span {
    font-size: 13px;
    font-weight: 500;
    color: $text-primary;
  }
}

// 响应式
@media (max-width: 767px) {
  .dashboard-page {
    .metrics-row {
      :deep(.el-col) {
        margin-bottom: 12px;
      }
    }

    .metric-content {
      gap: 12px;
    }

    .metric-icon {
      width: 40px;
      height: 40px;
      font-size: 18px;
    }

    .metric-value {
      font-size: 20px;
    }

    .recent-grid {
      grid-template-columns: 1fr !important;
    }

    .shortcuts-grid {
      grid-template-columns: repeat(3, 1fr) !important;
    }

    .shortcut-item {
      padding: 16px 8px;
    }
  }
}

// 长辈模式
.elderly-mode .dashboard-page {
  .metric-value { font-size: 28px !important; }
  .metric-label { font-size: 16px !important; }
  .item-title { font-size: 16px !important; }
  .item-meta { font-size: 14px !important; }
  .recent-title { font-size: 16px !important; }
  .recent-time { font-size: 14px !important; }
  .shortcut-name { font-size: 16px !important; }
  .section-card :deep(.el-card__header) { padding: 16px 20px !important; }
}
</style>