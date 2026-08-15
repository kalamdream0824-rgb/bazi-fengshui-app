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
  /** 藏干对应日主的十神（副星） */
  shiShen: string
  wuxing: WuxingKey
}

export interface Pillar {
  label: PillarKey
  gan: string
  zhi: string
  shiShen: string
  /** 自坐：本柱天干在本柱地支的十二长生（与星运的日干太极点口径不同） */
  ziZuo: string
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
  naYin: string
  xunKong: string
  /** 大运天干对日主的十神 */
  shiShen: string
  /** 大运星运：大运地支对日主的十二长生（与四柱星运同口径） */
  starFortune: string
  shenSha: string[]
}

export interface YunStart {
  year: number
  month: number
  day: number
  hour: number
  forward: boolean
}

export interface LiuNianItem {
  year: number
  age: number
  ganZhi: string
  naYin: string
  shiShen: string
  starFortune: string
  xunKong: string
  shenSha: string[]
}

export interface TrueSolarInfo {
  original: string
  adjusted: string
  offsetMinutes: number
  eotMinutes: number
  longitude: number
  originalShichen: string
  adjustedShichen: string
  boundaryChanged: boolean
}

export interface LiuNianInfo {
  ganZhi: string
  shenSha: string[]
}

export interface AuthResponse {
  token: string
  username: string
}

export interface MembershipInfo {
  username: string
  plan: string | null
  memberExpireAt: string | null
  isMember: boolean
}

export interface OrderInfo {
  id: number
  plan: string
  amountCents: number
  status: string
  provider: string | null
  providerTradeNo: string | null
  createdAt: string
  paidAt: string | null
}

export interface PaipanResult {
  solarText: string
  lunarText: string
  shengXiao: string
  timeZhi: string
  pillars: Record<PillarKey, Pillar>
  taiYuan: string
  taiYuanNaYin: string
  mingGong: string
  mingGongNaYin: string
  shenGong: string
  shenGongNaYin: string
  wuXing: Record<WuxingKey, number>
  daYun: DaYun[]
  yunStart: YunStart
  liuNianList: LiuNianItem[]
  currentYearGanZhi: string
  currentLiuNian?: LiuNianInfo
  trueSolar?: TrueSolarInfo
}
