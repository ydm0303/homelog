<template>
  <section class="app-card" :class="`app-card--${padding}`">
    <header
      v-if="title || description || icon || $slots.header || $slots.actions"
      class="app-card-header"
    >
      <div class="app-card-heading">
        <div class="app-card-heading-row">
          <span v-if="icon" class="app-card-icon" aria-hidden="true">
            <el-icon><component :is="icon" /></el-icon>
          </span>
          <h2 v-if="title" class="app-card-title">{{ title }}</h2>
          <slot name="tag" />
        </div>
        <p v-if="description" class="app-card-description">{{ description }}</p>
        <slot name="header" />
      </div>

      <div v-if="$slots.actions" class="app-card-actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="app-card-body">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="app-card-footer">
      <slot name="footer" />
    </footer>
  </section>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    /** 标题左侧的图标底座；配色由调用方通过 --app-card-icon-* 变量注入（见下） */
    icon?: Component
    padding?: 'none' | 'sm' | 'md'
  }>(),
  {
    title: '',
    description: '',
    icon: undefined,
    padding: 'md'
  }
)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/breakpoints' as *;

.app-card {
  // 底色由调用方注入（与图标底座同一套约定）：
  // 在页面里写 `.soft-card { background: ... }` 覆盖组件根元素，
  // 胜负取决于 scoped 样式的注入顺序，换构建方式就会翻车。
  background: var(--app-card-bg, #{$card-bg});
  border: 1px solid $border-color;
  border-radius: $border-radius-lg;
  overflow: hidden;
}

.app-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg-soft;
  border-bottom: 1px solid $border-color-light;
}

.app-card-heading {
  min-width: 0;
}

.app-card-heading-row {
  display: flex;
  align-items: center;
  gap: $spacing-md-soft;
  min-width: 0;
}

// 图标底座配色由调用方注入，避免组件内置一套与各模块色冲突的默认色。
// 自定义属性会从带 scoped 属性的父元素继承进子组件根元素，因此无需 :deep()。
// 完整用例见 views/Dashboard.vue 的 .panel-todo / .panel-recent / .panel-shortcut：
//   .panel-todo { --app-card-icon-bg: #{rgba($primary-color, $icon-tile-alpha)}; --app-card-icon-color: #{$primary-text}; }
.app-card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: $icon-tile-size-sm;
  height: $icon-tile-size-sm;
  border-radius: $border-radius-base;
  background: var(--app-card-icon-bg, #{$primary-light-bg});
  color: var(--app-card-icon-color, #{$primary-text});
  font-size: $icon-size-base;
}

.app-card-title {
  margin: 0;
  font-size: $text-title;
  font-weight: $font-weight-semibold;
  line-height: $line-height-tight;
  color: $text-primary;
}

.app-card-description {
  margin: $spacing-xs 0 0;
  font-size: $text-caption;
  line-height: $line-height-base;
  color: $text-tertiary;
}

.app-card-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md-soft;
  flex-shrink: 0;
}

.app-card-body {
  padding: $spacing-lg-soft;
}

.app-card-footer {
  padding: $spacing-md-soft $spacing-lg-soft;
  border-top: 1px solid $border-color-light;
}

.app-card--none {
  .app-card-body {
    padding: 0;
  }
}

.app-card--sm {
  .app-card-body {
    padding: $spacing-md-soft $spacing-md;
  }
}

@include down(md) {
  .app-card-header {
    flex-direction: column;
    align-items: stretch;
    padding: $spacing-md-soft $spacing-md;
  }

  .app-card-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .app-card-body {
    padding: $spacing-md;
  }

  .app-card--sm .app-card-body {
    padding: $spacing-md-soft;
  }
}
</style>
