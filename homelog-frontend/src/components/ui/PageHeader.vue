<template>
  <header class="page-header">
    <div class="page-header-main">
      <div class="page-header-text">
        <div class="page-header-title-row">
          <button v-if="back" type="button" class="page-back" @click="emit('back')">
            <el-icon class="page-back-icon"><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>

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

// 返回按钮：原生按钮，避免 el-button 的盒模型/基线把「返回」和标题错开一行
.page-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
  font-family: inherit;
  font-size: $text-heading;
  font-weight: $font-weight-semibold;
  line-height: $line-height-tight;
  color: $text-tertiary;
  transition: color $transition-fast $ease-out;

  .page-back-icon {
    font-size: $icon-size-lg;
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
