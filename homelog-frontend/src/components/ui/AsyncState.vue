<template>
  <div class="async-state" :style="{ minHeight }">
    <div v-if="loading" class="async-skeleton" :aria-busy="true" aria-live="polite">
      <div
        v-for="index in rows"
        :key="index"
        class="skeleton-row"
        :style="{ width: widths[(index - 1) % widths.length] }"
      />
    </div>

    <EmptyState
      v-else-if="error"
      tone="danger"
      :icon="WarningFilled"
      title="加载失败"
      :description="error"
    >
      <template #actions>
        <el-button type="primary" plain @click="emit('retry')">重新加载</el-button>
      </template>
    </EmptyState>

    <EmptyState
      v-else-if="empty"
      :icon="emptyIcon"
      :title="emptyTitle"
      :description="emptyDescription"
    >
      <template v-if="$slots['empty-actions']" #actions>
        <slot name="empty-actions" />
      </template>
      <!-- 插画型空态：由调用方决定图形，转发给 EmptyState 的 #figure -->
      <template v-if="$slots.figure" #figure>
        <slot name="figure" />
      </template>
    </EmptyState>

    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import EmptyState from './EmptyState.vue'

withDefaults(
  defineProps<{
    loading?: boolean
    error?: string
    empty?: boolean
    emptyTitle?: string
    emptyDescription?: string
    emptyIcon?: Component
    rows?: number
    minHeight?: string
  }>(),
  {
    loading: false,
    error: '',
    empty: false,
    emptyTitle: '暂无数据',
    emptyDescription: '',
    rows: 6,
    minHeight: '240px'
  }
)

const emit = defineEmits<{ retry: [] }>()

const widths = ['100%', '82%', '91%', '76%', '88%', '68%']
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.async-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.async-skeleton {
  display: flex;
  flex-direction: column;
  gap: $spacing-md-soft;
  padding: $spacing-lg $spacing-lg-soft;
}

.skeleton-row {
  height: $text-body;
  border-radius: $border-radius-base;
  background: linear-gradient(
    90deg,
    $border-color-light 25%,
    $surface-warm-soft 37%,
    $border-color-light 63%
  );
  background-size: 400% 100%;
  animation: skeleton-shimmer 1.4s ease infinite;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 100% 50%;
  }

  100% {
    background-position: 0 50%;
  }
}
</style>
