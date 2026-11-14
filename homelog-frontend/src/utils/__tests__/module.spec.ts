import { describe, expect, it } from 'vitest'
import { getModuleColorClass, getModuleIcon, getModuleTagType } from '../module'

describe('module registry', () => {
  it('maps built-in modules consistently', () => {
    expect(getModuleTagType('drug')).toBe('primary')
    expect(getModuleTagType('todo')).toBe('success')
    expect(getModuleTagType('note')).toBe('info')
    expect(getModuleTagType('idoc')).toBe('warning')
    expect(getModuleColorClass('drug')).toBe('drug')
    expect(getModuleIcon('todo')).toBeTruthy()
  })

  it('falls back for custom modules', () => {
    expect(getModuleTagType('custom')).toBe('info')
    expect(getModuleColorClass('custom')).toBe('note')
    expect(getModuleIcon('custom')).toBeTruthy()
  })
})
