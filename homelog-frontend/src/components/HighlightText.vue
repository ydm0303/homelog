<template>
  <span class="highlight-text">
    <template v-for="(segment, index) in segments" :key="index">
      <mark v-if="segment.highlight" class="highlight">{{ segment.text }}</mark>
      <template v-else>{{ segment.text }}</template>
    </template>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    text?: string | number | null
    keyword?: string
  }>(),
  {
    text: '',
    keyword: ''
  }
)

interface Segment {
  text: string
  highlight: boolean
}

// 纯文本分段渲染：Vue 自动转义，不依赖 v-html，也不会把用户内容当 HTML 执行
const segments = computed<Segment[]>(() => {
  const text = props.text === null || props.text === undefined ? '' : String(props.text)
  const keyword = (props.keyword || '').trim()
  if (!keyword) {
    return [{ text, highlight: false }]
  }

  const lowerText = text.toLowerCase()
  const lowerKeyword = keyword.toLowerCase()
  const result: Segment[] = []
  let cursor = 0
  let index = lowerText.indexOf(lowerKeyword)

  while (index !== -1) {
    if (index > cursor) {
      result.push({ text: text.slice(cursor, index), highlight: false })
    }
    result.push({ text: text.slice(index, index + keyword.length), highlight: true })
    cursor = index + keyword.length
    index = lowerText.indexOf(lowerKeyword, cursor)
  }

  if (cursor < text.length) {
    result.push({ text: text.slice(cursor), highlight: false })
  }

  return result.length > 0 ? result : [{ text, highlight: false }]
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.highlight {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  padding: 0 $spacing-micro;
  border-radius: $border-radius-micro;
}
</style>
