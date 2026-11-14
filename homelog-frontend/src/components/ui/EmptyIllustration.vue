<template>
  <svg
    :width="size"
    :height="height"
    viewBox="0 0 160 112"
    fill="none"
    role="img"
    aria-hidden="true"
    class="empty-illustration"
  >
    <rect class="illo-bg" x="0" y="0" width="160" height="112" rx="18" />

    <template v-if="variant === 'todo'">
      <rect class="illo-plant" x="24" y="58" width="14" height="30" rx="7" />
      <rect class="illo-plant" x="122" y="58" width="14" height="30" rx="7" />
      <path class="illo-roof" d="M80 32L113 53H47L80 32Z" />
      <rect class="illo-house" x="54" y="51" width="52" height="37" rx="7" />
      <rect class="illo-window" x="67" y="62" width="18" height="14" rx="3" />
      <circle class="illo-bubble" cx="118" cy="30" r="9" />
    </template>

    <template v-else>
      <circle class="illo-clock-ring" cx="80" cy="56" r="24" />
      <path class="illo-clock-hand" d="M80 56V42M80 56H92" />
      <circle class="illo-badge" cx="110" cy="38" r="10" />
      <path class="illo-badge-mark" d="M110 33V39M110 42.5V43.5" />
    </template>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    variant?: 'todo' | 'alert'
    size?: number
  }>(),
  {
    variant: 'todo',
    size: 160
  }
)

const height = computed(() => Math.round(props.size * 0.7))
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.empty-illustration {
  display: block;
}

.illo-bg {
  fill: $surface-warm-soft;
}

.illo-house {
  fill: $brand-300;
}

.illo-roof {
  fill: $brand-400;
}

.illo-window {
  fill: $card-bg;
  opacity: 0.85;
}

.illo-plant {
  fill: $success-bg;
}

.illo-bubble {
  fill: $card-bg;
}

.illo-clock-ring {
  fill: none;
  stroke: $primary-color;
  stroke-width: 3;
  opacity: 0.4;
}

.illo-clock-hand {
  stroke: $primary-color;
  stroke-width: 2;
  stroke-linecap: round;
}

.illo-badge {
  fill: $danger-bg;
}

.illo-badge-mark {
  stroke: $danger-text;
  stroke-width: 2;
  stroke-linecap: round;
}
</style>
