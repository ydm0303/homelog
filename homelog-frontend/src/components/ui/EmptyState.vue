<template>
  <div class="empty-state" :class="{ 'empty-state--compact': compact }">
    <!-- 传入 #figure 时用插画取代图标圆底，二者互斥不叠加 -->
    <div v-if="$slots.figure" class="empty-figure">
      <slot name="figure" />
    </div>
    <div v-else class="empty-icon" :class="`empty-icon--${tone}`">
      <el-icon><component :is="icon || Document" /></el-icon>
    </div>

    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>

    <div v-if="$slots.actions" class="empty-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: Component
    tone?: 'primary' | 'neutral' | 'danger'
    compact?: boolean
  }>(),
  {
    title: '暂无数据',
    description: '',
    tone: 'neutral',
    compact: false
  }
)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-3xl $spacing-lg;
  text-align: center;
}

.empty-figure {
  margin-bottom: $spacing-lg-soft;
}

.empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: $spacing-lg-soft;
  border-radius: $border-radius-pill;
  font-size: $icon-size-xl;

  &--neutral {
    background: $surface-warm;
    color: $text-placeholder;
  }

  &--primary {
    background: $primary-light-bg;
    color: $primary-text;
  }

  &--danger {
    background: $danger-bg;
    color: $danger-text;
  }
}

.empty-title {
  margin: 0;
  font-size: $text-title;
  font-weight: $font-weight-semibold;
  line-height: $line-height-tight;
  color: $text-primary;
}

.empty-description {
  max-width: 420px;
  margin: $spacing-sm 0 0;
  font-size: $text-label;
  line-height: $line-height-base;
  color: $text-tertiary;
}

.empty-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: $spacing-md-soft;
  margin-top: $spacing-lg;
}

.empty-state--compact {
  padding: $spacing-xl $spacing-md;

  .empty-icon {
    width: 40px;
    height: 40px;
    margin-bottom: $spacing-md-soft;
    font-size: $icon-size-lg;
  }

  .empty-figure {
    margin-bottom: $spacing-md;
  }

  .empty-title {
    font-size: $font-size-base;
  }

  .empty-description {
    margin-top: $spacing-xs;
    font-size: $font-size-xs;
  }

  .empty-actions {
    margin-top: $spacing-md-soft;
  }
}
</style>
