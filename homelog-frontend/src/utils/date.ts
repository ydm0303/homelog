import dayjs, { type Dayjs } from 'dayjs'

export type DateInput = string | number | Date | Dayjs | null | undefined

/** 安全解析日期；空值或非法值返回 null，避免界面出现 Invalid Date */
export function parseDate(value: DateInput): Dayjs | null {
  if (value === null || value === undefined || value === '') return null
  const date = dayjs(value)
  return date.isValid() ? date : null
}

export function formatDate(value: DateInput, format = 'YYYY-MM-DD', fallback = '—'): string {
  return parseDate(value)?.format(format) ?? fallback
}

export function formatDateTime(value: DateInput, format = 'YYYY-MM-DD HH:mm', fallback = '—'): string {
  return parseDate(value)?.format(format) ?? fallback
}

export function dayDiffFromToday(value: DateInput): number | null {
  const date = parseDate(value)
  if (!date) return null
  return date.startOf('day').diff(dayjs().startOf('day'), 'day')
}
