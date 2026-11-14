<template>
  <div class="global-search-page">
    <PageHeader title="全局搜索" description="在全部家庭记录中查找内容、标签和字段" />

    <!-- 搜索与筛选 -->
    <AppCard class="search-card" padding="sm">
      <div class="search-header">
        <el-input
          v-model="searchKeyword"
          :placeholder="`搜索：${initialKeyword || '输入关键词...'}`"
          :prefix-icon="Search"
          clearable
          size="large"
          @keyup.enter="handleKeywordSearch"
          class="search-input"
        >
          <template #append>
            <el-button
              type="primary"
              :loading="loading"
              @click="handleKeywordSearch"
            >
              搜索
            </el-button>
          </template>
        </el-input>
      </div>

      <div class="filter-content">
        <div class="filter-group">
          <span class="filter-label">模块：</span>
          <el-select
            v-model="filterModuleId"
            placeholder="全部模块"
            size="small"
            class="filter-select"
            @change="handleFilterChange"
          >
            <el-option label="全部模块" value="" />
            <el-option
              v-for="module in searchableModules"
              :key="module.id"
              :label="module.name"
              :value="module.id"
            />
          </el-select>
        </div>

        <div class="filter-group">
          <span class="filter-label">标签：</span>
          <el-select
            v-model="filterTag"
            placeholder="全部标签"
            size="small"
            class="filter-select"
            filterable
            @change="handleFilterChange"
          >
            <el-option label="全部标签" value="" />
            <el-option
              v-for="tag in allTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </div>

        <div class="filter-group">
          <span class="filter-label">时间：</span>
          <el-date-picker
            v-model="filterDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="small"
            class="filter-date"
            @change="handleFilterChange"
            value-format="YYYY-MM-DD"
          />
        </div>

        <el-button
          v-if="hasActiveFilters"
          @click="resetFilters"
          size="small"
        >
          <el-icon><Refresh /></el-icon>
          重置筛选
        </el-button>
      </div>
    </AppCard>

    <!-- 结果统计 -->
    <div class="result-stats" v-if="!loading && (results.length > 0 || total > 0)">
      找到 <span class="keyword">{{ total }}</span> 条结果
      <span v-if="searchKeyword" class="keyword-info">关于 "<em>{{ searchKeyword }}</em>"</span>
      <span class="time">（耗时 {{ searchTime }}ms）</span>
    </div>

    <!-- 结果列表 -->
    <AppCard class="results-card" padding="none">
      <AsyncState
        :loading="loading"
        :empty="!loading && results.length === 0"
        :empty-icon="Search"
        :empty-title="searchKeyword ? '未找到相关记录' : '输入关键词开始搜索'"
        :empty-description="
          searchKeyword
            ? '检查拼写、尝试其他关键词，或减少筛选条件'
            : '支持搜索标题、标签、字段内容，以及已禁用模块的历史数据'
        "
        min-height="320px"
      >
        <div class="results-list">
        <div
          v-for="record in results"
          :key="record.id"
          class="result-item"
          role="button"
          tabindex="0"
          @click="goToDetail(record)"
          @keyup.enter="goToDetail(record)"
        >
          <!-- 模块标识 + 时间 -->
          <div class="result-header">
            <el-tag
              :type="getModuleTagType(record.moduleKey)"
              size="small"
              effect="plain"
            >
              {{ record.moduleName }}
            </el-tag>
            <span class="result-time">{{ formatDateTime(record.createdAt) }}</span>
          </div>

          <!-- 标题/核心内容 -->
          <h3 class="result-title">
            <HighlightText
              :text="record.title || getFirstFieldValue(record)"
              :keyword="searchKeyword"
            />
          </h3>

          <!-- 高亮片段（纯文本分段渲染，不使用 v-html） -->
          <div v-if="record.highlightSnippet" class="result-snippet">
            <HighlightText :text="record.highlightSnippet" :keyword="searchKeyword" />
          </div>

          <!-- 字段预览 -->
          <div class="result-fields" v-if="Object.keys(getDisplayFields(record)).length > 0">
            <span
              v-for="(value, key) in getDisplayFields(record)"
              :key="key"
              class="field-chip"
            >
              <span class="field-label">{{ key }}：</span>
              <span class="field-value">
                <HighlightText :text="String(value)" :keyword="searchKeyword" />
              </span>
            </span>
          </div>

          <!-- 标签 -->
          <div class="result-tags" v-if="record.tags">
            <el-tag
              v-for="tag in record.tags.split(',')"
              :key="tag"
              size="small"
              effect="plain"
            >
              {{ tag.trim() }}
            </el-tag>
          </div>

          <!-- 元信息 -->
          <div class="result-meta">
            <span>创建：{{ record.creatorName }}</span>
            <span v-if="record.remindDate" :class="getRemindClass(record.remindDate)">
              <el-icon><Clock /></el-icon>
              提醒：{{ formatDate(record.remindDate) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
      </AsyncState>
    </AppCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useModuleStore } from '@/stores/module'
import { recordApi, searchApi } from '@/api'
import { Search, Refresh, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import HighlightText from '@/components/HighlightText.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AsyncState from '@/components/ui/AsyncState.vue'
import { formatDate as formatDateSafe, formatDateTime as formatDateTimeSafe, dayDiffFromToday } from '@/utils/date'
import { getModuleTagType } from '@/utils/module'

const router = useRouter()
const route = useRoute()
const moduleStore = useModuleStore()

// 响应式状态
const results = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const searchTime = ref(0)

const searchKeyword = ref('')
const filterModuleId = ref<number | null>(null)
const filterTag = ref('')
const filterDateRange = ref<[string, string] | null>(null)

const allTags = ref<string[]>([])
const searchStartTime = ref(0)

// 从 URL query 恢复搜索参数
const initialKeyword = route.query.q as string
const initialModuleId = route.query.moduleId ? Number(route.query.moduleId) : null
const initialTag = route.query.tag as string

if (initialKeyword) searchKeyword.value = initialKeyword
if (initialModuleId) filterModuleId.value = initialModuleId
if (initialTag) filterTag.value = initialTag

// 可搜索模块（包括已禁用但有历史数据的模块）
const searchableModules = computed(() => moduleStore.allModules.filter(m => m.id))

const hasActiveFilters = computed(() =>
  filterModuleId.value !== null ||
  filterTag.value !== '' ||
  filterDateRange.value !== null
)

// 顶栏搜索在 /search 页面再次提交时只改 query，不会重建组件，这里同步关键词
watch(
  () => route.query.q,
  (value) => {
    const next = (value as string) || ''
    if (next !== searchKeyword.value) {
      searchKeyword.value = next
      page.value = 1
      handleSearch()
    }
  }
)

onMounted(async () => {
  await moduleStore.fetchAllModules() // 获取所有模块（含禁用）
  await fetchAllTags()

  // 如果有初始关键词，自动搜索
  if (searchKeyword.value) {
    await handleSearch()
  }
})

async function fetchAllTags() {
  try {
    const response = await recordApi.tags()
    allTags.value = Array.isArray(response.data) ? response.data : []
  } catch (e) {
    console.error('Failed to fetch all tags:', e)
  }
}

async function handleSearch() {
  if (!searchKeyword.value.trim() && !filterModuleId.value && !filterTag.value && !filterDateRange.value) {
    results.value = []
    total.value = 0
    return
  }

  loading.value = true
  searchStartTime.value = Date.now()

  try {
    const params: any = {
      q: searchKeyword.value.trim(),
      page: page.value,
      size: pageSize.value
    }

    if (filterModuleId.value) params.moduleId = filterModuleId.value
    if (filterTag.value) params.tag = filterTag.value
    if (filterDateRange.value) {
      params.startDate = filterDateRange.value[0]
      params.endDate = filterDateRange.value[1]
    }

    const response = await searchApi.global(params)
    // 后端返回 { data, total, page, size }，不是裸数组
    const body = response.data
    results.value = Array.isArray(body?.data) ? body.data : []
    total.value = body?.total ?? results.value.length
    searchTime.value = Date.now() - searchStartTime.value

    // 更新 URL 不刷新页面
    const query: any = { ...route.query }
    query.q = searchKeyword.value || undefined
    query.moduleId = filterModuleId.value || undefined
    query.tag = filterTag.value || undefined
    router.replace({ query })
  } catch (error) {
    console.error('Search failed:', error)
    ElMessage.error('搜索失败')
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 新条件必须回到第一页，否则会停在旧页码上出现空结果
function handleFilterChange() {
  page.value = 1
  handleSearch()
}

function handleKeywordSearch() {
  page.value = 1
  handleSearch()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  handleSearch()
}

function handleSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  handleSearch()
}

function resetFilters() {
  filterModuleId.value = null
  filterTag.value = ''
  filterDateRange.value = null
  page.value = 1
  handleSearch()
}

function goToDetail(record: any) {
  router.push(`/module/${record.moduleKey}/${record.id}`)
}

// 搜索结果接口返回的 dataJson 是 JSON 字符串（RecordVO 才是对象），统一解析后再使用
function parseDataJson(record: any): Record<string, any> {
  const raw = record?.dataJson
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function getFirstFieldValue(record: any): string {
  const dataJson = parseDataJson(record)
  const keys = Object.keys(dataJson)
  if (keys.length === 0) return '无标题记录'
  const val = dataJson[keys[0]]
  return typeof val === 'string' ? val.substring(0, 50) : String(val).substring(0, 50)
}

function getDisplayFields(record: any): Record<string, any> {
  const dataJson = parseDataJson(record)
  const result: Record<string, any> = {}
  let count = 0
  for (const [key, val] of Object.entries(dataJson)) {
    if (!['is_private'].includes(key) && count < 4) {
      result[key] = typeof val === 'string' ? val.substring(0, 40) : String(val).substring(0, 40)
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

.global-search-page {
  .search-header {
    margin-bottom: 16px;

    .search-input {
      width: 100%;
      max-width: 720px;

      :deep(.el-input__inner) {
        font-size: 16px;
        height: 48px;
        padding: 0 16px 0 48px;
      }

      :deep(.el-input__prefix) {
        font-size: 18px;
        color: $text-secondary;
      }

      :deep(.el-button) {
        height: 48px;
        padding: 0 24px;
        font-size: 15px;
      }
    }
  }

  .result-stats {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 0;
    font-size: 13px;
    color: $text-secondary;

    .keyword {
      font-weight: 600;
      color: $primary-text;
    }

    .keyword-info {
      color: $text-primary;
    }

    .time {
      color: $text-placeholder;
    }
  }

  .loading-state {
    min-height: 300px;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
    text-align: center;

    .empty-icon {
      font-size: 64px;
      color: $text-placeholder;
      margin-bottom: 20px;
    }

    p {
      font-size: 16px;
      color: $text-secondary;
      margin: 0 0 8px;
    }

    .search-tips {
      margin-top: 24px;
      padding: 20px;
      background: $bg-color;
      border: 1px solid $border-color;
      border-radius: $border-radius-base;
      text-align: left;
      max-width: 400px;

      p {
        font-weight: 600;
        margin-bottom: 12px;
        color: $text-primary;
      }

      ul {
        margin: 0;
        padding-left: 20px;
        font-size: 13px;
        color: $text-secondary;
        line-height: 2;

        li {
          margin-bottom: 4px;
        }
      }
    }
  }

  .results-card {
    .results-list {
      .result-item {
        padding: 20px;
        border-bottom: 1px solid $border-color-light;
        cursor: pointer;
        transition: background 0.2s;

        &:last-child {
          border-bottom: none;
        }

        &:hover {
          background: $hover-bg;
        }

        .result-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 8px;

          .result-time {
            font-size: 12px;
            color: $text-placeholder;
            margin-left: auto;
          }
        }

        .result-title {
          font-size: 16px;
          font-weight: 600;
          color: $text-primary;
          margin: 0 0 8px;
          line-height: 1.5;

          .highlight {
            background: $warning-bg;
            color: $warning-text;
            padding: 0 2px;
            border-radius: 2px;
          }
        }

        .result-snippet {
          font-size: 13px;
          color: $text-secondary;
          line-height: 1.6;
          margin-bottom: 8px;
          padding: 8px 12px;
          background: $bg-color;
          border-radius: $border-radius-base;
          border-left: 3px solid $primary-color;

          .highlight {
            background: $warning-bg;
            color: $warning-text;
            padding: 0 2px;
            border-radius: 2px;
          }
        }

        .result-fields {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          margin-bottom: 8px;

          .field-chip {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 2px 8px;
            background: $bg-color;
            border: 1px solid $border-color-light;
            border-radius: $border-radius-base;
            font-size: 12px;

            .field-label {
              color: $text-secondary;
            }

            .field-value {
              color: $text-primary;
              max-width: 160px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;

              .highlight {
                background: $warning-bg;
                color: $warning-text;
                padding: 0 2px;
                border-radius: 2px;
              }
            }
          }
        }

        .result-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          margin-bottom: 8px;
        }

        .result-meta {
          display: flex;
          align-items: center;
          gap: 16px;
          font-size: 12px;
          color: $text-placeholder;

          .remind-overdue {
            color: $danger-text;
            font-weight: 500;
          }

          .remind-soon {
            color: $warning-text;
            font-weight: 500;
          }
        }
      }
    }

    .pagination {
      display: flex;
      justify-content: flex-end;
      padding: 16px 0 0;
    }
  }
}

@media (max-width: 767px) {
  .global-search-page {
    .search-header .search-input {
      :deep(.el-input__inner) {
        font-size: 16px; // 防止 iOS 聚焦时缩放
      }
    }
  }
}

// ============================================================
// Phase 1：搜索页结构
// ============================================================
.search-card {
  margin-bottom: 16px;
}

.search-card .filter-select {
  width: 160px;
}

.search-card .filter-date {
  width: 260px;
}

.search-card .search-header {
  margin-bottom: 0;
}

.search-card .filter-content {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid $border-color-light;

  .filter-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .filter-label {
    font-size: 13px;
    color: $text-secondary;
    white-space: nowrap;
  }
}

.results-card .pagination {
  padding: 14px 20px;
  border-top: 1px solid $border-color-light;
}

.results-card .results-list {
  padding: 0 4px;
}

@include down(md) {
  .search-card .filter-content {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .search-card .filter-group {
    display: flex;
    align-items: center;
    gap: 8px;

    .filter-select,
    .filter-date {
      flex: 1;
      width: 100%;
    }
  }

  .results-card .pagination {
    padding: 12px;
  }
}
</style>
