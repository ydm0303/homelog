<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand-section">
          <div class="logo">
            <el-icon><House /></el-icon>
            <span>家记</span>
          </div>
          <p class="slogan">家里的事，都记在这里</p>
          <p class="description">
            私有模块化家庭记录平台。电脑、手机浏览器均可访问，支持添加桌面快捷方式。
            管理员自由配置插拔式模块，内置药箱、待办、记事、证件档案，
            还能自建表单扩展任意记录类型；三级权限管控，数据私有，支持导出备份。
          </p>
          <div class="features">
            <el-tag effect="plain" size="small">多端自适应</el-tag>
            <el-tag effect="plain" size="small">PWA 安装</el-tag>
            <el-tag effect="plain" size="small">三级权限</el-tag>
            <el-tag effect="plain" size="small">数据私有</el-tag>
            <el-tag effect="plain" size="small">邮件提醒</el-tag>
            <el-tag effect="plain" size="small">导出备份</el-tag>
          </div>
        </div>
      </div>

      <div class="login-right">
        <div class="login-form-card">
          <div class="form-header">
            <h2>欢迎回来</h2>
            <p>登录后继续管理家庭记录</p>
          </div>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @submit.native.prevent="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="用户名或邮箱"
                :prefix-icon="User"
                clearable
                :autocomplete="false"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
                :autocomplete="false"
                type="password"
              />
            </el-form-item>

            <el-form-item>
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                block
                :loading="loading"
                native-type="submit"
                size="default"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>还没有账号？</span>
            <el-button type="text" @click="goRegister">立即注册</el-button>
          </div>

          <div class="pwa-hint" v-if="isMobile">
            <el-icon><Download /></el-icon>
            <span>支持添加到手机桌面，像原生 APP 一样使用</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <footer class="login-footer">
      家记 · 家里的事，都记在这里 | 私有家庭记录工具 · 数据自主可控
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Download, House } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginFormRef = ref<InstanceType<typeof import('element-plus').ElForm> | null>(null)
const loading = ref(false)
const isMobile = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: true
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ]
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)

  // 检查是否已登录
  if (authStore.isLoggedIn) {
    router.push(route.query.redirect as string || '/')
  }
})

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

async function handleLogin() {
  if (!loginFormRef.value) return

  loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.login(loginForm.username, loginForm.password)
      ElMessage.success('登录成功')
      router.push(route.query.redirect as string || '/')
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
    } finally {
      loading.value = false
    }
  })
}

function goRegister() {
  router.push('/register')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.login-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, $bg-color 0%, #f0f4f8 100%);
  overflow: hidden;
}

.login-container {
  flex: 1;
  display: flex;
  max-width: 1120px;
  width: 100%;
  margin: 0 auto;
  padding: 48px 24px;
  gap: 48px;
  align-items: center;
}

.login-left {
  flex: 1;
  max-width: 560px;
  padding: 48px 24px;
}

.brand-section {
  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 32px;
    font-weight: 700;
    color: $primary-color;
    margin-bottom: 16px;

    el-icon {
      font-size: 36px;
    }
  }

  .slogan {
    font-size: 24px;
    font-weight: 500;
    color: $text-primary;
    margin: 0 0 24px;
    line-height: 1.4;
  }

  .description {
    font-size: 16px;
    color: $text-secondary;
    line-height: 1.8;
    margin: 0 0 32px;
  }

  .features {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.login-right {
  flex: 0 0 400px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.login-form-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 32px;
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-lg;
}

.form-header {
  text-align: center;
  margin-bottom: 32px;

  h2 {
    font-size: 24px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: $text-secondary;
    margin: 0;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-input__prefix) {
    color: $text-placeholder;
  }
}

.form-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 14px;
  color: $text-secondary;

  :deep(.el-button) {
    padding: 0;
    color: $primary-color;
    font-weight: 500;
  }
}

.pwa-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 12px;
  background-color: $primary-light-bg;
  border-radius: $border-radius-base;
  font-size: 13px;
  color: $primary-color;

  el-icon {
    font-size: 16px;
  }
}

.login-footer {
  padding: 20px;
  text-align: center;
  font-size: 12px;
  color: $text-placeholder;
  border-top: 1px solid $border-color;
  background-color: $header-bg;
}

// 响应式
@media (max-width: 1023px) {
  .login-container {
    flex-direction: column;
    padding: 24px 16px;
    gap: 0;
    align-items: stretch;
  }

  .login-left {
    display: none;
  }

  .login-right {
    flex: 1;
    max-width: none;
  }

  .login-form-card {
    border-radius: 0;
    box-shadow: none;
    padding: 24px 16px;
    min-height: calc(100vh - 60px);
  }
}

@media (max-width: 480px) {
  .login-page {
    background: #fff;
  }

  .login-form-card {
    padding: 20px 12px;
  }

  .form-header h2 {
    font-size: 20px;
  }
}

// 长辈模式适配
.elderly-mode .login-page {
  font-size: 20px;

  .login-form-card {
    padding: 48px 40px;
  }

  :deep(.el-input__inner) {
    padding: 16px !important;
    height: 56px !important;
    font-size: 18px !important;
  }

  :deep(.el-button) {
    padding: 16px 32px !important;
    height: auto !important;
    font-size: 18px !important;
  }
}
</style>