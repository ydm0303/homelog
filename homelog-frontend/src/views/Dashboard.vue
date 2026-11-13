<template>
  <div class="dashboard-page">
    <section class="welcome-block">
      <div class="welcome-topline">
        <span class="welcome-badge">✦</span>
        <span>星期四 · 和家人一起慢慢来</span>
      </div>

      <div class="welcome-row">
        <div class="welcome-copy">
          <h1>欢迎回家，杨东铭</h1>
          <p>把家里的小事，一件一件安心收好。</p>
        </div>

        <div class="date-card">
          <span class="date-label">今天</span>
          <strong>11月12日</strong>
          <small>2026年 · 星期四</small>
        </div>
      </div>
    </section>

    <div class="panel-grid upper-grid">
      <section class="soft-card todo-card">
        <header class="card-header">
          <div class="title-wrap">
            <div class="mini-icon orange"><el-icon><Checked /></el-icon></div>
            <div class="title-copy">
              <h2>今日待办</h2>
              <small>把今天的小事安排好</small>
            </div>
          </div>

          <div class="header-right">
            <div class="stat-chips">
              <span>待办总数</span>
              <span>药品数量</span>
              <span>记事条数</span>
              <span>证件数量</span>
            </div>
            <button type="button" class="text-link" @click="goToModule('todo')">查看全部</button>
          </div>
        </header>

        <div v-if="todoList.length === 0" class="empty-state">
          <div class="empty-illustration todo-illustration">
            <span class="mini-house"></span>
            <span class="mini-plant plant-left"></span>
            <span class="mini-plant plant-right"></span>
            <span class="mini-bubble">待办</span>
          </div>
          <h3>还没有今日待办</h3>
          <p>把家务、采购、家人约定记在这里，不错过重要小事</p>
          <button class="primary-btn" type="button" @click="goToModule('todo')">添加第一个待办</button>
        </div>
      </section>

      <section class="soft-card expiring-card">
        <header class="card-header">
          <div class="title-wrap">
            <div class="mini-icon orange"><el-icon><AlarmClock /></el-icon></div>
            <div class="title-copy">
              <h2>即将到期</h2>
              <small>重要日期，提前放在心上</small>
            </div>
          </div>

          <button type="button" class="text-link" @click="goToModule('drug')">查看全部</button>
        </header>

        <div v-if="expiringList.length === 0" class="empty-state">
          <div class="empty-illustration alert-illustration">
            <span class="mini-clock"></span>
            <span class="mini-alert">!</span>
          </div>
          <h3>暂无即将到期项目</h3>
          <p>药品有效期、证件到期会在这里提前提醒你</p>
          <button class="primary-btn" type="button" @click="goToModule('drug')">新增提醒</button>
        </div>
      </section>
    </div>

    <div class="panel-grid lower-grid">
      <section class="soft-card recent-card-panel">
        <header class="card-header recent-header">
          <div class="title-wrap">
            <div class="mini-icon purple"><el-icon><Clock /></el-icon></div>
            <div class="title-copy">
              <h2>最近记录</h2>
              <small>家里的点滴，都有迹可循</small>
            </div>
          </div>

          <el-select
            v-model="recentModuleFilter"
            placeholder="筛选模块"
            size="small"
            class="filter-select"
            @change="fetchRecentRecords"
          >
            <el-option label="全部模块" value="" />
            <el-option v-for="module in enabledModules" :key="module.id" :label="module.name" :value="module.moduleKey" />
          </el-select>
        </header>

        <div v-if="recentRecords.length === 0" class="empty-state compact-empty">
          <h3>暂无记录</h3>
        </div>

        <div v-else class="record-list">
          <article v-for="record in recentRecords" :key="record.id" class="record-item" @click="goToDetail(record)">
            <div class="record-left">
              <span class="module-badge" :class="getModuleColorClass(record.moduleKey)">{{ record.moduleName }}</span>
              <div class="record-meta">{{ formatDateTime(record.createdAt) }}</div>
            </div>
            <div class="record-body">
              <h3>{{ record.title || getFirstFieldValue(record) }}</h3>
              <p>{{ record.summary || getRecordSummary(record) }}</p>
            </div>
            <div class="record-actions">
              <button type="button" aria-label="查看详情">↗</button>
            </div>
          </article>
        </div>
      </section>

      <aside class="soft-card shortcut-panel">
        <header class="card-header shortcut-header-row">
          <div class="title-wrap">
            <div class="mini-icon blue"><el-icon><Grid /></el-icon></div>
            <div class="title-copy">
              <h2>快捷入口</h2>
              <small>常用的家庭空间</small>
            </div>
          </div>
        </header>

        <div class="shortcut-grid">
          <button v-for="module in homeModules" :key="module.id" class="shortcut-item" type="button" @click="goToModule(module.moduleKey)">
            <span class="shortcut-icon" :class="module.moduleKey">
              <el-icon><component :is="getModuleIcon(module.moduleKey)" /></el-icon>
            </span>
            <span class="shortcut-name">{{ module.name }}</span>
            <span class="shortcut-arrow">›</span>
          </button>

          <button class="shortcut-item add-shortcut" type="button" @click="showCreateDialog = true">
            <span class="add-box">＋</span>
            <span class="shortcut-name">新增记录</span>
          </button>
        </div>

        <p class="shortcut-footer">每一份用心，都是家的小小光亮。</p>
      </aside>
    </div>

    <el-dialog v-model="showCreateDialog" title="选择要新增的记录类型" width="500px" :before-close="handleCreateDialogClose">
      <div class="module-select-grid">
        <div v-for="module in enabledModules" :key="module.id" class="module-select-item" :class="{ selected: createDialogModule === module.moduleKey }" @click="createDialogModule = module.moduleKey">
          <div class="module-select-icon" :class="module.moduleKey">
            <el-icon><component :is="getModuleIcon(module.moduleKey)" /></el-icon>
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
const showMetricStrip = ref(true)

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

    // 为记录面板增加摘要（避免空文本）
    recentRecords.value = recentRecords.value.map((record: any) => ({
      ...record,
      summary: getRecordSummary(record)
    }))

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
  return dayjs(dateStr).format('MM-DD HH:mm · YYYY')
}

function getFirstFieldValue(record: any) {
  if (!record.dataJson) return ''
  const keys = Object.keys(record.dataJson)
  if (keys.length === 0) return ''
  const val = record.dataJson[keys[0]]
  return typeof val === 'string' ? val.substring(0, 30) : String(val).substring(0, 30)
}

function getRecordSummary(record: any) {
  if (record.summary) return record.summary
  if (!record.dataJson) return '家里的点滴，认真记好每一刻。'
  const text = Object.values(record.dataJson).filter((value) => typeof value === 'string').join(' ')
  return text ? text.slice(0, 80) : '家里的点滴，认真记好每一刻。'
}

function getModuleColorClass(moduleKey: string) {
  const map: Record<string, string> = {
    note: 'note',
    drug: 'drug',
    todo: 'todo',
    idoc: 'idoc'
  }
  return map[moduleKey] || 'note'
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
  width: 100%;
  min-height: 100%;
  background: #f7f3ed;
  color: #2b2622;
}

.welcome-block {
  padding: 10px 0 18px;
}

.welcome-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #7c746d;
  font-size: 13px;
  letter-spacing: 0.02em;
}

.welcome-badge {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(221, 112, 78, 0.08);
  color: #dd704e;
  font-size: 12px;
}

.welcome-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.welcome-copy {
  h1 {
    margin: 0;
    font-size: clamp(2.25rem, 2.5vw, 3.1rem);
    line-height: 1.08;
    letter-spacing: -0.05em;
    font-weight: 800;
    color: #2b2622;
  }

  p {
    margin: 10px 0 0;
    color: #7c746d;
    font-size: 15px;
  }
}

.date-card {
  min-width: 162px;
  background: #fffbf7;
  border: 1px solid rgba(43, 38, 34, 0.05);
  border-radius: 16px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 22px rgba(221, 112, 78, 0.05);

  .date-label {
    margin-bottom: 6px;
    color: #7c746d;
    font-size: 12px;
  }

  strong {
    color: #2b2622;
    font-size: 26px;
    line-height: 1.1;
    letter-spacing: -0.04em;
  }

  small {
    margin-top: 6px;
    color: #7c746d;
    font-size: 12px;
  }
}

.panel-grid {
  display: grid;
  gap: 20px;
}

.upper-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 20px;
}

.lower-grid {
  grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr);
}

.soft-card {
  background: #fffbf7;
  border: 1px solid rgba(43, 38, 34, 0.04);
  border-radius: 16px;
  box-shadow: 0 16px 30px rgba(221, 112, 78, 0.04);
  padding: 18px 20px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mini-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;

  &.orange {
    background: rgba(221, 112, 78, 0.12);
    color: #dd704e;
  }

  &.purple {
    background: rgba(153, 129, 255, 0.12);
    color: #8d76d8;
  }

  &.blue {
    background: rgba(84, 150, 255, 0.12);
    color: #5f8de7;
  }
}

.title-copy {
  h2 {
    margin: 0;
    line-height: 1.2;
    font-size: 18px;
    color: #2b2622;
    font-weight: 700;
  }

  small {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: #7c746d;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stat-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;

  span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 22px;
    padding: 3px 8px;
    border-radius: 999px;
    background: #fff9f5;
    border: 1px solid #f5e2d8;
    color: #7c746d;
    font-size: 10px;
  }
}

.text-link {
  border: none;
  background: transparent;
  color: #dd704e;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 250px;
  padding: 10px 18px 4px;
  text-align: center;

  h3 {
    margin: 0 0 8px;
    font-size: 17px;
    color: #2b2622;
    font-weight: 700;
  }

  p {
    margin: 0 0 16px;
    max-width: 360px;
    line-height: 1.7;
    color: #7c746d;
    font-size: 13px;
  }
}

.empty-illustration {
  position: relative;
  width: 170px;
  height: 110px;
  margin-bottom: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff9f5 0%, #f7f3ed 100%);
  border: 1px solid rgba(221, 112, 78, 0.08);
}

.mini-house {
  position: absolute;
  left: 50%;
  bottom: 18px;
  width: 86px;
  height: 52px;
  border-radius: 12px 12px 10px 10px;
  background: linear-gradient(180deg, #f4c785 0%, #efa75c 100%);
  transform: translateX(-50%);
}

.mini-house::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -26px;
  width: 68px;
  height: 34px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #f8b56f 0%, #eb8f5d 100%);
  clip-path: polygon(50% 0, 100% 38%, 100% 100%, 0 100%, 0 38%);
  border-radius: 8px 8px 0 0;
}

.mini-plant {
  position: absolute;
  bottom: 10px;
  width: 17px;
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(180deg, #d7efc7 0%, #b7d79a 100%);
}

.mini-plant::before,
.mini-plant::after {
  content: '';
  position: absolute;
  bottom: 16px;
  width: 8px;
  height: 20px;
  border-radius: 8px;
  background: linear-gradient(180deg, #dfeecb 0%, #afd18f 100%);
}

.mini-plant::before { left: 1px; transform: rotate(-18deg); }
.mini-plant::after { right: 1px; transform: rotate(18deg); }

.plant-left { left: 30px; }
.plant-right { right: 30px; }

.mini-bubble {
  position: absolute;
  right: 22px;
  top: 18px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #dd704e;
  font-size: 11px;
  font-weight: 700;
  box-shadow: 0 8px 16px rgba(43, 38, 34, 0.06);
}

.mini-clock {
  position: absolute;
  left: 50%;
  top: 22px;
  width: 54px;
  height: 54px;
  transform: translateX(-50%);
  border: 3px solid rgba(221, 112, 78, 0.45);
  border-radius: 50%;
}

.mini-clock::before,
.mini-clock::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  background: #dd704e;
  border-radius: 2px;
  transform-origin: center bottom;
}

.mini-clock::before {
  width: 2px;
  height: 18px;
  transform: translate(-50%, -100%) rotate(38deg);
}

.mini-clock::after {
  width: 2px;
  height: 12px;
  transform: translate(-50%, -100%) rotate(130deg);
}

.mini-alert {
  position: absolute;
  right: 30px;
  bottom: 18px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #fbe2e2 0%, #f9d8d8 100%);
  color: #d56767;
  font-weight: 800;
  box-shadow: 0 10px 20px rgba(245, 108, 108, 0.12);
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, #dd704e 0%, #d9633d 100%);
  color: #fff;
  min-width: 150px;
  height: 38px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 22px rgba(221, 112, 78, 0.18);
}

.recent-header {
  margin-bottom: 12px;
}

.filter-select {
  width: 140px;

  :deep(.el-input__wrapper) {
    height: 32px;
    border-radius: 8px;
    border: 1px solid rgba(43, 38, 34, 0.05);
    background: rgba(255, 255, 255, 0.8);
    box-shadow: none;
  }
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  display: grid;
  grid-template-columns: 1.3fr minmax(0, 2.8fr) 40px;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(43, 38, 34, 0.04);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.52);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.8);
    box-shadow: 0 10px 18px rgba(43, 38, 34, 0.04);
  }
}

.record-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.module-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 20px;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;

  &.note { background: rgba(112, 161, 255, 0.12); color: #5a86d7; }
  &.drug { background: rgba(118, 175, 103, 0.12); color: #4e8a4c; }
  &.todo { background: rgba(221, 112, 78, 0.12); color: #d06a47; }
  &.idoc { background: rgba(216, 110, 110, 0.12); color: #c76767; }
}

.record-meta {
  color: #7c746d;
  font-size: 11px;
}

.record-body {
  min-width: 0;

  h3 {
    margin: 0 0 6px;
    color: #2b2622;
    font-size: 15px;
    font-weight: 700;
    line-height: 1.4;
  }

  p {
    margin: 0;
    color: rgba(43, 38, 34, 0.66);
    font-size: 12px;
    line-height: 1.65;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.record-actions {
  display: flex;
  justify-content: center;

  button {
    width: 26px;
    height: 26px;
    border: none;
    border-radius: 8px;
    background: rgba(221, 112, 78, 0.08);
    color: #dd704e;
    cursor: pointer;
    opacity: 0;
    transition: all 0.2s ease;
  }
}

.record-item:hover .record-actions button {
  opacity: 1;
}

.shortcut-panel {
  padding-bottom: 12px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.shortcut-item {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  align-items: center;
  gap: 8px;
  min-height: 72px;
  padding: 12px 10px 12px 12px;
  border: 1px solid rgba(43, 38, 34, 0.06);
  border-radius: 12px;
  background: #fffbf7;
  color: #2b2622;
  text-align: left;
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    border-color: rgba(221, 112, 78, 0.34);
    background: #fff7f2;
    transform: translateY(-1px);
  }
}

.shortcut-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 18px;

  &.drug { background: rgba(81, 163, 92, 0.08); color: #4e8a4c; }
  &.todo { background: rgba(221, 112, 78, 0.1); color: #dd704e; }
  &.note { background: rgba(94, 136, 255, 0.08); color: #5a86d7; }
  &.idoc { background: rgba(214, 111, 111, 0.08); color: #c76767; }
}

.shortcut-name {
  font-size: 13px;
  font-weight: 600;
  color: #2b2622;
}

.shortcut-arrow {
  color: rgba(43, 38, 34, 0.52);
  font-size: 24px;
  line-height: 1;
}

.add-shortcut {
  border-style: dashed;
  background: rgba(255, 255, 255, 0.2);
}

.add-box {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(221, 112, 78, 0.08);
  color: #dd704e;
  font-size: 22px;
  line-height: 1;
}

.shortcut-footer {
  margin: 16px 2px 0;
  color: #7c746d;
  font-size: 11px;
  text-align: center;
  letter-spacing: 0.02em;
}

.module-select-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.module-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 14px;
  border-radius: 12px;
  border: 1px solid rgba(43, 38, 34, 0.06);
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba(221, 112, 78, 0.34);
  }

  &.selected {
    background: rgba(221, 112, 78, 0.06);
    border-color: rgba(221, 112, 78, 0.34);
  }
}

.module-select-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;

  &.drug { background: rgba(81, 163, 92, 0.08); color: #4e8a4c; }
  &.todo { background: rgba(221, 112, 78, 0.10); color: #dd704e; }
  &.note { background: rgba(94, 136, 255, 0.08); color: #5a86d7; }
  &.idoc { background: rgba(214, 111, 111, 0.08); color: #c76767; }
}

@media (max-width: 980px) {
  .upper-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }

  .record-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .welcome-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .date-card {
    width: 100%;
  }

  .stat-chips {
    justify-content: flex-start;
  }

  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>