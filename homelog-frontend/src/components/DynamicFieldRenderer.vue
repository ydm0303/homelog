<template>
  <div class="dynamic-field-renderer">
    <!-- 文本输入 -->
    <el-input
      v-if="field.fieldType === 'text'"
      v-model="localValue"
      :placeholder="`请输入${field.fieldLabel}`"
      :clearable="true"
      :maxlength="255"
      @input="emitUpdate"
    />

    <!-- 多行文本 -->
    <el-input
      v-else-if="field.fieldType === 'textarea'"
      v-model="localValue"
      :placeholder="`请输入${field.fieldLabel}`"
      type="textarea"
      :rows="4"
      @input="emitUpdate"
    />

    <!-- 数字 -->
    <el-input-number
      v-else-if="field.fieldType === 'number'"
      v-model.number="localValue"
      :placeholder="`请输入${field.fieldLabel}`"
      :controls="true"
      :precision="2"
      @change="emitUpdate"
    />

    <!-- 日期 -->
    <el-date-picker
      v-else-if="field.fieldType === 'date'"
      v-model="localValue"
      type="date"
      :placeholder="`请选择${field.fieldLabel}`"
      value-format="YYYY-MM-DD"
      clearable
      style="width: 100%"
      @change="emitUpdate"
    />

    <!-- 时间 -->
    <el-time-picker
      v-else-if="field.fieldType === 'time'"
      v-model="localValue"
      :placeholder="`请选择${field.fieldLabel}`"
      value-format="HH:mm:ss"
      clearable
      style="width: 100%"
      @change="emitUpdate"
    />

    <!-- 单选 -->
    <el-radio-group
      v-else-if="field.fieldType === 'select'"
      v-model="localValue"
      @change="emitUpdate"
    >
      <el-radio
        v-for="option in parsedOptions"
        :key="option.value"
        :label="option.value"
        :disabled="disabled"
      >
        {{ option.label }}
      </el-radio>
    </el-radio-group>

    <!-- 多选 -->
    <el-checkbox-group
      v-else-if="field.fieldType === 'multi_select'"
      v-model="localValue"
      @change="emitUpdate"
    >
      <el-checkbox
        v-for="option in parsedOptions"
        :key="option.value"
        :label="option.value"
        :disabled="disabled"
      >
        {{ option.label }}
      </el-checkbox>
    </el-checkbox-group>

    <!-- 图片上传 -->
    <div v-else-if="field.fieldType === 'image'" class="image-uploader">
      <el-upload
        class="upload-component"
        action="#"
        :auto-upload="true"
        :on-preview="handlePreview"
        :on-remove="handleRemove"
        :on-success="handleSuccess"
        :on-error="handleError"
        :before-upload="beforeUpload"
        :file-list="imageList"
        :limit="9"
        :on-exceed="handleExceed"
        list-type="picture-card"
        :disabled="disabled"
      >
        <el-icon v-if="imageList.length < 9"><Plus /></el-icon>
        <div v-else class="upload-limit">最多 9 张</div>
      </el-upload>

      <!-- 移动端拍照入口 -->
      <div class="mobile-camera" v-if="isMobile">
        <label class="camera-btn">
          <el-icon><Camera /></el-icon>
          <span>拍照</span>
          <input type="file" accept="image/*" capture="environment" @change="handleCameraCapture" hidden />
        </label>
      </div>
    </div>

    <!-- 成员选择器 -->
    <el-select
      v-else-if="field.fieldType === 'member'"
      v-model="localValue"
      :placeholder="`请选择${field.fieldLabel}`"
      clearable
      filterable
      style="width: 100%"
      @change="emitUpdate"
    >
      <el-option
        v-for="member in spaceMembers"
        :key="member.id"
        :label="member.nickName || member.username"
        :value="member.id"
      />
    </el-select>

    <!-- 标签输入 -->
    <div v-else-if="field.fieldType === 'tag'" class="tag-input-wrapper">
      <div class="tag-list">
        <el-tag
          v-for="(tag, index) in tagArray"
          :key="index"
          closable
          @close="removeTag(index)"
          size="small"
        >
          {{ tag }}
        </el-tag>
      </div>
      <el-input
        v-model="tagInputValue"
        :placeholder="`输入标签，回车确认`"
        size="small"
        @keyup.enter="addTag"
        style="width: 200px"
      />
    </div>

    <!-- 布尔值 -->
    <el-switch
      v-else-if="field.fieldType === 'boolean'"
      v-model="localValue"
      :active-value="true"
      :inactive-value="false"
      :inline-prompt="true"
      :active-text="是"
      :inactive-text="否"
      @change="emitUpdate"
    />

    <!-- 默认文本 -->
    <el-input
      v-else
      v-model="localValue"
      :placeholder="`请输入${field.fieldLabel}`"
      @input="emitUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Plus, Camera, Picture } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { spaceApi, fileApi } from '@/api'

interface Field {
  id: number
  fieldKey: string
  fieldLabel: string
  fieldType: string
  isRequired: number
  isPrivate: number
  optionsJson: string
  sortOrder: number
}

interface Option {
  label: string
  value: string
}

interface Member {
  id: number
  username: string
  nickName: string
}

const props = defineProps<{
  field: Field
  value: any
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:value': [value: any]
}>()

const authStore = useAuthStore()

// 本地值
const localValue = ref(props.value)

// 解析选项
const parsedOptions = computed<Option[]>(() => {
  if (!props.field.optionsJson) return []
  try {
    return JSON.parse(props.field.optionsJson)
  } catch {
    return []
  }
})

// 标签输入相关
const tagArray = computed<string[]>({
  get: () => Array.isArray(localValue.value) ? localValue.value : [],
  set: (val) => {
    localValue.value = val
  }
})
const tagInputValue = ref('')

// 空间成员
const spaceMembers = ref<Member[]>([])

// 移动端检测
const isMobile = ref(false)
const checkMobile = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => { checkMobile(); window.addEventListener('resize', checkMobile) })

// 标签输入处理
function addTag() {
  const tag = tagInputValue.value.trim()
  if (tag && !tagArray.value.includes(tag)) {
    tagArray.value = [...tagArray.value, tag]
    tagInputValue.value = ''
  }
}

function removeTag(index: number) {
  tagArray.value = tagArray.value.filter((_, i) => i !== index)
}

// 监听 props.value 变化同步 tagArray
watch(() => props.value, (val) => {
  localValue.value = val
  if (props.field.fieldType === 'image' && Array.isArray(val)) {
    imageList.value = val.map((url: string, idx: number) => ({
      uid: `img-${idx}`,
      name: `image-${idx}.jpg`,
      url: url.startsWith('http') ? url : `${import.meta.env.VITE_API_BASE_URL || '/api'}/files${url}`,
      status: 'success'
    }))
  }
}, { immediate: true, deep: true })

watch(localValue, (val) => {
  emit('update:value', val)
})

async function fetchSpaceMembers() {
  try {
    const response = await spaceApi.getMembers(1)
    spaceMembers.value = response.data
  } catch (e) {
    console.error('Failed to fetch space members:', e)
  }
}

onMounted(() => {
  fetchSpaceMembers()
})

// 图片上传处理
function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function handleSuccess(response: any, file: any, fileList: any[]) {
  if (response.code === 200 && response.data?.url) {
    const newUrl = response.data.url
    const currentUrls = localValue.value || []
    if (!Array.isArray(currentUrls)) {
      localValue.value = [newUrl]
    } else {
      localValue.value = [...currentUrls, newUrl]
    }
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleError(err: any, file: any, fileList: any[]) {
  ElMessage.error('上传失败')
}

function handleRemove(file: any, fileList: any[]) {
  const urls = (localValue.value || []).filter((u: string) => u !== file.url)
  localValue.value = urls
}

function handlePreview(file: any) {
  // 预览由组件自动处理
}

function handleExceed(files: any[], fileList: any[]) {
  ElMessage.warning(`最多只能上传 9 张图片，已忽略多余文件`)
}

// 移动端拍照
function handleCameraCapture(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    uploadFile(file)
  }
}

async function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fileApi.upload(formData)
    if (response.code === 200 && response.data?.url) {
      const currentUrls = localValue.value || []
      if (!Array.isArray(currentUrls)) {
        localValue.value = [response.data.url]
      } else {
        localValue.value = [...currentUrls, response.data.url]
      }
    }
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

// 需要导入 ElMessage
import { ElMessage } from 'element-plus'
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.dynamic-field-renderer {
  width: 100%;
}

// 图片上传
.image-uploader {
  .upload-component {
    :deep(.el-upload-list__item) {
      width: 100px;
      height: 100px;

      :deep(.el-upload-list__item-thumbnail) {
        width: 100%;
        height: 100%;
      }
    }

    :deep(.el-upload--picture-card) {
      width: 100px;
      height: 100px;
      border: 2px dashed $border-color;
      border-radius: $border-radius-base;
      transition: all 0.2s ease;

      &:hover {
        border-color: $primary-color;
      }

      .el-icon {
        font-size: 28px;
        color: $text-placeholder;
      }
    }
  }

  .upload-limit {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    font-size: 12px;
    color: $text-placeholder;
  }

  .mobile-camera {
    margin-top: 12px;
  }

  .camera-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    background: $primary-light-bg;
    border: 1px solid $primary-light-border;
    border-radius: $border-radius-base;
    color: $primary-color;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: $primary-color;
      color: #fff;
    }

    el-icon {
      font-size: 16px;
    }
  }
}

// 标签输入样式
.tag-input-wrapper {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 40px;

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  :deep(.el-input) {
    width: 200px;
  }
}

// 响应式
@media (max-width: 767px) {
  .dynamic-field-renderer {
    :deep(.el-input__inner),
    :deep(.el-textarea__inner),
    :deep(.el-select__tags),
    :deep(.el-date-editor__editor) {
      font-size: 16px !important;
      padding: 14px 16px !important;
    }

    .image-uploader {
      :deep(.el-upload--picture-card) {
        width: 80px;
        height: 80px;
      }
    }
  }
}

// 长辈模式
.elderly-mode .dynamic-field-renderer {
  :deep(.el-input__inner),
  :deep(.el-textarea__inner),
  :deep(.el-select__tags),
  :deep(.el-date-editor__editor),
  :deep(.el-radio__label),
  :deep(.el-checkbox__label) {
    font-size: 17px !important;
    padding: 14px 16px !important;
    height: 52px !important;
  }

  :deep(.el-input-number) {
    :deep(.el-input__inner) {
      font-size: 17px !important;
      height: 52px !important;
    }
  }

  .camera-btn {
    padding: 14px 24px !important;
    font-size: 16px !important;
  }
}
</style>