<template>
  <div class="settings-page">
    <el-tabs v-model="activeTab" class="settings-tabs" @tab-click="handleTabClick">
      <!-- 成员管理 -->
      <el-tab-pane label="成员管理" name="members" :disabled="!isAdmin">
        <el-card class="tab-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>家庭成员</h3>
              <el-button type="primary" size="small" @click="showAddMember = true">
                <el-icon><Plus /></el-icon>
                添加成员
              </el-button>
            </div>
          </template>

          <el-table :data="members" border stripe style="width: 100%">
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
                  @change="val => updateMemberRole(scope.row, { receiveMail: val ? 1 : 0 })"
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
                        v-if="scope.row.role !== 1"
                        @click="updateMemberRole(scope.row, { role: 1 })"
                      >
                        设为管理员
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="scope.row.role !== 2"
                        @click="updateMemberRole(scope.row, { role: 2 })"
                      >
                        设为可编辑
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="scope.row.role !== 3"
                        @click="updateMemberRole(scope.row, { role: 3 })"
                      >
                        设为只读
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="scope.row.userId !== authStore.user?.id"
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
        </el-card>

        <!-- 添加成员对话框 -->
        <el-dialog v-model="showAddMember" title="添加家庭成员" width="500px">
          <el-form :model="addMemberForm" :rules="addMemberRules" label-width="80px">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="addMemberForm.email" placeholder="输入成员邮箱" clearable />
            </el-form-item>
            <el-form-item label="角色" prop="role">
              <el-radio-group v-model="addMemberForm.role">
                <el-radio :label="2">可编辑</el-radio>
                <el-radio :label="3">只读</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="邮件提醒">
              <el-switch v-model="addMemberForm.receiveMail" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showAddMember = false">取消</el-button>
            <el-button type="primary" @click="confirmAddMember" :loading="addingMember">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 权限矩阵 -->
      <el-tab-pane label="权限矩阵" name="permissions" :disabled="!isAdmin">
        <el-card class="tab-card" shadow="never">
          <h4 class="matrix-title">角色权限对照表</h4>
          <el-table :data="permissionMatrix" border stripe style="width: 100%">
            <el-table-column prop="permission" label="权限项" min-width="200" />
            <el-table-column prop="admin" label="管理员" width="120" align="center">
              <template #default="scope">
                <el-icon v-if="scope.row.admin"><Check color="#67c23a" /></el-icon>
                <el-icon v-else><Close color="#f56c6c" /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="editor" label="可编辑" width="120" align="center">
              <template #default="scope">
                <el-icon v-if="scope.row.editor"><Check color="#67c23a" /></el-icon>
                <el-icon v-else><Close color="#f56c6c" /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="readonly" label="只读" width="120" align="center">
              <template #default="scope">
                <el-icon v-if="scope.row.readonly"><Check color="#67c23a" /></el-icon>
                <el-icon v-else><Close color="#f56c6c" /></el-icon>
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
            style="width: 100%"
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
                  @change="val => updateModule({ ...scope.row, isEnable: val ? 1 : 0 })"
                />
              </template>
            </el-table-column>
            <el-table-column label="首页显示" width="100" align="center">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.showInHome"
                  :active-value="1"
                  :inactive-value="0"
                  @change="val => updateModule({ ...scope.row, showInHome: val ? 1 : 0 })"
                />
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序值" width="80" align="center" />
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

          <!-- 新建自定义表单 -->
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
                    <el-radio :label="module">单模块</el-radio>
                    <el-radio :label="all">全空间</el-radio>
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
                    开始导入
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
          <el-form :model="elderlyForm" label-width="120px">
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
              <el-slider v-model="elderlyForm.fontScale" :min="1" :max="2" :step="0.1" show-input />
            </el-form-item>
            <el-form-item label="按钮放大倍数">
              <el-slider v-model="elderlyForm.buttonScale" :min="1" :max="1.5" :step="0.1" show-input />
            </el-form-item>
            <el-form-item label="间距放大倍数">
              <el-slider v-model="elderlyForm.spacingScale" :min="1" :max="1.5" :step="0.1" show-input />
            </el-form-item>
            <el-form-item label="语音播报">
              <el-switch v-model="elderlyForm.voiceEnabled" active-text="开启" inactive-text="关闭" />
            </el-form-item>
            <el-form-item label="高对比度">
              <el-switch v-model="elderlyForm.highContrast" active-text="开启" inactive-text="关闭" />
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

      <!-- 关于版本 -->
      <el-tab-pane label="关于版本" name="about">
        <el-card class="tab-card" shadow="never">
          <div class="about-content">
            <div class="about-logo">
              <el-icon><House /></el-icon>
              <span>家记</span>
            </div>
            <p class="about-slogan">家里的事，都记在这里</p>
            <p class="about-version">版本：1.0.0 (MVP)</p>
            <p class="about-desc">
              私有模块化家庭记录 PWA 平台。电脑、手机浏览器均可访问，支持添加桌面快捷方式。
              管理员自由配置插拔式模块，内置药箱、待办、记事、证件档案，还能自建表单扩展任意记录类型；
              三级权限管控，数据私有，支持导出备份，邮件提醒兜底。
            </p>

            <el-divider />

            <h4>技术栈</h4>
            <div class="tech-stack">
              <el-tag effect="plain">Vue 3</el-tag>
              <el-tag effect="plain">TypeScript</el-tag>
              <el-tag effect="plain">Vite</el-tag>
              <el-tag effect="plain">Pinia</el-tag>
              <el-tag effect="plain">Element Plus</el-tag>
              <el-tag effect="plain">PWA</el-tag>
              <el-tag effect="plain">Spring Boot 2.7</el-tag>
              <el-tag effect="plain">Java 8</el-tag>
              <el-tag effect="plain">MySQL 8.0</el-tag>
              <el-tag effect="plain">MyBatis-Plus</el-tag>
              <el-tag effect="plain">JWT</el-tag>
            </div>

            <el-divider />

            <h4>免责声明</h4>
            <p class="disclaimer">
              本项目为个人自建工具，仅用于家庭内部记录，不提供医疗诊断建议，药品信息仅作存档参考。
              请遵医嘱用药，勿依赖本工具做医疗决策。
            </p>

            <el-divider />

            <p class="copyright">
              家记 · 家里的事，都记在这里 | 私有家庭记录工具 · 数据自主可控
            </p>
          </div>
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
import { authApi, metaApi, spaceApi, exportApi } from '@/api'
import {
  Plus, ArrowDown, Check, Close, Delete, Setting, Upload, Download, InfoFilled,
  House, Check as CheckIcon
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const moduleStore = useModuleStore()

const activeTab = ref('members')

const isAdmin = computed(() => authStore.isAdmin)

const members = ref<any[]>([])
const allModules = ref<any[]>([])
const enabledModules = computed(() => moduleStore.enabledModules)

const showAddMember = ref(false)
const addingMember = ref(false)
const addMemberForm = reactive({
  email: '',
  role: 2,
  receiveMail: 1
})
const addMemberRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const permissionMatrix = [
  { permission: '查看所有模块数据', admin: true, editor: true, readonly: true },
  { permission: '新增/编辑/删除记录', admin: true, editor: true, readonly: false },
  { permission: '仅编辑/删除自己的记录', admin: false, editor: true, readonly: false },
  { permission: '管理成员（增/删/改角色）', admin: true, editor: false, readonly: false },
  { permission: '模块开关/排序/改名', admin: true, editor: false, readonly: false },
  { permission: '创建自定义表单模块', admin: true, editor: false, readonly: false },
  { permission: '删除自定义表单模块', admin: true, editor: false, readonly: false },
  { permission: '查看私密字段/记录', admin: true, editor: false, readonly: false },
  { permission: '数据导出（单模块/全空间）', admin: true, editor: false, readonly: false },
  { permission: '药品 CSV 导入', admin: true, editor: false, readonly: false },
  { permission: '接收邮件提醒', admin: true, editor: true, readonly: true },
  { permission: '修改个人密码', admin: true, editor: true, readonly: true }
]

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
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: (rule: any, value: string, callback: any) => { if (value !== passwordForm.newPassword) callback(new Error('两次输入密码不一致')); else callback() }, trigger: 'blur' }]
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
    await fetchAllModules()
  }
  loadElderlySettings()
})

async function fetchMembers() {
  try {
    const response = await spaceApi.getMembers(1)
    members.value = response.data
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

function getRoleLabel(role: number) {
  switch (role) {
    case 1: return '管理员'
    case 2: return '可编辑'
    default: return '只读'
  }
}

function getRoleTagType(role: number) {
  switch (role) {
    case 1: return 'danger'
    case 2: return 'success'
    default: return 'info'
  }
}

async function updateMemberRole(member: any, changes: Partial<any>) {
  try {
    await spaceApi.updateMember({ spaceId: 1, userId: member.userId, ...changes })
    ElMessage.success('更新成功')
    fetchMembers()
  } catch {
    ElMessage.error('更新失败')
  }
}

async function removeMember(member: any) {
  try {
    await ElMessageBox.confirm(`确定要将 ${member.nickName || member.username} 移出家庭吗？`, '确认移出', {
      confirmButtonText: '移出', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger'
    })
    await spaceApi.deleteMember(1, member.userId)
    ElMessage.success('已移出')
    fetchMembers()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function confirmAddMember() {
  try {
    addingMember.value = true
    await spaceApi.createMember({
      spaceId: 1,
      email: addMemberForm.email,
      role: addMemberForm.role,
      receiveMail: addMemberForm.receiveMail
    })
    ElMessage.success('邀请已发送')
    showAddMember.value = false
    addMemberForm.email = ''
    addMemberForm.role = 2
    addMemberForm.receiveMail = 1
    fetchMembers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '邀请失败')
  } finally {
    addingMember.value = false
  }
}

async function updateModule(module: any) {
  try {
    await metaApi.updateModule(module)
    ElMessage.success('更新成功')
  } catch {
    ElMessage.error('更新失败')
  }
}

function goToModuleFields(module: any) {
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
  const csv = [headers.join(',')].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'drug_import_template.csv'
  a.click()
  window.URL.revokeObjectURL(url)
}

async function handleImport() {
  if (!importFile.value) return
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
    Object.assign(elderlyForm, JSON.parse(saved))
  }
  elderlyForm.enabled = localStorage.getItem('elderlyMode') === 'true'
}

function toggleElderlyMode() {
  localStorage.setItem('elderlyMode', elderlyForm.enabled.toString())
  localStorage.setItem('elderlySettings', JSON.stringify(elderlyForm))
  if (elderlyForm.enabled) {
    document.body.classList.add('elderly-mode')
  } else {
    document.body.classList.remove('elderly-mode')
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.settings-page {
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
      color: $primary-color;
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

  .about-content {
    text-align: center;
    padding: 32px 24px;

    .about-logo {
      display: inline-flex;
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

    .about-slogan {
      font-size: 18px;
      color: $text-secondary;
      margin: 0 0 24px;
    }

    .about-version {
      font-size: 14px;
      color: $text-placeholder;
      margin: 0 0 16px;
    }

    .about-desc {
      font-size: 14px;
      color: $text-secondary;
      line-height: 1.8;
      max-width: 600px;
      margin: 0 auto 24px;
      text-align: left;
    }

    .tech-stack {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 8px;
      margin: 16px 0;
    }

    .disclaimer {
      font-size: 13px;
      color: $text-secondary;
      line-height: 1.8;
      max-width: 600px;
      margin: 16px auto 0;
      text-align: left;
      padding: 12px;
      background: #fffbe8;
      border: 1px solid #fde2a2;
      border-radius: $border-radius-base;
    }

    .copyright {
      margin-top: 24px;
      font-size: 12px;
      color: $text-placeholder;
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

    .about-desc {
      text-align: center;
    }
  }
}

// 长辈模式
.elderly-mode .settings-page {
  :deep(.el-tabs__item) { font-size: 17px !important; }
  :deep(.el-table) { font-size: 15px !important; }
  :deep(.el-form-item__label) { font-size: 15px !important; }
  :deep(.el-input__inner) { font-size: 15px !important; padding: 14px 16px !important; height: 52px !important; }
  .matrix-title { font-size: 16px !important; }
  .about-slogan { font-size: 20px !important; }
  .about-desc { font-size: 16px !important; }
  .disclaimer { font-size: 15px !important; }
}
</style>