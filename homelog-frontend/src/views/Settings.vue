<template>
  <div class="settings-page">
    <PageHeader title="设置中心" description="管理家庭成员、模块配置、数据导出与适老化选项" />

    <el-tabs v-model="activeTab" class="settings-tabs" @tab-click="handleTabClick">
      <!-- 成员管理 -->
      <el-tab-pane label="成员管理" name="members" :disabled="!isAdmin">
        <el-card class="tab-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>家庭成员</h3>
              <el-button type="primary" size="small" @click="showAddMember = true">
                <el-icon><Plus /></el-icon>
                邀请成员
              </el-button>
            </div>
          </template>

          <el-table :data="members" border stripe class="w-full">
            <el-table-column prop="nickName" label="昵称" min-width="120" />
            <el-table-column prop="username" label="账号" min-width="140" />
            <el-table-column prop="email" label="邮箱" min-width="200" v-if="!isMobile" />
            <el-table-column label="角色" width="120">
              <template #default="scope">
                <el-tag :type="getRoleTagType(scope.row.role)" size="small">
                  {{ getRoleLabel(scope.row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="邮件提醒" width="120">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.receiveMail"
                  :active-value="1"
                  :inactive-value="0"
                  @change="(val: string | number | boolean) => updateMemberRole(scope.row, { receiveMail: val ? 1 : 0 })"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" :fixed="isMobile ? false : 'right'">
              <template #default="scope">
                <div class="row-actions">
                  <el-button
                    v-for="opt in assignableOptions(scope.row.role)"
                    :key="opt.value"
                    link
                    size="small"
                    type="primary"
                    @click="updateMemberRole(scope.row, { role: opt.value })"
                  >
                    设为{{ opt.label }}
                  </el-button>
                  <el-button
                    v-if="canRemoveMember(scope.row)"
                    link
                    size="small"
                    type="danger"
                    @click="removeMember(scope.row)"
                  >
                    移出家庭
                  </el-button>
                  <span
                    v-if="assignableOptions(scope.row.role).length === 0 && !canRemoveMember(scope.row)"
                    class="no-action"
                  >—</span>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 待处理邀请（邀请人可撤销，设计文档 v1.2 §8） -->
          <div v-if="pendingInvitations.length > 0" class="pending-invitations">
            <div class="pending-invitations__head">
              <span class="pending-invitations__title">待处理的邀请</span>
              <el-tag type="warning" size="small" effect="light" round>{{ pendingInvitations.length }}</el-tag>
            </div>
            <ul class="invite-list">
              <li v-for="inv in pendingInvitations" :key="inv.id" class="invite-item">
                <div class="invite-item__avatar">{{ inviteeInitial(inv.inviteeNickname) }}</div>
                <div class="invite-item__main">
                  <div class="invite-item__name">
                    {{ inv.inviteeNickname || '未命名成员' }}
                    <el-tag :type="getRoleTagType(inv.role)" size="small" effect="plain">
                      {{ getRoleLabel(inv.role) }}
                    </el-tag>
                  </div>
                  <div class="invite-item__meta">
                    <el-icon><Clock /></el-icon>
                    <span>邀请您加入「{{ inv.spaceName || currentSpaceName }}」· {{ formatInviteTime(inv.createdAt) }}</span>
                  </div>
                </div>
                <div class="invite-item__status">
                  <el-tag type="info" size="small" effect="light" round>等待对方应答</el-tag>
                </div>
                <el-button link size="small" type="danger" @click="cancelInvitation(inv)">撤销</el-button>
              </li>
            </ul>
          </div>
        </el-card>

        <!-- 邀请成员对话框 -->
        <el-dialog v-model="showAddMember" title="邀请成员加入" width="500px" @closed="resetInviteeCheck">
          <el-form :model="addMemberForm" :rules="addMemberRules" label-width="100px">
            <el-form-item label="账号或邮箱" prop="identifier">
              <el-input
                v-model="addMemberForm.identifier"
                placeholder="对方的账号或注册邮箱"
                clearable
              >
                <template v-if="inviteeCheck === 'checking'" #suffix>
                  <el-icon class="is-loading"><Loading /></el-icon>
                </template>
              </el-input>
              <div class="invitee-check" :class="`invitee-check--${inviteeCheck}`">
                <template v-if="inviteeCheck === 'ok'">
                  <el-icon><CircleCheckFilled /></el-icon>
                  <span>已找到账号：<b>{{ inviteeNickname }}</b><em v-if="inviteeEmail">（{{ inviteeEmail }}）</em>，确认邀请 TA？</span>
                </template>
                <template v-else-if="inviteeCheck === 'notfound'">
                  <el-icon><CircleCloseFilled /></el-icon>
                  <span>该账号或邮箱尚未注册，请核对后再发送，或让对方先注册</span>
                </template>
                <template v-else-if="inviteeCheck === 'badformat'">
                  <el-icon><WarningFilled /></el-icon>
                  <span>格式不正确：请输入合法用户名（字母/数字/_/-，3~32位）或邮箱地址</span>
                </template>
              </div>
            </el-form-item>
            <el-form-item label="角色" prop="role">
              <el-radio-group v-model="addMemberForm.role">
                <el-radio
                  v-for="opt in inviteOptions"
                  :key="opt.value"
                  :label="opt.value"
                >
                  {{ opt.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showAddMember = false">取消</el-button>
            <el-button
              type="primary"
              :loading="addingMember"
              :disabled="inviteeCheck === 'notfound' || inviteeCheck === 'badformat'"
              @click="confirmAddMember"
            >发送邀请</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 空间管理 -->
      <el-tab-pane label="空间管理" name="space" :disabled="!isAdmin">
        <el-card class="tab-card space-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>当前家庭空间</h3>
            </div>
          </template>

          <div class="space-info">
            <div class="info-item">
              <span class="info-label">空间名称</span>
              <span class="info-value">{{ currentSpaceName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">我的角色</span>
              <el-tag :type="getRoleTagType(authStore.role)" size="small">{{ getRoleLabel(authStore.role) }}</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">在籍成员数</span>
              <span class="info-value info-stat">{{ currentSpace?.memberCount ?? '-' }}</span>
            </div>
          </div>

          <div class="space-section">
            <h4 class="section-title">修改名称</h4>
            <p class="section-desc">空间名称会显示在所有成员的侧边栏空间切换器中。</p>
            <div class="inline-form">
              <el-input v-model="spaceNameInput" maxlength="64" clearable placeholder="输入新的空间名称" class="form-input" />
              <el-button type="primary" :loading="renamingSpace" @click="renameSpace">保存</el-button>
            </div>
          </div>

          <template v-if="authStore.isOwner">
            <div class="danger-zone">
              <h4 class="section-title danger-title">解散家庭空间</h4>
              <p class="danger-note">
                解散后该空间将从所有成员的列表中消失，仅所有者可执行，操作不可自行恢复。请输入空间名称确认。
              </p>
              <div class="inline-form">
                <el-input v-model="dissolveNameInput" :placeholder="currentSpaceName" class="form-input" />
                <el-button type="danger" :loading="dissolvingSpace" @click="dissolveSpace">解散空间</el-button>
              </div>
            </div>
          </template>
        </el-card>
      </el-tab-pane>

      <!-- 权限矩阵 -->
      <el-tab-pane label="权限矩阵" name="permissions" :disabled="!isAdmin">
        <el-card class="tab-card" shadow="never">
          <h4 class="matrix-title">角色权限对照表</h4>
          <el-table :data="permissionMatrix" border stripe class="w-full">
            <el-table-column prop="permission" label="权限项" min-width="200" />
            <el-table-column
              v-for="col in permissionColumns"
              :key="col.prop"
              :label="col.label"
              width="110"
              align="center"
            >
              <template #default="scope">
                <el-icon v-if="scope.row.flags[col.prop]"><Check color="var(--el-color-success)" /></el-icon>
                <el-icon v-else><Close color="var(--el-color-danger)" /></el-icon>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 模块管理 -->
      <el-tab-pane label="模块管理" name="modules" :disabled="!isAdmin">
        <el-card class="tab-card" shadow="never">
          <template #header>
            <h3>模块配置（拖拽排序、开关、改名）</h3>
          </template>

          <el-table
            :data="allModules"
            border
            stripe
            class="w-full"
            row-key="id"
          >
            <el-table-column type="index" label="排序" width="60" align="center" />
            <el-table-column prop="name" label="显示名称" min-width="140">
              <template #default="scope">
                <el-input
                  v-model="scope.row.name"
                  size="small"
                  @blur="updateModule(scope.row)"
                  clearable
                />
              </template>
            </el-table-column>
            <el-table-column prop="moduleKey" label="模块 Key" width="120" />
            <el-table-column prop="moduleType" label="类型" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.moduleType === 1 ? 'info' : 'success'" size="small">
                  {{ scope.row.moduleType === 1 ? '内置' : '自定义' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="80" align="center">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.isEnable"
                  :active-value="1"
                  :inactive-value="0"
                  @change="(val: string | number | boolean) => updateModule({ ...scope.row, isEnable: val ? 1 : 0 })"
                />
              </template>
            </el-table-column>
            <el-table-column label="首页显示" width="100" align="center">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.showInHome"
                  :active-value="1"
                  :inactive-value="0"
                  @change="(val: string | number | boolean) => updateModule({ ...scope.row, showInHome: val ? 1 : 0 })"
                />
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序号" width="80" align="center" v-if="!isMobile" />
            <el-table-column label="操作" width="120" :fixed="isMobile ? false : 'right'">
              <template #default="scope">
                <div class="row-actions">
                  <el-button link size="small" type="primary" @click="goToModuleFields(scope.row)">
                    字段配置
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 数据导出导入 -->
      <el-tab-pane label="数据导出导入" name="export-import" :disabled="!isAdmin">
        <el-row :gutter="24">
          <!-- 导出 -->
          <el-col :xs="24" :lg="12">
            <el-card class="tab-card" shadow="never">
              <h4>导出数据</h4>

              <el-form :model="exportForm" label-width="100px" inline>
                <el-form-item label="导出范围">
                  <el-radio-group v-model="exportForm.scope">
                    <el-radio label="module">单模块</el-radio>
                    <el-radio label="all">全空间</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="选择模块" v-if="exportForm.scope === 'module'">
                  <el-select v-model="exportForm.moduleId" placeholder="选择模块" style="width: 200px">
                    <el-option
                      v-for="m in enabledModules"
                      :key="m.id"
                      :label="m.name"
                      :value="m.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleExport" :loading="exporting">
                    <el-icon><Download /></el-icon>
                    导出
                  </el-button>
                </el-form-item>
              </el-form>

              <el-divider />
              <p class="export-note">
                <el-icon><InfoFilled /></el-icon>
                单模块导出：Excel 文件；全空间导出：ZIP 包（含 Excel + 所有图片）
              </p>
            </el-card>
          </el-col>

          <!-- 导入 -->
          <el-col :xs="24" :lg="12">
            <el-card class="tab-card" shadow="never">
              <h4>药品 CSV 导入</h4>

              <el-form :model="importForm" label-width="100px">
                <el-form-item label="CSV 文件">
                  <el-upload
                    action="#"
                    :auto-upload="false"
                    :on-change="handleImportFileChange"
                    :show-file-list="true"
                    list-type="text"
                    accept=".csv"
                  >
                    <el-button type="primary">选择 CSV 文件</el-button>
                  </el-upload>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    @click="handleImport"
                    :loading="importing"
                    :disabled="!importFile"
                  >
                    <el-icon><Upload /></el-icon>
                    导入
                  </el-button>
                  <el-button @click="downloadTemplate" style="margin-left: 12px">
                    <el-icon><Download /></el-icon>
                    下载模板
                  </el-button>
                </el-form-item>
              </el-form>

              <el-divider />
              <p class="import-note">
                <el-icon><InfoFilled /></el-icon>
                仅支持药品模块 CSV 导入，需使用标准模板。导入前会预检数据冲突。
              </p>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 账户安全 -->
      <el-tab-pane label="账户安全" name="account">
        <el-card class="tab-card" shadow="never">
          <h4>修改密码</h4>
          <el-form :model="passwordForm" :rules="passwordRules" label-width="100px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="changingPassword">修改密码</el-button>
            </el-form-item>
          </el-form>

          <el-divider />
          <h4>登录设备管理</h4>
          <el-empty description="暂无设备管理功能，敬请期待" />
        </el-card>
      </el-tab-pane>

      <!-- 提醒推送 -->
      <el-tab-pane label="提醒推送" name="push">
        <el-card class="tab-card" shadow="never">
          <h4>浏览器到期提醒推送</h4>
          <template v-if="pushState === 'unsupported'">
            <el-alert type="info" :closable="false" show-icon
              title="当前浏览器不支持消息推送"
              description="需使用 Chrome / Edge / Firefox，或 iOS 16.4+ 已添加到主屏幕的家记。" />
          </template>
          <template v-else-if="!isStandalone">
            <el-alert type="warning" :closable="false" show-icon
              title="请先将家记添加到主屏幕"
              description="网页标签模式下无法稳定接收推送。iOS：Safari 分享 → 添加到主屏幕；Android/桌面：浏览器菜单 → 安装应用。" />
          </template>
          <template v-else-if="pushState === 'denied'">
            <el-alert type="error" :closable="false" show-icon
              title="通知权限已被拒绝"
              description="请在浏览器站点设置中重新允许「通知」后刷新本页。" />
          </template>
          <template v-else>
            <p class="push-status">
              当前状态：<el-tag :type="subscribed ? 'success' : 'info'" size="small">
                {{ subscribed ? '已开启推送' : '未开启推送' }}
              </el-tag>
            </p>
            <el-button
              v-if="!subscribed"
              type="primary"
              :loading="busy"
              @click="onEnablePush"
            >开启到期提醒推送</el-button>
            <el-button
              v-else
              :loading="busy"
              @click="onDisablePush"
            >关闭推送</el-button>
          </template>
        </el-card>
      </el-tab-pane>

      <!-- 适老化设置 -->
      <el-tab-pane label="适老化设置" name="elderly">
        <el-card class="tab-card" shadow="never">
          <h4>长辈模式</h4>
          <el-form :model="elderlyForm" label-width="100px">
            <el-form-item label="长辈模式">
              <el-switch
                v-model="elderlyForm.enabled"
                @change="toggleElderlyMode"
                inline-prompt
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item label="字体放大倍数">
              <el-slider
                v-model="elderlyForm.fontScale"
                :min="1"
                :max="2"
                :step="0.1"
                show-input
                @change="applyElderlySettings"
              />
            </el-form-item>
            <el-form-item label="按钮放大倍数">
              <el-slider
                v-model="elderlyForm.buttonScale"
                :min="1"
                :max="1.5"
                :step="0.1"
                show-input
                @change="applyElderlySettings"
              />
            </el-form-item>
            <el-form-item label="间距放大倍数">
              <el-slider
                v-model="elderlyForm.spacingScale"
                :min="1"
                :max="1.5"
                :step="0.1"
                show-input
                @change="applyElderlySettings"
              />
            </el-form-item>
            <el-form-item label="高对比度">
              <el-switch
                v-model="elderlyForm.highContrast"
                active-text="开启"
                inactive-text="关闭"
                @change="applyElderlySettings"
              />
            </el-form-item>
          </el-form>

          <el-divider />
          <h4>预览效果</h4>
          <el-card class="preview-card">
            <p class="preview-text">这是预览文本，字号会随设置调整</p>
            <el-button type="primary" class="preview-btn">主要按钮</el-button>
            <el-button class="preview-btn">次要按钮</el-button>
            <el-input placeholder="输入框示例" class="preview-input" />
          </el-card>
        </el-card>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { authApi, metaApi, spaceApi, invitationApi, exportApi } from '@/api'
import {
  Plus, Check, Close, Upload, Download, InfoFilled,
  Loading, CircleCheckFilled, CircleCloseFilled, WarningFilled, Clock
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import { formatDateTime } from '@/utils/date'
import {
  ROLE,
  ASSIGNABLE_ROLES,
  roleLabel,
  roleTagType,
  canManageTargetRole,
  canGrantRole
} from '@/utils/role'
import { useIsMobile } from '@/composables/useMediaQuery'
import { usePush } from '@/composables/usePush'

const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

const isMobile = useIsMobile()
const { pushState, subscribed, busy, isStandalone, refreshStatus, enablePush, disablePush } = usePush()

const activeTab = ref('members')

const isAdmin = computed(() => authStore.isAdmin)

const members = ref<any[]>([])
const invitations = ref<any[]>([])
// 仅展示仍待处理（status=1）的邀请，已接受/拒绝的后端默认不返回
const pendingInvitations = computed(() => invitations.value.filter((i: any) => i.status === 1))
const allModules = ref<any[]>([])
const enabledModules = computed(() => moduleStore.enabledModules)

// 空间管理（改名 / 解散）——数据源来自 authStore.fetchSpaces()
const currentSpace = computed(() => authStore.currentSpace)
const currentSpaceName = computed(() => authStore.currentSpace?.name || '家庭空间')
const spaceNameInput = ref('')
const dissolveNameInput = ref('')
const renamingSpace = ref(false)
const dissolvingSpace = ref(false)

const showAddMember = ref(false)
const addingMember = ref(false)
const addMemberForm = reactive({
  identifier: '',
  role: ROLE.MEMBER as number
})
const addMemberRules = {
  identifier: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// —— 邀请标识真实性预检：失焦/输入停顿后查库，命中给昵称确认、未命中就地提示 ——
const IDENTIFIER_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$|^[A-Za-z0-9_-]{3,32}$/
type InviteeCheckState = 'idle' | 'checking' | 'ok' | 'notfound' | 'badformat' | 'error'
const inviteeCheck = ref<InviteeCheckState>('idle')
const inviteeNickname = ref('')
const inviteeEmail = ref('')
let checkTimer: ReturnType<typeof setTimeout> | null = null
let lastCheckedIdentifier = ''

watch(() => addMemberForm.identifier, (val) => {
  const key = (val || '').trim()
  if (checkTimer) clearTimeout(checkTimer)
  if (!key) {
    inviteeCheck.value = 'idle'
    inviteeNickname.value = ''
    inviteeEmail.value = ''
    lastCheckedIdentifier = ''
    return
  }
  if (!IDENTIFIER_RE.test(key)) {
    inviteeCheck.value = 'badformat'
    lastCheckedIdentifier = ''
    return
  }
  if (key === lastCheckedIdentifier && inviteeCheck.value !== 'error') return
  inviteeCheck.value = 'checking'
  checkTimer = setTimeout(() => runInviteeCheck(key), 400)
})

async function runInviteeCheck(key: string) {
  try {
    const res = await invitationApi.check(key)
    const data = res.data || {}
    lastCheckedIdentifier = key
    if (data.exists) {
      inviteeCheck.value = 'ok'
      inviteeNickname.value = data.nickname || ''
      inviteeEmail.value = data.email || ''
    } else {
      inviteeCheck.value = 'notfound'
      inviteeNickname.value = ''
      inviteeEmail.value = ''
    }
  } catch {
    // 网络/权限等异常不打断输入，仅在发送时由 create 接口兜底报错
    inviteeCheck.value = 'error'
  }
}

function resetInviteeCheck() {
  if (checkTimer) clearTimeout(checkTimer)
  inviteeCheck.value = 'idle'
  inviteeNickname.value = ''
  inviteeEmail.value = ''
  lastCheckedIdentifier = ''
}

/**
 * 角色权限对照表（设计文档 v1.2 §4.2，按 D3 沿用现有实现语义）。
 * flags 的 key 与 permissionColumns 一一对应，新增档位只改这两处。
 */
const permissionColumns = [
  { prop: 'owner', label: '所有者' },
  { prop: 'admin', label: '管理员' },
  { prop: 'member', label: '普通成员' },
  { prop: 'readonly', label: '只读' }
] as const

const permissionMatrix = [
  { permission: '查看所有模块数据', flags: { owner: true, admin: true, member: true, readonly: true } },
  { permission: '新增记录', flags: { owner: true, admin: true, member: true, readonly: false } },
  { permission: '编辑自己创建的记录', flags: { owner: true, admin: true, member: true, readonly: false } },
  { permission: '编辑他人创建的记录', flags: { owner: true, admin: true, member: false, readonly: false } },
  { permission: '删除记录', flags: { owner: true, admin: true, member: false, readonly: false } },
  { permission: '查看私密字段 / 私密记录', flags: { owner: true, admin: true, member: false, readonly: false } },
  { permission: '管理成员（邀请 / 改角色 / 移除）', flags: { owner: true, admin: true, member: false, readonly: false } },
  { permission: '指派或移除管理员', flags: { owner: true, admin: false, member: false, readonly: false } },
  { permission: '模块开关 / 排序 / 改名', flags: { owner: true, admin: true, member: false, readonly: false } },
  { permission: '解散家庭空间', flags: { owner: true, admin: false, member: false, readonly: false } },
  { permission: '主动退出家庭空间', flags: { owner: false, admin: true, member: true, readonly: true } },
  { permission: '接收邮件提醒', flags: { owner: true, admin: true, member: true, readonly: true } },
  { permission: '修改个人密码', flags: { owner: true, admin: true, member: true, readonly: true } }
]

/**
 * 某成员行可被指派到的角色选项。
 * 与后端 updateMember 完全对齐：先判「能不能碰这个人」(canManageTargetRole)，
 * 再逐个档判「能不能放到那档」(canGrantRole)。两级都过才显示该选项。
 */
function assignableOptions(targetRole: number) {
  if (!canManageTargetRole(authStore.role, targetRole)) return []
  return ASSIGNABLE_ROLES.filter(
    opt => opt.value !== targetRole && canGrantRole(authStore.role, opt.value)
  )
}

/** 当前用户能否移除该行成员：不能移自己，且只能移除严格低于自己的档（同后端 deleteMember） */
function canRemoveMember(row: any): boolean {
  if (row.userId === authStore.user?.id) return false
  return canManageTargetRole(authStore.role, row.role)
}

/** 邀请对话框可选角色：随邀请人档位收窄 */
const inviteOptions = computed(() => ASSIGNABLE_ROLES.filter(opt => canGrantRole(authStore.role, opt.value)))

const exportForm = reactive({
  scope: 'module' as 'module' | 'all',
  moduleId: null as number | null
})
const exporting = ref(false)

const importForm = reactive({})
const importFile = ref<File | null>(null)
const importing = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: (_rule: any, value: string, callback: any) => { if (value !== passwordForm.newPassword) callback(new Error('两次输入密码不一致')); else callback() }, trigger: 'blur' }]
}
const changingPassword = ref(false)

const elderlyForm = reactive({
  enabled: false,
  fontScale: 1.3,
  buttonScale: 1.2,
  spacingScale: 1.2,
  voiceEnabled: false,
  highContrast: false
})

onMounted(async () => {
  await moduleStore.fetchModules()
  if (isAdmin.value) {
    await fetchMembers()
    await fetchInvitations()
    await fetchAllModules()
    spaceNameInput.value = currentSpaceName.value
  }
  loadElderlySettings()
  await refreshStatus()
})

async function onEnablePush() {
  const ok = await enablePush()
  if (ok) ElMessage.success('已开启到期提醒推送')
  else if (pushState.value === 'denied') ElMessage.warning('浏览器通知权限被拒绝，请在站点设置中允许通知')
  else ElMessage.error('开启推送失败，请稍后重试')
}

async function onDisablePush() {
  if (await disablePush()) ElMessage.success('已关闭推送')
  else ElMessage.error('操作失败')
}

async function fetchMembers() {
  try {
    const response = await spaceApi.getMembers()
    members.value = response.data
  } catch (e) {
    console.error(e)
  }
}

async function fetchInvitations() {
  try {
    const response = await invitationApi.list({ direction: 'sent', pendingOnly: true })
    invitations.value = Array.isArray(response.data) ? response.data : []
  } catch (e) {
    console.error(e)
  }
}

async function fetchAllModules() {
  try {
    const response = await metaApi.getAllModules()
    allModules.value = response.data
  } catch (e) {
    console.error(e)
  }
}

function handleTabClick(tab: any) {
  if (tab.name === 'modules' && isAdmin.value) {
    fetchAllModules()
  }
}

// 角色文案/颜色统一委托 @/utils/role（档位语义单点定义，见设计文档 v1.2 §4.4）
function getRoleLabel(role: number) {
  return roleLabel(role)
}

function getRoleTagType(role: number) {
  return roleTagType(role)
}

async function updateMemberRole(member: any, changes: Partial<any>) {
  try {
    await spaceApi.updateMember({ userId: member.userId, ...changes })
    ElMessage.success('更新成功')
    fetchMembers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  }
}

async function removeMember(member: any) {
  const name = member.nickName || member.username || '该成员'
  try {
    // 完整连带删除不可逆：先明确告知会一并删除其全部记录与图片
    await ElMessageBox.confirm(
      `确定将 ${name} 移出家庭吗？\n\n其在该空间创建的所有记录、图片与消息都会被永久删除，且无法恢复。`,
      '确认移出并删除数据',
      {
        confirmButtonText: '移出并删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await spaceApi.deleteMember(member.userId, true)
    ElMessage.success('已移除该成员及其内容')
    fetchMembers()
    authStore.fetchSpaces().catch(() => {})
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function confirmAddMember() {
  const key = addMemberForm.identifier.trim()
  if (!IDENTIFIER_RE.test(key)) {
    ElMessage.warning('请输入合法的账号（字母/数字/_/-，3~32位）或邮箱地址')
    return
  }
  if (inviteeCheck.value === 'notfound') {
    ElMessage.warning('该账号或邮箱尚未注册，请确认无误或让对方先注册')
    return
  }
  try {
    addingMember.value = true
    await invitationApi.create({ identifier: key, role: addMemberForm.role })
    ElMessage.success('邀请已发送')
    showAddMember.value = false
    addMemberForm.identifier = ''
    addMemberForm.role = ROLE.MEMBER
    resetInviteeCheck()
    fetchInvitations()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '邀请失败')
  } finally {
    addingMember.value = false
  }
}

async function cancelInvitation(invitation: any) {
  try {
    await invitationApi.cancel(invitation.id)
    ElMessage.success('已撤销邀请')
    fetchInvitations()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '撤销失败')
  }
}

function formatInviteTime(val: string) {
  return val ? formatDateTime(val) : '-'
}

function inviteeInitial(name?: string) {
  const n = (name || '').trim()
  return n ? n.charAt(0).toUpperCase() : '家'
}

async function renameSpace() {
  const name = spaceNameInput.value.trim()
  if (!name) {
    ElMessage.warning('请输入新的空间名称')
    return
  }
  if (name === currentSpaceName.value) {
    ElMessage.info('名称未变化')
    return
  }
  try {
    renamingSpace.value = true
    await spaceApi.update(name)
    await authStore.fetchSpaces()
    spaceNameInput.value = name
    ElMessage.success('空间名称已更新')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    renamingSpace.value = false
  }
}

async function dissolveSpace() {
  if (!dissolveNameInput.value.trim()) {
    ElMessage.warning('请输入空间名称以确认解散')
    return
  }
  try {
    dissolvingSpace.value = true
    await spaceApi.dissolve(dissolveNameInput.value.trim())
    ElMessage.success('家庭空间已解散')
    // 当前空间失效：清掉上下文后再切到剩余空间（无则退出登录）
    localStorage.removeItem('current_space_id')
    const remaining = await authStore.fetchSpaces().catch(() => [])
    dissolveNameInput.value = ''
    if (remaining.length > 0) {
      await authStore.switchSpace(remaining[0].id)
    } else {
      await authStore.logout()
      router.push('/login')
      return
    }
    window.location.assign('/')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '解散失败')
  } finally {
    dissolvingSpace.value = false
  }
}

async function updateModule(module: any) {
  try {
    // 后端用 moduleId 定位模块，且会用请求中的字段整体覆盖，所以要带上整行
    await metaApi.updateModule({ ...module, moduleId: module.id })
    ElMessage.success('更新成功')
  } catch {
    ElMessage.error('更新失败')
  }
}

function goToModuleFields(_module: any) {
  // TODO: 跳转到字段配置页面
  ElMessage.info('字段配置功能开发中...')
}

async function handleExport() {
  if (exportForm.scope === 'module' && !exportForm.moduleId) {
    ElMessage.warning('请选择要导出的模块')
    return
  }
  try {
    exporting.value = true
    const params: any = { scope: exportForm.scope }
    if (exportForm.scope === 'module' && exportForm.moduleId) {
      params.moduleId = exportForm.moduleId
    }
    const response = await exportApi.exportAll(params)
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = exportForm.scope === 'all' ? 'homelog-full-export.zip' : `homelog-module-${exportForm.moduleId}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function handleImportFileChange(file: any) {
  importFile.value = file.raw
}

function downloadTemplate() {
  // 生成 CSV 模板
  const headers = ['药品名', '有效期', '存放位置', '用法用量', '适用症状', '禁忌', '购买日期', '剩余数量', '绑定成员邮箱']
  // 示例行：必填列（药品名、有效期）填示例值并标注"必填"，选填列留空
  const exampleRow = ['感冒灵颗粒（必填）', '2027-12-31（必填）', '', '', '', '', '', '', '']
  const csv = '\uFEFF' + [headers.join(','), exampleRow.join(',')].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'drug_import_template.csv'
  a.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success('模板已下载')
}

async function handleImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的 CSV 文件')
    return
  }
  try {
    importing.value = true
    const formData = new FormData()
    formData.append('file', importFile.value)
    await exportApi.importDrugCsv(formData)
    ElMessage.success('导入成功')
    importFile.value = null
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function changePassword() {
  // 表单验证省略
  try {
    changingPassword.value = true
    await authApi.updatePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    changingPassword.value = false
  }
}

function loadElderlySettings() {
  const saved = localStorage.getItem('elderlySettings')
  if (saved) {
    try {
      Object.assign(elderlyForm, JSON.parse(saved))
    } catch {
      // 本地设置损坏时回退默认值
    }
  }
  elderlyForm.enabled = localStorage.getItem('elderlyMode') === 'true'
  applyElderlySettings()
}

// 把设置写入 body class / dataset / CSS 变量，页面样式统一读取变量
function applyElderlySettings() {
  const body = document.body
  const fontScale = Number(elderlyForm.fontScale) || 1.3
  const buttonScale = Number(elderlyForm.buttonScale) || 1.2
  const spacingScale = Number(elderlyForm.spacingScale) || 1.2

  body.classList.toggle('elderly-mode', elderlyForm.enabled)
  body.dataset.elderly = String(elderlyForm.enabled)
  body.classList.toggle('high-contrast', elderlyForm.highContrast)
  body.dataset.highContrast = String(elderlyForm.highContrast)

  body.style.setProperty('--elderly-font-scale', fontScale.toFixed(2))
  body.style.setProperty('--elderly-button-scale', buttonScale.toFixed(2))
  body.style.setProperty('--elderly-spacing-scale', spacingScale.toFixed(2))

  localStorage.setItem('elderlyMode', String(elderlyForm.enabled))
  localStorage.setItem('elderlySettings', JSON.stringify(elderlyForm))
}

function toggleElderlyMode() {
  applyElderlySettings()
  ElMessage.success(elderlyForm.enabled ? '已开启长辈模式' : '已关闭长辈模式')
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.settings-page {
  .w-full {
    width: 100%;
  }

  .row-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px;

    :deep(.el-button + .el-button) {
      margin-left: 0;
    }

    .no-action {
      color: $text-placeholder;
    }
  }

  .settings-tabs {
    :deep(.el-tabs__nav) {
      border-bottom: 1px solid $border-color;
      margin-bottom: 0;
    }

    :deep(.el-tabs__item) {
      font-size: 15px;
      font-weight: 500;
    }
  }

  .tab-card {
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0;

      h3, h4 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: $text-primary;
      }
    }
  }

  // ---------- 待处理邀请卡片列表 ----------
  .pending-invitations {
    margin-top: $spacing-lg;

    &__head {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      margin-bottom: $spacing-sm;

      .pending-invitations__title {
        font-size: $text-label;
        font-weight: $font-weight-semibold;
        color: $text-secondary;
      }
    }

    .invite-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: $spacing-sm;
    }

    .invite-item {
      display: flex;
      align-items: center;
      gap: $spacing-md;
      padding: $spacing-md;
      background: $surface-warm;
      border: 1px solid $primary-soft-border;
      border-left: 3px solid $primary-color;
      border-radius: $border-radius-base;
      transition: box-shadow $transition-base $ease-out, background $transition-base $ease-out;

      &:hover {
        background: $hover-bg;
        box-shadow: $shadow-sm;
      }

      &__avatar {
        flex: 0 0 auto;
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: $text-title;
        font-weight: $font-weight-semibold;
        color: $primary-text;
        background: $primary-light-bg;
      }

      &__main {
        flex: 1 1 auto;
        min-width: 0;
      }

      &__name {
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        font-size: $text-body;
        font-weight: $font-weight-medium;
        color: $text-primary;

        :deep(.el-tag) {
          border-radius: 999px;
        }
      }

      &__meta {
        margin-top: 2px;
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: $text-caption;
        color: $text-tertiary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      &__status {
        flex: 0 0 auto;
      }
    }
  }

  // ---------- 邀请对话框：标识预检反馈 ----------
  .invitee-check {
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: $text-caption;
    line-height: $line-height-base;

    .el-icon {
      flex: 0 0 auto;
    }

    b {
      font-weight: $font-weight-semibold;
    }

    em {
      font-style: normal;
      color: $text-tertiary;
    }

    &--ok {
      color: $success-text;
    }

    &--notfound,
    &--badformat {
      color: $danger-text;
    }

    &--checking,
    &--error,
    &--idle {
      color: transparent;
      height: 0;
      margin: 0;
      overflow: hidden;
    }
  }

  // ---------- 空间管理 ----------
  .space-card {
    .space-info {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: $spacing-md;
      padding: $spacing-md;
      background: $surface-warm-soft;
      border: 1px solid $border-color-light;
      border-radius: $border-radius-base;

      .info-item {
        display: flex;
        flex-direction: column;
        gap: $spacing-xs;

        :deep(.el-tag) {
          align-self: flex-start;
          border-radius: 999px;
          padding: 0 $spacing-md;
          height: 24px;
          line-height: 24px;
          font-weight: $font-weight-medium;
        }

        .info-label {
          font-size: $text-caption;
          color: $text-tertiary;
        }

        .info-value {
          font-size: $text-body;
          font-weight: $font-weight-medium;
          color: $text-primary;
        }

        .info-stat {
          font-size: $text-title;
          font-weight: $font-weight-semibold;
        }
      }
    }

    .space-section {
      margin-top: $spacing-lg;

      .section-title {
        margin: 0;
        font-size: $text-title;
        font-weight: $font-weight-semibold;
        color: $text-primary;
      }

      .section-desc {
        margin: $spacing-xs 0 $spacing-md;
        font-size: $text-label;
        color: $text-tertiary;
        line-height: $line-height-base;
      }
    }

    .inline-form {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: $spacing-sm;

      .form-input {
        flex: 1 1 240px;
        max-width: 320px;
      }
    }

    .danger-zone {
      margin-top: $spacing-lg;
      padding: $spacing-md;
      background: $danger-bg;
      border-left: 3px solid $danger-color;
      border-radius: $border-radius-base;

      .danger-title {
        color: $danger-text;
      }

      .danger-note {
        margin: $spacing-xs 0 $spacing-md;
        font-size: $text-label;
        color: $text-secondary;
        line-height: $line-height-base;
      }
    }
  }

  .matrix-title {
    margin: 0 0 16px;
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  .export-note,
  .import-note {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin: 16px 0 0;
    padding: 12px;
    background: $primary-light-bg;
    border-radius: $border-radius-base;
    font-size: 13px;
    color: $text-secondary;
    line-height: 1.6;

    :deep(.el-icon) {
      flex-shrink: 0;
      margin-top: 2px;
      color: $primary-text;
    }
  }

  .preview-card {
    padding: 24px;
    background: $bg-color;
    border: 1px solid $border-color;
    border-radius: $border-radius-base;

    .preview-text {
      margin: 0 0 16px;
      color: $text-primary;
    }

    .preview-btn {
      margin-right: 12px;
      margin-bottom: 12px;
    }

    .preview-input {
      width: 300px;
    }
  }

  .push-status {
    margin: 0 0 16px;
    color: $text-secondary;
  }
}

// 响应式
@media (max-width: 767px) {
  .settings-page {
    :deep(.el-tabs__nav) {
      overflow-x: auto;
      white-space: nowrap;
    }

    .tab-card {
      :deep(.el-table) {
        font-size: 13px;
      }
    }

    .space-card {
      .space-info {
        grid-template-columns: 1fr;
      }

      .inline-form .form-input {
        max-width: none;
      }
    }

    .pending-invitations {
      .invite-item {
        flex-wrap: wrap;
        gap: $spacing-sm;

        &__main {
          flex: 1 1 calc(100% - 56px);
        }

        &__status {
          order: 3;
          width: 100%;
          padding-left: 56px;
        }

        > .el-button {
          margin-left: auto;
        }
      }
    }
  }
}

// 长辈模式
</style>
