<template>
  <div class="dashboard-page">
    <section class="welcome-block">
      <div class="welcome-row">
        <div class="welcome-copy">
          <div class="welcome-topline">
            <span class="welcome-badge"><el-icon><Sunny /></el-icon></span>
            <span>{{ greetingText }} · {{ weekdayText }} · 和家人一起慢慢来</span>
          </div>

          <h1>欢迎回家，{{ displayName }}</h1>
          <p>把家里的小事，一件一件安心收好。</p>
        </div>

        <div class="date-card">
          <span class="date-label">今天</span>
          <strong>{{ monthDayText }}</strong>
          <small>{{ yearWeekdayText }} · {{ timeText }}</small>
        </div>
      </div>
    </section>

    <div v-loading="loading" class="panel-grid upper-grid">
      <section class="soft-card todo-card">
        <header class="card-header">
          <div class="title-wrap">
            <div class="mini-icon orange"><el-icon><Checked /></el-icon></div>
            <div class="title-copy">
              <h2>今日待办</h2>
              <small>把今天的小事安排好</small>
            </div>
          </div>

          <button type="button" class="text-link" @click="goToModule('todo')">查看全部 <span class="link-arrow" aria-hidden="true">›</span></button>
        </header>

        <div class="card-body">
          <div v-if="!loading && todoList.length === 0" class="empty-state">
            <div class="empty-figure">
              <EmptyIllustration variant="todo" :size="170" />
            </div>
            <h3>还没有今日待办</h3>
            <p>把家务、采购、家人约定记在这里，不错过重要小事</p>
            <button class="primary-btn" type="button" @click="goToModule('todo')">添加第一个待办</button>
          </div>

          <div v-else class="simple-list">
            <article
              v-for="item in todoList"
              :key="item.id"
              class="simple-item"
              role="button"
              tabindex="0"
              @click="goToDetail(item)"
              @keyup.enter="goToDetail(item)"
            >
              <span class="simple-dot"></span>
              <div class="simple-item-body">
                <h3>{{ item.title || getFirstFieldValue(item) }}</h3>
                <p>{{ getRecordSummary(item) }}</p>
              </div>
              <span class="simple-arrow">›</span>
            </article>
          </div>
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

          <button type="button" class="text-link" @click="goToModule('drug')">查看全部 <span class="link-arrow" aria-hidden="true">›</span></button>
        </header>

        <div v-if="!loading && expiringList.length === 0" class="empty-state">
          <div class="empty-figure">
            <EmptyIllustration variant="alert" :size="170" />
          </div>
          <h3>暂无即将到期项目</h3>
          <p>药品有效期、证件到期会在这里提前提醒你</p>
          <button class="primary-btn" type="button" @click="goToModule('drug')">新增提醒</button>
        </div>

        <div v-else class="simple-list">
          <article
            v-for="item in expiringList"
            :key="`${item.moduleKey}-${item.id}`"
            class="simple-item"
            role="button"
            tabindex="0"
            @click="goToDetail(item)"
            @keyup.enter="goToDetail(item)"
          >
            <span class="module-badge" :class="getModuleColorClass(item.moduleKey)">{{ item.moduleName }}</span>
            <div class="simple-item-body">
              <h3>{{ item.title || getFirstFieldValue(item) }}</h3>
              <p>{{ getRecordSummary(item) }}</p>
            </div>
            <span class="expire-badge" :class="getExpiringClass(item.remindDate)">
              {{ getExpiringLabel(item.remindDate) }}
            </span>
          </article>
        </div>
      </section>
    </div>

    <div v-loading="loading" class="panel-grid lower-grid">
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

        <EmptyState v-if="!loading && recentRecords.length === 0" compact title="暂无记录" description="新记录会显示在这里" />

        <div v-else class="record-list">
          <article
            v-for="record in recentRecords"
            :key="record.id"
            class="record-item"
            role="button"
            tabindex="0"
            @click="goToDetail(record)"
            @keyup.enter="goToDetail(record)"
          >
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

      <aside class="soft-card shortcut-panel" :class="{ 'is-collapsed': shortcutCollapsed }">
        <header class="card-header shortcut-header-row">
          <div class="title-wrap">
            <div class="mini-icon blue"><el-icon><Grid /></el-icon></div>
            <div class="title-copy">
              <h2>快捷入口</h2>
              <small>常用的家庭空间</small>
            </div>
          </div>

          <button
            type="button"
            class="collapse-circle"
            :class="{ collapsed: shortcutCollapsed }"
            :aria-expanded="!shortcutCollapsed"
            aria-label="收起快捷入口"
            @click="shortcutCollapsed = !shortcutCollapsed"
          >
            <el-icon><ArrowLeft /></el-icon>
          </button>
        </header>

        <div v-show="!shortcutCollapsed" class="shortcut-body">
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
        </div>
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
import { recordApi, dashboardApi } from '@/api'
import { Checked, AlarmClock, Clock, Sunny, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { formatDateTime, dayDiffFromToday } from '@/utils/date'
import { getModuleIcon, getModuleColorClass } from '@/utils/module'
import EmptyIllustration from '@/components/ui/EmptyIllustration.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 响应式状态
const todoList = ref<any[]>([])
const expiringList = ref<any[]>([])
const recentRecords = ref<any[]>([])
const recentModuleFilter = ref('')
const loading = ref(false)

const showCreateDialog = ref(false)
const createDialogModule = ref('')

// 卡片收起状态：标题栏按钮直接折叠，不弹确认
const shortcutCollapsed = ref(false)

const enabledModules = computed(() => moduleStore.enabledModules)
const homeModules = computed(() => moduleStore.homeModules)

// 当前日期时间：挂载后按秒刷新，跨天/跨时段时问候语也能实时更新
const now = ref(dayjs())
const WEEKDAY_LABELS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const weekdayText = computed(() => WEEKDAY_LABELS[now.value.day()])
const monthDayText = computed(() => now.value.format('M月D日'))
const yearWeekdayText = computed(() => `${now.value.format('YYYY年')} · ${weekdayText.value}`)
const timeText = computed(() => now.value.format('HH:mm'))
const greetingText = computed(() => {
  const hour = now.value.hour()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 登录人昵称（优先昵称，其次用户名）
const displayName = computed(() => authStore.user?.nickName || authStore.user?.username || '家人')

let clockTimer: ReturnType<typeof setInterval> | null = null

function refreshNow() {
  now.value = dayjs()
}

onMounted(async () => {
  refreshNow()
  clockTimer = setInterval(refreshNow, 1000)

  await moduleStore.fetchModules()
  await fetchDashboardData()
})

async function fetchDashboardData() {
  loading.value = true
  try {
    todoList.value = []
    expiringList.value = []
    recentRecords.value = []

    // 首页聚合接口：一次请求拿待办、到期、最近记录
    const response = await dashboardApi.summary()
    const summary = response.data || {}

    todoList.value = summary.todos || []
    expiringList.value = summary.expiring || []
    recentRecords.value = (summary.recent || []).map((record: any) => ({
      ...record,
      summary: getRecordSummary(record)
    }))
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
  } finally {
    loading.value = false
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
      const records = response.data?.data || []
      recentRecords.value.push(...records.map((r: any) => ({ ...r, moduleKey: module.moduleKey, moduleName: module.name })))
    } catch (e) {
      console.error(e)
    }
  }
  recentRecords.value.sort((a, b) => dayjs(b.createdAt).diff(dayjs(a.createdAt)))
  recentRecords.value = recentRecords.value.slice(0, 8)
}

function goToModule(moduleKey: string) {
  router.push(`/module/${moduleKey}`)
}

function goToDetail(record: any) {
  router.push(`/module/${record.moduleKey}/${record.id}`)
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

function getExpiringLabel(remindDate: string) {
  const diff = dayDiffFromToday(remindDate)
  if (diff === null) return '日期未知'
  if (diff < 0) return '已过期'
  if (diff === 0) return '今天到期'
  if (diff === 1) return '明天到期'
  return `${diff}天后到期`
}

function getExpiringClass(remindDate: string) {
  const diff = dayDiffFromToday(remindDate)
  if (diff === null) return ''
  if (diff <= 0) return 'danger'
  if (diff <= 3) return 'warning'
  return ''
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

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/decorations' as *;

.dashboard-page {
  width: 100%;
  min-height: 100%;
  background: $bg-color;
  color: $text-primary;
}

.welcome-block {
  padding: 10px 0 18px;
}

.welcome-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: $text-tertiary;
  font-size: 13px;
  letter-spacing: 0.02em;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: $primary-color;
  font-size: 15px;
  flex-shrink: 0;
}

.welcome-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.welcome-copy {
  h1 {
    margin: 0;
    font-size: $text-display;
    line-height: 1.15;
    letter-spacing: -0.02em;
    font-weight: $font-weight-bold;
    color: $text-primary;
  }

  p {
    margin: 8px 0 0;
    color: $text-tertiary;
    font-size: 15px;
  }
}

.date-card {
  min-width: 162px;
  background: $surface-warm;
  border: 1px solid $border-color;
  border-radius: $border-radius-lg;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: none;

  .date-label {
    margin-bottom: 6px;
    color: $text-tertiary;
    font-size: 12px;
  }

  strong {
    color: $text-primary;
    font-size: 26px;
    line-height: 1.1;
    letter-spacing: -0.04em;
  }

  small {
    margin-top: 6px;
    color: $text-tertiary;
    font-size: 12px;
  }
}

.panel-grid {
  display: grid;
  gap: 20px;
}

.upper-grid {
  // 今日待办承载计数胶囊，横向空间需求更大，故比「即将到期」更宽
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  margin-bottom: 20px;
}

.lower-grid {
  grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr);
}

.soft-card {
  background: $surface-warm;
  border: 1px solid $border-color;
  border-radius: 18px;
  padding: 18px 20px;

  // 收起后标题栏不再保留下边距，避免卡片底部多出一条空隙
  &.is-collapsed .card-header {
    margin-bottom: 0;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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
    background: rgba($primary-color, 0.12);
    color: $primary-text;
  }

  &.purple {
    background: rgba($accent-purple, 0.12);
    color: $accent-purple;
  }

  &.blue {
    background: rgba($module-note, 0.12);
    color: $module-note;
  }
}

.title-copy {
  h2 {
    margin: 0;
    line-height: 1.2;
    font-size: 18px;
    color: $text-primary;
    font-weight: 700;
  }

  small {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: $text-tertiary;
    white-space: nowrap;
  }
}

.text-link {
  border: none;
  background: transparent;
  color: $primary-text;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;

  .link-arrow {
    font-size: 14px;
    line-height: 1;
  }

  &:hover {
    color: $primary-hover;
  }
}

// 「快捷入口」标题栏的收起圆钮
.collapse-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  flex-shrink: 0;
  border: 1px solid $border-color-light;
  border-radius: 50%;
  background: $card-bg;
  color: $text-placeholder;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;

  &:hover {
    background: $hover-bg;
    border-color: rgba($primary-color, 0.3);
    color: $primary-text;
  }

  &.collapsed {
    transform: rotate(180deg);
  }
}

.empty-figure {
  margin-bottom: 18px;
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
    color: $text-primary;
    font-weight: 700;
  }

  p {
    margin: 0 0 16px;
    max-width: 360px;
    line-height: 1.7;
    color: $text-tertiary;
    font-size: 13px;
  }
}

.primary-btn {
  border: none;
  background: $gradient-primary;
  color: $card-bg;
  min-width: 150px;
  height: 38px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 22px rgba($primary-color, 0.18);
}

.simple-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.simple-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid rgba($text-primary, 0.05);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: $card-bg;
    border-color: rgba($primary-color, 0.25);
  }
}

.simple-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $primary-color;
  flex-shrink: 0;
}

.simple-item-body {
  flex: 1;
  min-width: 0;

  h3 {
    margin: 0 0 3px;
    color: $text-primary;
    font-size: 14px;
    font-weight: 700;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 0;
    color: $text-tertiary;
    font-size: 12px;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.simple-arrow {
  color: rgba($text-primary, 0.45);
  font-size: 22px;
  line-height: 1;
}

.expire-badge {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba($module-drug, 0.12);
  color: $success-text;
  font-size: 11px;
  font-weight: 700;

  &.warning {
    background: rgba($warning-color, 0.14);
    color: $warning-text;
  }

  &.danger {
    background: rgba($danger-color, 0.14);
    color: $danger-text;
  }
}

.recent-header {
  margin-bottom: 12px;
}

.filter-select {
  width: 90px;

  :deep(.el-input__wrapper) {
    height: 32px;
    border-radius: 8px;
    border: 1px solid rgba($text-primary, 0.05);
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
  border: 1px solid rgba($text-primary, 0.04);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.52);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.8);
    box-shadow: 0 10px 18px rgba($text-primary, 0.04);
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

  &.note { background: rgba($module-note, 0.12); color: $module-note; }
  &.drug { background: rgba($module-drug, 0.12); color: $success-text; }
  &.todo { background: rgba($primary-color, 0.12); color: $module-todo; }
  &.idoc { background: rgba($module-idoc, 0.12); color: $module-idoc; }
}

.record-meta {
  color: $text-tertiary;
  font-size: 11px;
}

.record-body {
  min-width: 0;

  h3 {
    margin: 0 0 6px;
    color: $text-primary;
    font-size: 15px;
    font-weight: 700;
    line-height: 1.4;
  }

  p {
    margin: 0;
    color: rgba($text-primary, 0.66);
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
    background: rgba($primary-color, 0.08);
    color: $primary-text;
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
  border: 1px solid rgba($text-primary, 0.06);
  border-radius: 12px;
  background: $surface-warm;
  color: $text-primary;
  text-align: left;
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    border-color: rgba($primary-color, 0.34);
    background: $surface-warm-soft;
    transform: translateY(-2px);
    box-shadow: $shadow-hover;
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

  &.drug { background: rgba($module-drug, 0.08); color: $success-text; }
  &.todo { background: rgba($primary-color, 0.1); color: $primary-text; }
  &.note { background: rgba($module-note, 0.08); color: $module-note; }
  &.idoc { background: rgba($module-idoc, 0.08); color: $module-idoc; }
}

.shortcut-name {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
}

.shortcut-arrow {
  color: rgba($text-primary, 0.52);
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
  background: rgba($primary-color, 0.08);
  color: $primary-text;
  font-size: 22px;
  line-height: 1;
}

.shortcut-footer {
  margin: 16px 2px 0;
  color: $text-tertiary;
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
  border: 1px solid rgba($text-primary, 0.06);
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba($primary-color, 0.34);
  }

  &.selected {
    background: rgba($primary-color, 0.06);
    border-color: rgba($primary-color, 0.34);
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

  &.drug { background: rgba($module-drug, 0.08); color: $success-text; }
  &.todo { background: rgba($primary-color, 0.10); color: $primary-text; }
  &.note { background: rgba($module-note, 0.08); color: $module-note; }
  &.idoc { background: rgba($module-idoc, 0.08); color: $module-idoc; }
}

@media (max-width: 1023px) {
  .upper-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }

  .record-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 639px) {
  .welcome-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .date-card {
    width: 100%;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
