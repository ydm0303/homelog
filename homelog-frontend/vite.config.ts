import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
      resolvers: [ElementPlusResolver()],
      eslintrc: {
        enabled: true
      }
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: 'src/components.d.ts'
    }),
    VitePWA({
      registerType: 'prompt',
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      injectRegister: false,
      manifest: {
        name: '家记 - 家里的事，都记在这里',
        short_name: '家记',
        description: '模块化家庭私有记录平台：药品、待办、记事、证件',
        start_url: '/',
        display: 'standalone',
        // Android Chrome 会忽略 standalone 而给窄屏套 minimal-ui（全屏浅灰栏），
        // 部分国产 WebView 干脆不支持 standalone。用 override 争一个无地址栏的外壳。
        // 不放 fullscreen：那会连状态栏一起去掉，首页顶部内容容易顶进刘海。
        // 注意：iOS Safari 不认 display_override，只认上面的 display。
        display_override: ['standalone', 'minimal-ui'],
        orientation: 'portrait',
        // 启动闪屏 + 状态栏底色必须等于首屏背景色（$bg-color），否则冷启动会闪一块异色
        background_color: '#fffaf5',
        theme_color: '#fffaf5',
        lang: 'zh-CN',
        icons: [
          {
            src: '/icons/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any'
          },
          {
            src: '/icons/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable'
          }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{css,js,html,webmanifest,png,svg}']
      },
      devOptions: {
        enabled: true,
        type: 'module'
      }
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          if (id.includes('xlsx') || id.includes('jszip') || id.includes('file-saver')) return 'export-vendor'
          if (id.includes('axios') || id.includes('dayjs')) return 'utils-vendor'
          if (id.includes('vue') || id.includes('pinia')) return 'vue-vendor'
          return 'vendor'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  }
})