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
            <div class="form-label">用户名</div>
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名（字母数字下划线，3-20位）"
                :prefix-icon="User"
                clearable
                :autocomplete="false"
              />
            </el-form-item>

            <div class="form-label">邮箱</div>
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="邮箱（用于接收提醒、找回密码）"
                :prefix-icon="Message"
                clearable
                :autocomplete="false"
              />
            </el-form-item>

            <div class="form-label">昵称</div>
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

            <div class="form-label">密码</div>
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

            <div class="form-label">确认密码</div>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                placeholder="请再次输入密码"
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
                class="submit-btn"
                type="primary"
                block
                :loading="loading"
                native-type="submit"
                size="default"
              >
                立即注册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="register-line">
            <span>已有账号？</span>
            <el-button type="text" @click="goLogin">立即登录</el-button>
          </div>

          <div class="form-note">让家的每一刻，都有迹可循｜你的家庭记录仅对你可见</div>

          <div class="pwa-hint" v-if="isMobile">
            <el-icon><Download /></el-icon>
            <span>支持添加到手机桌面，像原生 APP 一样使用</span>
          </div>
        </div>
      </main>
    </div>

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
import { getFriendlyAuthErrorMessage } from '@/api'

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
      ElMessage.error(getFriendlyAuthErrorMessage(error, '注册失败，请稍后重试'))
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
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background: linear-gradient(135deg, #f8f4ef 0%, #f4efe7 32%, #f7f3ee 100%);
}

.login-shell {
  width: min(1180px, 100%);
  min-height: 760px;
  display: flex;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(221, 112, 78, 0.12);
  border-radius: 32px;
  box-shadow: 0 28px 60px rgba(43, 38, 34, 0.08);
  overflow: hidden;
  backdrop-filter: blur(6px);
}

.brand-panel {
  position: relative;
  flex: 1.2;
  padding: 54px 52px 34px;
  background: linear-gradient(180deg, #f7f3ed 0%, #f3efe9 100%);
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
  color: #dd704e;
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
  color: #dd704e;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.brand-copy h1 {
  margin: 0;
  color: #2b2622;
  font-size: clamp(2.4rem, 2.8vw, 4rem);
  line-height: 1.08;
  letter-spacing: -0.05em;
  font-weight: 800;
}

.description {
  margin: 18px 0 0;
  max-width: 560px;
  color: rgba(43, 38, 34, 0.75);
  font-size: 1.08rem;
  line-height: 1.8;
}

.illustration {
  position: relative;
  width: min(100%, 540px);
  height: 240px;
  margin-top: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.34));
  border: 1px solid rgba(221, 112, 78, 0.08);
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
  background: linear-gradient(180deg, #f1c38a 0%, #f2b267 100%);
  border-radius: 22px 22px 18px 18px;
  box-shadow: 0 18px 24px rgba(122, 87, 44, 0.12);
}

.house-scene::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -56px;
  width: 160px;
  height: 86px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #f7b26d 0%, #ed8a5b 100%);
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
  background: linear-gradient(180deg, #e7f4df 0%, #bfd9b7 100%);
  box-shadow: inset 0 0 0 6px rgba(255, 255, 255, 0.34);
}

.plant::before,
.plant::after {
  content: '';
  position: absolute;
  bottom: 45px;
  width: 18px;
  height: 54px;
  background: linear-gradient(180deg, #d9efc9, #a3c98c);
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
  border: 1px solid rgba(221, 112, 78, 0.1);
  color: #dd704e;
  box-shadow: 0 14px 22px rgba(43, 38, 34, 0.08);
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
  color: rgba(43, 38, 34, 0.7);
  font-size: 0.85rem;
  letter-spacing: 0.04em;
}

.login-panel {
  flex: 0.92;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 42px 36px;
  background: rgba(255, 255, 255, 0.62);
}

.login-card {
  width: min(100%, 430px);
  background: #fff;
  border: 1px solid rgba(43, 38, 34, 0.04);
  border-radius: 28px;
  padding: 32px 28px 24px;
  box-shadow: 0 20px 40px rgba(43, 38, 34, 0.08);
}

.form-header {
  text-align: left;
  margin-bottom: 28px;

  h2 {
    margin: 0;
    color: #2b2622;
    font-size: clamp(1.8rem, 2vw, 2.5rem);
    line-height: 1.2;
    font-weight: 800;
    letter-spacing: -0.04em;
  }

  p {
    margin: 10px 0 0;
    color: rgba(43, 38, 34, 0.68);
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

  :deep(.el-checkbox__label) {
    color: rgba(43, 38, 34, 0.7);
    font-size: 13px;

    a {
      color: #dd704e;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #2b2622;
  font-size: 0.92rem;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  height: 52px;
  border-radius: 14px;
  background: #f8f6f3;
  border: 1px solid rgba(43, 38, 34, 0.06);
  box-shadow: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba(221, 112, 78, 0.3);
  }

  &.is-focus {
    background: #fff;
    border-color: rgba(221, 112, 78, 0.7);
    box-shadow: 0 0 0 4px rgba(221, 112, 78, 0.12);
  }
}

:deep(.el-input__inner) {
  color: #2b2622;
  font-size: 0.98rem;
}

:deep(.el-input__prefix) {
  color: #9f8b82;
}

:deep(.el-checkbox) {
  --el-checkbox-checked-bg-color: #dd704e;
  --el-checkbox-checked-border-color: #dd704e;
  --el-checkbox-checked-text-color: #2b2622;
  color: #2b2622;
  font-weight: 500;
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #dd704e 0%, #d9633d 100%);
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  box-shadow: 0 14px 24px rgba(221, 112, 78, 0.2);

  &:hover,
  &:focus {
    background: linear-gradient(135deg, #d66743 0%, #c95533 100%);
  }
}

.register-line {
  margin-top: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(43, 38, 34, 0.72);
  font-size: 0.95rem;

  :deep(.el-button) {
    padding: 0;
    height: auto;
    color: #dd704e;
    font-weight: 700;
  }
}

.form-note {
  margin-top: 18px;
  text-align: center;
  color: rgba(43, 38, 34, 0.5);
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
  background-color: #fff5ee;
  border-radius: 12px;
  color: #dd704e;
  font-size: 0.82rem;
}

.terms-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 8px 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(43, 38, 34, 0.7);

  h3 {
    text-align: center;
    color: #2b2622;
    margin-bottom: 4px;
  }

  .terms-update {
    text-align: center;
    font-size: 12px;
    color: rgba(43, 38, 34, 0.5);
    margin: 0 0 24px;
    font-weight: normal;
  }

  h4 {
    font-size: 15px;
    font-weight: 700;
    color: #2b2622;
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
    border-radius: 12px;
    color: #c77b2d;
  }
}

@media (max-width: 1024px) {
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

@media (max-width: 640px) {
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
    font-size: 2.3rem;
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

  .form-label {
    font-size: 0.86rem;
  }
}
</style>