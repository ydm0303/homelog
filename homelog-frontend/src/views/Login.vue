<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="brand-panel">
        <div class="brand-header">
          <div class="logo-mark">
            <el-icon class="brand-icon"><House /></el-icon>
            <span>家记</span>
          </div>
        </div>

        <div class="brand-copy">
          <p class="eyebrow">把日子，认真收好</p>
          <h1>家里的事，都记在这里</h1>
          <p class="description">
            日常的点滴、重要的计划，还有那些值得珍藏的小事，都有一个温暖的归处。
          </p>
        </div>

        <div class="illustration" aria-hidden="true">
          <div class="house-scene"></div>
          <div class="plant plant-left"></div>
          <div class="plant plant-right"></div>
          <div class="todo-bubble todo-bubble-one">待办</div>
          <div class="todo-bubble todo-bubble-two">清单</div>
        </div>

        <div class="brand-foot">只属于你和家人的私密空间</div>
      </aside>

      <main class="login-panel">
        <div class="login-card">
          <div class="form-header">
            <h2>欢迎回来，登录你的家记</h2>
            <p>登录后继续管理家庭记录</p>
          </div>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @submit.prevent="handleLogin"
          >
            <div class="form-label">用户名或邮箱</div>
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名或邮箱"
                :prefix-icon="User"
                clearable
                autocomplete="off"
              />
            </el-form-item>

            <div class="form-label">密码</div>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                autocomplete="off"
                type="password"
              />
            </el-form-item>

            <div class="form-inline">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <a href="#" class="forgot-link" @click.prevent="handleForgotPassword">忘记密码？</a>
            </div>

            <el-form-item>
              <el-button
                class="submit-btn"
                type="primary"
                block
                :loading="loading"
                native-type="submit"
                size="default"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="register-line">
            <span>还没有账号？</span>
            <el-button type="text" @click="goRegister">立即注册</el-button>
          </div>

          <div class="form-note">让家的每一刻，都有迹可循｜你的家庭记录仅对你可见</div>

          <div class="pwa-hint" v-if="isMobile">
            <el-icon><Download /></el-icon>
            <span>支持添加到手机桌面，像原生 APP 一样使用</span>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Download, House } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getFriendlyAuthErrorMessage } from '@/api'

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

  if (authStore.isLoggedIn) {
    router.push(route.query.redirect as string || '/')
  }
})

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

onUnmounted(() => window.removeEventListener('resize', checkMobile))

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
      ElMessage.error(getFriendlyAuthErrorMessage(error, '登录失败，请检查用户名和密码'))
    } finally {
      loading.value = false
    }
  })
}

function goRegister() {
  router.push('/register')
}

function handleForgotPassword() {
  ElMessage.info('密码找回功能正在完善，建议联系管理员或在设置页完成安全重置。')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/decorations' as *;

.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background: $gradient-login-bg;
}

.login-shell {
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  min-height: 760px;
  display: flex;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba($primary-color, 0.12);
  border-radius: 32px;
  box-shadow: 0 24px 56px rgba(80, 50, 30, 0.12);
  overflow: hidden;
  backdrop-filter: blur(6px);
}

.brand-panel {
  position: relative;
  flex: 1.2;
  min-width: 0;
  padding: 54px 52px 34px;
  background: $gradient-brand-panel;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.brand-header {
  display: flex;
  align-items: center;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: $primary-text;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.brand-icon {
  font-size: 2.2rem;
}

.brand-copy {
  max-width: 560px;
  padding-top: 18px;
}

.eyebrow {
  margin: 0 0 12px;
  color: $primary-text;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.brand-copy h1 {
  margin: 0;
  color: $text-primary;
  font-size: clamp(2.4rem, 2.8vw, 4rem);
  line-height: 1.08;
  letter-spacing: -0.05em;
  font-weight: 800;
}

.description {
  margin: 18px 0 0;
  max-width: 560px;
  color: rgba($text-primary, 0.75);
  font-size: 1.08rem;
  line-height: 1.8;
}

.illustration {
  position: relative;
  width: 100%;
  max-width: 540px;
  height: 240px;
  margin-top: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.34));
  border: 1px solid rgba($primary-color, 0.08);
  border-radius: 28px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.house-scene {
  position: absolute;
  left: 50%;
  bottom: 22px;
  width: 200px;
  height: 120px;
  transform: translateX(-50%);
  background: $gradient-house;
  border-radius: 22px 22px 18px 18px;
  box-shadow: 0 18px 24px $illustration-house-shadow;
}

.house-scene::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -56px;
  width: 160px;
  height: 86px;
  transform: translateX(-50%);
  background: $gradient-house-roof;
  clip-path: polygon(50% 0, 100% 42%, 100% 100%, 0 100%, 0 42%);
  border-radius: 12px 12px 0 0;
}

.house-scene::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 16px;
  width: 118px;
  height: 54px;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.32);
  border-radius: 12px;
}

.plant {
  position: absolute;
  bottom: 18px;
  width: 56px;
  height: 96px;
  border-radius: 26px 26px 12px 12px;
  background: $gradient-plant;
  box-shadow: inset 0 0 0 6px rgba(255, 255, 255, 0.34);
}

.plant::before,
.plant::after {
  content: '';
  position: absolute;
  bottom: 45px;
  width: 18px;
  height: 54px;
  background: $gradient-leaf;
  border-radius: 14px;
}

.plant::before {
  left: 8px;
  transform: rotate(-18deg);
}

.plant::after {
  right: 8px;
  transform: rotate(18deg);
}

.plant-left {
  left: 92px;
}

.plant-right {
  right: 92px;
}

.todo-bubble {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 86px;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba($primary-color, 0.1);
  color: $primary-text;
  box-shadow: 0 14px 22px rgba($text-primary, 0.08);
  font-size: 0.8rem;
  font-weight: 600;
}

.todo-bubble-one {
  top: 26px;
  right: 78px;
}

.todo-bubble-two {
  left: 72px;
  bottom: 18px;
}

.brand-foot {
  margin-top: 20px;
  color: rgba($text-primary, 0.7);
  font-size: 0.85rem;
  letter-spacing: 0.04em;
}

.login-panel {
  flex: 0.92;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 42px 36px;
  background: rgba(255, 255, 255, 0.62);
}

.login-card {
  width: 100%;
  max-width: 430px;
  background: $card-bg;
  border: 1px solid rgba($text-primary, 0.04);
  border-radius: 28px;
  padding: 32px 28px 24px;
  box-shadow: 0 16px 36px rgba(80, 50, 30, 0.1);
}

.form-header {
  text-align: left;
  margin-bottom: 28px;

  h2 {
    margin: 0;
    color: $text-primary;
    font-size: clamp(1.8rem, 2vw, 2.5rem);
    line-height: 1.2;
    font-weight: 800;
    letter-spacing: -0.04em;
  }

  p {
    margin: 10px 0 0;
    color: rgba($text-primary, 0.68);
    font-size: 0.96rem;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__content) {
    display: block;
  }

  :deep(.el-form-item__error) {
    margin-top: 6px;
    font-size: 12px;
  }
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: $text-primary;
  font-size: 0.92rem;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  height: 52px;
  border-radius: 14px;
  background: $field-bg;
  border: 1px solid rgba($text-primary, 0.06);
  box-shadow: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba($primary-color, 0.3);
  }

  &.is-focus {
    background: $card-bg;
    border-color: rgba($primary-color, 0.7);
    box-shadow: 0 0 0 4px rgba($primary-color, 0.12);
  }
}

:deep(.el-input__inner) {
  color: $text-primary;
  font-size: 0.98rem;
}

:deep(.el-input__prefix) {
  color: $text-placeholder;
}

.form-inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 8px 0 18px;
}

:deep(.el-checkbox) {
  --el-checkbox-checked-bg-color: #{$primary-color};
  --el-checkbox-checked-border-color: #{$primary-color};
  color: $text-primary;
  font-weight: 500;
}

.forgot-link {
  color: $primary-text;
  font-size: 0.9rem;
  text-decoration: none;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.8;
  }
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 14px;
  border: none;
  background: $gradient-primary;
  color: $card-bg;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  box-shadow: 0 14px 24px rgba($primary-color, 0.2);

  &:hover,
  &:focus {
    background: $gradient-primary-hover;
  }
}

.register-line {
  margin-top: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba($text-primary, 0.72);
  font-size: 0.95rem;

  :deep(.el-button) {
    padding: 0;
    height: auto;
    color: $primary-text;
    font-weight: 700;
  }
}

.form-note {
  margin-top: 18px;
  text-align: center;
  color: rgba($text-primary, 0.5);
  font-size: 0.75rem;
  line-height: 1.8;
  letter-spacing: 0.02em;
}

.pwa-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 12px;
  background-color: $primary-light-bg;
  border-radius: 12px;
  color: $primary-text;
  font-size: 0.82rem;
}

@media (max-width: 1023px) {
  .login-page {
    padding: 20px 16px;
  }

  .login-shell {
    min-height: auto;
    flex-direction: column;
    border-radius: 24px;
  }

  .brand-panel {
    min-height: 360px;
    padding: 32px 24px 24px;
  }

  .login-panel {
    padding: 24px 20px 30px;
  }
}

@media (max-width: 639px) {
  .login-page {
    padding: 14px;
  }

  .login-shell {
    border-radius: 20px;
  }

  .brand-panel {
    padding: 24px 18px 18px;
  }

  .logo-mark {
    font-size: 1.7rem;
  }

  .brand-copy h1 {
    font-size: 1.9rem;
  }

  .form-header h2 {
    font-size: 1.55rem;
  }

  .description {
    font-size: 0.94rem;
  }

  .illustration {
    height: 180px;
  }

  .plant-left {
    left: 36px;
  }

  .plant-right {
    right: 36px;
  }

  .house-scene {
    width: 160px;
    height: 96px;
  }

  .login-card {
    padding: 24px 18px 20px;
    border-radius: 20px;
  }

  .form-inline {
    flex-direction: column;
    align-items: flex-start;
  }

  .register-line {
    flex-wrap: wrap;
  }
}
</style>
