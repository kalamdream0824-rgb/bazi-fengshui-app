import { describe, expect, it } from 'vitest'
import { buildYears, daysInMonth, formatDateTime, parseInput, timeToShichen } from './datePicker'

describe('datePicker', () => {
  it('闰年 2 月 29 天，平年 28 天', () => {
    expect(daysInMonth(2024, 2)).toBe(29)
    expect(daysInMonth(2023, 2)).toBe(28)
    expect(daysInMonth(2024, 4)).toBe(30)
  })

  it('时辰映射：23/0 子、13 未、11 午', () => {
    expect(timeToShichen(23)).toBe('子')
    expect(timeToShichen(0)).toBe('子')
    expect(timeToShichen(13)).toBe('未')
    expect(timeToShichen(11)).toBe('午')
  })

  it('解析与格式化往返一致', () => {
    const parsed = parseInput('1995-10-08T14:30')
    expect(parsed).toEqual({ y: 1995, m: 10, d: 8, h: 14, min: 30 })
    expect(formatDateTime(parsed)).toBe('1995-10-08T14:30')
  })

  it('年份范围包含当前年份与 1900', () => {
    const years = buildYears()
    expect(years[0]).toBe(new Date().getFullYear())
    expect(years[years.length - 1]).toBe(1900)
  })
})
