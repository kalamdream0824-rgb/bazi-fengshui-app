import type { PaipanResult } from '@/types/bazi'
import { computeWangShuai } from './geJu'
import type { ExplainBlockKey, ExplainPoint } from './explainer'
import {
  BLOCK_REFERENCE,
  CHANG_SHENG_TIP,
  PILLAR_ROLE,
  SHISHEN_COMBO_TIP,
  SHISHEN_TIP,
  WANG_SHUAI_TIP,
} from './explainerDictionary'

/**
 * 解读规则层（组合型解读）
 *
 * 每条规则 = 检测条件 + 句型模板 + 词库引用，不枚举组合文案。
 * 输出必须引用盘内具体证据（干支/十神/得分等），且保持温和参考口径。
 */

export interface ExplainRule {
  key: string
  /** 块内排序权重：越高越优先展示 */
  weight: number
  /** 产出到哪个解读块 */
  block: ExplainBlockKey
  match: (result: PaipanResult) => boolean
  build: (result: PaipanResult) => ExplainPoint
}

/** 日主旺衰进解读（复用 computeWangShuai，只描述状态） */
const wangShuaiRule: ExplainRule = {
  key: 'wangshuai',
  weight: 100,
  block: 'daymaster',
  match: () => true,
  build: (result) => {
    const ws = computeWangShuai(result)
    return {
      label: `日主旺衰（${ws.score} 分）`,
      text: `${WANG_SHUAI_TIP[ws.level]}。本盘按得令/得地/得势计分 ${ws.score} 分，${BLOCK_REFERENCE}。`,
    }
  },
}

/** 自坐进解读：日主根基状态 */
const ziZuoRule: ExplainRule = {
  key: 'zizuo',
  weight: 80,
  block: 'daymaster',
  match: () => true,
  build: (result) => {
    const day = result.pillars.day
    return {
      label: `日主自坐（${day.gan}${day.zhi}）`,
      text: `${day.gan}日主自坐${day.zhi}为「${day.ziZuo}」，${CHANG_SHENG_TIP[day.ziZuo] ?? '传统称此状态需结合全局论断'}，${BLOCK_REFERENCE}。`,
    }
  },
}

/** 副星进解读：日支藏干揭示内在层次 */
const fuXingRule: ExplainRule = {
  key: 'fuxing',
  weight: 70,
  block: 'pillars',
  match: (result) => result.pillars.day.hideGan.length > 0,
  build: (result) => {
    const day = result.pillars.day
    const pairs = day.hideGan.map((h) => `${h.gan}（${h.shiShen}）`).join('、')
    const firstTip = day.hideGan[0] ? SHISHEN_TIP[day.hideGan[0].shiShen] ?? '' : ''
    return {
      label: `日支藏干（${day.zhi}）`,
      text: `日支${day.zhi}藏${pairs}，${firstTip}，${BLOCK_REFERENCE}。`,
    }
  },
}

/** 大运十神进解读：当前大运主题具体化 */
const daYunShiShenRule: ExplainRule = {
  key: 'dayun-shishen',
  weight: 90,
  block: 'dayun',
  match: (result) => {
    const dy = result.daYun.find((d) => d.isCurrent)
    return Boolean(dy && dy.shiShen)
  },
  build: (result) => {
    const dy = result.daYun.find((d) => d.isCurrent)!
    return {
      label: `大运主题（${dy.ganZhi}）`,
      text: `当前大运天干${dy.ganZhi[0]}为日主之${dy.shiShen}，${SHISHEN_TIP[dy.shiShen] ?? ''}，${BLOCK_REFERENCE}。`,
    }
  },
}

interface ComboDef {
  name: string
  /** 每组任选其一，所有组都满足才算命中（如 [['伤官'], ['正印']]） */
  groups: string[][]
}

/** 经典十神组合（P1，按展示优先级排序） */
const SHISHEN_COMBOS: ComboDef[] = [
  { name: '伤官配印', groups: [['伤官'], ['正印']] },
  { name: '食神生财', groups: [['食神'], ['正财', '偏财']] },
  { name: '杀印相生', groups: [['七杀'], ['正印', '偏印']] },
  { name: '官印相生', groups: [['正官'], ['正印']] },
  { name: '食神制杀', groups: [['食神'], ['七杀']] },
  { name: '伤官生财', groups: [['伤官'], ['正财', '偏财']] },
  { name: '比劫夺财', groups: [['比肩', '劫财'], ['正财', '偏财']] },
  { name: '枭神夺食', groups: [['偏印'], ['食神']] },
]

/** 命局十神集合：四柱天干 + 日支藏干（副星） */
function shiShenSet(result: PaipanResult): Set<string> {
  const set = new Set<string>()
  for (const key of ['year', 'month', 'day', 'time'] as const) {
    const p = result.pillars[key]
    if (p.shiShen && p.shiShen !== '日主') set.add(p.shiShen)
    p.hideGan.forEach((h) => set.add(h.shiShen))
  }
  return set
}

function hitCombos(ssSet: Set<string>): ComboDef[] {
  return SHISHEN_COMBOS.filter((combo) => {
    return combo.groups.every((group) => group.some((s) => ssSet.has(s)))
  })
}

/** 十神组合检测：命局呈现经典组合 → 组合词条引用 */
const shiShenComboRule: ExplainRule = {
  key: 'shishen-combo',
  weight: 85,
  block: 'shishen',
  match: (result) => hitCombos(shiShenSet(result)).length > 0,
  build: (result) => {
    const combos = hitCombos(shiShenSet(result)).slice(0, 2)
    const names = combos.map((c) => `「${c.name}」`).join('、')
    const tips = combos.map((c) => `${c.name}：${SHISHEN_COMBO_TIP[c.name] ?? '传统称此组合需结合全局论断'}`).join('；')
    return {
      label: '十神组合',
      text: `命局呈现${names}的组合，${tips}，${BLOCK_REFERENCE}。`,
    }
  },
}

/** 神煞落宫：神煞 × 宫位，输出前 2 个（按柱序） */
const shenShaPalaceRule: ExplainRule = {
  key: 'shensha-palace',
  weight: 75,
  block: 'shensha',
  match: (result) => ['year', 'month', 'day', 'time'].some((k) => result.pillars[k as keyof typeof result.pillars].shenSha.length > 0),
  build: (result) => {
    const hits: { name: string; palace: string; role: string }[] = []
    for (const key of ['year', 'month', 'day', 'time'] as const) {
      for (const ss of result.pillars[key].shenSha) {
        if (hits.length >= 2) break
        const palace = key === 'year' ? '年柱' : key === 'month' ? '月柱' : key === 'day' ? '日柱' : '时柱'
        hits.push({ name: ss, palace, role: PILLAR_ROLE[key] })
      }
      if (hits.length >= 2) break
    }
    if (hits.length === 0) {
      return { label: '神煞落宫', text: `本命局未出现高频神煞落宫信息，${BLOCK_REFERENCE}。` }
    }
    return {
      label: '神煞落宫',
      text: `${hits.map((h) => `「${h.name}」落在${h.palace}，${h.role}`).join('；')}，${BLOCK_REFERENCE}。`,
    }
  },
}

/** P0 规则集（后续 P1/P2 规则直接追加） */
export const EXPLAIN_RULES: ExplainRule[] = [
  wangShuaiRule,
  ziZuoRule,
  fuXingRule,
  daYunShiShenRule,
  shiShenComboRule,
  shenShaPalaceRule,
]

/** 取某块命中的组合规则，按权重降序取前 n 条 */
export function matchedRules(result: PaipanResult, block: ExplainBlockKey, limit = 2): ExplainRule[] {
  return EXPLAIN_RULES.filter((r) => r.block === block && r.match(result))
    .sort((a, b) => b.weight - a.weight)
    .slice(0, limit)
}
