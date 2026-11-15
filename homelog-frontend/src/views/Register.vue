<template>
  <AuthLayout>
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
          autocomplete="off"
        />
      </el-form-item>

      <div class="form-label">邮箱</div>
      <el-form-item prop="email">
        <el-input
          v-model="registerForm.email"
          placeholder="邮箱（用于接收提醒、找回密码）"
          :prefix-icon="Message"
          clearable
          autocomplete="off"
        />
      </el-form-item>

      <div class="form-label">昵称</div>
      <el-form-item prop="nickName">
        <el-input
          v-model="registerForm.nickName"
          placeholder="昵称（家人看到的名字）"
          :prefix-icon="UserFilled"
          clearable
          autocomplete="off"
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
          autocomplete="off"
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
          autocomplete="off"
          type="password"
        />
      </el-form-item>

      <el-form-item prop="agreeTerms">
        <el-checkbox v-model="registerForm.agreeTerms">
          我已阅读并同意 <a href="javascript:void(0)" @click="showTerms = true">《用户协议与隐私政策》</a>
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
  </AuthLayout>

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
        <li>不收集任何超出功能必要范围的个人信息，不进行用户画像、广告推送</li>
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message, Lock, UserFilled } from '@element-plus/icons-vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getFriendlyAuthErrorMessage } from '@/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const registerFormRef = ref<InstanceType<typeof import('element-plus').ElForm> | null>(null)
const loading = ref(false)
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
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '用户名仅支持字母、数字、下划线，3-20 位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称不超过 32 字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_rule: any, value: string, callback: any) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      }, trigger: 'blur'
    }
  ],
  agreeTerms: [
    // required 对 boolean 为 false 不生效，必须显式校验
    { validator: (_rule: any, value: boolean, callback: any) => {
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
  if (authStore.isLoggedIn) {
    router.push(route.query.redirect as string || '/')
  }
})

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
@use '@/styles/variables' as *;

.terms-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 8px 0;
  font-size: $text-body;
  line-height: 1.8;
  color: rgba($text-primary, 0.7);

  h3 {
    text-align: center;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .terms-update {
    text-align: center;
    font-size: $text-caption;
    color: rgba($text-primary, 0.5);
    margin: 0 0 24px;
    font-weight: $font-weight-normal;
  }

  h4 {
    font-size: $font-size-md;
    font-weight: $font-weight-bold;
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
    background: $warning-bg;
    border: 1px solid $warning-border;
    border-radius: $border-radius-lg;
    color: $warning-text;
  }
}
</style>
