// 模块元数据状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'
import type { ModuleMetaVO, FieldVO } from '@/types'

export const useModuleStore = defineStore('module', () => {
  const modules = ref<ModuleMetaVO[]>([])
  const allModules = ref<ModuleMetaVO[]>([])
  const loading = ref(false)

  // 当前启用的模块（用于菜单渲染）
  const enabledModules = computed(() =>
    modules.value.filter(m => m.isEnable === 1).sort((a, b) => a.sortOrder - b.sortOrder)
  )

  // 首页显示的模块
  const homeModules = computed(() =>
    enabledModules.value.filter(m => m.showInHome === 1)
  )

  // 获取启用的模块列表
  async function fetchModules() {
    loading.value = true
    try {
      const response = await api.get<ModuleMetaVO[]>('/meta/modules/list')
      modules.value = response.data
    } finally {
      loading.value = false
    }
  }

  // 获取所有模块（管理员用）
  async function fetchAllModules() {
    loading.value = true
    try {
      const response = await api.get<ModuleMetaVO[]>('/meta/modules/all')
      allModules.value = response.data
    } finally {
      loading.value = false
    }
  }

  // 获取单个模块详情
  async function fetchModule(moduleId: number) {
    const response = await api.get<ModuleMetaVO>('/meta/modules/detail', { params: { moduleId } })
    return response.data
  }

  // 更新模块（仅管理员）
  async function updateModule(moduleId: number, data: Partial<ModuleMetaVO>) {
    const response = await api.post<ModuleMetaVO>('/meta/modules/update', { moduleId, ...data })
    // 更新本地缓存
    const index = modules.value.findIndex(m => m.id === moduleId)
    if (index !== -1) modules.value[index] = response.data
    const allIndex = allModules.value.findIndex(m => m.id === moduleId)
    if (allIndex !== -1) allModules.value[allIndex] = response.data
    return response.data
  }

  // 创建自定义模块（仅管理员）
  async function createModule(data: { name: string; moduleKey: string; sortOrder?: number }) {
    const response = await api.post<ModuleMetaVO>('/meta/modules/create', data)
    return response.data
  }

  // 删除自定义模块（仅管理员）
  async function deleteModule(moduleId: number) {
    await api.post('/meta/modules/delete', { moduleId })
    // 更新本地缓存
    const index = modules.value.findIndex(m => m.id === moduleId)
    if (index !== -1) modules.value.splice(index, 1)
    const allIndex = allModules.value.findIndex(m => m.id === moduleId)
    if (allIndex !== -1) allModules.value.splice(allIndex, 1)
  }

  // 根据 key 查找模块
  function getModuleByKey(moduleKey: string) {
    return modules.value.find(m => m.moduleKey === moduleKey)
  }

  // 获取字段定义
  function getFields(moduleKey: string): FieldVO[] {
    const module = getModuleByKey(moduleKey)
    return module?.fields || []
  }

  // 获取必填字段
  function getRequiredFields(moduleKey: string): FieldVO[] {
    return getFields(moduleKey).filter(f => f.isRequired === 1)
  }

  // 获取私密字段
  function getPrivateFields(moduleKey: string): FieldVO[] {
    return getFields(moduleKey).filter(f => f.isPrivate === 1)
  }

  return {
    modules,
    allModules,
    loading,
    enabledModules,
    homeModules,
    fetchModules,
    fetchAllModules,
    fetchModule,
    updateModule,
    createModule,
    deleteModule,
    getModuleByKey,
    getFields,
    getRequiredFields,
    getPrivateFields
  }
})