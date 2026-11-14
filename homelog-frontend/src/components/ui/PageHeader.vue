<template>
  <header class="page-header">
    <div class="page-header-main">
      <el-button v-if="back" class="page-back" link @click="emit('back')">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <div class="page-header-text">
        <div class="page-header-title-row">
          <h1 class="page-title">{{ title }}</h1>
          <slot name="tag" />
        </div>

        <p v-if="description || $slots.description" class="page-description">
          <slot name="description">{{ description }}</slot>
        </p>

        <slot name="meta" />
      </div>
    </div>

    <div v-if="$slots.actions" class="page-header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    back?: boolean
  }>(),
  {
    description: '',
    back: false
  }
)

const emit = defineEmits<{ back: [] }>()
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/breakpoints' as *;

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: $spacing-lg;
}

.page-header-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.page-back {
  margin-top: 2px;
  flex-shrink: 0;
  color: $text-tertiary;

  :deep(.el-icon) {
    font-size: $icon-size-sm;
  }

  &:hover {
    color: $primary-text;
  }
}

.page-header-text {
  min-width: 0;
}

.page-header-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.page-title {
  margin: 0;
  font-size: $text-heading;
  font-weight: $font-weight-semibold;
  line-height: $line-height-tight;
  color: $text-primary;
  letter-spacing: -0.01em;
}

.page-description {
  margin: 6px 0 0;
  font-size: $text-label;
  line-height: $line-height-base;
  color: $text-tertiary;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

@include down(md) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .page-title {
    font-size: 20px;
  }
}
</style>
