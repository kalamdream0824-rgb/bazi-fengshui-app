import type { PaipanResult, WuxingKey } from '@/types/bazi'
import { GAN_WUXING } from './wuxing'

/** 生肖六合（以生肖名索引）：鼠牛、虎猪、兔狗、龙鸡、蛇猴、马羊 */
const SHENGXIAO_LIUHE: Record<string, string> = {
  鼠: '牛',
  牛: '鼠',
  虎: '猪',
  猪: '虎',
  兔: '狗',
  狗: '兔',
  龙: '鸡',
  鸡: '龙',
  蛇: '猴',
  猴: '蛇',
  马: '羊',
  羊: '马',
}

/** 地支六合 */
const ZHI_LIUHE: Record<string, string> = {
  子: '丑',
  丑: '子',
  寅: '亥',
  亥: '寅',
  卯: '戌',
  戌: '卯',
  辰: '酉',
  酉: '辰',
  巳: '申',
  申: '巳',
  午: '未',
  未: '午',
}

/** 地支六冲 */
const ZHI_LIUCHONG: Record<string, string> = {
  子: '午',
  午: '子',
  丑: '未',
  未: '丑',
  寅: '申',
  申: '寅',
  卯: '酉',
  酉: '卯',
  辰: '戌',
  戌: '辰',
  巳: '亥',
  亥: '巳',
}

/** 地支三合局 */
const ZHI_SANHE_GROUPS: string[][] = [
  ['申', '子', '辰'],
  ['寅', '午', '戌'],
  ['巳', '酉', '丑'],
  ['亥', '卯', '未'],
]

/** 天干五合 */
const TIAN_GAN_HE: Record<string, string> = {
  甲: '己',
  己: '甲',
  乙: '庚',
  庚: '乙',
  丙: '辛',
  辛: '丙',
  丁: '壬',
  壬: '丁',
  戊: '癸',
  癸: '戊',
}

/** 三十纳音 → 五行 */
const NAYIN_WUXING: Record<string, WuxingKey> = {
  海中金: 'jin',
  剑锋金: 'jin',
  白蜡金: 'jin',
  沙中金: 'jin',
  金箔金: 'jin',
  钗钏金: 'jin',
  炉中火: 'huo',
  山头火: 'huo',
  霹雳火: 'huo',
  山下火: 'huo',
  覆灯火: 'huo',
  天上火: 'huo',
  大林木: 'mu',
  杨柳木: 'mu',
  松柏木: 'mu',
  平地木: 'mu',
  桑柘木: 'mu',
  石榴木: 'mu',
  路旁土: 'tu',
  城头土: 'tu',
  屋上土: 'tu',
  壁上土: 'tu',
  大驿土: 'tu',
  沙中土: 'tu',
  涧下水: 'shui',
  泉中水: 'shui',
  长流水: 'shui',
  天河水: 'shui',
  大溪水: 'shui',
  大海水: 'shui',
}

const SHENG: Record<WuxingKey, WuxingKey> = { mu: 'huo', huo: 'tu', tu: 'jin', jin: 'shui', shui: 'mu' }
const KE: Record<WuxingKey, WuxingKey> = { mu: 'tu', tu: 'shui', shui: 'huo', huo: 'jin', jin: 'mu' }

export type Relation3 = '相生' | '相克' | '比和' | '—'
export type ZhiRelation = '六合' | '三合' | '六冲' | '—'

export function dayMasterRelation(a: WuxingKey, b: WuxingKey): Relation3 {
  if (a === b) return '比和'
  if (SHENG[a] === b || SHENG[b] === a) return '相生'
  if (KE[a] === b || KE[b] === a) return '相克'
  return '—'
}

export function nayinRelation(a: string, b: string): Relation3 {
  const wa = NAYIN_WUXING[a]
  const wb = NAYIN_WUXING[b]
  if (!wa || !wb) return '—'
  return dayMasterRelation(wa, wb)
}

export function zhiRelation(a: string, b: string): ZhiRelation {
  if (ZHI_LIUHE[a] === b) return '六合'
  if (ZHI_LIUCHONG[a] === b) return '六冲'
  if (ZHI_SANHE_GROUPS.some((g) => g.includes(a) && g.includes(b) && a !== b)) return '三合'
  return '—'
}

export function tianGanHe(a: string, b: string): boolean {
  return TIAN_GAN_HE[a] === b
}

export interface CompatibilityResult {
  score: number
  grade: string
  shengXiaoRelation: '六合' | '—'
  dayMasterRelation: Relation3
  nayinRelation: Relation3
  spousePalaceRelation: ZhiRelation
  yearRelation: ZhiRelation
  yearGanHe: boolean
  tips: string[]
}

const WUXING_KEYS: WuxingKey[] = ['jin', 'mu', 'shui', 'huo', 'tu']

/**
 * 合婚参考规则（A3，前端实现，后端可替换）：
 * 基础分 60 + 生肖六合 15 + 日主五行生克 ±15 + 日柱纳音生克 ±10
 * + 夫妻宫（日支）六合 10 / 三合 8 / 六冲 -10
 * + 年支六合 5 / 三合 4 / 六冲 -5 + 年干五合 5 + 五行缺补（每项 3，上限 12）
 */
export function computeCompatibility(a: PaipanResult, b: PaipanResult): CompatibilityResult {
  let score = 60
  const tips: string[] = []

  const shengXiaoRelation = SHENGXIAO_LIUHE[a.shengXiao] === b.shengXiao ? '六合' : '—'
  if (shengXiaoRelation === '六合') {
    score += 15
    tips.push(`生肖 ${a.shengXiao}·${b.shengXiao} 六合`)
  }

  const aWu = GAN_WUXING[a.pillars.day.gan]
  const bWu = GAN_WUXING[b.pillars.day.gan]
  const dayMaster = dayMasterRelation(aWu, bWu)
  if (dayMaster === '相生') {
    score += 15
    tips.push(`日主 ${a.pillars.day.gan}·${b.pillars.day.gan} 五行相生`)
  } else if (dayMaster === '相克') {
    score -= 15
    tips.push(`日主 ${a.pillars.day.gan}·${b.pillars.day.gan} 五行相克，需多磨合`)
  }

  const nayin = nayinRelation(a.pillars.day.naYin, b.pillars.day.naYin)
  if (nayin === '相生') {
    score += 10
    tips.push(`日柱纳音 ${a.pillars.day.naYin}·${b.pillars.day.naYin} 相生`)
  } else if (nayin === '相克') {
    score -= 10
    tips.push(`日柱纳音 ${a.pillars.day.naYin}·${b.pillars.day.naYin} 相克`)
  }

  const spouse = zhiRelation(a.pillars.day.zhi, b.pillars.day.zhi)
  if (spouse === '六合') {
    score += 10
    tips.push(`夫妻宫 ${a.pillars.day.zhi}·${b.pillars.day.zhi} 六合`)
  } else if (spouse === '三合') {
    score += 8
    tips.push(`夫妻宫 ${a.pillars.day.zhi}·${b.pillars.day.zhi} 三合`)
  } else if (spouse === '六冲') {
    score -= 10
    tips.push(`夫妻宫 ${a.pillars.day.zhi}·${b.pillars.day.zhi} 相冲，需多磨合`)
  }

  const yearRel = zhiRelation(a.pillars.year.zhi, b.pillars.year.zhi)
  if (yearRel === '六合') {
    score += 5
    tips.push(`年支 ${a.pillars.year.zhi}·${b.pillars.year.zhi} 六合`)
  } else if (yearRel === '三合') {
    score += 4
  } else if (yearRel === '六冲') {
    score -= 5
    tips.push(`年支 ${a.pillars.year.zhi}·${b.pillars.year.zhi} 相冲`)
  }

  const yearGanHe = tianGanHe(a.pillars.year.gan, b.pillars.year.gan)
  if (yearGanHe) {
    score += 5
    tips.push(`年干 ${a.pillars.year.gan}·${b.pillars.year.gan} 天干五合`)
  }

  let complement = 0
  for (const k of WUXING_KEYS) {
    if (a.wuXing[k] === 0 && b.wuXing[k] > 0) complement += 1
    if (b.wuXing[k] === 0 && a.wuXing[k] > 0) complement += 1
  }
  if (complement > 0) {
    score += Math.min(complement * 3, 12)
    tips.push(`五行互补：${complement} 个缺失五行可由对方补足`)
  }

  const clamped = Math.max(0, Math.min(100, score))
  const grade = clamped >= 85 ? '上等' : clamped >= 70 ? '中等偏上' : clamped >= 60 ? '中等' : '需多磨合'
  return {
    score: clamped,
    grade,
    shengXiaoRelation,
    dayMasterRelation: dayMaster,
    nayinRelation: nayin,
    spousePalaceRelation: spouse,
    yearRelation: yearRel,
    yearGanHe,
    tips,
  }
}
