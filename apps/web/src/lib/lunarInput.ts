import { Lunar, LunarYear, Solar } from 'lunar-javascript'
import { pad2 } from './datePicker'

export interface LunarDateValue {
  year: number
  /** 1-12 */
  month: number
  /** 是否闰月 */
  leap: boolean
  day: number
  hour: number
  minute: number
}

/** 某农历年的闰月号（0 = 无闰月） */
export function lunarYearLeapMonth(year: number): number {
  return LunarYear.fromYear(year).getLeapMonth()
}

/** 某农历月（可闰）的天数 */
export function lunarMonthDayCount(year: number, month: number, leap: boolean): number {
  return LunarYear.fromYear(year).getMonth(month, leap).getDayCount()
}

/** 农历 → 公历 ISO datetime（闰月以负数月传入 lunar 库） */
export function lunarToSolarDateTime(value: LunarDateValue): string {
  const lunar = Lunar.fromYmd(value.year, value.leap ? -value.month : value.month, value.day)
  const solar = lunar.getSolar()
  return `${solar.getYear()}-${pad2(solar.getMonth())}-${pad2(solar.getDay())}T${pad2(value.hour)}:${pad2(value.minute)}`
}

/** 公历 ISO → 农历值（月/时/分从 ISO 字符串直接解析，避免时区偏差） */
export function solarToLunar(solarIso: string): LunarDateValue {
  const [datePart, timePart = '00:00'] = solarIso.split('T')
  const [y, m, d] = datePart.split('-').map(Number)
  const [h, min] = timePart.split(':').map(Number)
  const lunar = Solar.fromYmd(y, m, d).getLunar()
  return {
    year: lunar.getYear(),
    month: Math.abs(lunar.getMonth()),
    leap: lunar.getMonth() < 0,
    day: lunar.getDay(),
    hour: Number.isFinite(h) ? h : 0,
    minute: Number.isFinite(min) ? min : 0,
  }
}

/** 农历文本：1995年闰8月14日 */
export function lunarText(value: LunarDateValue): string {
  return `${value.year}年${value.leap ? '闰' : ''}${value.month}月${value.day}日`
}

/** 公历 ISO → 展示文本：1995-10-08 14:30 */
export function solarText(solarIso: string): string {
  return solarIso.replace('T', ' ')
}
