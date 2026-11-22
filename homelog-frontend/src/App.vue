<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <div v-if="needRefresh" class="pwa-update-bar" role="alert">
      <span>发现新版本，更新后即可继续使用最新功能</span>
      <el-button type="primary" size="small" @click="handleUpdate">立即更新</el-button>
      <el-button size="small" text @click="dismiss">稍后</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
// 认证状态由路由守卫在首次导航前初始化，避免刷新页面时先跳登录页
import { onMounted, onUnmounted } from 'vue'
import router from '@/router'
import { usePwaUpdate } from '@/composables/usePwaUpdate'

const { needRefresh, handleUpdate, dismiss } = usePwaUpdate()

// Service Worker 收到 push 通知点击后，通过 message 请求 SPA 内跳转（避免整页重载）
function onSwMessage(event: MessageEvent) {
  const data = event.data
  if (data?.type === 'navigate' && typeof data.url === 'string') {
    router.push(data.url).catch(() => {})
  }
}

onMounted(() => navigator.serviceWorker?.addEventListener('message', onSwMessage))
onUnmounted(() => navigator.serviceWorker?.removeEventListener('message', onSwMessage))
</script>

<style lang="scss">
@use '@/styles/variables' as *;

#app {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.pwa-update-bar {
  position: fixed;
  bottom: calc(24px + env(safe-area-inset-bottom));
  left: 50%;
  transform: translateX(-50%);
  z-index: $z-index-fixed;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: $card-bg;
  border: 1px solid $border-color;
  border-radius: 10px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  font-size: 14px;
  max-width: 92vw;
}
</style>