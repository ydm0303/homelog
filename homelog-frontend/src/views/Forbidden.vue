<template>
  <div class="error-page">
    <div class="error-card">
      <div class="error-code">403</div>
      <h1>无权访问</h1>
      <p>当前账号没有访问该页面的权限，请联系空间管理员。</p>
      <div class="error-actions">
        <el-button type="primary" @click="goHome">回到首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.error-page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.error-card {
  width: min(100%, 520px);
  padding: 48px 32px;
  text-align: center;
  background: $surface-warm;
  border: 1px solid rgba(43, 38, 34, 0.05);
  border-radius: 20px;
  box-shadow: $shadow-lg;
}

.error-code {
  color: $primary-text;
  font-size: clamp(64px, 12vw, 96px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.06em;
}

h1 {
  margin: 18px 0 8px;
  color: $text-primary;
  font-size: 24px;
}

p {
  margin: 0 0 24px;
  color: $text-secondary;
  font-size: 14px;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
