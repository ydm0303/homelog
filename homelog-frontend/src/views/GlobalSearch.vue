<template>
  <div class="global-search-page">
    <!-- 搜索头部 -->
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

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-content">
        <div class="filter-group">
          <span class="filter-label">模块：</span>
          <el-select
            v-model="filterModuleId"
            placeholder="全部模块"
            size="small"
            style="width: 160px"
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
            style="width: 160px"
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
            style="width: 260px"
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
    </el-card>

    <!-- 结果统计 -->
    <div class="result-stats" v-if="!loading && (results.length > 0 || total > 0)">
      找到 <span class="keyword">{{ total }}</span> 条结果
      <span v-if="searchKeyword" class="keyword-info">关于 "<em>{{ escapeHtml(searchKeyword) }}</em>"</span>
      <span class="time">（耗时 {{ searchTime }}ms）</span>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="results.length === 0" class="empty-state">
      <el-icon class="empty-icon"><Search /></el-icon>
      <p v-if="searchKeyword">未找到相关记录</p>
      <p v-else>请输入关键词开始搜索</p>
      <div class="search-tips" v-if="searchKeyword">
        <p>尝试以下建议：</p>
        <ul>
          <li>检查拼写是否正确</li>
          <li>尝试不同的关键词</li>
          <li>减少筛选条件</li>
          <li>搜索已禁用模块的历史数据</li>
        </ul>
      </div>
    </div>

    <!-- 结果列表 -->
    <el-card v-else class="results-card" shadow="never">
      <div class="results-list">
        <div
          v-for="record in results"
          :key="record.id"
          class="result-item"
          @click="goToDetail(record)"
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
          <h3 class="result-title" v-html="highlightKeyword(record.title || getFirstFieldValue(record))" />

          <!-- 高亮片段（后端返回纯文本，需转义后再高亮，不能直接 v-html） -->
          <div
            v-if="record.highlightSnippet"
            class="result-snippet"
            v-html="highlightKeyword(record.highlightSnippet)"
          />

          <!-- 字段预览 -->
          <div class="result-fields" v-if="Object.keys(getDisplayFields(record)).length > 0">
            <span
              v-for="(value, key) in getDisplayFields(record)"
              :key="key"
              class="field-chip"
            >
              <span class="field-label">{{ key }}：</span>
              <span class="field-value" v-html="highlightKeyword(String(value))"/>
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useModuleStore } from '@/stores/module'
import { searchApi } from '@/api'
import { Search, Refresh, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

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
    // 获取所有模块的标签（用于筛选下拉）
    const tagSet = new Set<string>()
    for (const module of searchableModules.value) {
      if (!module.id) continue
      try {
        const response = await searchApi.global({
          q: '',
          moduleId: module.id,
          page: 1,
          size: 200
        })
        const body = response.data
        const list = Array.isArray(body?.data) ? body.data : []
        list.forEach((r: any) => {
          r.tags?.split(',').forEach((t: string) => t.trim() && tagSet.add(t.trim()))
        })
      } catch (e) {
        // 忽略单模块错误
      }
    }
    allTags.value = Array.from(tagSet).sort()
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
    const params: Record<string, any> = {
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
    const query = { ...route.query }
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

function getModuleTagType(moduleKey: string) {
  const types: Record<string, string> = {
    drug: 'primary',
    todo: 'success',
    note: 'info',
    idoc: 'warning'
  }
  return types[moduleKey] || 'info'
}

function getRemindClass(remindDate: string) {
  const diff = dayjs(remindDate).diff(dayjs(), 'day')
  if (diff < 0) return 'remind-overdue'
  if (diff <= 3) return 'remind-soon'
  return ''
}

function highlightKeyword(text: string) {
  if (!searchKeyword.value.trim()) return escapeHtml(text)
  // 用户输入需先转义正则元字符（如 ( [ + \），否则 new RegExp 会抛语法错误导致渲染中断
  const pattern = escapeHtml(searchKeyword.value.trim()).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapeHtml(text).replace(
    new RegExp(`(${pattern})`, 'gi'),
    '<mark class="highlight">$1</mark>'
  )
}

function escapeHtml(text: string): string {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
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

  .filter-card {
    margin-bottom: 16px;

    .filter-content {
      display: flex;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
      padding: 8px 0;

      .filter-group {
        display: flex;
        align-items: center;
        gap: 8px;

        .filter-label {
          font-size: 13px;
          color: $text-secondary;
          white-space: nowrap;
        }
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
      color: $primary-color;
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
            background: #fffbe8;
            color: #e6a23c;
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
            background: #fffbe8;
            color: #e6a23c;
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
                background: #fffbe8;
                color: #e6a23c;
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
            color: #f56c6c;
            font-weight: 500;
          }

          .remind-soon {
            color: #e6a23c;
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
    .filter-content {
      flex-direction: column;
      align-items: stretch;

      .filter-group {
        :deep(.el-select),
        :deep(.el-date-picker) {
          width: 100% !important;
        }
      }
    }

    .search-header .search-input {
      :deep(.el-input__inner) {
        font-size: 16px !important; // 防止 iOS 缩放
      }
    }
  }
}

.elderly-mode .global-search-page {
  .search-header :deep(.el-input__inner) { font-size: 18px !important; height: 56px !important; }
  .search-header :deep(.el-button) { font-size: 17px !important; padding: 0 28px !important; height: 56px !important; }
  .filter-label { font-size: 15px !important; }
  .result-title { font-size: 18px !important; }
  .result-snippet { font-size: 15px !important; }
  .field-chip { font-size: 14px !important; }
  .result-meta { font-size: 14px !important; }
}
</style>