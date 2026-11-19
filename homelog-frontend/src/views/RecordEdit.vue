<template>
  <div class="record-edit-page" v-if="module">
    <!-- 页面头部 -->
    <PageHeader :title="isEdit ? '编辑记录' : '新增记录'" back @back="goBack">
      <template #tag>
        <el-tag :type="getModuleTagType(module.moduleKey)" size="default">
          {{ module.name }}
        </el-tag>
      </template>
    </PageHeader>

    <!-- 表单 -->
    <AppCard class="edit-card">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="validationRules"
      label-width="100px"
      class="edit-form"
      @submit.prevent="handleSave"
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
          :label="field.fieldLabel"
          :prop="`dataJson.${field.fieldKey}`"
          :rules="getFieldRules(field)"
        >
          <DynamicFieldRenderer
            :field="field"
            :value="formData.dataJson[field.fieldKey]"
            :module-key="moduleKey"
            @update:value="val => updateFieldValue(field.fieldKey, val)"
            :disabled="isReadonly || field.fieldKey === 'creator_id'"
          />
        </el-form-item>
      </template>

      <!-- 提醒日期（药品模块由有效期自动派生，不单独展示） -->
      <el-form-item v-if="moduleKey !== 'drug'" label="提醒日期" prop="remindDate">
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
          placeholder="多个标签用逗号分隔，如：家务,重要,药品"
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
    </el-form>

    <template #footer v-if="canSave">
      <div class="edit-action-bar">
        <el-button
          v-if="isEdit"
          @click="handleDelete"
          type="danger"
          :disabled="saving"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
        <el-button @click="handleSave" type="primary" :loading="saving">
          <el-icon><Check /></el-icon>
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </template>
    </AppCard>

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
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { recordApi } from '@/api'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import DynamicFieldRenderer from '@/components/DynamicFieldRenderer.vue'
import { Delete, Check } from '@element-plus/icons-vue'
import { getModuleTagType } from '@/utils/module'

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
const isReadonly = computed(() => authStore.isReadonly)

const canSave = computed(() => !isReadonly.value)

const hasTitleField = computed(() => moduleKey !== 'drug') // 药品模块标题由药品名派生，不单独展示

const allFields = computed(() => module.value?.fields || [])

// 模块字段里确实存在 title 字段（内置待办/记事相册及部分自定义模块）
const isTitleAField = () => allFields.value.some((f: any) => f.fieldKey === 'title')

// 可见字段（非管理员过滤私密字段）
// 模块自带 title 字段时，页面顶部的硬编码「标题」框已负责录入，
// 这里过滤掉重复的 title 字段，避免出现两个标题输入框。
const visibleFields = computed(() => {
  const fields = isAdmin.value
    ? allFields.value
    : allFields.value.filter((f: any) => f.isPrivate !== 1)

  if (!hasTitleField.value) return fields
  return fields.filter((f: any) => f.fieldKey !== 'title')
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
    restoreDraft()
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
        validator: (_rule: any, value: any, callback: any) => {
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

  // 药品模块专属默认值
  if (moduleKey === 'drug') {
    if (!formData.value.dataJson.unit) {
      formData.value.dataJson.unit = '盒'
    }
    if (formData.value.dataJson.min_stock === undefined || formData.value.dataJson.min_stock === null) {
      formData.value.dataJson.min_stock = 2
    }
    // 有效期默认今天 + 2 年
    if (!formData.value.dataJson.expire_date) {
      formData.value.dataJson.expire_date = dayjs().add(2, 'year').format('YYYY-MM-DD')
    }
  }
}

async function fetchRecord() {
  try {
    const response = await recordApi.detail(Number(recordId), module.value?.id)
    const record = response.data

    formData.value.dataJson = record.dataJson || {}
    // 标题以 jj_record.title 为准；为空时回退到历史数据里的 dataJson.title
    formData.value.title = record.title || formData.value.dataJson.title || ''
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
    formRef.value?.validateField(`dataJson.${key}`).catch(() => {})
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
      // 模块若带 title 字段（表单里已被过滤），用顶部标题框的值补回 dataJson，
      // 既满足后端必填字段校验，又保证 dataJson 与 jj_record.title 一致。
      const submitData = { ...formData.value.dataJson }
      if (hasTitleField.value && isTitleAField()) {
        submitData.title = formData.value.title
      }

      const payload = {
        moduleId: module.value.id,
        title: formData.value.title,
        dataJson: submitData,
        imagesJson: formData.value.imagesJson,
        tags: formData.value.tags,
        remindDate: formData.value.remindDate
      }

      if (isEdit.value) {
        await recordApi.update({
          recordId: Number(recordId),
          moduleId: module.value.id,
          title: formData.value.title,
          dataJson: submitData,
          imagesJson: formData.value.imagesJson,
          tags: formData.value.tags,
          remindDate: formData.value.remindDate
        })
        ElMessage.success('保存成功')
        clearDraft()
      } else {
        const response = await recordApi.create(payload)
        ElMessage.success('创建成功')
        clearDraft()
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

function draftKey() {
  return `draft_${moduleKey}_${recordId || 'new'}`
}

function saveDraft() {
  // 草稿只服务新增流程，编辑态直接保存即可
  if (isEdit.value) return

  const draft = {
    title: formData.value.title,
    dataJson: formData.value.dataJson,
    imagesJson: formData.value.imagesJson,
    tags: formData.value.tags,
    remindDate: formData.value.remindDate,
    updatedAt: Date.now()
  }
  localStorage.setItem(draftKey(), JSON.stringify(draft))
  showDraftSaved.value = true
  // 2秒后自动隐藏
  setTimeout(() => {
    showDraftSaved.value = false
  }, 2000)
}

function clearDraft() {
  localStorage.removeItem(draftKey())
}

// 恢复草稿
function restoreDraft() {
  const saved = localStorage.getItem(draftKey())
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
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.record-edit-page {
  .edit-form {
    :deep(.el-form-item) {
      margin-bottom: 20px;
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

// 底部操作栏：位于卡片 footer 内的普通行，不做悬浮/吸底
.edit-action-bar {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-sm;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

// 长辈模式
// ============================================================
// Phase 1：编辑页宽度与表单节奏
// ============================================================
.edit-card {
  max-width: 960px;
}
</style>
