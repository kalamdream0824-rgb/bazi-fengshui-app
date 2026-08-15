import type { PaipanResult } from '@/types/bazi'
import { computeWangShuai } from './geJu'
import type { ExplainBlockKey, ExplainPoint } from './explainer'
import { BLOCK_REFERENCE, CHANG_SHENG_TIP, SHISHEN_TIP, WANG_SHUAI_TIP } from './explainerDictionary'

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

/** P0 规则集（后续 P1/P2 规则直接追加） */
export const EXPLAIN_RULES: ExplainRule[] = [wangShuaiRule, ziZuoRule, fuXingRule, daYunShiShenRule]

/** 取某块命中的组合规则，按权重降序取前 n 条 */
export function matchedRules(result: PaipanResult, block: ExplainBlockKey, limit = 2): ExplainRule[] {
  return EXPLAIN_RULES.filter((r) => r.block === block && r.match(result))
    .sort((a, b) => b.weight - a.weight)
    .slice(0, limit)
}
