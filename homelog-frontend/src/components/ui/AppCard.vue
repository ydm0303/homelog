<template>
  <section class="app-card" :class="`app-card--${padding}`">
    <header v-if="title || description || $slots.header || $slots.actions" class="app-card-header">
      <div class="app-card-heading">
        <h2 v-if="title" class="app-card-title">{{ title }}</h2>
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
withDefaults(
  defineProps<{
    title?: string
    description?: string
    padding?: 'none' | 'sm' | 'md'
  }>(),
  {
    title: '',
    description: '',
    padding: 'md'
  }
)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/breakpoints' as *;

.app-card {
  background: $card-bg;
  border: 1px solid $border-color;
  border-radius: $border-radius-lg;
  overflow: hidden;
}

.app-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid $border-color-light;
}

.app-card-heading {
  min-width: 0;
}

.app-card-title {
  margin: 0;
  font-size: $text-title;
  font-weight: $font-weight-semibold;
  line-height: $line-height-tight;
  color: $text-primary;
}

.app-card-description {
  margin: 4px 0 0;
  font-size: $text-caption;
  line-height: $line-height-base;
  color: $text-tertiary;
}

.app-card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.app-card-body {
  padding: 20px;
}

.app-card-footer {
  padding: 12px 20px;
  border-top: 1px solid $border-color-light;
}

.app-card--none {
  .app-card-body {
    padding: 0;
  }
}

.app-card--sm {
  .app-card-body {
    padding: 12px 16px;
  }
}

@include down(md) {
  .app-card-header {
    flex-direction: column;
    align-items: stretch;
    padding: 14px 16px;
  }

  .app-card-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .app-card-body {
    padding: 16px;
  }

  .app-card--sm .app-card-body {
    padding: 12px;
  }
}
</style>
