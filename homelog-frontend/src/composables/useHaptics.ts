/**
 * 触觉反馈（Vibration API）。
 *
 * 支持度：Android Chrome / WebView 可用；iOS Safari 全版本不支持 navigator.vibrate，
 * 调用会被静默忽略（不抛错），所以对 iPhone 是「无感」而非「报错」。
 *
 * 强度参考原生：轻点 10ms、成功 12/40/12 的双段、警示 40ms 长震。
 */
export type HapticStyle = 'light' | 'success' | 'warning'

const PATTERNS: Record<HapticStyle, number | number[]> = {
  light: 10,
  success: [12, 40, 12],
  warning: 40
}

let enabled = true

// 用户在系统层关闭动效时一并关掉震动，避免违背无障碍偏好
if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)')
  enabled = !reduced.matches
  const onChange = (e: MediaQueryListEvent) => {
    enabled = !e.matches
  }
  // addEventListener 在旧 Safari 上是 addListener，缺失时直接放弃监听
  if (typeof reduced.addEventListener === 'function') {
    reduced.addEventListener('change', onChange)
  }
}

/** 触发一次触觉反馈；不可用或未授权时静默返回 */
export function haptic(style: HapticStyle = 'light') {
  if (!enabled) return
  try {
    navigator.vibrate?.(PATTERNS[style])
  } catch {
    // 部分 WebView 未授权时 vibrate 会抛 SecurityError，忽略即可
  }
}

/** 全局开关：设置页的「触感反馈」选项接这里 */
export function setHapticsEnabled(value: boolean) {
  enabled = value
}

export function useHaptics() {
  return { haptic, setHapticsEnabled }
}
