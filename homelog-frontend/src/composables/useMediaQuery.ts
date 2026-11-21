import { onMounted, onUnmounted, ref, type Ref } from 'vue'

/**
 * 媒体查询组合式函数。
 * 传入 number 视为 max-width 断点（如 767 → '(max-width: 767px)'），
 * 传入 string 视为完整 CSS 媒体查询串。
 */
export function useMediaQuery(query: string | number): Readonly<Ref<boolean>> {
  const mq = typeof query === 'number' ? `(max-width: ${query}px)` : query
  const matches = ref(false)
  let mql: MediaQueryList | null = null

  const onChange = (e: MediaQueryListEvent) => {
    matches.value = e.matches
  }

  onMounted(() => {
    mql = window.matchMedia(mq)
    matches.value = mql.matches
    mql.addEventListener('change', onChange)
  })

  onUnmounted(() => {
    mql?.removeEventListener('change', onChange)
  })

  return matches
}

/** <768px 视为手机 */
export function useIsMobile(): Readonly<Ref<boolean>> {
  return useMediaQuery(767)
}

/** <1024px 视为平板及手机（侧边栏收起、抽屉菜单的断点） */
export function useIsTabletDown(): Readonly<Ref<boolean>> {
  return useMediaQuery(1023)
}
