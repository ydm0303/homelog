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
        </div>
      </div>

      <div class="login-right">
        <div class="login-form-card">
          <div class="form-header">
            <h2>创建账号</h2>
            <p>注册后自动创建家庭空间，邀请家人共同记录</p>
          </div>

          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="login-form"
            @submit.prevent="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名（字母数字下划线，3-20位）"
                :prefix-icon="User"
                clearable
                :autocomplete="false"
              />
            </el-form-item>

            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="邮箱（用于接收提醒、找回密码）"
                :prefix-icon="Message"
                clearable
                :autocomplete="false"
              />
            </el-form-item>

            <el-form-item prop="nickName">
              <el-input
                v-model="registerForm.nickName"
                placeholder="昵称（家人看到的名字）"
                :prefix-icon="UserFilled"
                clearable
                :autocomplete="false"
                maxlength="32"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                placeholder="密码（6-64位）"
                :prefix-icon="Lock"
                show-password
                :autocomplete="false"
                type="password"
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                placeholder="确认密码"
                :prefix-icon="Lock"
                show-password
                :autocomplete="false"
                type="password"
              />
            </el-form-item>

            <el-form-item prop="agreeTerms">
              <el-checkbox v-model="registerForm.agreeTerms">
                我已阅读并同意<a href="javascript:void(0)" @click="showTerms = true">《用户协议与隐私政策》</a>
              </el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                block
                :loading="loading"
                native-type="submit"
                size="default"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>已有账号？</span>
            <el-button type="text" @click="goLogin">立即登录</el-button>
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

    <!-- 协议弹窗 -->
    <el-dialog
      v-model="showTerms"
      title="用户协议与隐私政策"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="terms-content">
        <h3>家记 用户协议与隐私政策</h3>
        <p class="terms-update">更新日期：2026-11-03</p>

        <h4>一、服务说明</h4>
        <p>家记是一个私有家庭记录工具，旨在帮助家庭成员收纳药品、待办、记事、证件等零散信息。本服务为个人自建部署，数据存储在您自行配置的数据库中，开发者无法访问您的数据。</p>

        <h4>二、数据所有权</h4>
        <p>您创建的所有记录、上传的图片、配置的模块字段等数据，归您（家庭空间）所有。我们不主张对您数据的任何所有权。</p>

        <h4>三、隐私保护</h4>
        <ul>
          <li>注册时收集的用户名、邮箱、昵称仅用于账号识别、邮件提醒发送、家庭成员展示</li>
          <li>密码采用 BCrypt 加密存储，明文不落库，我们无法知晓您的密码</li>
          <li>不收集任何超出功能必要范围的个人信息，不进行用户画像、广告推荐</li>
        </ul>

        <h4>四、医疗免责声明</h4>
        <p class="disclaimer">
          <strong>重要：</strong>本工具中的药品档案、用药记录仅作存档参考，不提供医疗诊断建议，不替代医生处方。
          请遵医嘱用药，勿依赖本工具做医疗决策。因使用本工具导致的任何后果，开发者不承担责任。
        </p>

        <h4>五、数据备份与导出</h4>
        <p>管理员可随时导出全空间数据（Excel + 图片压缩包），建议定期备份。数据删除后不可恢复，请谨慎操作。</p>

        <h4>六、服务终止</h4>
        <p>如您停止使用本服务，可自行导出数据后删除部署实例。我们不保留您的任何数据副本。</p>
      </div>
      <template #footer>
        <el-button @click="showTerms = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message, Lock, UserFilled, Download, House } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const registerFormRef = ref<InstanceType<typeof import('element-plus').ElForm> | null>(null)
const loading = ref(false)
const isMobile = ref(false)
const showTerms = ref(false)

const registerForm = reactive({
  username: '',
  email: '',
  nickName: '',
  password: '',
  confirmPassword: '',
  agreeTerms: false
})

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '用户名仅支持字母、数字、下划线，3-20位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称不超过32位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule: any, value: string, callback: any) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      }, trigger: 'blur'
    }
  ],
  agreeTerms: [
    // required 对 boolean 的 false 不生效，必须显式校验
    { validator: (rule: any, value: boolean, callback: any) => {
        if (!value) {
          callback(new Error('请阅读并同意用户协议'))
        } else {
          callback()
        }
      }, trigger: 'change'
    }
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

onUnmounted(() => window.removeEventListener('resize', checkMobile))

async function handleRegister() {
  if (!registerFormRef.value) return

  registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.register({
        username: registerForm.username,
        password: registerForm.password,
        email: registerForm.email,
        nickName: registerForm.nickName
      })
      ElMessage.success('注册成功，欢迎使用家记！')
      router.push(route.query.redirect as string || '/')
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.query.redirect } })
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
    margin: 0;
    line-height: 1.4;
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

  :deep(.el-checkbox__label) {
    font-size: 13px;
    color: $text-secondary;

    a {
      color: $primary-color;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 14px;
  color: $text-secondary;

  :deep(.el-button) {
    height: auto;
    padding: 0;
    font-size: 14px;
    line-height: 1;
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

.terms-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 8px 0;
  font-size: 14px;
  line-height: 1.8;
  color: $text-secondary;

  h3 {
    text-align: center;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .terms-update {
    text-align: center;
    font-size: 12px;
    color: $text-placeholder;
    margin: 0 0 24px;
    font-weight: normal;
  }

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin: 24px 0 12px;
  }

  p {
    margin: 0 0 12px;
  }

  ul {
    margin: 0 0 12px;
    padding-left: 20px;

    li {
      margin-bottom: 8px;
    }
  }

  .disclaimer {
    padding: 16px;
    background: #fffbe8;
    border: 1px solid #fde2a2;
    border-radius: $border-radius-base;
    color: #e6a23c;
  }
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

  :deep(.el-checkbox__label) {
    font-size: 16px !important;
  }

  .terms-content {
    font-size: 16px !important;
  }
}
</style>