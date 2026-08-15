import { describe, expect, it } from 'vitest'
import {
  lunarMonthDayCount,
  lunarText,
  lunarToSolarDateTime,
  lunarYearLeapMonth,
  solarText,
  solarToLunar,
} from './lunarInput'

describe('lunarInput 农历转换', () => {
  it('普通农历月转公历：1996年2月20日 → 1996-04-07', () => {
    expect(
      lunarToSolarDateTime({ year: 1996, month: 2, leap: false, day: 20, hour: 10, minute: 0 }),
    ).toBe('1996-04-07T10:00')
  })

  it('闰月转换：1995年闰8月14日 → 1995-10-08（与 fixtures 公历一致）', () => {
    expect(
      lunarToSolarDateTime({ year: 1995, month: 8, leap: true, day: 14, hour: 14, minute: 30 }),
    ).toBe('1995-10-08T14:30')
  })

  it('公历反查农历：1995-10-08 14:30 → 1995 闰8月14日（含时分）', () => {
    const v = solarToLunar('1995-10-08T14:30')
    expect(v).toMatchObject({ year: 1995, month: 8, leap: true, day: 14, hour: 14, minute: 30 })
    expect(lunarText(v)).toBe('1995年闰8月14日')
  })

  it('闰月查询与月天数：1995 闰八月 30 天，1996 无闰月', () => {
    expect(lunarYearLeapMonth(1995)).toBe(8)
    expect(lunarYearLeapMonth(1996)).toBe(0)
    expect(lunarMonthDayCount(1995, 8, true)).toBe(30)
    expect(lunarMonthDayCount(1996, 2, false)).toBe(30)
  })

  it('solarText 展示格式', () => {
    expect(solarText('1995-10-08T14:30')).toBe('1995-10-08 14:30')
  })
})
