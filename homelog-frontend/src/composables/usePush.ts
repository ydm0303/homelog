import { ref, computed } from 'vue'
import { pushApi } from '@/api'

// 订阅状态为模块级单例，设置页与首页均可读取
const pushState = ref<'unsupported' | 'denied' | 'granted' | 'default'>('unsupported')
const subscribed = ref(false)
const busy = ref(false)

const isStandalone = computed(
  () =>
    window.matchMedia('(display-mode: standalone)').matches ||
    // iOS Safari 专有标识
    (navigator as any).standalone === true
)

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  const outputArray = new Uint8Array(rawData.length)
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i)
  }
  return outputArray
}

function detectPlatform(): string {
  const ua = navigator.userAgent
  if (/iPhone|iPad|iOS/.test(ua)) return 'ios'
  if (/Android/.test(ua)) return 'android'
  return 'desktop'
}

async function refreshStatus() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    pushState.value = 'unsupported'
    return
  }
  pushState.value = Notification.permission
  const registration = await navigator.serviceWorker.getRegistration()
  const sub = await registration?.pushManager.getSubscription()
  subscribed.value = !!sub
}

async function enablePush(): Promise<boolean> {
  if (pushState.value === 'unsupported') return false
  busy.value = true
  try {
    if (Notification.permission === 'denied') return false

    const registration = await navigator.serviceWorker.ready
    let subscription = await registration.pushManager.getSubscription()
    if (!subscription) {
      const keyRes = await pushApi.getVapidPublicKey()
      const publicKey: string = keyRes.data?.publicKey ?? keyRes.data
      if (!publicKey) throw new Error('未获取到 VAPID 公钥')
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        // TS 5.9 DOM lib 将 BufferSource 收紧为 ArrayBufferView<ArrayBuffer>，
        // 而 urlBase64ToUint8Array 返回 Uint8Array<ArrayBufferLike>；运行时二者等价，此处仅做类型对齐。
        applicationServerKey: urlBase64ToUint8Array(publicKey) as unknown as ArrayBuffer
      })
    }
    const json = subscription.toJSON()
    await pushApi.saveSubscription({
      endpoint: json.endpoint!,
      p256dh: (json.keys as any)?.p256dh ?? '',
      auth: (json.keys as any)?.auth ?? '',
      platform: detectPlatform()
    })
    await refreshStatus()
    return true
  } finally {
    busy.value = false
  }
}

async function disablePush(): Promise<boolean> {
  busy.value = true
  try {
    const registration = await navigator.serviceWorker.ready
    const subscription = await registration.pushManager.getSubscription()
    if (subscription) {
      await pushApi.removeSubscription(subscription.endpoint).catch(() => {})
      await subscription.unsubscribe()
    }
    await refreshStatus()
    return true
  } finally {
    busy.value = false
  }
}

export function usePush() {
  return {
    pushState,
    subscribed,
    busy,
    isStandalone,
    refreshStatus,
    enablePush,
    disablePush
  }
}
