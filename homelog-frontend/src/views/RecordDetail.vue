<template>
  <div class="record-detail-page" v-if="record">
    <!-- 返回按钮 + 操作栏 -->
    <PageHeader :title="record.title || '无标题记录'" back @back="goBack">
      <template #tag>
        <el-tag :type="getModuleTagType(record.moduleKey)" size="default">
          {{ record.moduleName }}
        </el-tag>
      </template>

      <template #actions>
        <el-dropdown size="default">
          <el-button>
            操作 <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="canEdit" @click="goToEdit">
                <el-icon><Edit /></el-icon>
                编辑
              </el-dropdown-item>
              <el-dropdown-item v-if="canDelete" @click="handleDelete" class="danger-item">
                <el-icon><Delete /></el-icon>
                删除
              </el-dropdown-item>
              <el-dropdown-item v-if="record.isPrivate === 0" @click="togglePrivate(true)">
                <el-icon><Lock /></el-icon>
                设为私密
              </el-dropdown-item>
              <el-dropdown-item v-if="record.isPrivate === 1" @click="togglePrivate(false)">
                <el-icon><Unlock /></el-icon>
                取消私密
              </el-dropdown-item>
              <el-dropdown-item @click="exportRecord">
                <el-icon><Download /></el-icon>
                导出记录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </PageHeader>

    <!-- 私密标记 -->
    <el-alert
      v-if="record.isPrivate === 1 && isAdmin"
      title="此记录为私密记录，仅管理员可见"
      type="warning"
      show-icon
      class="private-alert"
    />

    <!-- 详情内容 -->
    <el-card class="detail-card" shadow="never">
      <!-- 药品专属：有效期状态 + 库存 + 快捷操作 -->
      <div v-if="isDrugModule" class="drug-panel">
        <div class="drug-status" :class="expiryStatusClass">
          <span class="drug-status-label">有效期</span>
          <span class="drug-status-date">
            {{ record.dataJson.expire_date ? formatDate(record.dataJson.expire_date) : '未设置' }}
          </span>
          <span v-if="expiryStatusText" class="drug-status-days">{{ expiryStatusText }}</span>
        </div>
        <div class="drug-stock" :class="{ 'stock-low': isLowStock }">
          <span class="drug-status-label">库存</span>
          <span class="drug-status-value">
            {{ record.dataJson.quantity ?? '—' }} {{ record.dataJson.unit || '' }}
          </span>
          <span v-if="isLowStock" class="drug-status-days">库存不足</span>
        </div>
        <div v-if="canEdit" class="drug-actions">
          <el-button size="small" :disabled="busy" @click="adjustQuantity(-1)">
            <el-icon><Minus /></el-icon>
            已服用
          </el-button>
          <el-button size="small" type="primary" plain :disabled="busy" @click="adjustQuantity(1)">
            <el-icon><Plus /></el-icon>
            补货
          </el-button>
        </div>
      </div>

      <!-- 图片画廊 -->
      <div v-if="record.imagesJson && record.imagesJson.length > 0" class="image-gallery">
        <h3 class="section-title">
          <el-icon><Picture /></el-icon>
          图片 ({{ record.imagesJson.length }})
        </h3>
        <div class="gallery-grid">
          <div
            v-for="(img, index) in record.imagesJson"
            :key="index"
            class="gallery-item"
            role="button"
            tabindex="0"
            :aria-label="`查看图片 ${Number(index) + 1}`"
            @click="openImagePreview(Number(index))"
            @keyup.enter="openImagePreview(Number(index))"
          >
            <img :src="getImageUrl(img)" :alt="`图片 ${Number(index) + 1}`" loading="lazy" />
            <div class="image-overlay">
              <el-icon><ZoomIn /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 字段详情 -->
      <div class="fields-section">
        <h3 class="section-title">
          <el-icon><Document /></el-icon>
          详细信息
        </h3>

        <div class="fields-grid">
          <div
            v-for="field in visibleFields"
            :key="field.fieldKey"
            class="field-item"
          >
            <label class="field-label">{{ field.fieldLabel }}</label>
            <div class="field-value">
              <span
                v-if="isEmptyValue(record.dataJson[field.fieldKey])"
                class="empty-value"
              >—</span>

              <template v-else-if="field.fieldType === 'image'">
                <img
                  v-for="(src, idx) in getImageList(record.dataJson[field.fieldKey])"
                  :key="idx"
                  :src="src"
                  alt=""
                  class="inline-image"
                />
              </template>

              <el-tag
                v-else-if="field.fieldType === 'member'"
                effect="plain"
                size="small"
              >
                {{ getMemberName(record.dataJson[field.fieldKey]) }}
              </el-tag>

              <template v-else-if="field.fieldType === 'multi_select'">
                <el-tag
                  v-for="label in getOptionLabels(field, record.dataJson[field.fieldKey])"
                  :key="label"
                  effect="plain"
                  size="small"
                  class="value-tag"
                >
                  {{ label }}
                </el-tag>
              </template>

              <el-tag
                v-else-if="field.fieldType === 'select'"
                effect="plain"
                size="small"
              >
                {{ getOptionLabel(field, record.dataJson[field.fieldKey]) }}
              </el-tag>

              <el-tag
                v-else-if="field.fieldType === 'boolean'"
                :type="record.dataJson[field.fieldKey] ? 'success' : 'info'"
                size="small"
              >
                {{ record.dataJson[field.fieldKey] ? '是' : '否' }}
              </el-tag>

              <pre
                v-else-if="field.fieldType === 'textarea'"
                class="textarea-value"
              >{{ record.dataJson[field.fieldKey] }}</pre>

              <template v-else-if="field.fieldType === 'date'">
                {{ formatFieldDate(record.dataJson[field.fieldKey]) }}
              </template>

              <template v-else>
                {{ record.dataJson[field.fieldKey] }}
              </template>
            </div>
          </div>
        </div>

        <!-- 私密字段提示 -->
        <div v-if="hasPrivateFields && !isAdmin" class="private-fields-notice">
          <el-icon><Lock /></el-icon>
          <span>此记录包含私密字段，仅管理员可见</span>
        </div>
      </div>

      <!-- 标签 -->
      <div v-if="record.tags" class="tags-section">
        <h3 class="section-title">
          <el-icon><PriceTag /></el-icon>
          标签
        </h3>
        <div class="tags-list">
          <el-tag
            v-for="tag in record.tags.split(',')"
            :key="tag"
            size="default"
            effect="plain"
            class="tag-item"
          >
            {{ tag.trim() }}
          </el-tag>
        </div>
      </div>

      <!-- 元信息 -->
      <div class="meta-section">
        <h3 class="section-title">
          <el-icon><InfoFilled /></el-icon>
          记录信息
        </h3>
        <div class="meta-grid">
          <div class="meta-item">
            <span class="meta-label">创建人</span>
            <span class="meta-value">{{ record.creatorName }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">创建时间</span>
            <span class="meta-value">{{ formatDateTime(record.createdAt) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">更新时间</span>
            <span class="meta-value">{{ formatDateTime(record.updatedAt) }}</span>
          </div>
          <div v-if="record.remindDate" class="meta-item">
            <span class="meta-label">提醒日期</span>
            <span class="meta-value" :class="getRemindClass(record.remindDate)">
              {{ formatDate(record.remindDate) }}
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">所属模块</span>
            <span class="meta-value">{{ record.moduleName }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 图片预览弹窗 -->
    <el-dialog
      v-model="imagePreviewVisible"
      :fullscreen="true"
      :close-on-click-modal="true"
      class="image-preview-dialog"
    >
      <img
        v-if="currentPreviewIndex >= 0"
        :src="getImageUrl(record.imagesJson[currentPreviewIndex])"
        alt="预览图片"
        class="preview-image"
      />
      <template #footer>
        <div class="preview-nav">
          <el-button
            v-if="currentPreviewIndex > 0"
            @click="currentPreviewIndex--"
            circle
          >
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <span>{{ currentPreviewIndex + 1 }} / {{ record.imagesJson.length }}</span>
          <el-button
            v-if="currentPreviewIndex < record.imagesJson.length - 1"
            @click="currentPreviewIndex++"
            circle
          >
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>

  <!-- 加载中 -->
  <div v-else class="loading-state">
    <el-skeleton :rows="5" animated />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { recordApi, spaceApi } from '@/api'
import {
  ArrowLeft, ArrowDown, Edit, Delete, Lock, Unlock, Download,
  Picture, Document, PriceTag, InfoFilled, ZoomIn, ArrowRight, Minus, Plus
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import { formatDate as formatDateSafe, formatDateTime as formatDateTimeSafe, dayDiffFromToday } from '@/utils/date'
import { resolveFileUrl } from '@/utils/file'
import { getModuleTagType } from '@/utils/module'
import { getExpiryState } from '@/utils/drug'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

const recordId = route.params.recordId as string
const moduleKey = route.params.moduleKey as string

const record = ref<any>(null)
const loading = ref(true)
const busy = ref(false)

const imagePreviewVisible = ref(false)
const currentPreviewIndex = ref(0)

// 空间成员（用于把 member 字段的 ID 映射成姓名）
const members = ref<any[]>([])

const isAdmin = computed(() => authStore.isAdmin)

const canEdit = computed(() => {
  if (!authStore.canWrite) return false
  if (isAdmin.value) return true
  return Number(record.value?.creatorId) === Number(authStore.user?.id)
})

const canDelete = computed(() => authStore.canDelete)

const currentModule = computed(() => moduleStore.getModuleByKey(moduleKey))
const allFields = computed(() => currentModule.value?.fields || [])

const isDrugModule = computed(() => moduleKey === 'drug')

// 药品有效期状态（列表/详情共用 getExpiryState，避免各写各的）
const expiryState = computed(() =>
  isDrugModule.value ? getExpiryState(record.value?.dataJson?.expire_date) : null
)
const expiryStatusClass = computed(() => {
  const s = expiryState.value
  if (!s) return ''
  return s.status === 'expired' ? 'expired' : s.status === 'soon' ? 'soon' : 'normal'
})
const expiryStatusText = computed(() => {
  const s = expiryState.value
  if (!s) return ''
  if (s.status === 'expired') return `已过期 ${Math.abs(s.days)} 天`
  if (s.days === 0) return '今天到期'
  return `剩余 ${s.days} 天`
})
const isLowStock = computed(() => {
  const q = Number(record.value?.dataJson?.quantity)
  const min = Number(record.value?.dataJson?.min_stock)
  if (Number.isNaN(q) || Number.isNaN(min)) return false
  return q < min
})

// 可见字段（过滤私密字段；药品模块标题由标题栏展示，详情里不再重复 title 字段）
const visibleFields = computed(() => {
  const fields = isAdmin.value
    ? allFields.value
    : allFields.value.filter(f => f.isPrivate !== 1)

  if (isDrugModule.value) return fields
  return fields.filter(f => f.fieldKey !== 'title')
})

const hasPrivateFields = computed(() => {
  return allFields.value.some(f => f.isPrivate === 1)
})

onMounted(async () => {
  await moduleStore.fetchModules()
  await fetchRecord()
  await fetchMembers()
})

async function fetchMembers() {
  try {
    const response = await spaceApi.getMembers()
    members.value = response.data || []
  } catch (e) {
    console.error('Failed to fetch space members:', e)
  }
}

// 兼容后端不同版本的序列化形式：dataJson 可能是对象或 JSON 字符串，imagesJson 可能是数组、JSON 字符串或逗号分隔
function normalizeRecord(raw: any) {
  const record = { ...raw }

  if (typeof record.dataJson === 'string') {
    try {
      record.dataJson = JSON.parse(record.dataJson)
    } catch {
      record.dataJson = {}
    }
  }
  if (!record.dataJson || typeof record.dataJson !== 'object') {
    record.dataJson = {}
  }

  if (typeof record.imagesJson === 'string') {
    try {
      const parsed = JSON.parse(record.imagesJson)
      record.imagesJson = Array.isArray(parsed) ? parsed : []
    } catch {
      record.imagesJson = record.imagesJson
        .split(',')
        .map((item: string) => item.trim())
        .filter(Boolean)
    }
  }
  if (!Array.isArray(record.imagesJson)) {
    record.imagesJson = record.imagesJson ? [record.imagesJson] : []
  }

  return record
}

async function fetchRecord() {
  loading.value = true
  try {
    const moduleId = currentModule.value?.id
    if (!moduleId) {
      ElMessage.error('模块不存在或已禁用')
      router.push('/')
      return
    }

    const response = await recordApi.detail(Number(recordId), moduleId)
    record.value = normalizeRecord(response.data)
  } catch (error) {
    console.error('Failed to fetch record:', error)
    ElMessage.error('获取记录详情失败')
    router.push(`/module/${moduleKey}`)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function goToEdit() {
  router.push(`/module/${moduleKey}/${recordId}/edit`)
}

async function handleDelete() {
  const moduleId = currentModule.value?.id
  if (!moduleId) return

  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？删除后不可恢复', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await recordApi.delete(Number(recordId), moduleId)
    ElMessage.success('删除成功')
    router.push(`/module/${moduleKey}`)
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function togglePrivate(isPrivate: boolean) {
  const moduleId = currentModule.value?.id
  if (!moduleId) return

  try {
    await recordApi.update({
      recordId: Number(recordId),
      moduleId,
      dataJson: { ...record.value.dataJson, is_private: isPrivate }
    })
    ElMessage.success(isPrivate ? '已设为私密' : '已取消私密')
    record.value.isPrivate = isPrivate ? 1 : 0
  } catch {
    ElMessage.error('操作失败')
  }
}

function exportRecord() {
  // TODO: 实现单条记录导出
  ElMessage.info('单条记录导出功能开发中...')
}

async function adjustQuantity(delta: number) {
  const moduleId = currentModule.value?.id
  if (!moduleId) return

  const current = Number(record.value?.dataJson?.quantity ?? 0)
  const next = Math.max(0, current + delta)
  if (next === current) return

  busy.value = true
  try {
    await recordApi.update({
      recordId: Number(recordId),
      moduleId,
      dataJson: { ...record.value.dataJson, quantity: next }
    })
    record.value.dataJson.quantity = next
    ElMessage.success(delta > 0 ? '已补货 +1' : '已服用 -1')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    busy.value = false
  }
}

function openImagePreview(index: number) {
  currentPreviewIndex.value = index
  imagePreviewVisible.value = true
}

function getImageUrl(path: string) {
  return resolveFileUrl(path)
}

function isEmptyValue(value: any): boolean {
  return value === null || value === undefined || value === ''
}

// 图片字段可能是单张、多张或逗号分隔字符串，统一成可直接用于 src 的 URL 列表
function getImageList(value: any): string[] {
  const list = Array.isArray(value)
    ? value
    : typeof value === 'string'
      ? value.split(',').map((item: string) => item.trim())
      : [value]
  return list.filter(Boolean).map((v: string) => getImageUrl(v))
}

function parseOptions(field: any): Array<{ label: string; value: any }> {
  if (!field.optionsJson) return []
  try {
    const parsed = JSON.parse(field.optionsJson)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function getOptionLabel(field: any, value: any): string {
  const opt = parseOptions(field).find(o => String(o.value) === String(value))
  return opt?.label || String(value)
}

function getOptionLabels(field: any, value: any): string[] {
  const list = Array.isArray(value) ? value : [value]
  return list.map(v => getOptionLabel(field, v))
}

// 把 member 字段存的值（用户 ID）映射成昵称/用户名
function getMemberName(value: any): string {
  const member = members.value.find((m: any) => String(m.id) === String(value))
  return member?.nickName || member?.username || String(value)
}

function formatFieldDate(value: any): string {
  return formatDateSafe(value)
}

function getRemindClass(remindDate: string) {
  const diff = dayDiffFromToday(remindDate)
  if (diff === null) return ''
  if (diff < 0) return 'remind-overdue'
  if (diff <= 3) return 'remind-soon'
  return ''
}

function formatDate(dateStr: string) {
  return formatDateSafe(dateStr)
}

function formatDateTime(dateStr: string) {
  return formatDateTimeSafe(dateStr, 'YYYY-MM-DD HH:mm:ss')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.record-detail-page {
  .detail-card {
    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
      margin: 0 0 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid $border-color-light;

      el-icon {
        font-size: 18px;
        color: $primary-text;
      }
    }

    .section-title:first-child {
      margin-top: 0;
    }
  }

  .image-gallery {
    margin-bottom: 24px;

    .gallery-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 12px;
    }

    .gallery-item {
      position: relative;
      aspect-ratio: 4/3;
      border-radius: $border-radius-base;
      overflow: hidden;
      cursor: pointer;
      border: 1px solid $border-color;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s ease;
      }

      &:hover img {
        transform: scale(1.05);
      }

      .image-overlay {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.4);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.2s ease;
        color: $card-bg;
        font-size: 24px;
      }

      &:hover .image-overlay {
        opacity: 1;
      }
    }
  }

  .drug-panel {
    display: flex;
    align-items: stretch;
    gap: 12px;
    flex-wrap: wrap;
    padding: 16px;
    margin-bottom: 24px;
    background: $surface-warm;
    border: 1px solid $border-color-light;
    border-radius: $border-radius-base;

    .drug-status,
    .drug-stock {
      flex: 1;
      min-width: 160px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 12px 16px;
      border-radius: $border-radius-base;
      background: $card-bg;

      .drug-status-label {
        font-size: $text-caption;
        color: $text-secondary;
      }

      .drug-status-date,
      .drug-status-value {
        font-size: 20px;
        font-weight: 600;
        color: $text-primary;
      }

      .drug-status-days {
        font-size: 13px;
        font-weight: 500;
      }

      &.expired {
        .drug-status-date,
        .drug-status-days {
          color: $danger-text;
        }
      }

      &.soon {
        .drug-status-date,
        .drug-status-days {
          color: $warning-text;
        }
      }

      &.normal {
        .drug-status-date,
        .drug-status-days {
          color: $success-text;
        }
      }

      &.stock-low {
        .drug-status-value,
        .drug-status-days {
          color: $danger-text;
        }
      }
    }

    .drug-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .fields-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
  }

  .field-item {
    min-width: 0;

    .field-label {
      display: block;
      font-size: $text-label;
      font-weight: $font-weight-medium;
      color: $text-secondary;
      margin-bottom: 6px;
    }

    .field-value {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: $text-primary;
      line-height: 1.6;
      word-break: break-word;

      .empty-value {
        color: $text-placeholder;
        font-style: italic;
      }

      .inline-image {
        max-width: 100%;
        max-height: 200px;
        border-radius: $border-radius-base;
        border: 1px solid $border-color-light;
      }

      .value-tag {
        margin: 0;
      }

      .textarea-value {
        width: 100%;
        margin: 0;
        font-family: inherit;
        font-size: 14px;
        line-height: 1.6;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
  }

  .private-fields-notice {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    margin-top: 16px;
    background: $warning-bg;
    border: 1px solid $warning-border;
    border-radius: $border-radius-base;
    color: $warning-text;
    font-size: 13px;
  }

  .tags-section {
    margin-top: 24px;
  }

  .tags-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .tag-item {
    font-size: 13px;
  }

  .meta-section {
    margin-top: 24px;
  }

  .meta-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 12px;
  }

  .meta-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;

    .meta-label {
      font-size: $text-caption;
      color: $text-secondary;
    }

    .meta-value {
      font-size: 14px;
      color: $text-primary;
      font-weight: 500;

      &.remind-overdue {
        color: $danger-text;
      }

      &.remind-soon {
        color: $warning-text;
      }
    }
  }
}

.image-preview-dialog {
  :deep(.el-dialog__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    overflow: hidden;
  }

  .preview-image {
    max-width: 100%;
    max-height: 90vh;
    object-fit: contain;
  }

  .preview-nav {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 16px;
    color: $text-primary;
    font-size: $text-body;
  }
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

// 响应式
@media (max-width: 767px) {
  .record-detail-page {
    .detail-header {
      flex-direction: column;
      align-items: stretch;
      gap: 12px;
    }

    .detail-title h1 {
      font-size: 20px;
    }

    .fields-grid {
      grid-template-columns: 1fr;
    }

    .meta-grid {
      grid-template-columns: 1fr;
    }

    .gallery-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}

// 长辈模式
</style>
