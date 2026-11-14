<template>
  <div class="message-center-page">
    <PageHeader title="消息中心" description="家庭成员的操作提醒与到期通知">
      <template #actions>
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          :loading="markingAllRead"
          @click="markAllRead"
        >
          <el-icon><Check /></el-icon>
          全部标记已读 ({{ unreadCount }})
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选栏 -->
    <AppCard class="filter-card" padding="sm">
      <div class="filter-row">
        <el-select
          v-model="filterStatus"
          placeholder="全部状态"
          size="default"
          class="filter-status"
          @change="handleFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option label="未读" value="0" />
          <el-option label="已读" value="1" />
        </el-select>

        <el-input
          v-model="filterKeyword"
          placeholder="搜索消息内容..."
          :prefix-icon="Search"
          clearable
          size="default"
          class="filter-keyword"
          @keyup.enter="handleFilterChange"
        />

        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="filter-date"
          @change="handleFilterChange"
        />

        <el-button @click="resetFilter">重置筛选</el-button>
      </div>
    </AppCard>

    <!-- 消息列表 -->
    <AppCard class="list-card" padding="none">
      <AsyncState
        :loading="loading"
        :empty="!loading && messages.length === 0"
        :empty-icon="Bell"
        :empty-title="filterStatus === '0' ? '暂无未读消息' : '暂无消息记录'"
        empty-description="有新的家庭动态或到期提醒时，会在这里通知你"
        min-height="320px"
      >
        <el-table
          :data="messages"
          class="message-table"
          style="width: 100%"
          row-class-name="message-row"
          :row-style="getRowStyle"
        >
        <el-table-column
          type="selection"
          width="55"
          align="center"
          v-if="showSelection"
        />

        <el-table-column
          prop="content"
          label="消息内容"
          min-width="300"
          show-overflow-tooltip
        >
          <template #default="scope">
            <div class="message-content">
              <HighlightText :text="scope.row.content" :keyword="filterKeyword" />
            </div>
          </template>
        </el-table-column>

        <el-table-column
          label="关联记录"
          width="200"
        >
          <template #default="scope">
            <div v-if="scope.row.recordId" class="record-link">
              <el-tag
                :type="getModuleTagType(scope.row.moduleKey)"
                size="small"
                effect="plain"
              >
                {{ scope.row.moduleName }}
              </el-tag>
              <el-button
                link
                size="small"
                @click.stop="goToRecord(scope.row)"
              >
                {{ scope.row.recordTitle || '查看详情' }}
              </el-button>
            </div>
            <span v-else class="no-record">—</span>
          </template>
        </el-table-column>

        <el-table-column
          prop="type"
          label="类型"
          width="100"
          align="center"
        >
          <template #default="scope">
            <el-tag :type="getTypeTagType(scope.row.type)" size="small" effect="plain">
              {{ getTypeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="scope">
            <el-tag :type="scope.row.isRead === 0 ? 'primary' : 'info'" size="small" effect="plain">
              {{ scope.row.isRead === 0 ? '未读' : '已读' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="createdAt"
          label="接收时间"
          width="180"
        >
          <template #default="scope">
            {{ formatDateTime(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="140"
          fixed="right"
          align="center"
        >
          <template #default="scope">
            <el-button
              v-if="scope.row.isRead === 0"
              link
              size="small"
              @click.stop="markRead(scope.row)"
            >
              标记已读
            </el-button>
            <el-button
              link
              size="small"
              type="danger"
              @click.stop="deleteMessage(scope.row)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { messageApi } from '@/api'
import { Search, Bell, Check, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HighlightText from '@/components/HighlightText.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AsyncState from '@/components/ui/AsyncState.vue'
import { formatDateTime as formatDateTimeSafe } from '@/utils/date'
import { getModuleTagType } from '@/utils/module'

const router = useRouter()
const route = useRoute()

// 响应式状态
const messages = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const unreadCount = ref(0)
const markingAllRead = ref(false)

const filterStatus = ref<string | number>('')
const filterKeyword = ref('')
const filterDateRange = ref<[string, string] | null>(null)
const showSelection = ref(false)

// 从 URL query 恢复搜索关键词
const initialKeyword = route.query.q as string
if (initialKeyword) {
  filterKeyword.value = initialKeyword
}

let unreadTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await fetchMessages()
  await fetchUnreadCount()

  // 启动未读数轮询
  startUnreadPolling()
})

onUnmounted(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})

function startUnreadPolling() {
  unreadTimer = setInterval(async () => {
    if (!document.hidden) {
      await fetchUnreadCount()
    }
  }, 30000)
}

async function fetchMessages() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: pageSize.value
    }

    if (filterStatus.value !== '') params.isRead = filterStatus.value
    if (filterKeyword.value) params.keyword = filterKeyword.value
    if (filterDateRange.value) {
      params.startDate = filterDateRange.value[0]
      params.endDate = filterDateRange.value[1]
    }

    const response = await messageApi.list(params)
    // 后端返回 { data, total, page, size }，不是裸数组
    const body = response.data
    messages.value = Array.isArray(body?.data) ? body.data : []
    total.value = body?.total ?? messages.value.length
  } catch (error) {
    console.error('Failed to fetch messages:', error)
    ElMessage.error('获取消息列表失败')
  } finally {
    loading.value = false
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

function handleFilterChange() {
  page.value = 1
  fetchMessages()
}

function resetFilter() {
  filterStatus.value = ''
  filterKeyword.value = ''
  filterDateRange.value = null
  handleFilterChange()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  fetchMessages()
}

function handleSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  fetchMessages()
}

async function markRead(message: any) {
  try {
    await messageApi.markRead(message.id)
    message.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    ElMessage.success('已标记已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function markAllRead() {
  markingAllRead.value = true
  try {
    await messageApi.markAllRead()
    messages.value.forEach(m => { m.isRead = 1 })
    unreadCount.value = 0
    ElMessage.success('全部标记已读')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    markingAllRead.value = false
  }
}

async function deleteMessage(message: any) {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await messageApi.remove(message.id)
    const idx = messages.value.findIndex(m => m.id === message.id)
    if (idx !== -1) {
      messages.value.splice(idx, 1)
      total.value = Math.max(0, total.value - 1)
      if (message.isRead === 0) unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    ElMessage.success('已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function goToRecord(message: any) {
  if (!message.recordId || !message.moduleKey) return
  router.push(`/module/${message.moduleKey}/${message.recordId}`)
}

function getRowStyle({ row }: any) {
  return row.isRead === 0 ? { 'font-weight': 500 } : {}
}

function getTypeTagType(type: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  const types: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
    remind: 'warning',
    system: 'info',
    mention: 'primary'
  }
  return types[type] || 'info'
}

function getTypeLabel(type: string) {
  const labels: Record<string, string> = {
    remind: '到期提醒',
    system: '系统通知',
    mention: '@提及'
  }
  return labels[type] || type
}

function formatDateTime(dateStr: string) {
  return formatDateTimeSafe(dateStr)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.message-center-page {
  .filter-card {
    margin-bottom: 16px;

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      padding: 8px 0;
    }
  }

  .list-card {
    :deep(.el-table) {
      .message-content {
        line-height: 1.6;
        word-break: break-word;

        .highlight {
          background: $warning-bg;
          color: $warning-text;
          padding: 0 2px;
          border-radius: 2px;
        }
      }

      .record-link {
        display: flex;
        align-items: center;
        gap: 8px;

        .el-button {
          padding: 0;
          color: $primary-text;
          font-size: 13px;
        }
      }

      .no-record {
        color: $text-placeholder;
        font-size: 13px;
      }

      .message-row {
        transition: background 0.2s;

        &:hover {
          background: $hover-bg;
        }
      }
    }
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 0;
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
      margin: 0;
      font-size: 15px;
    }
  }
}

@media (max-width: 767px) {
  .message-center-page {
    .page-header {
      flex-direction: column;
      align-items: stretch;
      gap: 12px;

      h1 { font-size: 18px; }
    }

    .filter-row {
      flex-direction: column;
      align-items: stretch;

      .filter-status,
      .filter-keyword,
      .filter-date {
        width: 100%;
      }

      :deep(.el-button) {
        width: 100%;
      }
    }
  }
}

// ============================================================
// Phase 1：消息中心结构
// ============================================================
.message-center-page {
  .filter-card {
    margin-bottom: 16px;
  }

  .filter-status {
    width: 140px;
  }

  .filter-keyword,
  .filter-date {
    width: 280px;
  }

  .list-card .pagination {
    padding: 14px 20px;
    border-top: 1px solid $border-color-light;
  }
}
</style>
