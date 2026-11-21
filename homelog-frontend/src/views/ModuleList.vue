<template>
  <div class="module-list-page">
    <PageHeader :title="currentModuleName || '模块列表'" :description="pageDescription">
      <template #actions>
        <el-button v-if="canCreate" type="primary" :icon="Plus" @click="handleCreate">
          新增记录
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索筛选栏 -->
    <AppCard class="filter-card" padding="sm">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题、标签、内容..."
          :prefix-icon="Search"
          clearable
          size="default"
          class="filter-search"
          @keyup.enter="handleSearch"
        />

        <el-select
          v-model="selectedTag"
          placeholder="标签筛选"
          size="default"
          class="filter-tag"
          @change="handleSearch"
          filterable
        >
          <el-option
            v-for="tag in allTags"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>

        <el-segmented
          v-model="viewMode"
          :options="viewOptions"
          class="view-segment"
          @change="handleViewModeChange"
        />

        <el-button
          class="advanced-toggle"
          :class="{ 'is-active': showAdvancedFilter }"
          text
          @click="showAdvancedFilter = !showAdvancedFilter"
        >
          高级筛选
          <el-icon><ArrowUp v-if="showAdvancedFilter" /><ArrowDown v-else /></el-icon>
        </el-button>
      </div>

      <!-- 药品专属：过期状态快捷筛选 tab -->
      <div v-if="isDrugModule" class="drug-tabs">
        <el-segmented
          v-model="expiryFilter"
          :options="expiryFilterOptions"
          class="expiry-tabs"
        />
      </div>

      <!-- 高级筛选 -->
      <el-collapse-transition>
        <div v-show="showAdvancedFilter" class="advanced-filter">
          <div class="advanced-filter-content">
            <el-date-picker
              v-model="filterDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="filter-date"
              @change="handleSearch"
            />
            <el-input
              v-model="filterCreator"
              placeholder="创建人"
              :prefix-icon="User"
              clearable
              class="filter-creator"
              @keyup.enter="handleSearch"
            />
            <el-checkbox v-if="isAdmin" v-model="showPrivateOnly" @change="handleSearch">
              仅显示私密记录 (管理员)
            </el-checkbox>
            <el-button @click="resetFilter">重置筛选</el-button>
          </div>
        </div>
      </el-collapse-transition>
    </AppCard>

    <!-- 列表内容 -->
    <AppCard class="list-card" padding="none">
      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'" v-loading="loading" class="list-view">
        <div class="mobile-record-list">
          <article
            v-for="record in displayRecords"
            :key="`mobile-${record.id}`"
            class="mobile-record"
            role="button"
            tabindex="0"
            @click="goToDetail(record)"
            @keyup.enter="goToDetail(record)"
          >
            <div class="mobile-record-top">
              <span class="mobile-record-title">{{ record.title || getDisplayValue(record) }}</span>
              <span
                v-if="record.remindDate"
                class="remind-tag"
                :class="getRemindClass(record.remindDate)"
              >
                {{ formatDate(record.remindDate) }}
              </span>
            </div>
            <p class="mobile-record-summary">{{ getDisplayValue(record) || '暂无摘要' }}</p>
            <div class="mobile-record-meta">
              <span>{{ record.creatorName }}</span>
              <span>{{ formatDateTime(record.createdAt) }}</span>
            </div>
          </article>

          <EmptyState
            v-if="displayRecords.length === 0 && !loading"
            compact
            :icon="hasActiveFilters ? Search : Document"
            :title="emptyTitle"
            :description="emptyDescription"
          >
            <template #actions>
              <el-button v-if="hasActiveFilters" @click="resetFilter">清空筛选</el-button>
              <el-button v-else-if="canCreate" type="primary" @click="handleCreate">
                {{ emptyActionText }}
              </el-button>
            </template>
          </EmptyState>
        </div>

        <el-table
          :data="displayRecords"
          class="record-table"
          style="width: 100%"
          :default-sort="{ prop: 'createdAt', order: 'descending' }"
          :row-class-name="getRowClass"
          @row-click="goToDetail"
        >
          <el-table-column :label="isDrugModule ? '药品名' : '记录'" min-width="260">
            <template #default="scope">
              <div class="record-cell">
                <span class="record-title">{{ scope.row.title || getDisplayValue(scope.row) }}</span>
                <span v-if="isDrugModule" class="record-summary">{{ getDrugSummary(scope.row) || '暂无摘要' }}</span>
                <span v-else class="record-summary">{{ getDisplayValue(scope.row) || '暂无摘要' }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column v-if="!isDrugModule" label="标签" width="180">
            <template #default="scope">
              <div v-if="scope.row.tags" class="tag-list">
                <el-tag
                  v-for="tag in scope.row.tags.split(',')"
                  :key="tag"
                  size="small"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>

          <el-table-column v-if="isDrugModule" label="有效期" width="150">
            <template #default="scope">
              <div v-if="getExpireDate(scope.row)" class="expiry-cell" :class="expiryClass(scope.row)">
                <span class="expiry-date">{{ formatDateSafe(getExpireDate(scope.row), 'YYYY-MM-DD') }}</span>
                <span class="expiry-status">{{ expiryText(scope.row) }}</span>
              </div>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>

          <el-table-column v-if="isDrugModule" label="库存" width="120">
            <template #default="scope">
              <div class="stock-cell" :class="getStockState(scope.row)">
                <span class="stock-qty">
                  {{ getDrugStock(scope.row) }}<span class="stock-unit">{{ getDrugUnit(scope.row) }}</span>
                </span>
                <el-tag v-if="getStockState(scope.row) === 'out'" size="small" type="danger" effect="dark">缺货</el-tag>
                <el-tag v-else-if="getStockState(scope.row) === 'low'" size="small" type="warning" effect="dark">库存不足</el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column v-if="isDrugModule" label="存放位置" width="120">
            <template #default="scope">
              <span class="cell-muted">{{ recordLocation(scope.row) || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="!isDrugModule" label="提醒" width="120">
            <template #default="scope">
              <span
                v-if="scope.row.remindDate"
                class="remind-tag"
                :class="getRemindClass(scope.row.remindDate)"
              >
                {{ formatDate(scope.row.remindDate) }}
              </span>
              <span v-else class="cell-muted">—</span>
            </template>
          </el-table-column>

          <el-table-column v-if="!isDrugModule" prop="creatorName" label="创建人" width="100">
            <template #default="scope">
              <span class="cell-muted">{{ scope.row.creatorName }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="!isDrugModule" label="创建时间" width="150">
            <template #default="scope">
              <span class="cell-muted">{{ formatDateTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="128" fixed="right" align="right">
            <template #default="scope">
              <div class="row-actions" @click.stop>
                <el-tooltip content="查看详情" placement="top" :show-after="300">
                  <el-button text circle class="action-btn" aria-label="查看详情" @click="goToDetail(scope.row)">
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip v-if="canEdit(scope.row)" content="编辑" placement="top" :show-after="300">
                  <el-button text circle class="action-btn" aria-label="编辑" @click="goToEdit(scope.row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-dropdown trigger="click" size="small">
                  <el-button text circle class="action-btn" aria-label="更多操作">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="togglePrivate(scope.row, scope.row.isPrivate === 0)">
                        <el-icon>
                          <Lock v-if="scope.row.isPrivate === 0" />
                          <Unlock v-else />
                        </el-icon>
                        {{ scope.row.isPrivate === 0 ? '设为私密' : '取消私密' }}
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="canDelete(scope.row)"
                        class="danger-item"
                        @click="handleDelete(scope.row)"
                      >
                        <el-icon><Delete /></el-icon>
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <EmptyState
              compact
              :icon="hasActiveFilters ? Search : Document"
              :title="emptyTitle"
              :description="emptyDescription"
            >
              <template #actions>
                <el-button v-if="hasActiveFilters" @click="resetFilter">清空筛选</el-button>
                <el-button v-else-if="canCreate" type="primary" @click="handleCreate">
                  创建第一条记录
                </el-button>
              </template>
            </EmptyState>
          </template>
        </el-table>

      </div>

      <!-- 卡片视图 -->
      <div v-else-if="viewMode === 'card'" v-loading="loading" class="card-view">
        <div class="card-grid">
          <div
            v-for="record in displayRecords"
            :key="record.id"
            class="record-card"
            role="button"
            tabindex="0"
            @click="goToDetail(record)"
            @keyup.enter="goToDetail(record)"
          >
            <div class="card-header">
              <el-tag size="small" :type="getModuleTagType(currentModuleKey)">
                {{ currentModuleName }}
              </el-tag>
              <span class="card-time">{{ formatDateTime(record.createdAt) }}</span>
            </div>
            <h4 class="card-title">{{ record.title || getDisplayValue(record) }}</h4>
            <div class="card-fields">
              <span
                v-for="(value, key) in getDisplayFields(record)"
                :key="key"
                class="field-item"
              >
                <span class="field-label">{{ key }}:</span>
                <span class="field-value">{{ value }}</span>
              </span>
            </div>
            <div class="card-tags" v-if="record.tags">
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

        <EmptyState
          v-if="records.length === 0 && !loading"
          compact
          :icon="hasActiveFilters ? Search : Document"
          :title="emptyTitle"
          :description="emptyDescription"
        >
          <template #actions>
            <el-button v-if="hasActiveFilters" @click="resetFilter">清空筛选</el-button>
            <el-button v-else-if="canCreate" type="primary" @click="handleCreate">
              创建第一条记录
            </el-button>
          </template>
        </EmptyState>
      </div>

      <!-- 时间轴视图 -->
      <div v-else-if="viewMode === 'timeline'" v-loading="loading" class="timeline-view">
        <el-timeline>
          <el-timeline-item
            v-for="record in displayRecords"
            :key="record.id"
            :timestamp="formatDateTime(record.createdAt)"
            :type="getModuleTagType(record.moduleKey)"
            :icon="getModuleIcon(record.moduleKey)"
            placement="top"
          >
            <div class="timeline-content" role="button" tabindex="0" @click="goToDetail(record)" @keyup.enter="goToDetail(record)">
              <div class="timeline-header">
                <h4>{{ record.title || getDisplayValue(record) }}</h4>
                <el-tag size="small" :type="getModuleTagType(record.moduleKey)">
                  {{ record.moduleName }}
                </el-tag>
              </div>
              <div class="timeline-fields">
                <span
                  v-for="(value, key) in getDisplayFields(record)"
                  :key="key"
                >
                  {{ key }}: {{ value }}
                </span>
              </div>
              <div class="timeline-meta">
                <span>创建人: {{ record.creatorName }}</span>
                <span v-if="record.remindDate">提醒: {{ formatDate(record.remindDate) }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>

        <EmptyState
          v-if="records.length === 0 && !loading"
          compact
          :icon="hasActiveFilters ? Search : Clock"
          :title="emptyTitle"
          :description="emptyDescription"
        >
          <template #actions>
            <el-button v-if="hasActiveFilters" @click="resetFilter">清空筛选</el-button>
          </template>
        </EmptyState>
      </div>

      <!-- 分页（药品模块拉全量，无需分页） -->
      <div class="pagination" v-if="!isDrugModule && total > pageSize">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </AppCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { recordApi } from '@/api'
import {
  Search, Plus, ArrowDown, ArrowUp, View, Edit, Delete, Lock, Unlock,
  User, Document, Clock, MoreFilled
} from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate as formatDateSafe, formatDateTime as formatDateTimeSafe, dayDiffFromToday } from '@/utils/date'
import { getModuleIcon, getModuleTagType } from '@/utils/module'
import { getExpiryState } from '@/utils/drug'
import { useIsMobile } from '@/composables/useMediaQuery'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 路由参数
const currentModuleKey = computed(() => route.params.moduleKey as string)

// 响应式状态
const records = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const DRUG_LIST_MAX = 1000 // 药箱体量小，一次拉全量做客户端过期筛选/计数
const searchKeyword = ref('')
const selectedTag = ref('')
const viewMode = ref('list')
// 窄屏（手机）默认切到卡片视图，避免表格横向滚动 + fixed 列遮挡
const isMobile = useIsMobile()
watch(isMobile, (mobile) => {
  if (mobile) viewMode.value = 'card'
}, { immediate: true })
const showAdvancedFilter = ref(false)
const filterDateRange = ref<[string, string] | null>(null)
const filterCreator = ref('')
const showPrivateOnly = ref(false)
const expiryFilter = ref<'all' | 'expired' | 'soon' | 'normal'>('all')
const allTags = ref<string[]>([])

const currentModule = computed(() => moduleStore.getModuleByKey(currentModuleKey.value))
const currentModuleName = computed(() => currentModule.value?.name || '')
const currentModuleId = computed(() => currentModule.value?.id)
const fields = computed(() => currentModule.value?.fields || [])
const isDrugModule = computed(() => currentModuleKey.value === 'drug')

// 药品模块：按有效期升序（最早过期在前）+ 客户端过期状态筛选（数据量小，前端排序即可）
const displayRecords = computed(() => {
  if (!isDrugModule.value) return records.value
  let list = [...records.value]
  if (expiryFilter.value !== 'all') {
    list = list.filter(r => getDrugExpiryState(r)?.status === expiryFilter.value)
  }
  list.sort((a, b) => {
    const da = getExpireDate(a)
    const db = getExpireDate(b)
    if (!da && !db) return 0
    if (!da) return 1
    if (!db) return -1
    return da.localeCompare(db)
  })
  return list
})

// 过期状态计数（用于筛选 tab，基于已加载的全量药品记录）
const expiryCounts = computed(() => {
  const counts = { all: records.value.length, expired: 0, soon: 0, normal: 0 }
  records.value.forEach((r) => {
    const s = getDrugExpiryState(r)
    if (s) counts[s.status]++
  })
  return counts
})

const expiryFilterOptions = computed(() => [
  { label: `全部 ${expiryCounts.value.all}`, value: 'all' },
  { label: `已过期 ${expiryCounts.value.expired}`, value: 'expired' },
  { label: `即将过期 ${expiryCounts.value.soon}`, value: 'soon' },
  { label: `正常 ${expiryCounts.value.normal}`, value: 'normal' }
])

const isAdmin = computed(() => authStore.isAdmin)
const isReadonly = computed(() => authStore.isReadonly)
const canCreate = computed(() => authStore.canWrite)

const viewOptions = [
  { label: '列表', value: 'list' },
  { label: '卡片', value: 'card' },
  { label: '时间轴', value: 'timeline' }
]

const hasActiveFilters = computed(() =>
  Boolean(
    searchKeyword.value ||
      selectedTag.value ||
      filterDateRange.value ||
      filterCreator.value.trim() ||
      showPrivateOnly.value ||
      expiryFilter.value !== 'all'
  )
)

const emptyTitle = computed(() => {
  if (hasActiveFilters.value) return '没有符合条件的记录'
  if (isDrugModule.value) return '药箱还是空的'
  return '还没有记录'
})
const emptyDescription = computed(() => {
  if (hasActiveFilters.value) return '换个关键词，或清空筛选后再看看'
  if (isDrugModule.value) return '记录家里有哪些药，快过期或库存不足时会提醒你'
  return '把家里的小事记下来，之后随时能找到'
})
const emptyActionText = computed(() => (isDrugModule.value ? '添加第一种药' : '创建第一条记录'))
const pageDescription = computed(() =>
  total.value > 0 ? `共 ${total.value} 条记录` : '查看和管理这个模块下的所有记录'
)

onMounted(async () => {
  // 必须先加载模块列表，否则刷新页面时 currentModuleId 仍为空会被误判为模块不存在
  await moduleStore.fetchModules()
  await loadModule()
})

// 同一组件在 /module/a 与 /module/b 之间切换时路由不会重建组件，需手动重新加载
watch(currentModuleKey, (key) => {
  if (!key) return
  clearFilterState()
  loadModule()
})

async function loadModule() {
  if (!currentModuleId.value) {
    ElMessage.error('模块不存在或已禁用')
    router.push('/')
    return
  }

  await fetchRecords()
  await fetchAllTags()
}

async function fetchRecords() {
  if (!currentModuleId.value) return

  loading.value = true
  try {
    const params: any = {
      moduleId: currentModuleId.value,
      page: isDrugModule.value ? 1 : page.value,
      size: isDrugModule.value ? DRUG_LIST_MAX : pageSize.value
    }

    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (selectedTag.value) params.tag = selectedTag.value
    if (filterDateRange.value) {
      params.startDate = filterDateRange.value[0]
      params.endDate = filterDateRange.value[1]
    }
    if (filterCreator.value.trim()) params.creator = filterCreator.value.trim()
    if (showPrivateOnly.value) params.privateOnly = true

    const response = await recordApi.list(params)
    const body = response.data || {}
    records.value = body.data || []
    total.value = body.total ?? records.value.length
  } catch (error) {
    console.error('Failed to fetch records:', error)
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchAllTags() {
  if (!currentModuleId.value) return
  try {
    const response = await recordApi.tags(currentModuleId.value)
    allTags.value = Array.isArray(response.data) ? response.data : []
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  page.value = 1
  fetchRecords()
}

function handleViewModeChange(mode: string | number | boolean) {
  viewMode.value = String(mode)
}

function handlePageChange(newPage: number) {
  page.value = newPage
  fetchRecords()
}

function handleSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  fetchRecords()
}

function handleCreate() {
  router.push(`/module/${currentModuleKey.value}/create`)
}

function goToDetail(record: any) {
  router.push(`/module/${currentModuleKey.value}/${record.id}`)
}

function goToEdit(record: any) {
  router.push(`/module/${currentModuleKey.value}/${record.id}/edit`)
}

function canEdit(record: any) {
  if (isReadonly.value) return false
  if (isAdmin.value) return true
  return record.creatorId === authStore.user?.id
}

function canDelete(_record: any) {
  return isAdmin.value
}

async function handleDelete(record: any) {
  const moduleId = currentModuleId.value
  if (!moduleId) return

  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？删除后不可恢复', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await recordApi.delete(record.id, moduleId)
    ElMessage.success('删除成功')
    fetchRecords()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function togglePrivate(record: any, isPrivate: boolean) {
  const moduleId = currentModuleId.value
  if (!moduleId) return

  try {
    await recordApi.update({
      recordId: record.id,
      moduleId,
      dataJson: { ...record.dataJson, is_private: isPrivate }
    })
    ElMessage.success(isPrivate ? '已设为私密' : '已取消私密')
    fetchRecords()
  } catch {
    ElMessage.error('操作失败')
  }
}

function clearFilterState() {
  searchKeyword.value = ''
  selectedTag.value = ''
  filterDateRange.value = null
  filterCreator.value = ''
  showPrivateOnly.value = false
  expiryFilter.value = 'all'
  page.value = 1
}

function resetFilter() {
  clearFilterState()
  fetchRecords()
}

function getDisplayValue(record: any): string {
  if (!record.dataJson) return ''
  const keys = Object.keys(record.dataJson)
  if (keys.length === 0) return ''
  // 优先显示第一个非系统字段
  for (const key of keys) {
    if (!['is_private'].includes(key)) {
      const val = record.dataJson[key]
      return typeof val === 'string' ? val.substring(0, 50) : String(val).substring(0, 50)
    }
  }
  return ''
}

function getDisplayFields(record: any): Record<string, any> {
  if (!record.dataJson) return {}
  const result: Record<string, any> = {}
  let count = 0
  for (const [key, val] of Object.entries(record.dataJson)) {
    if (!['is_private'].includes(key) && count < 3) {
      // 查找字段标签
      const field = fields.value.find(f => f.fieldKey === key)
      const label = field?.fieldLabel || key
      result[label] = typeof val === 'string' ? val.substring(0, 30) : String(val).substring(0, 30)
      count++
    }
  }
  return result
}

function getRemindClass(remindDate: string) {
  const diff = dayDiffFromToday(remindDate)
  if (diff === null) return ''
  if (diff < 0) return 'remind-overdue'
  if (diff <= 3) return 'remind-soon'
  return ''
}

function getExpireDate(record: any): string | null {
  const val = record.dataJson?.expire_date
  return val ? String(val) : null
}

function getDrugExpiryState(record: any) {
  return getExpiryState(getExpireDate(record))
}

function expiryClass(record: any) {
  const state = getDrugExpiryState(record)
  if (!state) return ''
  if (state.status === 'expired') return 'remind-overdue'
  if (state.status === 'soon') return 'remind-soon'
  return ''
}

function expiryText(record: any) {
  const state = getDrugExpiryState(record)
  if (!state) return ''
  if (state.status === 'expired') return `已过期${Math.abs(state.days)}天`
  if (state.days === 0) return '今天到期'
  if (state.status === 'soon') return `${state.days}天后到期`
  return '正常'
}

function getDrugStock(record: any): string {
  const q = record.dataJson?.quantity
  return q === null || q === undefined || q === '' ? '—' : String(q)
}

function getDrugUnit(record: any): string {
  return record.dataJson?.unit || ''
}

// 库存状态：缺货(≤0) / 低库存(低于最低库存) / 正常
function getStockState(record: any): 'out' | 'low' | 'ok' | null {
  const q = Number(record.dataJson?.quantity)
  if (Number.isNaN(q)) return null
  if (q <= 0) return 'out'
  const min = Number(record.dataJson?.min_stock)
  if (!Number.isNaN(min) && q < min) return 'low'
  return 'ok'
}

// 药品列表行高亮（过期/即将过期加左侧色条）
function getRowClass({ row }: { row: any }) {
  let cls = 'record-row'
  if (!isDrugModule.value) return cls
  const state = getDrugExpiryState(row)
  if (state?.status === 'expired') return `${cls} row-expired`
  if (state?.status === 'soon') return `${cls} row-soon`
  return cls
}

// 药品摘要：优先显示用法用量/适用症状，避免重复药品名
function getDrugSummary(record: any): string {
  const d = record.dataJson || {}
  return d.usage_dosage || d.indications || ''
}

function recordLocation(record: any): string {
  return record.dataJson?.location || ''
}

function formatDate(dateStr: string) {
  return formatDateSafe(dateStr, 'MM-DD')
}

function formatDateTime(dateStr: string) {
  return formatDateTimeSafe(dateStr)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/breakpoints' as *;

.module-list-page {
  .filter-card {
    margin-bottom: 16px;

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      padding: 8px 0;
    }

    .drug-tabs {
      padding-top: 8px;
    }

    .advanced-toggle {
      color: $text-secondary;
      transition: color $transition-fast $ease-out;

      &.is-active,
      &:hover {
        color: $primary-text;
      }
    }

    .advanced-filter {
      margin-top: 8px;
      padding-top: 12px;
      border-top: 1px solid $border-color-light;
    }

    .advanced-filter-content {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      padding: 4px 0;
    }
  }

  .list-card {
    :deep(.el-table) {
      .field-value {
        color: $text-secondary;
        font-size: 13px;
      }

      .remind-overdue {
        color: $danger-text;
        font-weight: 500;
      }

      .remind-soon {
        color: $warning-text;
        font-weight: 500;
      }

      .danger-item {
        color: $danger-text;
      }
    }
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 0;
  }

  .card-view {
    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
      padding: 8px 0;
    }

    .record-card {
      padding: 16px;
      background: $card-bg;
      border: 1px solid $border-color;
      border-radius: $border-radius-base;
      transition: all 0.2s ease;
      cursor: pointer;

      &:hover {
        border-color: $primary-text;
        box-shadow: $shadow-hover;
        transform: translateY(-2px);
      }

      .card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;
      }

      .card-time {
        font-size: 12px;
        color: $text-placeholder;
      }

      .card-title {
        font-size: 15px;
        font-weight: 600;
        color: $text-primary;
        margin: 0 0 12px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .card-fields {
        display: flex;
        flex-direction: column;
        gap: 6px;
        margin-bottom: 12px;

        .field-item {
          display: flex;
          gap: 8px;
          font-size: 13px;

          .field-label {
            color: $text-secondary;
            min-width: 60px;
          }

          .field-value {
            color: $text-primary;
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }

      .card-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      text-align: center;

      .empty-icon {
        font-size: 48px;
        color: $text-placeholder;
        margin-bottom: 16px;
      }

      p {
        color: $text-secondary;
        margin: 0 0 20px;
        font-size: 15px;
      }
    }
  }

  .timeline-view {
    :deep(.el-timeline-item) {
      .timeline-content {
        cursor: pointer;
        padding: 4px 0;

        &:hover {
          .timeline-header h4 {
            color: $primary-text;
          }
        }
      }

      .timeline-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        h4 {
          margin: 0;
          font-size: 15px;
          font-weight: 600;
          color: $text-primary;
        }
      }

      .timeline-fields {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        margin-bottom: 8px;
        font-size: 13px;
        color: $text-secondary;
      }

      .timeline-meta {
        display: flex;
        gap: 16px;
        font-size: 12px;
        color: $text-placeholder;
      }
    }
  }
}

// 响应式
@media (max-width: 767px) {
  .module-list-page {
    .filter-row {
      flex-direction: column;
      align-items: stretch;

      :deep(.el-button) {
        width: 100%;
      }
    }

    .advanced-filter-content {
      flex-direction: column;
      align-items: stretch;
    }

    .card-grid {
      grid-template-columns: 1fr;
    }
  }
}

// ============================================================
// Phase 1：列表页结构、表格与移动端卡片
// ============================================================
.module-list-page {
  .filter-search {
    width: 320px;
    max-width: 100%;
  }

  .filter-tag,
  .filter-creator {
    width: 160px;
  }

  .filter-date {
    width: 280px;
  }

  .view-segment {
    margin-left: auto;
  }

  .remind-tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 999px;
    background: $surface-warm;
    color: $text-secondary;
    font-size: 12px;
    font-variant-numeric: tabular-nums;

    &.remind-overdue {
      background: $danger-bg;
      color: $danger-text;
    }

    &.remind-soon {
      background: $warning-bg;
      color: $warning-text;
    }
  }
}

.record-table {
  :deep(.el-table__cell) {
    padding: 12px 0;
  }

  :deep(th.el-table__cell) {
    padding: 10px 0;
    font-size: 12px;
    letter-spacing: 0.04em;
  }

  :deep(.el-table__row) {
    cursor: pointer;
  }

  .record-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  .record-title {
    overflow: hidden;
    color: $text-primary;
    font-size: 14px;
    font-weight: $font-weight-semibold;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .record-summary {
    overflow: hidden;
    color: $text-tertiary;
    font-size: $text-label;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .cell-muted {
    color: $text-tertiary;
    font-size: $text-label;
  }

  .expiry-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .expiry-date {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: $text-primary;

      &::before {
        content: '';
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: currentColor;
        flex-shrink: 0;
      }
    }

    .expiry-status {
      font-size: 12px;
      font-weight: 500;
      color: $text-secondary;
    }

    &.remind-overdue {
      .expiry-date,
      .expiry-status {
        color: $danger-text;
      }
    }

    &.remind-soon {
      .expiry-date,
      .expiry-status {
        color: $warning-text;
      }
    }
  }

  .stock-cell {
    display: flex;
    align-items: center;
    gap: 6px;

    .stock-qty {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;

      .stock-unit {
        margin-left: 2px;
        font-size: 12px;
        font-weight: 400;
        color: $text-secondary;
      }
    }

    &.out .stock-qty {
      color: $danger-text;
    }

    &.low .stock-qty {
      color: $warning-text;
    }
  }

  :deep(.el-table__row.row-expired) > td.el-table__cell {
    background-color: $danger-bg;
  }

  :deep(.el-table__row.row-expired) > td.el-table__cell:first-child {
    box-shadow: inset 3px 0 0 $danger-text;
  }

  :deep(.el-table__row.row-expired:hover) > td.el-table__cell {
    background-color: $danger-bg;
  }

  :deep(.el-table__row.row-soon) > td.el-table__cell {
    background-color: $warning-bg;
  }

  :deep(.el-table__row.row-soon) > td.el-table__cell:first-child {
    box-shadow: inset 3px 0 0 $warning-text;
  }

  :deep(.el-table__row.row-soon:hover) > td.el-table__cell {
    background-color: $warning-bg;
  }

  .row-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 2px;
    opacity: 0.55;
    transition: opacity $transition-fast $ease-out;
  }

  :deep(.el-table__row:hover) .row-actions {
    opacity: 1;
  }

  .action-btn {
    width: 28px;
    height: 28px;
    color: $text-tertiary;

    :deep(.el-icon) {
      font-size: $icon-size-sm;
    }
  }

  :deep(.danger-item) {
    color: $danger-text;
  }
}

.mobile-record-list {
  display: none;
}

.list-card > .pagination {
  padding: 14px 20px;
  border-top: 1px solid $border-color-light;
}

@include down(md) {
  .module-list-page {
    .filter-search,
    .filter-tag,
    .filter-date,
    .filter-creator {
      width: 100%;
    }

    .view-segment {
      width: 100%;
      margin-left: 0;
    }
  }

  .record-table {
    display: none;
  }

  .mobile-record-list {
    display: flex;
    flex-direction: column;
  }

  .mobile-record {
    padding: 14px 16px;
    border-bottom: 1px solid $border-color-light;
    cursor: pointer;

    &:last-child {
      border-bottom: none;
    }

    &:active {
      background: $surface-warm-soft;
    }
  }

  .mobile-record-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  .mobile-record-title {
    overflow: hidden;
    color: $text-primary;
    font-size: 14px;
    font-weight: $font-weight-semibold;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .mobile-record-summary {
    overflow: hidden;
    margin: 6px 0 8px;
    color: $text-tertiary;
    font-size: 12px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .mobile-record-meta {
    display: flex;
    gap: 12px;
    color: $text-placeholder;
    font-size: 11px;
  }

  .list-card > .pagination {
    padding: 12px;
  }
}
</style>
