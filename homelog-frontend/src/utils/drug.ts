import dayjs from 'dayjs'
import { parseDate, type DateInput } from './date'

export type ExpiryStatus = 'expired' | 'soon' | 'normal'

export interface ExpiryState {
  status: ExpiryStatus
  /** 距过期的天数：正数=未来 N 天，0=今天，负数=已过期 N 天 */
  days: number
}

/**
 * 判定药品有效期状态（列表/详情/首页共用，避免各写各的过期逻辑）
 * @param expireDate 有效期（data_json.expire_date）
 * @param leadDays 提前预警天数，默认 30 天
 */
export function getExpiryState(expireDate: DateInput, leadDays = 30): ExpiryState | null {
  const date = parseDate(expireDate)
  if (!date) return null
  const days = date.startOf('day').diff(dayjs().startOf('day'), 'day')
  const status: ExpiryStatus = days < 0 ? 'expired' : days <= leadDays ? 'soon' : 'normal'
  return { status, days }
}
