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

    <div class="panel-grid upper-grid">
      <AppCard class="soft-card panel-todo" :icon="Checked" title="今日待办" description="把今天的小事安排好">
        <template #actions>
          <button type="button" class="text-link" @click="goToModule('todo')">
            查看全部 <span class="link-arrow" aria-hidden="true">›</span>
          </button>
        </template>

        <AsyncState
          :loading="summaryLoading"
          :error="summaryError"
          :empty="todoList.length === 0"
          empty-title="还没有今日待办"
          empty-description="把家务、采购、家人约定记在这里，不错过重要小事"
          :rows="3"
          min-height="250px"
          @retry="fetchDashboardData"
        >
          <template #figure>
            <EmptyIllustration variant="todo" :size="170" />
          </template>
          <template #empty-actions>
            <el-button type="primary" @click="goToModule('todo')">添加第一个待办</el-button>
          </template>

          <div class="simple-list">
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
              <span class="simple-arrow" aria-hidden="true">›</span>
            </article>
          </div>
        </AsyncState>
      </AppCard>

      <AppCard class="soft-card panel-expiring" :icon="AlarmClock" title="即将到期" description="重要日期，提前放在心上">
        <template #actions>
          <button type="button" class="text-link" @click="goToModule('drug')">
            查看全部 <span class="link-arrow" aria-hidden="true">›</span>
          </button>
        </template>

        <AsyncState
          :loading="summaryLoading"
          :error="summaryError"
          :empty="expiringList.length === 0"
          empty-title="暂无即将到期项目"
          empty-description="药品有效期、证件到期会在这里提前提醒你"
          :rows="3"
          min-height="250px"
          @retry="fetchDashboardData"
        >
          <template #figure>
            <EmptyIllustration variant="alert" :size="170" />
          </template>
          <template #empty-actions>
            <el-button type="primary" @click="goToModule('drug')">新增提醒</el-button>
          </template>

          <div class="simple-list">
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
        </AsyncState>
      </AppCard>
    </div>

    <div class="panel-grid lower-grid">
      <AppCard class="soft-card panel-recent" :icon="Clock" title="最近记录" description="家里的点滴，都有迹可循">
        <template #actions>
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
        </template>

        <AsyncState
          :loading="recentLoading"
          :error="recentError"
          :empty="recentRecords.length === 0"
          empty-title="暂无记录"
          empty-description="新记录会显示在这里"
          :rows="4"
          min-height="280px"
          @retry="fetchRecentRecords"
        >
          <div class="record-list">
            <article
              v-for="record in visibleRecentRecords"
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
                <button type="button" aria-label="查看详情" @click.stop="goToDetail(record)">↗</button>
              </div>
            </article>

            <button
              v-if="recentRecords.length > RECENT_LIMIT"
              type="button"
              class="record-toggle"
              @click="recentExpanded = !recentExpanded"
            >
              {{ recentExpanded ? '收起' : `展开全部 ${recentRecords.length} 条` }}
              <el-icon><ArrowUp v-if="recentExpanded" /><ArrowDown v-else /></el-icon>
            </button>
          </div>
        </AsyncState>
      </AppCard>

      <AppCard
        class="soft-card shortcut-panel panel-shortcut"
        :class="{ 'is-collapsed': shortcutCollapsed }"
        :icon="Grid"
        title="快捷入口"
        description="常用的家庭空间"
      >
        <template #actions>
          <button
            type="button"
            class="collapse-circle"
            :class="{ collapsed: shortcutCollapsed }"
            :aria-expanded="!shortcutCollapsed"
            :aria-label="shortcutCollapsed ? '展开快捷入口' : '收起快捷入口'"
            @click="shortcutCollapsed = !shortcutCollapsed"
          >
            <el-icon><ArrowLeft /></el-icon>
          </button>
        </template>

        <!-- 折叠只隐藏内容，卡片外壳保持同尺寸，避免收起时头部上下跳动 -->
        <div v-show="!shortcutCollapsed" class="shortcut-body">
          <div class="shortcut-grid">
            <button v-for="module in homeModules" :key="module.id" class="shortcut-item" type="button" @click="goToModule(module.moduleKey)">
              <span class="shortcut-icon" :class="module.moduleKey">
                <el-icon><component :is="getModuleIcon(module.moduleKey)" /></el-icon>
              </span>
              <span class="shortcut-name">{{ module.name }}</span>
              <span class="shortcut-arrow" aria-hidden="true">›</span>
            </button>

            <button class="shortcut-item add-shortcut" type="button" @click="showCreateDialog = true">
              <span class="add-box" aria-hidden="true">＋</span>
              <span class="shortcut-name">新增记录</span>
            </button>
          </div>

          <p class="shortcut-footer">每一份用心，都是家的小小光亮。</p>
        </div>
      </AppCard>
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
import { Checked, AlarmClock, Clock, Sunny, Grid, ArrowLeft, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { formatDateTime, dayDiffFromToday } from '@/utils/date'
import { getModuleIcon, getModuleColorClass } from '@/utils/module'
import AppCard from '@/components/ui/AppCard.vue'
import AsyncState from '@/components/ui/AsyncState.vue'
import EmptyIllustration from '@/components/ui/EmptyIllustration.vue'

const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 响应式状态
const todoList = ref<any[]>([])
const expiringList = ref<any[]>([])
const recentRecords = ref<any[]>([])
const recentModuleFilter = ref('')

// 加载与错误分开两套：整页汇总（待办/到期/最近）与「筛选模块」触发的局部重取。
// 此前两者共用一个 loading 且失败只 console.error，接口挂了会落到空态，
// 用户以为「没有数据」而不是「加载失败」。
const summaryLoading = ref(false)
const summaryError = ref('')
const recentLoading = ref(false)
const recentError = ref('')

// 最近记录初始展示条数，超出则折叠为「展开全部」
const RECENT_LIMIT = 6
// 每个模块的取数条数：沿用改造前各接口的 size=3，本次只优化呈现，不动接口参数
const RECENT_FETCH_SIZE = 3
const recentExpanded = ref(false)
const visibleRecentRecords = computed(() =>
  recentExpanded.value ? recentRecords.value : recentRecords.value.slice(0, RECENT_LIMIT)
)

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
  summaryLoading.value = true
  summaryError.value = ''
  recentError.value = ''

  try {
    // 首页聚合接口：一次请求拿待办、到期、最近记录
    const response = await dashboardApi.summary()
    const summary = response.data || {}

    todoList.value = summary.todos || []
    expiringList.value = summary.expiring || []
    recentRecords.value = (summary.recent || []).map((record: any) => ({
      ...record,
      summary: getRecordSummary(record)
    }))
    recentExpanded.value = false
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
    // 三个面板同源：整体失败就三处都给出「加载失败 + 重试」，不要伪装成空数据
    summaryError.value = '首页数据加载失败，请检查网络后重试'
    recentError.value = summaryError.value
  } finally {
    summaryLoading.value = false
  }
}

async function fetchRecentRecords() {
  recentLoading.value = true
  recentError.value = ''

  const targets = enabledModules.value.filter(
    (module) => !recentModuleFilter.value || module.moduleKey === recentModuleFilter.value
  )

  try {
    // 逐模块串行 await 会把 N 次往返叠成一次长白等，且单个失败被静默吞掉；
    // 改为并发取数 + 统计失败数，全失败进错误态，部分失败给出明确提示。
    const settled = await Promise.allSettled(
      targets.map((module) =>
        recordApi
          .list({ moduleId: module.id, page: 1, size: RECENT_FETCH_SIZE })
          .then((response) =>
            (response.data?.data || []).map((record: any) => ({
              ...record,
              moduleKey: module.moduleKey,
              moduleName: module.name
            }))
          )      )
    )

    const failed = settled.filter((result) => result.status === 'rejected')
    if (failed.length === targets.length && targets.length > 0) {
      recentError.value = '最近记录加载失败，请稍后重试'
      recentRecords.value = []
      return
    }
    if (failed.length > 0) {
      ElMessage.warning(`${failed.length} 个模块的最近记录加载失败`)
    }

    recentRecords.value = settled
      .flatMap((result) => (result.status === 'fulfilled' ? result.value : []))
      .sort((a, b) => dayjs(b.createdAt).diff(dayjs(a.createdAt)))
      .slice(0, RECENT_LIMIT * 2)
    recentExpanded.value = false
  } catch (error) {
    console.error('Failed to fetch recent records:', error)
    recentError.value = '最近记录加载失败，请稍后重试'
  } finally {
    recentLoading.value = false
  }
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
@use '@/styles/breakpoints' as *;

.dashboard-page {
  width: 100%;
  min-height: 100%;
  background: $bg-color;
  color: $text-primary;
}

// ============================================================
// 欢迎区
// ============================================================
.welcome-block {
  padding: $spacing-md-soft 0 $spacing-lg-soft;
}

.welcome-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $spacing-lg-soft;
}

.welcome-topline {
  display: flex;
  align-items: center;
  gap: $spacing-md-soft;
  margin-bottom: $spacing-md;
  color: $text-tertiary;
  font-size: $text-label;
  letter-spacing: 0.02em;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: $primary-color;
  font-size: $text-body-lg;
  flex-shrink: 0;
}

.welcome-copy {
  min-width: 0;

  h1 {
    margin: 0;
    font-size: $text-display;
    line-height: 1.15;
    letter-spacing: -0.02em;
    font-weight: $font-weight-bold;
    color: $text-primary;
  }

  p {
    margin: $spacing-sm 0 0;
    color: $text-tertiary;
    font-size: $text-body-lg;
  }
}

// 日期卡与页面同色会「糊」在一起，改用白底卡片形成层次
.date-card {
  min-width: 162px;
  background: $card-bg;
  border: 1px solid $border-color;
  border-radius: $border-radius-lg;
  padding: $spacing-md-soft $spacing-lg-soft;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-sm;

  .date-label {
    margin-bottom: $spacing-xs;
    color: $text-tertiary;
    font-size: $text-caption;
    letter-spacing: 0.04em;
  }

  strong {
    color: $text-primary;
    font-size: $text-stat;
    line-height: 1.1;
    letter-spacing: -0.04em;
    font-variant-numeric: tabular-nums;
  }

  small {
    margin-top: $spacing-xs;
    color: $text-tertiary;
    font-size: $text-caption;
    white-space: nowrap;
  }
}

// ============================================================
// 面板栅格
// ============================================================
.panel-grid {
  display: grid;
  gap: $spacing-lg-soft;
}

.upper-grid {
  // 今日待办承载条目正文，横向空间需求更大，故比「即将到期」更宽
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  margin-bottom: $spacing-lg-soft;
}

.lower-grid {
  grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr);
}

// 首页四块面板沿用暖白底，与消息中心/全局搜索的白卡区分「概览」与「明细」。
// 注意是注入变量而不是直接写 background：直接覆盖组件根元素的 background
// 要靠 scoped 样式的注入顺序取胜，属于运气而非设计。
.soft-card {
  --app-card-bg: #{$surface-warm};
}

.panel-todo,
.panel-expiring {
  --app-card-icon-bg: #{rgba($primary-color, $icon-tile-alpha)};
  --app-card-icon-color: #{$primary-text};
}

.panel-recent {
  --app-card-icon-bg: #{rgba($accent-purple, $icon-tile-alpha)};
  --app-card-icon-color: #{$accent-purple};
}

.panel-shortcut {
  --app-card-icon-bg: #{rgba($module-note, $icon-tile-alpha)};
  --app-card-icon-color: #{$module-note};
}

// 卡片头部右侧的文字型入口
.text-link {
  border: none;
  background: transparent;
  color: $primary-text;
  font-size: $text-caption;
  font-family: inherit;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: $spacing-micro;
  white-space: nowrap;
  @include transition-interactive;
  @include pressable;

  .link-arrow {
    font-size: $text-body;
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
  width: $icon-button-size;
  height: $icon-button-size;
  padding: 0;
  flex-shrink: 0;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-pill;
  background: $card-bg;
  color: $text-placeholder;
  font-size: $text-label;
  cursor: pointer;
  @include transition-interactive;

  &:hover {
    background: $hover-bg;
    border-color: rgba($primary-color, 0.3);
    color: $primary-text;
  }

  &.collapsed {
    transform: rotate(180deg);
  }
}

// 折叠后的卡片只保留头部：贴回内容原生高度，
// 否则 AsyncState 的 min-height 会让收起态卡片底部空出一大片。
.shortcut-panel.is-collapsed {
  :deep(.app-card-body) {
    padding: 0;
  }
}

// ============================================================
// 列表行
// ============================================================
.simple-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.simple-item {
  display: flex;
  align-items: center;
  gap: $spacing-md-soft;
  padding: $spacing-sm $spacing-md-soft;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-lg;
  background: $card-bg;
  cursor: pointer;
  @include transition-interactive;

  &:hover {
    border-color: rgba($primary-color, 0.25);
    box-shadow: $shadow-sm;
    transform: translateY(-$shift-on-hover);
  }

  &:focus-visible {
    outline: none;
    border-color: $primary-color;
    box-shadow: 0 0 0 3px rgba($primary-color, 0.12);
  }
}

.simple-dot {
  width: 8px;
  height: 8px;
  border-radius: $border-radius-pill;
  background: $primary-color;
  flex-shrink: 0;
}

.simple-item-body {
  flex: 1;
  min-width: 0;

  h3 {
    margin: 0 0 $spacing-micro;
    color: $text-primary;
    font-size: $text-body;
    font-weight: $font-weight-semibold;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 0;
    color: $text-tertiary;
    font-size: $text-caption;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.simple-arrow {
  color: rgba($text-primary, 0.45);
  font-size: $icon-size-lg;
  line-height: 1;
  @include transition-interactive;
}

.simple-item:hover .simple-arrow {
  color: $primary-text;
}

.expire-badge {
  flex-shrink: 0;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-pill;
  background: rgba($module-drug, 0.12);
  color: $success-text;
  font-size: $text-caption;
  font-weight: $font-weight-semibold;

  &.warning {
    background: rgba($warning-color, 0.14);
    color: $warning-text;
  }

  &.danger {
    background: rgba($danger-color, 0.14);
    color: $danger-text;
  }
}

// ============================================================
// 最近记录
// ============================================================
.filter-select {
  width: 90px;

  :deep(.el-input__wrapper) {
    height: $icon-tile-size;
    border-radius: $border-radius-base;
    border: 1px solid $border-color-light;
    background: $card-bg;
    box-shadow: none;
  }
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md-soft;
}

.record-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  margin-top: $spacing-micro;
  padding: $spacing-sm $spacing-md-soft;
  border: 1px dashed $border-color;
  border-radius: $border-radius-lg;
  background: transparent;
  color: $text-secondary;
  font-family: inherit;
  font-size: $text-label;
  cursor: pointer;
  @include transition-interactive;
  @include pressable;

  &:hover {
    color: $primary-text;
    border-color: rgba($primary-text, 0.4);
    background: $card-bg;
  }
}

.record-item {
  display: grid;
  grid-template-columns: 1.3fr minmax(0, 2.8fr) $icon-tile-size;
  gap: $spacing-md-soft;
  align-items: center;
  padding: $spacing-md-soft;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-lg;
  background: $card-bg;
  cursor: pointer;
  @include transition-interactive;

  &:hover {
    border-color: rgba($primary-color, 0.25);
    box-shadow: $shadow-sm;
    transform: translateY(-$shift-on-hover);
  }

  &:focus-visible {
    outline: none;
    border-color: $primary-color;
    box-shadow: 0 0 0 3px rgba($primary-color, 0.12);
  }
}

.record-left {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  min-width: 0;
}

.module-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: $spacing-lg-soft;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-pill;
  font-size: $text-caption;
  font-weight: $font-weight-semibold;

  &.note {
    background: rgba($module-note, 0.12);
    color: $module-note;
  }

  &.drug {
    background: rgba($module-drug, 0.12);
    color: $success-text;
  }

  &.todo {
    background: rgba($primary-color, 0.12);
    color: $module-todo;
  }

  &.idoc {
    background: rgba($module-idoc, 0.12);
    color: $module-idoc;
  }
}

.record-meta {
  color: $text-tertiary;
  font-size: $text-caption;
}

.record-body {
  min-width: 0;

  h3 {
    margin: 0 0 $spacing-sm;
    color: $text-primary;
    font-size: $text-body-lg;
    font-weight: $font-weight-semibold;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 0;
    color: $text-tertiary;
    font-size: $text-caption;
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
    width: $icon-button-size;
    height: $icon-button-size;
    border: none;
    border-radius: $border-radius-base;
    background: rgba($primary-color, 0.08);
    color: $primary-text;
    font-size: $text-label;
    cursor: pointer;
    @include transition-interactive;
    @include pressable;
  }
}

// 「操作按钮悬停才显形」是鼠标专属交互：触屏没有 hover，
// 因此在 lg 以下必须常驻可见，否则该操作在手机上等于不存在。
// 原先只有 :hover 一条路径，键盘用户也永远聚焦不到它。
// 同时这两个图标钮视觉尺寸只有 28px，触屏下补足可点区（不改视觉）。
@include down(lg) {
  .record-actions button {
    opacity: 1;
    @include touch-hit-area;
  }

  .collapse-circle {
    @include touch-hit-area;
  }
}

@include up(lg) {
  .record-actions button {
    opacity: 0;
  }

  // focus-within 让 Tab 走到行内按钮时同样显形
  .record-item:hover .record-actions button,
  .record-item:focus-within .record-actions button {
    opacity: 1;
  }
}

// ============================================================
// 快捷入口
// ============================================================
.shortcut-body {
  display: flex;
  flex-direction: column;
  gap: $spacing-md-soft;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: $spacing-md-soft;
}

.shortcut-item {
  display: grid;
  grid-template-columns: $icon-tile-size-sm 1fr auto;
  align-items: center;
  gap: $spacing-sm;
  min-height: 72px;
  padding: $spacing-md-soft $spacing-sm $spacing-md-soft $spacing-md-soft;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-lg;
  background: $card-bg;
  color: $text-primary;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  @include transition-interactive;

  &:hover {
    border-color: rgba($primary-color, 0.34);
    box-shadow: $shadow-hover;
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0) scale(0.99);
  }

  &:focus-visible {
    outline: none;
    border-color: $primary-color;
    box-shadow: 0 0 0 3px rgba($primary-color, 0.12);
  }
}

.shortcut-icon {
  width: $icon-tile-size-sm;
  height: $icon-tile-size-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $border-radius-base;
  font-size: $icon-size-base;

  &.drug {
    background: rgba($module-drug, 0.08);
    color: $success-text;
  }

  &.todo {
    background: rgba($primary-color, 0.1);
    color: $primary-text;
  }

  &.note {
    background: rgba($module-note, 0.08);
    color: $module-note;
  }

  &.idoc {
    background: rgba($module-idoc, 0.08);
    color: $module-idoc;
  }
}

.shortcut-name {
  font-size: $text-label;
  font-weight: $font-weight-semibold;
  color: $text-primary;
}

.shortcut-arrow {
  color: rgba($text-primary, 0.52);
  font-size: $icon-size-xl;
  line-height: 1;
}

.add-shortcut {
  border-style: dashed;
  background: transparent;
}

.add-box {
  width: $icon-tile-size-sm;
  height: $icon-tile-size-sm;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: $border-radius-base;
  background: rgba($primary-color, 0.08);
  color: $primary-text;
  font-size: $icon-size-xl;
  line-height: 1;
}

.shortcut-footer {
  margin: 0;
  color: $text-tertiary;
  font-size: $text-caption;
  text-align: center;
  letter-spacing: 0.02em;
}

// ============================================================
// 新增记录弹窗
// ============================================================
.module-select-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: $spacing-md-soft;
}

.module-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-lg-soft $spacing-md;
  border-radius: $border-radius-lg;
  border: 1px solid $border-color-light;
  background: $surface-warm;
  cursor: pointer;
  @include transition-interactive;

  &:hover {
    border-color: rgba($primary-color, 0.34);
  }

  &.selected {
    background: rgba($primary-color, 0.06);
    border-color: rgba($primary-color, 0.34);
  }
}

.module-select-icon {
  width: $icon-tile-size;
  height: $icon-tile-size;
  border-radius: $border-radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $icon-size-lg;

  &.drug {
    background: rgba($module-drug, 0.08);
    color: $success-text;
  }

  &.todo {
    background: rgba($primary-color, 0.1);
    color: $primary-text;
  }

  &.note {
    background: rgba($module-note, 0.08);
    color: $module-note;
  }

  &.idoc {
    background: rgba($module-idoc, 0.08);
    color: $module-idoc;
  }
}

// ============================================================
// 响应式：lg 以下单列，sm 以下欢迎区堆叠（与改造前一致，不回退）
// ============================================================
@include down(lg) {
  .upper-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }

  .record-item {
    grid-template-columns: 1fr;
  }
}

@include down(sm) {
  .welcome-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .date-card {
    width: 100%;
  }

  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
