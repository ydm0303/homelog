/// <reference lib="webworker" />
import { precacheAndRoute } from 'workbox-precaching'
import { registerRoute } from 'workbox-routing'
import { NetworkFirst, CacheFirst } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'

declare let self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: Array<{ url: string; revision: string | null }>
}

precacheAndRoute(self.__WB_MANIFEST)

// 图片文件：CacheFirst，长缓存（先注册，优先于通用 API 路由匹配）
registerRoute(
  ({ url }) => url.pathname.startsWith('/api/files'),
  new CacheFirst({
    cacheName: 'files',
    plugins: [new ExpirationPlugin({ maxEntries: 120, maxAgeSeconds: 30 * 86400 })]
  })
)

// 业务 GET 接口：NetworkFirst，断网回退缓存；必须排除 /api/auth/*（登录/刷新不能被旧缓存污染）
registerRoute(
  ({ url, request }) =>
    url.pathname.startsWith('/api/') &&
    !url.pathname.startsWith('/api/auth/') &&
    request.method === 'GET',
  new NetworkFirst({
    cacheName: 'api',
    networkTimeoutSeconds: 5,
    plugins: [new ExpirationPlugin({ maxEntries: 80, maxAgeSeconds: 300 })]
  })
)

self.addEventListener('install', () => self.skipWaiting())
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()))

interface PushPayload {
  title?: string
  body?: string
  url?: string
}

self.addEventListener('push', (event) => {
  let payload: PushPayload = {}
  try {
    payload = event.data ? JSON.parse(event.data.text()) : {}
  } catch {
    payload = { title: '家记提醒', body: event.data?.text() || '' }
  }
  const title = payload.title || '家记提醒'
  event.waitUntil(
    self.registration.showNotification(title, {
      body: payload.body || '',
      icon: '/icons/icon-192.png',
      badge: '/icons/icon-192.png',
      data: { url: payload.url || '/' },
      tag: 'homelog-remind'
    })
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const targetUrl = (event.notification.data as { url?: string })?.url || '/'
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.postMessage({ type: 'navigate', url: targetUrl })
          return client.focus()
        }
      }
      return self.clients.openWindow(targetUrl)
    })
  )
})
