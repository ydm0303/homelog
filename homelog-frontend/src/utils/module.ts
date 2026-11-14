import type { Component } from 'vue'
import { Checked, CreditCard, FirstAidKit, Grid, Picture } from '@element-plus/icons-vue'

export type ModuleTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

interface ModuleVisual {
  icon: Component
  tagType: ModuleTagType
  colorClass: string
}

const MODULE_VISUALS: Record<string, ModuleVisual> = {
  drug: { icon: FirstAidKit, tagType: 'primary', colorClass: 'drug' },
  todo: { icon: Checked, tagType: 'success', colorClass: 'todo' },
  note: { icon: Picture, tagType: 'info', colorClass: 'note' },
  idoc: { icon: CreditCard, tagType: 'warning', colorClass: 'idoc' }
}

// 自定义模块统一用默认视觉，避免各页面各自 fallback 不一致
const DEFAULT_VISUAL: ModuleVisual = {
  icon: Grid,
  tagType: 'info',
  colorClass: 'note'
}

function getVisual(moduleKey: string): ModuleVisual {
  return MODULE_VISUALS[moduleKey] || DEFAULT_VISUAL
}

export function getModuleIcon(moduleKey: string): Component {
  return getVisual(moduleKey).icon
}

export function getModuleTagType(moduleKey: string): ModuleTagType {
  return getVisual(moduleKey).tagType
}

export function getModuleColorClass(moduleKey: string): string {
  return getVisual(moduleKey).colorClass
}
