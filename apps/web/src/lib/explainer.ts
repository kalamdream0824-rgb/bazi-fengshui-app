import type { PaipanResult, PillarKey, WuxingKey } from '@/types/bazi'
import { GAN_WUXING, WUXING_LABEL } from './wuxing'
import { matchedRules } from './explainerRules'
import {
  BLOCK_REFERENCE,
  GAN_IMAGE,
  PILLAR_ROLE,
  REFERENCE,
  SHEN_SHA_TIP,
  SHISHEN_TIP,
  TERM_TIPS,
  WUXING_TRAIT,
} from './explainerDictionary'

/** 保持对外兼容导出（BaziTable / 测试引用） */
export { SHISHEN_TIP, TERM_TIPS }

export type ExplainBlockKey = 'daymaster' | 'pillars' | 'shishen' | 'shensha' | 'dayun' | 'liunian'

export interface ExplainPoint {
  label: string
  text: string
}

export interface ExplainBlock {
  key: ExplainBlockKey
  title: string
  points: ExplainPoint[]
}

export interface ChartExplanation {
  overview: string[]
  blocks: ExplainBlock[]
}

function countStrongWeak(wuXing: Record<WuxingKey, number>): { strong: string[]; weak: string[]; even: boolean } {
  const keys = Object.keys(wuXing) as WuxingKey[]
  const values = keys.map((k) => wuXing[k])
  const max = Math.max(...values)
  const min = Math.min(...values)
  if (max === min) {
    return { strong: [], weak: [], even: true }
  }
  return {
    strong: keys.filter((k) => wuXing[k] === max).map((k) => WUXING_LABEL[k]),
    weak: keys.filter((k) => wuXing[k] === min).map((k) => WUXING_LABEL[k]),
    even: false,
  }
}

function dayMasterText(result: PaipanResult): string {
  const gan = result.pillars.day.gan
  const wuXing = GAN_WUXING[gan]
  return `你的日主是「${gan}${WUXING_LABEL[wuXing]}」——${GAN_IMAGE[gan]}。`
}

function balanceText(result: PaipanResult): string {
  const { strong, weak, even } = countStrongWeak(result.wuXing)
  if (even) {
    return '五行分布较为均衡，传统上多与平和、全面的倾向相连。'
  }
  return `五行中${strong.join('、')}偏旺、${weak.join('、')}偏弱，传统上常被解读为与${strong
    .map((k) => k)
    .join('、')}相关的特质较明显、${weak.map((k) => k).join('、')}相关特质较内敛。`
}

function shenShaText(name: string): string {
  return SHEN_SHA_TIP[name] ?? `传统命理神煞之一（${name}），需结合全局论断，${BLOCK_REFERENCE}。`
}

function uniqueShenSha(list: string[][]): string[] {
  return Array.from(new Set(list.flat().filter(Boolean)))
}

/** 生成整份新人解读（总览 + 六块） */
export function explain(result: PaipanResult): ChartExplanation {
  const dayGan = result.pillars.day.gan
  const dayWuxing = WUXING_LABEL[GAN_WUXING[dayGan]]
  const overview = [
    dayMasterText(result),
    `${balanceText(result)}${WUXING_TRAIT[GAN_WUXING[dayGan]]}。`,
    REFERENCE,
  ]

  const pillarKeys: PillarKey[] = ['year', 'month', 'day', 'time']
  const pillarPoints = pillarKeys.map((k) => ({
    label: `${k === 'year' ? '年柱' : k === 'month' ? '月柱' : k === 'day' ? '日柱' : '时柱'}（${result.pillars[k].gan}${result.pillars[k].zhi}）`,
    text: PILLAR_ROLE[k],
  }))

  const shiShenSet = Array.from(new Set(pillarKeys.map((k) => result.pillars[k].shiShen)))
  const shiShenPoints = [
    {
      label: `日主十神（${result.pillars.day.shiShen}）`,
      text: SHISHEN_TIP[result.pillars.day.shiShen] ?? '传统命理十神之一，需结合全局论断。',
    },
    ...shiShenSet
      .filter((s) => s !== result.pillars.day.shiShen)
      .map((s) => ({
        label: s,
        text: SHISHEN_TIP[s] ?? '传统命理十神之一，需结合全局论断。',
      })),
    { label: '整体', text: `四柱十神分布反映的是性格与关系层面的传统参考，${BLOCK_REFERENCE}。` },
  ]

  const pillarShenSha = uniqueShenSha(pillarKeys.map((k) => result.pillars[k].shenSha))
  const shenShaPoints =
    pillarShenSha.length > 0
      ? pillarShenSha.map((s) => ({ label: s, text: shenShaText(s) }))
      : [{ label: '神煞', text: `本命局未出现高频神煞，${BLOCK_REFERENCE}。` }]

  const currentDaYun = result.daYun.find((d) => d.isCurrent)
  const dayunPoints = currentDaYun
    ? [
        {
          label: `当前大运（${currentDaYun.ganZhi}）`,
          text: `年龄 ${currentDaYun.ageRange}（${currentDaYun.yearRange}），传统框架中这一步运与「${currentDaYun.ganZhi}」相关的主题相连，${BLOCK_REFERENCE}。`,
        },
      ]
    : [{ label: '大运', text: `暂无当前大运数据，${BLOCK_REFERENCE}。` }]

  const liuNian = result.currentLiuNian
  const liunianPoints = liuNian
    ? [
        {
          label: `今年（${liuNian.ganZhi}）`,
          text: `传统框架中常以「${liuNian.ganZhi}」与命局的互动来看今年的倾向，${
            liuNian.shenSha.length > 0 ? `今年涉及神煞：${liuNian.shenSha.join('、')}。` : ''
          }${BLOCK_REFERENCE}。`,
        },
      ]
    : [{ label: '流年', text: `暂无今年流年数据，${BLOCK_REFERENCE}。` }]

  /** 词典点 + 组合规则点（按权重取前 2，避免刷屏） */
  const withRules = (key: ExplainBlockKey, points: ExplainPoint[]): ExplainPoint[] => [
    ...points,
    ...matchedRules(result, key).map((r) => r.build(result)),
  ]

  return {
    overview,
    blocks: [
      {
        key: 'daymaster',
        title: '日主与五行',
        points: withRules('daymaster', [
          { label: `日主（${dayGan}${dayWuxing}）`, text: dayMasterText(result) },
          { label: '五行分布', text: balanceText(result) },
        ]),
      },
      { key: 'pillars', title: '四柱定位', points: withRules('pillars', pillarPoints) },
      { key: 'shishen', title: '十神性格', points: shiShenPoints },
      { key: 'shensha', title: '神煞参考', points: shenShaPoints },
      { key: 'dayun', title: '当前大运', points: withRules('dayun', dayunPoints) },
      { key: 'liunian', title: '流年参考', points: liunianPoints },
    ],
  }
}

/** 单块解读（ProPage 按页签取用） */
export function explainBlock(explanation: ChartExplanation, key: ExplainBlockKey): ExplainBlock | null {
  return explanation.blocks.find((b) => b.key === key) ?? null
}
