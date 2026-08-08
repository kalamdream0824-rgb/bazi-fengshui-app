export function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

export function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate()
}

export function buildYears(): number[] {
  const current = new Date().getFullYear()
  const years: number[] = []
  for (let y = current; y >= 1900; y -= 1) {
    years.push(y)
  }
  return years
}

/** 时辰：子 23-0、丑 1-2、寅 3-4、卯 5-6、辰 7-8、巳 9-10、午 11-12、未 13-14、申 15-16、酉 17-18、戌 19-20、亥 21-22 */
export function timeToShichen(hour: number): string {
  if (hour === 23 || hour === 0) return '子'
  if (hour === 1 || hour === 2) return '丑'
  if (hour === 3 || hour === 4) return '寅'
  if (hour === 5 || hour === 6) return '卯'
  if (hour === 7 || hour === 8) return '辰'
  if (hour === 9 || hour === 10) return '巳'
  if (hour === 11 || hour === 12) return '午'
  if (hour === 13 || hour === 14) return '未'
  if (hour === 15 || hour === 16) return '申'
  if (hour === 17 || hour === 18) return '酉'
  if (hour === 19 || hour === 20) return '戌'
  return '亥'
}

export interface DateTimeValue {
  y: number
  m: number
  d: number
  h: number
  min: number
}

export function parseInput(value: string): DateTimeValue {
  const now = new Date()
  const fallback: DateTimeValue = { y: now.getFullYear(), m: now.getMonth() + 1, d: now.getDate(), h: now.getHours(), min: now.getMinutes() }
  if (!value) return fallback
  const [datePart, timePart = '00:00'] = value.split('T')
  const [y, m, d] = datePart.split('-').map(Number)
  const [h, min] = timePart.split(':').map(Number)
  if (!y || !m || !d || h === undefined || min === undefined) return fallback
  return { y, m, d, h, min }
}

export function formatDateTime(v: DateTimeValue): string {
  return `${v.y}-${pad2(v.m)}-${pad2(v.d)}T${pad2(v.h)}:${pad2(v.min)}`
}
