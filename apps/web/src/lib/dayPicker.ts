import type { AlmanacInfo } from './almanac'
import { getAlmanacFor } from './almanac'

export type DayEventType = 'marry' | 'open' | 'move' | 'travel' | 'dig' | 'bury'

export const EVENT_TYPES: { value: DayEventType; label: string; desc: string }[] = [
  { value: 'marry', label: '嫁娶', desc: '结婚、订婚、纳采' },
  { value: 'open', label: '开业', desc: '开市、交易、纳财' },
  { value: 'move', label: '搬家', desc: '入宅、移徙' },
  { value: 'travel', label: '出行', desc: '出行、远行' },
  { value: 'dig', label: '动土', desc: '动土、修造、装修' },
  { value: 'bury', label: '安葬', desc: '安葬、入殓、移柩' },
]

interface EventRule {
  yi: string[]
  ji: string[]
  goodZhiXing: string[]
  badZhiXing: string[]
}

const RULES: Record<DayEventType, EventRule> = {
  marry: { yi: ['嫁娶', '结婚', '纳采', '订婚', '领证'], ji: ['安葬', '破土', '开市', '丧葬'], goodZhiXing: [], badZhiXing: ['破', '危', '闭'] },
  open: { yi: ['开市', '开业', '交易', '纳财', '挂匾'], ji: ['安葬', '破土', '祭祀'], goodZhiXing: ['开', '成', '定'], badZhiXing: ['破', '闭'] },
  move: { yi: ['入宅', '搬家', '移徙', '安床'], ji: ['安葬', '开市', '动土'], goodZhiXing: [], badZhiXing: ['破', '闭'] },
  travel: { yi: ['出行', '旅游', '远行', '赴任'], ji: ['安葬', '动土', '开市'], goodZhiXing: ['开', '成', '定'], badZhiXing: ['破', '危', '闭'] },
  dig: { yi: ['动土', '破土', '修造', '装修', '起基'], ji: ['安葬', '嫁娶'], goodZhiXing: ['建', '满', '定', '成', '开'], badZhiXing: ['破', '闭'] },
  bury: { yi: ['安葬', '入殓', '移柩', '成服', '除服', '启钻'], ji: ['嫁娶', '开市', '纳采', '动土', '搬家'], goodZhiXing: [], badZhiXing: ['破'] },
}

export interface PickedDay {
  offset: number
  dateText: string
  ganZhi: string
  yi: string[]
  ji: string[]
  zhiXing: string
  chongDesc: string
  sha: string
  score: number
  reasons: string[]
  recommended: boolean
}

export function evaluateDay(type: DayEventType, almanac: AlmanacInfo, shengXiao?: string) {
  const rule = RULES[type]
  if (almanac.ji.includes('诸事不宜')) {
    return { score: -99, reasons: ['今日诸事不宜'], pass: false }
  }
  const hitJi = almanac.ji.find((t) => rule.ji.includes(t))
  if (hitJi) {
    return { score: -99, reasons: [`忌「${hitJi}」`], pass: false }
  }
  const hitYi = almanac.yi.filter((t) => rule.yi.includes(t))
  if (hitYi.length === 0) {
    return { score: -99, reasons: ['宜项不含该事项'], pass: false }
  }

  let score = hitYi.length * 2
  const reasons = [`宜「${hitYi.join('、')}」`]
  if (rule.goodZhiXing.includes(almanac.zhiXing)) {
    score += 1
    reasons.push(`${almanac.zhiXing}日`)
  }
  if (rule.badZhiXing.includes(almanac.zhiXing)) {
    score -= 2
    reasons.push(`${almanac.zhiXing}日需谨慎`)
  }
  if (shengXiao && almanac.chongShengXiao === shengXiao) {
    score -= 3
    reasons.push(`冲${shengXiao}（${almanac.chongShengXiao}），可优先避开`)
  }
  return { score, reasons, pass: true }
}

export function pickDays(
  type: DayEventType,
  days = 60,
  shengXiao?: string,
  almanacFn: (offset: number) => AlmanacInfo = getAlmanacFor,
): PickedDay[] {
  const list: PickedDay[] = []
  for (let i = 1; i <= days; i += 1) {
    const alm = almanacFn(i)
    const { score, reasons, pass } = evaluateDay(type, alm, shengXiao)
    const d = new Date()
    d.setDate(d.getDate() + i)
    list.push({
      offset: i,
      dateText: `${d.getMonth() + 1}月${d.getDate()}日`,
      ganZhi: alm.ganZhi,
      yi: alm.yi,
      ji: alm.ji,
      zhiXing: alm.zhiXing,
      chongDesc: alm.chongDesc,
      sha: alm.sha,
      score: pass ? score : -99,
      reasons,
      recommended: pass && score > 0,
    })
  }
  return list.filter((d) => d.recommended).sort((a, b) => b.score - a.score)
}
