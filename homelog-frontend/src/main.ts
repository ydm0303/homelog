import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import ElementPlus from 'element-plus'
// 顺序要求：Element Plus 默认样式 → 主题变量桥接 → 全局样式与组件皮肤
import 'element-plus/dist/index.css'
import '@/styles/element-theme.scss'
import '@/styles/index.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { registerSW } from 'virtual:pwa-register'

// 注册 Service Worker
registerSW({
  onNeedRefresh() {
    if (confirm('发现新版本，是否刷新？')) {
      location.reload()
    }
  },
  onOfflineReady() {
    console.log('应用已准备好离线使用')
  }
})

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')