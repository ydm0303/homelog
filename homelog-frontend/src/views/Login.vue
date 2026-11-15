<template>
  <AuthLayout>
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
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getFriendlyAuthErrorMessage } from '@/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginFormRef = ref<InstanceType<typeof import('element-plus').ElForm> | null>(null)
const loading = ref(false)

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
  if (authStore.isLoggedIn) {
    router.push(route.query.redirect as string || '/')
  }
})

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

.form-inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 8px 0 18px;
}

.forgot-link {
  color: $primary-text;
  font-size: $text-label;
  text-decoration: none;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.8;
  }
}

@media (max-width: 639px) {
  .form-inline {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
