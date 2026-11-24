import { ref } from 'vue'
import { registerSW } from 'virtual:pwa-register'

const needRefresh = ref(false)
const offlineReady = ref(false)

const updateSW = registerSW({
  immediate: true,
  onNeedRefresh() {
    needRefresh.value = true
  },
  onOfflineReady() {
    offlineReady.value = true
  }
})

async function handleUpdate() {
  await updateSW(true)
  window.location.reload()
}

function dismiss() {
  needRefresh.value = false
  offlineReady.value = false
}

export function usePwaUpdate() {
  return { needRefresh, offlineReady, handleUpdate, dismiss }
}
