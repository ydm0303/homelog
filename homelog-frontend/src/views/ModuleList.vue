<template>
  <div class="module-list-page">
    <!-- 搜索筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题、标签、内容..."
          :prefix-icon="Search"
          clearable
          size="default"
          @keyup.enter="handleSearch"
          style="max-width: 320px; flex: 1"
        />

        <el-select
          v-model="selectedTag"
          placeholder="标签筛选"
          size="default"
          style="width: 160px"
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

        <el-select
          v-model="viewMode"
          placeholder="视图模式"
          size="default"
          style="width: 140px"
          @change="handleViewModeChange"
        >
          <el-option label="列表视图" value="list" />
          <el-option label="卡片视图" value="card" />
          <el-option label="时间轴" value="timeline" />
        </el-select>

        <el-button
          v-if="isAdmin || isEditor"
          type="primary"
          :icon="Plus"
          @click="handleCreate"
        >
          新增
        </el-button>
      </div>

      <!-- 高级筛选 -->
      <el-collapse-transition>
        <div v-show="showAdvancedFilter" class="advanced-filter">
          <el-divider direction="horizontal" />
          <div class="advanced-filter-content">
            <el-date-picker
              v-model="filterDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 280px"
              @change="handleSearch"
            />
            <el-input
              v-model="filterCreator"
              placeholder="创建人"
              :prefix-icon="User"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
            <el-checkbox v-model="showPrivateOnly" @change="handleSearch">
              仅显示私密记录 (管理员)
            </el-checkbox>
            <el-button @click="resetFilter">重置筛选</el-button>
          </div>
        </div>
      </el-collapse-transition>

      <div class="filter-actions">
        <el-button link @click="showAdvancedFilter = !showAdvancedFilter">
          <el-icon><ArrowDown /></el-icon>
          高级筛选
        </el-button>
        <span class="result-count">共 {{ total }} 条记录</span>
      </div>
    </el-card>

    <!-- 列表内容 -->
    <el-card class="list-card" shadow="never">
      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'" class="list-view">
        <el-table
          :data="records"
          :loading="loading"
          border
          stripe
          style="width: 100%"
          :default-sort="{ prop: 'createdAt', order: 'descending' }"
        >
          <el-table-column
            prop="title"
            label="标题"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column
            label="核心字段"
            min-width="200"
          >
            <template #default="scope">
              <span class="field-value">{{ getDisplayValue(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="tags"
            label="标签"
            width="140"
            show-overflow-tooltip
          >
            <template #default="scope">
              <el-tag
                v-if="scope.row.tags"
                v-for="tag in scope.row.tags.split(',')"
                :key="tag"
                size="small"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="creatorName"
            label="创建人"
            width="100"
          />
          <el-table-column
            prop="remindDate"
            label="提醒日期"
            width="120"
          >
            <template #default="scope">
              <span v-if="scope.row.remindDate" :class="getRemindClass(scope.row.remindDate)">
                {{ formatDate(scope.row.remindDate) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            prop="createdAt"
            label="创建时间"
            width="160"
          >
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="160"
            fixed="right"
          >
            <template #default="scope">
              <el-dropdown size="small">
                <el-button link>
                  操作 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="goToDetail(scope.row)">
                      <el-icon><View /></el-icon>
                      查看详情
                    </el-dropdown-item>
                    <el-dropdown-item v-if="canEdit(scope.row)" @click="goToEdit(scope.row)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="canDelete(scope.row)"
                      @click="handleDelete(scope.row)"
                      class="danger-item"
                    >
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="scope.row.isPrivate === 0"
                      @click="togglePrivate(scope.row, true)"
                    >
                      <el-icon><Lock /></el-icon>
                      设为私密
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="scope.row.isPrivate === 1"
                      @click="togglePrivate(scope.row, false)"
                    >
                      <el-icon><Unlock /></el-icon>
                      取消私密
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
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
      </div>

      <!-- 卡片视图 -->
      <div v-else-if="viewMode === 'card'" class="card-view">
        <div class="card-grid">
          <div
            v-for="record in records"
            :key="record.id"
            class="record-card"
            @click="goToDetail(record)"
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

        <div v-if="records.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Document /></el-icon>
          <p>暂无记录</p>
          <el-button
            v-if="isAdmin || isEditor"
            type="primary"
            @click="handleCreate"
          >
            创建第一条记录
          </el-button>
        </div>

        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <!-- 时间轴视图 -->
      <div v-else-if="viewMode === 'timeline'" class="timeline-view">
        <el-timeline>
          <el-timeline-item
            v-for="record in records"
            :key="record.id"
            :timestamp="formatDateTime(record.createdAt)"
            :type="getTimelineType(record.moduleKey)"
            :icon="getTimelineIcon(record.moduleKey)"
            placement="top"
          >
            <div class="timeline-content" @click="goToDetail(record)">
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

        <div v-if="records.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Clock /></el-icon>
          <p>暂无记录</p>
        </div>

        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { recordApi } from '@/api'
import {
  Search, Plus, ArrowDown, View, Edit, Delete, Lock, Unlock,
  User, Document, Clock
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

// 路由参数
const currentModuleKey = route.params.moduleKey as string

// 响应式状态
const records = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const selectedTag = ref('')
const viewMode = ref<'list' | 'card' | 'timeline'>('list')
const showAdvancedFilter = ref(false)
const filterDateRange = ref<[string, string] | null>(null)
const filterCreator = ref('')
const showPrivateOnly = ref(false)
const allTags = ref<string[]>([])

const currentModule = computed(() => moduleStore.getModuleByKey(currentModuleKey))
const currentModuleName = computed(() => currentModule.value?.name || '')
const currentModuleId = computed(() => currentModule.value?.id)
const fields = computed(() => currentModule.value?.fields || [])

const isAdmin = computed(() => authStore.isAdmin)
const isEditor = computed(() => authStore.isEditor)
const isReadonly = computed(() => authStore.isReadonly)

onMounted(async () => {
  if (!currentModuleId.value) {
    router.push('/')
    return
  }

  await moduleStore.fetchModules()
  await fetchRecords()
  await fetchAllTags()
})

async function fetchRecords() {
  if (!currentModuleId.value) return

  loading.value = true
  try {
    const params: Record<string, any> = {
      moduleId: currentModuleId.value,
      page: page.value,
      size: pageSize.value
    }

    if (searchKeyword.value) params.q = searchKeyword.value
    if (selectedTag.value) params.tag = selectedTag.value
    if (filterDateRange.value) {
      params.startDate = filterDateRange.value[0]
      params.endDate = filterDateRange.value[1]
    }
    if (filterCreator.value) params.creator = filterCreator.value

    const response = await api.get('/records/list', { params })
    records.value = response.data
    // 后端暂不支持总数，用当前页长度估算
    total.value = response.data.length + (page.value - 1) * pageSize.value
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
    const response = await api.get('/records/list', {
      params: { moduleId: currentModuleId.value, page: 1, size: 500 }
    })
    const tagSet = new Set<string>()
    response.data.forEach((r: any) => {
      r.tags?.split(',').forEach((t: string) => t.trim() && tagSet.add(t.trim()))
    })
    allTags.value = Array.from(tagSet).sort()
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  page.value = 1
  fetchRecords()
}

function handleViewModeChange(mode: 'list' | 'card' | 'timeline') {
  viewMode.value = mode
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
  router.push(`/module/${currentModuleKey}/create`)
}

function goToDetail(record: any) {
  router.push(`/module/${currentModuleKey}/${record.id}`)
}

function goToEdit(record: any) {
  router.push(`/module/${currentModuleKey}/${record.id}/edit`)
}

function canEdit(record: any) {
  if (isReadonly.value) return false
  if (isAdmin.value) return true
  return record.creatorId === authStore.user?.id
}

function canDelete(record: any) {
  return isAdmin.value
}

async function handleDelete(record: any) {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？删除后不可恢复', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await recordApi.delete(record.id, currentModuleId.value)
    ElMessage.success('删除成功')
    fetchRecords()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function togglePrivate(record: any, isPrivate: boolean) {
  try {
    await recordApi.update({
      recordId: record.id,
      moduleId: currentModuleId.value,
      dataJson: { ...record.dataJson, is_private: isPrivate }
    })
    ElMessage.success(isPrivate ? '已设为私密' : '已取消私密')
    fetchRecords()
  } catch {
    ElMessage.error('操作失败')
  }
}

function resetFilter() {
  selectedTag.value = ''
  filterDateRange.value = null
  filterCreator.value = ''
  showPrivateOnly.value = false
  handleSearch()
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
  const diff = dayjs(remindDate).diff(dayjs(), 'day')
  if (diff < 0) return 'remind-overdue'
  if (diff <= 3) return 'remind-soon'
  return ''
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

function getTimelineType(moduleKey: string) {
  const types: Record<string, string> = {
    drug: 'primary',
    todo: 'success',
    note: 'info',
    idoc: 'warning'
  }
  return types[moduleKey] || 'info'
}

function getTimelineIcon(moduleKey: string) {
  const icons: Record<string, string> = {
    drug: 'FirstAidKit',
    todo: 'Checked',
    note: 'Picture',
    idoc: 'CreditCard'
  }
  return icons[moduleKey] || 'Document'
}

function formatDate(dateStr: string) {
  return dayjs(dateStr).format('MM-DD')
}

function formatDateTime(dateStr: string) {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

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

    .advanced-filter {
      padding-top: 16px;
      margin-top: 8px;
    }

    .advanced-filter-content {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      padding: 16px 0 8px;
    }

    .filter-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 8px;
      color: $text-secondary;
      font-size: 13px;
    }
  }

  .list-card {
    :deep(.el-table) {
      .field-value {
        color: $text-secondary;
        font-size: 13px;
      }

      .remind-overdue {
        color: #f56c6c;
        font-weight: 500;
      }

      .remind-soon {
        color: #e6a23c;
        font-weight: 500;
      }

      .danger-item {
        color: #f56c6c;
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
            color: $primary-color;
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

      :deep(.el-input),
      :deep(.el-select) {
        width: 100% !important;
        max-width: none !important;
      }

      :deep(.el-button) {
        width: 100%;
      }
    }

    .advanced-filter-content {
      flex-direction: column;
      align-items: stretch;

      :deep(.el-date-picker),
      :deep(.el-input) {
        width: 100% !important;
      }
    }

    .card-grid {
      grid-template-columns: 1fr !important;
    }
  }
}

// 长辈模式
.elderly-mode .module-list-page {
  :deep(.el-table) {
    font-size: 16px !important;
  }

  :deep(.el-pagination) {
    font-size: 15px !important;
  }

  .card-title { font-size: 18px !important; }
  .field-label { font-size: 15px !important; }
  .field-value { font-size: 15px !important; }
  .timeline-header h4 { font-size: 17px !important; }
  .timeline-fields { font-size: 15px !important; }
}
</style>