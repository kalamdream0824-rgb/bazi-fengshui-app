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

const SHENG: Record<WuxingKey, WuxingKey> = { mu: 'huo', huo: 'tu', tu: 'jin', jin: 'shui', shui: 'mu' }
const KE: Record<WuxingKey, WuxingKey> = { mu: 'tu', tu: 'shui', shui: 'huo', huo: 'jin', jin: 'mu' }

export type DayMasterRelation = '相生' | '相克' | '比和' | '—'

export function dayMasterRelation(a: WuxingKey, b: WuxingKey): DayMasterRelation {
  if (a === b) return '比和'
  if (SHENG[a] === b || SHENG[b] === a) return '相生'
  if (KE[a] === b || KE[b] === a) return '相克'
  return '—'
}

export interface CompatibilityResult {
  score: number
  grade: string
  shengXiaoRelation: '六合' | '—'
  dayMasterRelation: DayMasterRelation
  tips: string[]
}

const WUXING_KEYS: WuxingKey[] = ['jin', 'mu', 'shui', 'huo', 'tu']

/** Mock 规则（示例）：基础分 60 + 生肖六合 15 + 五行互补 10 + 日主相生 15 / 相克 -15 */
export function computeCompatibility(a: PaipanResult, b: PaipanResult): CompatibilityResult {
  let score = 60
  const tips: string[] = []

  const shengXiaoRelation = SHENGXIAO_LIUHE[a.shengXiao] === b.shengXiao ? '六合' : '—'
  if (shengXiaoRelation === '六合') {
    score += 15
    tips.push(`生肖 ${a.shengXiao}·${b.shengXiao} 六合`)
  }

  const lackInA = WUXING_KEYS.filter((k) => a.wuXing[k] === 0 && b.wuXing[k] > 0)
  const lackInB = WUXING_KEYS.filter((k) => b.wuXing[k] === 0 && a.wuXing[k] > 0)
  if (lackInA.length > 0 || lackInB.length > 0) {
    score += 10
    tips.push('五行互补：一方缺失的五行可由另一方补足')
  }

  const aWu = GAN_WUXING[a.pillars.day.gan]
  const bWu = GAN_WUXING[b.pillars.day.gan]
  const rel = dayMasterRelation(aWu, bWu)
  if (rel === '相生') {
    score += 15
    tips.push(`日主 ${a.pillars.day.gan}·${b.pillars.day.gan} 五行相生`)
  } else if (rel === '相克') {
    score -= 15
    tips.push(`日主 ${a.pillars.day.gan}·${b.pillars.day.gan} 五行相克，需多磨合`)
  }

  const clamped = Math.max(0, Math.min(100, score))
  const grade = clamped >= 85 ? '上等' : clamped >= 70 ? '中等偏上' : clamped >= 60 ? '中等' : '需多磨合'
  return { score: clamped, grade, shengXiaoRelation, dayMasterRelation: rel, tips }
}
