import type { PaipanResult, PillarKey, WuxingKey } from '@/types/bazi'
import { GAN_WUXING, WUXING_LABEL, ZHI_WUXING } from './wuxing'

/**
 * 月令格局 + 日主旺衰粗判（专业细盘第二批）
 *
 * 口径公示（流派间存在差异，本项目取通行简化口径，仅作文化参考）：
 * - 月令格局：月支藏干透干优先取格（四柱天干有透出），不透取本气；十神 → 八正格命名。
 * - 日主旺衰：得分制——得令（月支生扶）＋2；年/日/时支生扶各＋1；年/月/时干比劫＋1、印＋0.5；
 *   总分 ≥4 偏强、≤1.5 偏弱、否则中和。不做用神/喜忌推论。
 */

export type WuXingRelation = 'yin' | 'bi' | 'shishen' | 'cai' | 'guansha'

const SHENG: Record<WuxingKey, WuxingKey> = { mu: 'huo', huo: 'tu', tu: 'jin', jin: 'shui', shui: 'mu' }
const KE: Record<WuxingKey, WuxingKey> = { mu: 'tu', tu: 'shui', shui: 'huo', huo: 'jin', jin: 'mu' }

/** 日主五行 vs 其他五行的关系：印（生我）/ 比劫（同我）/ 食伤（我生）/ 财（我克）/ 官杀（克我） */
export function wuxingRelation(day: WuxingKey, other: WuxingKey): WuXingRelation {
  if (day === other) return 'bi'
  if (SHENG[other] === day) return 'yin'
  if (SHENG[day] === other) return 'shishen'
  if (KE[day] === other) return 'cai'
  return 'guansha'
}

const PILLAR_KEYS: PillarKey[] = ['year', 'month', 'day', 'time']

export interface GeJu {
  /** 取格藏干 */
  gan: string
  /** 取格藏干对日主的十神 */
  shiShen: string
  /** 格局名（如 正印格 / 七杀格 / 月劫格） */
  name: string
  /** 是否透干取格（false = 取月支本气） */
  trans: boolean
  /** 月柱干支 */
  monthGanZhi: string
}

const GE_JU_NAME: Record<string, string> = {
  正官: '正官格',
  七杀: '七杀格',
  正财: '正财格',
  偏财: '偏财格',
  正印: '正印格',
  偏印: '偏印格',
  食神: '食神格',
  伤官: '伤官格',
  比肩: '建禄格',
  劫财: '月劫格',
}

export function computeGeJu(result: PaipanResult): GeJu {
  const month = result.pillars.month
  const hideGan = month.hideGan
  const ganSet = new Set(PILLAR_KEYS.map((k) => result.pillars[k].gan))
  const trans = hideGan.some((h) => ganSet.has(h.gan))
  const hit = hideGan.find((h) => ganSet.has(h.gan)) ?? hideGan[0]
  return {
    gan: hit.gan,
    shiShen: hit.shiShen,
    name: GE_JU_NAME[hit.shiShen] ?? `${hit.shiShen}格`,
    trans,
    monthGanZhi: `${month.gan}${month.zhi}`,
  }
}

export interface WangShuaiItem {
  label: string
  score: number
  note: string
}

export interface WangShuai {
  score: number
  level: '偏强' | '中和' | '偏弱'
  items: WangShuaiItem[]
}

const ZHI_LABEL: Record<'year' | 'day' | 'time', string> = { year: '年支', day: '日支', time: '时支' }
const GAN_LABEL: Record<'year' | 'month' | 'time', string> = { year: '年干', month: '月干', time: '时干' }

export function computeWangShuai(result: PaipanResult): WangShuai {
  const dayWuxing = GAN_WUXING[result.pillars.day.gan]
  const items: WangShuaiItem[] = []
  let score = 0

  const monthZhi = result.pillars.month.zhi
  const monthWuxing = ZHI_WUXING[monthZhi]
  const monthRel = wuxingRelation(dayWuxing, monthWuxing)
  if (monthRel === 'yin' || monthRel === 'bi') {
    score += 2
    items.push({ label: '得令', score: 2, note: `月支${monthZhi}（${WUXING_LABEL[monthWuxing]}）生扶日主` })
  } else {
    items.push({ label: '月令', score: 0, note: `月支${monthZhi}（${WUXING_LABEL[monthWuxing]}）不直接生扶日主` })
  }

  for (const key of ['year', 'day', 'time'] as const) {
    const zhi = result.pillars[key].zhi
    const w = ZHI_WUXING[zhi]
    const rel = wuxingRelation(dayWuxing, w)
    if (rel === 'yin' || rel === 'bi') {
      score += 1
      items.push({ label: ZHI_LABEL[key], score: 1, note: `${zhi}（${WUXING_LABEL[w]}）生扶日主` })
    } else {
      items.push({ label: ZHI_LABEL[key], score: 0, note: `${zhi}（${WUXING_LABEL[w]}）不生扶日主` })
    }
  }

  for (const key of ['year', 'month', 'time'] as const) {
    const gan = result.pillars[key].gan
    const rel = wuxingRelation(dayWuxing, GAN_WUXING[gan])
    if (rel === 'bi') {
      score += 1
      items.push({ label: GAN_LABEL[key], score: 1, note: `${gan}（${WUXING_LABEL[GAN_WUXING[gan]]}）与日主同类` })
    } else if (rel === 'yin') {
      score += 0.5
      items.push({ label: GAN_LABEL[key], score: 0.5, note: `${gan}（${WUXING_LABEL[GAN_WUXING[gan]]}）生日主` })
    } else {
      items.push({ label: GAN_LABEL[key], score: 0, note: `${gan}（${WUXING_LABEL[GAN_WUXING[gan]]}）不生扶日主` })
    }
  }

  const level = score >= 4 ? '偏强' : score <= 1.5 ? '偏弱' : '中和'
  return { score, level, items }
}
