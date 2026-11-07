<template>
  <div class="record-detail-page" v-if="record">
    <!-- 返回按钮 + 操作栏 -->
    <div class="detail-header">
      <el-button @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <div class="detail-title">
        <el-tag :type="getModuleTagType(record.moduleKey)" size="medium">
          {{ record.moduleName }}
        </el-tag>
        <h1 v-if="record.title">{{ record.title }}</h1>
        <h1 v-else class="untitled">无标题记录</h1>
      </div>

      <div class="detail-actions">
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
      </div>
    </div>

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
            @click="openImagePreview(index)"
          >
            <img :src="getImageUrl(img)" :alt="`图片 ${index + 1}`" loading="lazy" />
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
            <div class="field-value" v-html="formatFieldValue(field, record.dataJson[field.fieldKey])" />
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
            size="medium"
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
import { recordApi } from '@/api'
import {
  ArrowLeft, ArrowDown, Edit, Delete, Lock, Unlock, Download,
  Picture, Document, PriceTag, InfoFilled, ZoomIn, ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

const recordId = route.params.recordId as string
const moduleKey = route.params.moduleKey as string

const record = ref<any>(null)
const loading = ref(true)

const imagePreviewVisible = ref(false)
const currentPreviewIndex = ref(0)

const isAdmin = computed(() => authStore.isAdmin)
const isEditor = computed(() => authStore.isEditor)
const isReadonly = computed(() => authStore.isReadonly)

const canEdit = computed(() => {
  if (isReadonly.value) return false
  if (isAdmin.value) return true
  return record.value?.creatorId === authStore.user?.id
})

const canDelete = computed(() => isAdmin.value)

const currentModule = computed(() => moduleStore.getModuleByKey(moduleKey))
const allFields = computed(() => currentModule.value?.fields || [])

// 可见字段（过滤私密字段）
const visibleFields = computed(() => {
  if (isAdmin.value) return allFields.value
  return allFields.value.filter(f => f.isPrivate !== 1)
})

const hasPrivateFields = computed(() => {
  return allFields.value.some(f => f.isPrivate === 1)
})

onMounted(async () => {
  await moduleStore.fetchModules()
  await fetchRecord()
})

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
    record.value = response.data
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

function openImagePreview(index: number) {
  currentPreviewIndex.value = index
  imagePreviewVisible.value = true
}

function getImageUrl(path: string) {
  if (path.startsWith('http') || path.startsWith('//')) return path
  return `${import.meta.env.VITE_API_BASE_URL || '/api'}/files${path}`
}

function formatFieldValue(field: any, value: any): string {
  if (value === null || value === undefined || value === '') {
    return '<span class="empty-value">—</span>'
  }

  switch (field.fieldType) {
    case 'image':
      if (Array.isArray(value)) {
        return value.map((v: string) =>
          `<img src="${escapeHtml(getImageUrl(v))}" alt="" class="inline-image" />`
        ).join('')
      }
      return `<img src="${escapeHtml(getImageUrl(value))}" alt="" class="inline-image" />`

    case 'member':
      return `<el-tag effect="plain" size="small">成员 ID: ${escapeHtml(String(value))}</el-tag>`

    case 'select':
    case 'multi_select':
      if (field.optionsJson) {
        try {
          const options = JSON.parse(field.optionsJson)
          if (Array.isArray(value)) {
            return value.map((v: string) => {
              const opt = options.find((o: any) => o.value === v)
              return `<el-tag effect="plain" size="small">${escapeHtml(String(opt?.label || v))}</el-tag>`
            }).join(' ')
          }
          const opt = options.find((o: any) => o.value === value)
          return `<el-tag effect="plain" size="small">${escapeHtml(String(opt?.label || value))}</el-tag>`
        } catch {
          return escapeHtml(String(value))
        }
      }
      return escapeHtml(String(value))

    case 'boolean':
      return value ? '<el-tag type="success" size="small">是</el-tag>' : '<el-tag type="info" size="small">否</el-tag>'

    case 'date':
      return escapeHtml(dayjs(value).format('YYYY-MM-DD'))

    case 'number':
      return `<span class="number-value">${escapeHtml(String(value))}</span>`

    case 'textarea':
      return `<pre class="textarea-value">${escapeHtml(String(value))}</pre>`

    default:
      return escapeHtml(String(value))
  }
}

function escapeHtml(text: string): string {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
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

function formatDate(dateStr: string) {
  return dayjs(dateStr).format('YYYY-MM-DD')
}

function formatDateTime(dateStr: string) {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.record-detail-page {
  .detail-header {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid $border-color;

    .back-btn {
      margin-top: 4px;
    }

    .detail-title {
      flex: 1;
      min-width: 0;
    }

    h1 {
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
      margin: 8px 0 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .untitled {
      color: $text-placeholder;
      font-style: italic;
    }

    .detail-actions {
      flex-shrink: 0;
    }
  }

  .private-alert {
    margin-bottom: 16px;
    :deep(.el-alert) {
      border-radius: $border-radius-base;
    }
  }

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
        color: $primary-color;
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
        color: #fff;
        font-size: 24px;
      }

      &:hover .image-overlay {
        opacity: 1;
      }
    }
  }

  .fields-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
  }

  .field-item {
    padding: 12px 16px;
    background: $bg-color;
    border: 1px solid $border-color-light;
    border-radius: $border-radius-base;

    .field-label {
      display: block;
      font-size: 13px;
      font-weight: 500;
      color: $text-secondary;
      margin-bottom: 6px;
    }

    .field-value {
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
    }
  }

  .private-fields-notice {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    margin-top: 16px;
    background: #fffbe8;
    border: 1px solid #fde2a2;
    border-radius: $border-radius-base;
    color: #e6a23c;
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
    padding: 12px 16px;
    background: $bg-color;
    border: 1px solid $border-color-light;
    border-radius: $border-radius-base;

    .meta-label {
      font-size: 12px;
      color: $text-secondary;
    }

    .meta-value {
      font-size: 14px;
      color: $text-primary;
      font-weight: 500;

      &.remind-overdue {
        color: #f56c6c;
      }

      &.remind-soon {
        color: #e6a23c;
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
    color: #fff;
    font-size: 14px;
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
.elderly-mode .record-detail-page {
  .detail-title h1 { font-size: 28px !important; }
  .field-label { font-size: 15px !important; }
  .field-value { font-size: 16px !important; }
  .meta-label { font-size: 14px !important; }
  .meta-value { font-size: 16px !important; }
  .section-title { font-size: 18px !important; }
}
</style>