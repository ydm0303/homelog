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
            <el-table-column prop="email" label="邮箱" min-width="200" />
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
                  :disabled="scope.row.userId === authStore.user?.id"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <el-dropdown size="small">
                  <el-button link>操作 <el-icon><ArrowDown /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="opt in assignableOptions(scope.row.role)"
                        :key="opt.value"
                        @click="updateMemberRole(scope.row, { role: opt.value })"
                      >
                        设为{{ opt.label }}
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="canRemoveMember(scope.row)"
                        @click="removeMember(scope.row)"
                        class="danger-item"
                      >
                        <el-icon><Delete /></el-icon>
                        移出家庭
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>

          <!-- 待处理邀请（邀请人可撤销，设计文档 v1.2 §8） -->
          <div v-if="pendingInvitations.length > 0" class="pending-invitations">
            <el-divider content-position="left">待处理的邀请</el-divider>
            <el-table :data="pendingInvitations" border stripe class="w-full" size="small">
              <el-table-column prop="inviteeNickname" label="被邀请人" min-width="120" />
              <el-table-column label="角色" width="110">
                <template #default="scope">
                  <el-tag :type="getRoleTagType(scope.row.role)" size="small">{{ getRoleLabel(scope.row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="邀请时间" width="180">
                <template #default="scope">{{ formatInviteTime(scope.row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center">
                <template #default="scope">
                  <el-button link size="small" type="danger" @click="cancelInvitation(scope.row)">撤销</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 邀请成员对话框 -->
        <el-dialog v-model="showAddMember" title="邀请成员加入" width="500px">
          <el-form :model="addMemberForm" :rules="addMemberRules" label-width="100px">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="addMemberForm.email" placeholder="对方账号的注册邮箱" clearable />
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
            <p class="invite-tip">对方需已注册家记账号。邀请发送后，对方在消息中心同意即可加入。</p>
          </el-form>
          <template #footer>
            <el-button @click="showAddMember = false">取消</el-button>
            <el-button type="primary" @click="confirmAddMember" :loading="addingMember">发送邀请</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 空间管理 -->
      <el-tab-pane label="空间管理" name="space" :disabled="!isAdmin">
        <el-card class="tab-card" shadow="never">
          <h4>当前家庭空间</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="空间名称">{{ currentSpaceName }}</el-descriptions-item>
            <el-descriptions-item label="我的角色">{{ getRoleLabel(authStore.role) }}</el-descriptions-item>
            <el-descriptions-item label="在籍成员数">{{ currentSpace?.memberCount ?? '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">修改名称</el-divider>
          <el-form inline>
            <el-form-item label="新名称">
              <el-input v-model="spaceNameInput" maxlength="64" clearable style="width: 240px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="renamingSpace" @click="renameSpace">保存</el-button>
            </el-form-item>
          </el-form>

          <template v-if="authStore.isOwner">
            <el-divider content-position="left">解散家庭空间</el-divider>
            <p class="danger-note">
              解散后该空间将从所有成员的列表中消失。仅所有者可执行，操作不可自行恢复。
            </p>
            <el-form inline>
              <el-form-item label="输入空间名确认">
                <el-input v-model="dissolveNameInput" :placeholder="currentSpaceName" style="width: 240px" />
              </el-form-item>
              <el-form-item>
                <el-button type="danger" :loading="dissolvingSpace" @click="dissolveSpace">解散空间</el-button>
              </el-form-item>
            </el-form>
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
            <el-table-column prop="sortOrder" label="排序号" width="80" align="center" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-dropdown size="small">
                  <el-button link>操作 <el-icon><ArrowDown /></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="goToModuleFields(scope.row)">
                        <el-icon><Setting /></el-icon>
                        字段配置
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="scope.row.moduleType === 2"
                        @click="deleteCustomModule(scope.row)"
                        class="danger-item"
                      >
                        <el-icon><Delete /></el-icon>
                        删除模块
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>

          <!-- 新建自定义表单模块 -->
          <div class="create-custom-module" v-if="isAdmin">
            <el-divider content-position="left">新建自定义表单模块</el-divider>
            <el-form :model="customModuleForm" :rules="customModuleRules" label-width="100px" inline>
              <el-form-item label="模块名称" prop="name">
                <el-input v-model="customModuleForm.name" placeholder="如：宠物档案" clearable style="width: 200px" />
              </el-form-item>
              <el-form-item label="模块 Key" prop="moduleKey">
                <el-input v-model="customModuleForm.moduleKey" placeholder="英文，如 pet" clearable style="width: 200px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="createCustomModule" :loading="creatingModule">
                  <el-icon><Plus /></el-icon>
                  创建模块
                </el-button>
              </el-form-item>
            </el-form>
          </div>
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
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useModuleStore } from '@/stores/module'
import { authApi, metaApi, spaceApi, invitationApi, exportApi } from '@/api'
import {
  Plus, ArrowDown, Check, Close, Delete, Setting, Upload, Download, InfoFilled
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

const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

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
  email: '',
  role: ROLE.MEMBER as number
})
const addMemberRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
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
  { permission: '创建 / 删除自定义表单模块', flags: { owner: true, admin: true, member: false, readonly: false } },
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

const customModuleForm = reactive({
  name: '',
  moduleKey: ''
})
const customModuleRules = {
  name: [{ required: true, message: '请输入模块名称', trigger: 'blur' }],
  moduleKey: [{ required: true, message: '请输入模块 Key', trigger: 'blur' }, { pattern: /^[a-z][a-z0-9_]*$/, message: '仅支持小写字母、数字、下划线，且必须以字母开头', trigger: 'blur' }]
}
const creatingModule = ref(false)

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
})

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
  try {
    addingMember.value = true
    await invitationApi.create({ email: addMemberForm.email, role: addMemberForm.role })
    ElMessage.success('邀请已发送')
    showAddMember.value = false
    addMemberForm.email = ''
    addMemberForm.role = ROLE.MEMBER
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

async function deleteCustomModule(module: any) {
  try {
    await ElMessageBox.confirm(`确定要删除自定义模块「${module.name}」吗？数据将保留但模块入口会消失`, '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger'
    })
    await metaApi.deleteModule(module.id)
    ElMessage.success('删除成功')
    fetchAllModules()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function createCustomModule() {
  try {
    creatingModule.value = true
    await metaApi.createModule(customModuleForm)
    ElMessage.success('创建成功')
    customModuleForm.name = ''
    customModuleForm.moduleKey = ''
    fetchAllModules()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    creatingModule.value = false
  }
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

  .matrix-title {
    margin: 0 0 16px;
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  .create-custom-module {
    margin-top: 24px;
    padding-top: 24px;
    border-top: 1px solid $border-color;

    :deep(.el-form-item) {
      margin-bottom: 0;
      margin-right: 16px;
    }
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

    el-icon {
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
  }
}

// 长辈模式
</style>
