<template>
  <div class="message-center-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>消息中心</h1>
      <div class="header-actions">
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          size="small"
          @click="markAllRead"
          :loading="markingAllRead"
        >
          <el-icon><Check /></el-icon>
          全部标记已读 ({{ unreadCount }})
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-select
          v-model="filterStatus"
          placeholder="全部状态"
          size="default"
          style="width: 140px"
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
          style="width: 280px"
          @keyup.enter="handleFilterChange"
        />

        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 280px"
          @change="handleFilterChange"
        />

        <el-button @click="resetFilter">重置筛选</el-button>
      </div>
    </el-card>

    <!-- 消息列表 -->
    <el-card class="list-card" shadow="never">
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="messages.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Bell /></el-icon>
        <p v-if="filterStatus === '0'">暂无未读消息</p>
        <p v-else>暂无消息记录</p>
      </div>

      <el-table
        v-else
        :data="messages"
        border
        stripe
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
            <div class="message-content" v-html="highlightKeyword(scope.row.content)" />
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { messageApi } from '@/api'
import { Search, Bell, Check, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

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

onMounted(async () => {
  await fetchMessages()
  await fetchUnreadCount()

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
    // 后端暂无删除接口，前端本地移除
    const idx = messages.value.findIndex(m => m.id === message.id)
    if (idx !== -1) {
      messages.value.splice(idx, 1)
      total.value--
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

function getModuleTagType(moduleKey: string) {
  const types: Record<string, string> = {
    drug: 'primary',
    todo: 'success',
    note: 'info',
    idoc: 'warning'
  }
  return types[moduleKey] || 'info'
}

function getTypeTagType(type: string) {
  const types: Record<string, string> = {
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

function highlightKeyword(content: string) {
  if (!filterKeyword.value) return escapeHtml(content)
  // 用户输入需先转义正则元字符（如 ( [ + \），否则 new RegExp 会抛语法错误导致渲染中断
  const keyword = escapeHtml(filterKeyword.value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapeHtml(content).replace(
    new RegExp(`(${keyword})`, 'gi'),
    '<mark class="highlight">$1</mark>'
  )
}

function escapeHtml(text: string): string {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function formatDateTime(dateStr: string) {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.message-center-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h1 {
      font-size: 22px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
    }
  }

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
          background: #fffbe8;
          color: #e6a23c;
          padding: 0 2px;
          border-radius: 2px;
        }
      }

      .record-link {
        display: flex;
        align-items: center;
        gap: 8px;

        :deep(.el-button) {
          padding: 0;
          color: $primary-color;
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

      :deep(.el-select),
      :deep(.el-input),
      :deep(.el-date-picker) {
        width: 100% !important;
      }

      :deep(.el-button) {
        width: 100%;
      }
    }
  }
}

.elderly-mode .message-center-page {
  :deep(.el-table) { font-size: 15px !important; }
  :deep(.el-pagination) { font-size: 14px !important; }
  .page-header h1 { font-size: 24px !important; }
  .message-content { font-size: 15px !important; }
}
</style>