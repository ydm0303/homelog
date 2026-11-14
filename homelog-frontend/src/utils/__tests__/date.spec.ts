import { describe, expect, it } from 'vitest'
import dayjs from 'dayjs'
import { dayDiffFromToday, formatDate, formatDateTime, parseDate } from '../date'

describe('date utils', () => {
  it('formats valid dates', () => {
    expect(formatDate('2026-11-14')).toBe('2026-11-14')
    expect(formatDateTime('2026-11-14T20:30:00')).toBe('2026-11-14 20:30')
  })

  it('returns a fallback for empty or invalid dates', () => {
    expect(formatDate('')).toBe('—')
    expect(formatDate(null)).toBe('—')
    expect(formatDate('not-a-date')).toBe('—')
    expect(formatDate('not-a-date', 'YYYY-MM-DD', '未知')).toBe('未知')
    expect(parseDate('invalid')).toBeNull()
  })

  it('calculates day differences from today', () => {
    expect(dayDiffFromToday(dayjs().format('YYYY-MM-DD'))).toBe(0)
    expect(dayDiffFromToday(dayjs().add(2, 'day').format('YYYY-MM-DD'))).toBe(2)
    expect(dayDiffFromToday(dayjs().subtract(1, 'day').format('YYYY-MM-DD'))).toBe(-1)
  })
})
