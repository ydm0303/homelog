<template>
  <div class="record-edit-page" v-if="module">
    <!-- 页面头部 -->
    <div class="edit-header">
      <el-button @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <div class="edit-title">
        <el-tag :type="getModuleTagType(module.moduleKey)" size="medium">
          {{ module.name }}
        </el-tag>
        <h1>{{ isEdit ? '编辑记录' : '新增记录' }}</h1>
      </div>

      <div class="edit-actions">
        <el-button
          v-if="isEdit"
          @click="handleDelete"
          type="danger"
          :disabled="saving"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
        <el-button @click="handleSave" type="primary" :loading="saving" :disabled="!canSave">
          <el-icon><Check /></el-icon>
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </div>

    <!-- 表单 -->
    <el-form
      ref="formRef"
      :model="formData"
      :rules="validationRules"
      label-width="120px"
      class="edit-form"
      @submit.native.prevent="handleSave"
    >
      <!-- 标题字段 -->
      <el-form-item
        v-if="hasTitleField"
        label="标题"
        prop="title"
        :rules="[{ required: true, message: '请输入标题', trigger: 'blur' }]"
      >
        <el-input v-model="formData.title" placeholder="请输入标题" clearable />
      </el-form-item>

      <!-- 动态字段渲染 -->
      <template v-for="field in visibleFields" :key="field.id">
        <el-form-item
          :label="field.fieldLabel + (field.isRequired === 1 ? ' *' : '')"
          :prop="field.fieldKey"
          :rules="getFieldRules(field)"
        >
          <DynamicFieldRenderer
            :field="field"
            :value="formData.dataJson[field.fieldKey]"
            @update:value="val => updateFieldValue(field.fieldKey, val)"
            :disabled="isReadonly || field.fieldKey === 'creator_id'"
          />
        </el-form-item>
      </template>

      <!-- 提醒日期 -->
      <el-form-item label="提醒日期" prop="remindDate">
        <el-date-picker
          v-model="formData.remindDate"
          type="date"
          placeholder="选择提醒日期"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 100%"
        />
      </el-form-item>

      <!-- 标签 -->
      <el-form-item label="标签" prop="tags">
        <el-input
          v-model="formData.tags"
          placeholder="多个标签用逗号分隔，如：家庭,重要,药品"
          clearable
        />
      </el-form-item>

      <!-- 私密标记 -->
      <el-form-item v-if="isAdmin" label="私密记录">
        <el-switch
          v-model="formData.dataJson.is_private"
          :active-value="1"
          :inactive-value="0"
          inline-prompt
          active-text="仅管理员可见"
          inactive-text="正常可见"
        />
      </el-form-item>

      <!-- 底部操作栏 -->
      <el-form-item class="form-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving" :disabled="!canSave">
          <el-icon><Check /></el-icon>
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 草稿保存提示 -->
    <el-tooltip
      v-if="showDraftSaved"
      content="草稿已自动保存"
      placement="top"
      class="draft-tooltip"
    >
      <el-icon class="draft-saved-icon"><Check /></el-icon>
    </el-tooltip>
  </div>

  <!-- 加载中 -->
  <div v-else class="loading-state">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { recordApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import DynamicFieldRenderer from '@/components/DynamicFieldRenderer.vue'
import {
  ArrowLeft, Delete, Check, User, Calendar, Picture,
  FirstAidKit, CreditCard, Picture as PictureIcon, Checked
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

const formRef = ref<InstanceType<typeof import('element-plus').ElForm> | null>(null)
const recordId = route.params.recordId as string
const moduleKey = route.params.moduleKey as string
const isEdit = computed(() => !!recordId)

const module = ref<any>(null)
const formData = ref({
  title: '',
  dataJson: {} as Record<string, any>,
  imagesJson: [] as string[],
  tags: '',
  remindDate: '' as string | ''
})
const validationRules = ref<Record<string, any>>({})
const saving = ref(false)
const showDraftSaved = ref(false)

const isAdmin = computed(() => authStore.isAdmin)
const isEditor = computed(() => authStore.isEditor)
const isReadonly = computed(() => authStore.isReadonly)

const canSave = computed(() => !isReadonly.value)

const hasTitleField = computed(() => true) // 所有模块都有标题

const allFields = computed(() => module.value?.fields || [])

// 可见字段（非管理员过滤私密字段）
const visibleFields = computed(() => {
  if (isAdmin.value) return allFields.value
  return allFields.value.filter((f: any) => f.isPrivate !== 1)
})

onMounted(async () => {
  await moduleStore.fetchModules()
  module.value = moduleStore.getModuleByKey(moduleKey)

  if (!module.value) {
    router.push('/')
    return
  }

  buildValidationRules()

  if (isEdit.value) {
    await fetchRecord()
  } else {
    // 新增：初始化默认值
    initDefaultValues()
  }

  // 自动保存草稿
  startAutoSave()
})

function buildValidationRules() {
  const rules: Record<string, any> = {}
  visibleFields.value.forEach((field: any) => {
    if (field.isRequired === 1) {
      rules[field.fieldKey] = [
        { required: true, message: `${field.fieldLabel} 为必填项`, trigger: 'blur' }
      ]
    }
    // 根据字段类型添加额外校验
    if (field.fieldType === 'number') {
      rules[field.fieldKey] = rules[field.fieldKey] || []
      rules[field.fieldKey].push({
        validator: (rule: any, value: any, callback: any) => {
          if (value !== '' && value !== null && value !== undefined && isNaN(Number(value))) {
            callback(new Error('请输入有效数字'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      })
    }
  })
  validationRules.value = rules
}

function initDefaultValues() {
  // 设置默认值
  visibleFields.value.forEach((field: any) => {
    if (field.fieldType === 'boolean') {
      formData.value.dataJson[field.fieldKey] = false
    } else if (field.fieldType === 'number') {
      formData.value.dataJson[field.fieldKey] = 0
    } else if (field.fieldType === 'date' && field.fieldKey.includes('purchase')) {
      // 购买日期默认今天
      formData.value.dataJson[field.fieldKey] = dayjs().format('YYYY-MM-DD')
    }
  })
}

async function fetchRecord() {
  try {
    const response = await recordApi.detail(Number(recordId), module.value?.id)
    const record = response.data

    formData.value.title = record.title || ''
    formData.value.dataJson = record.dataJson || {}
    formData.value.imagesJson = record.imagesJson || []
    formData.value.tags = record.tags || ''
    formData.value.remindDate = record.remindDate || ''
  } catch (error) {
    console.error('Failed to fetch record:', error)
    ElMessage.error('获取记录详情失败')
    router.push(`/module/${moduleKey}`)
  }
}

function updateFieldValue(key: string, value: any) {
  formData.value.dataJson = { ...formData.value.dataJson, [key]: value }
  // 触发表单验证
  nextTick(() => {
    formRef.value?.validateField(key).catch(() => {})
  })
}

function getFieldRules(field: any) {
  return validationRules.value[field.fieldKey] || []
}

async function handleSave() {
  if (!formRef.value) return

  formRef.value.validate(async (valid: boolean) => {
    if (!valid) {
      ElMessage.warning('请检查表单填写是否完整')
      return
    }

    saving.value = true
    try {
      const payload = {
        moduleId: module.value.id,
        title: formData.value.title,
        dataJson: formData.value.dataJson,
        imagesJson: formData.value.imagesJson,
        tags: formData.value.tags,
        remindDate: formData.value.remindDate
      }

      if (isEdit.value) {
        await recordApi.update({
          recordId: Number(recordId),
          moduleId: module.value.id,
          title: formData.value.title,
          dataJson: formData.value.dataJson,
          imagesJson: formData.value.imagesJson,
          tags: formData.value.tags,
          remindDate: formData.value.remindDate
        })
        ElMessage.success('保存成功')
      } else {
        const response = await recordApi.create(payload)
        ElMessage.success('创建成功')
        router.push(`/module/${moduleKey}/${response.data.id}`)
      }
    } catch (error) {
      console.error('Save failed:', error)
      ElMessage.error(isEdit.value ? '保存失败' : '创建失败')
    } finally {
      saving.value = false
    }
  })
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？删除后不可恢复', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await recordApi.delete(Number(recordId), module.value.id)
    ElMessage.success('删除成功')
    router.push(`/module/${moduleKey}`)
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function goBack() {
  if (isEdit.value) {
    router.push(`/module/${moduleKey}/${recordId}`)
  } else {
    router.push(`/module/${moduleKey}`)
  }
}

// 自动保存草稿
let autoSaveTimer: number
function startAutoSave() {
  autoSaveTimer = window.setInterval(() => {
    if (!saving.value && !isEdit.value && Object.keys(formData.value.dataJson).some(k => formData.value.dataJson[k])) {
      saveDraft()
    }
  }, 30000) // 30秒

  // 页面卸载前保存
  window.addEventListener('beforeunload', saveDraft)
}

function saveDraft() {
  const draftKey = `draft_${moduleKey}_${recordId || 'new'}`
  const draft = {
    title: formData.value.title,
    dataJson: formData.value.dataJson,
    imagesJson: formData.value.imagesJson,
    tags: formData.value.tags,
    remindDate: formData.value.remindDate,
    updatedAt: Date.now()
  }
  localStorage.setItem(draftKey, JSON.stringify(draft))
  showDraftSaved.value = true
  // 2秒后自动隐藏
  setTimeout(() => {
    showDraftSaved.value = false
  }, 2000)
}

// 恢复草稿
function restoreDraft() {
  const draftKey = `draft_${moduleKey}_${recordId || 'new'}`
  const saved = localStorage.getItem(draftKey)
  if (saved) {
    try {
      const draft = JSON.parse(saved)
      // 只恢复 1 小时内的草稿
      if (Date.now() - draft.updatedAt < 3600000) {
        formData.value.title = draft.title || ''
        formData.value.dataJson = draft.dataJson || {}
        formData.value.imagesJson = draft.imagesJson || []
        formData.value.tags = draft.tags || ''
        formData.value.remindDate = draft.remindDate || ''
        ElMessage.info('已恢复草稿')
      }
    } catch (e) {
      console.error('Failed to restore draft:', e)
    }
  }
}

// 组件卸载清理
onUnmounted(() => {
  clearInterval(autoSaveTimer)
  window.removeEventListener('beforeunload', saveDraft)
})

function getModuleTagType(moduleKey: string) {
  const types: Record<string, string> = {
    drug: 'primary',
    todo: 'success',
    note: 'info',
    idoc: 'warning'
  }
  return types[moduleKey] || 'info'
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.record-edit-page {
  .edit-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid $border-color;

    .back-btn {
      margin-top: 2px;
    }

    .edit-title {
      flex: 1;

      h1 {
        font-size: 22px;
        font-weight: 600;
        color: $text-primary;
        margin: 8px 0 0;
      }
    }

    .edit-actions {
      display: flex;
      gap: 12px;
    }
  }

  .edit-form {
    :deep(.el-form-item) {
      margin-bottom: 20px;

      &.form-actions {
        margin-top: 32px;
        padding-top: 24px;
        border-top: 1px solid $border-color;
        display: flex;
        justify-content: flex-end;
        gap: 12px;
      }
    }

    :deep(.el-form-item__label) {
      font-weight: 500;
      color: $text-primary;
    }

    :deep(.el-form-item__error) {
      font-size: 12px;
      margin-top: 6px;
    }
  }

  :deep(el-switch) {
    margin-top: 4px;
  }
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

// 长辈模式
.elderly-mode .record-edit-page {
  .edit-title h1 { font-size: 26px !important; }
  :deep(.el-form-item__label) { font-size: 16px !important; }
  :deep(.el-input__inner) { font-size: 16px !important; padding: 14px 16px !important; height: 52px !important; }
  :deep(.el-textarea__inner) { font-size: 16px !important; padding: 14px 16px !important; }
  :deep(.el-select__tags) { font-size: 15px !important; }
  :deep(el-date-picker) { font-size: 16px !important; }
  :deep(.el-form-item__error) { font-size: 14px !important; }
  .edit-actions :deep(.el-button) { padding: 14px 28px !important; font-size: 16px !important; height: auto !important; }
}

// 草稿保存提示样式
.draft-tooltip :deep(.el-tooltip__content) {
  background: #67c23a;
  color: #fff;
  font-size: 13px;
  padding: 8px 12px;
  border-radius: $border-radius-base;

  .el-tooltip__arrow {
    &::after {
      border-top-color: #67c23a;
    }
  }
}

.draft-saved-icon {
  font-size: 20px;
  color: #67c23a;
  animation: popIn 0.3s ease;
}

@keyframes popIn {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>