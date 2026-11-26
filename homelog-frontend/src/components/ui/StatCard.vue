<template>
  <div class="stat-card" :class="`stat-card--${tone}`">
    <span v-if="icon" class="stat-icon">
      <el-icon><component :is="icon" /></el-icon>
    </span>

    <div class="stat-body">
      <span class="stat-value">{{ value }}</span>
      <span class="stat-label">{{ label }}</span>
    </div>

    <span v-if="hint" class="stat-hint">{{ hint }}</span>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    icon?: Component
    tone?: 'primary' | 'success' | 'warning' | 'info' | 'neutral'
    hint?: string
  }>(),
  {
    icon: undefined,
    tone: 'primary',
    hint: ''
  }
)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/breakpoints' as *;

.stat-card {
  display: flex;
  align-items: center;
  gap: $spacing-md-soft;
  min-width: 0;
  padding: $spacing-md $spacing-lg-soft;
  background: $card-bg;
  border: 1px solid $border-color;
  border-radius: $border-radius-lg;
}

.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: $icon-tile-size;
  height: $icon-tile-size;
  flex-shrink: 0;
  border-radius: $border-radius-base;
  font-size: $icon-size-lg;
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-value {
  font-size: $text-stat;
  font-weight: $font-weight-bold;
  line-height: $line-height-tight;
  color: $text-primary;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  margin-top: $spacing-xs;
  font-size: $text-caption;
  color: $text-tertiary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-hint {
  margin-left: auto;
  font-size: $text-caption;
  color: $text-placeholder;
}

.stat-card--primary .stat-icon {
  background: rgba($primary-color, 0.1);
  color: $primary-text;
}

.stat-card--success .stat-icon {
  background: rgba($success-color, 0.12);
  color: $success-text;
}

.stat-card--warning .stat-icon {
  background: rgba($warning-color, 0.14);
  color: $warning-text;
}

.stat-card--info .stat-icon {
  background: rgba($module-note, 0.12);
  color: $module-note;
}

.stat-card--neutral .stat-icon {
  background: $surface-warm;
  color: $text-tertiary;
}

@include down(md) {
  .stat-card {
    padding: $spacing-md-soft;
  }

  .stat-value {
    font-size: $text-heading;
  }
}
</style>
