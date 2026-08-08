export type Gender = 'male' | 'female'

export type WuxingKey = 'jin' | 'mu' | 'shui' | 'huo' | 'tu'

export type PillarKey = 'year' | 'month' | 'day' | 'time'

export interface PaipanRequest {
  name?: string
  gender: Gender
  /** ISO 格式公历时间，如 1995-10-08T14:30:00 */
  solarDateTime: string
  birthPlace?: string
  trueSolarTime: boolean
}

export interface HideGan {
  gan: string
  wuxing: WuxingKey
}

export interface Pillar {
  label: PillarKey
  gan: string
  zhi: string
  shiShen: string
  hideGan: HideGan[]
  naYin: string
  diShi: string
  xunKong: string
  shenSha: string[]
}

export interface DaYun {
  ageRange: string
  ganZhi: string
  yearRange: string
  isCurrent: boolean
}

export interface TrueSolarInfo {
  original: string
  adjusted: string
  offsetMinutes: number
  eotMinutes: number
  longitude: number
}

export interface PaipanResult {
  solarText: string
  lunarText: string
  shengXiao: string
  timeZhi: string
  pillars: Record<PillarKey, Pillar>
  wuXing: Record<WuxingKey, number>
  daYun: DaYun[]
  currentYearGanZhi: string
  trueSolar?: TrueSolarInfo
}
