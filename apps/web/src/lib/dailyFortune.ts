import type { PaipanResult, WuxingKey } from '@/types/bazi'
import type { AlmanacInfo } from './almanac'
import { GAN_WUXING } from './wuxing'

const YANG = new Set(['甲', '丙', '戊', '庚', '壬'])

function isYang(gan: string): boolean {
  return YANG.has(gan)
}

const SHENG: Record<WuxingKey, WuxingKey> = { mu: 'huo', huo: 'tu', tu: 'jin', jin: 'shui', shui: 'mu' }
const KE: Record<WuxingKey, WuxingKey> = { mu: 'tu', tu: 'shui', shui: 'huo', huo: 'jin', jin: 'mu' }

/** 以日主为「我」，求某天干的十神 */
export function shiShenOf(dayGan: string, masterGan: string): string {
  const a = GAN_WUXING[dayGan]
  const b = GAN_WUXING[masterGan]
  const same = isYang(dayGan) === isYang(masterGan)
  if (a === b) return same ? '比肩' : '劫财'
  if (SHENG[a] === b) return same ? '偏印' : '正印' // a 生 b：生我者印
  if (SHENG[b] === a) return same ? '食神' : '伤官' // b 生 a：我生者食伤
  if (KE[a] === b) return same ? '偏官' : '正官' // 克我者官杀
  if (KE[b] === a) return same ? '偏财' : '正财' // 我克者财
  return '—'
}

const SHI_SHEN_SUMMARY: Record<string, string> = {
  比肩: '今天适合把重心放在自己身上，专注手头的事。',
  劫财: '今天社交活跃，注意合作与开销的分寸。',
  食神: '今天心态松弛，适合表达、分享与享受生活。',
  伤官: '今天思路活跃、有话直说，沟通时注意语气。',
  正财: '今天务实求稳，适合处理财务与日常事务。',
  偏财: '今天对机会比较敏感，投资与消费都宜三思。',
  正官: '今天宜守规矩、推进计划，责任事项优先。',
  偏官: '今天压力感偏强，宜冷静应对，不硬碰硬。',
  正印: '今天适合学习、整理与向他人求助。',
  偏印: '今天灵感较多，适合独处思考，避免钻牛角尖。',
}

const LUCKY_COLOR: Record<WuxingKey, string> = {
  jin: '白 / 金',
  mu: '青 / 绿',
  shui: '黑 / 蓝',
  huo: '红 / 紫',
  tu: '黄 / 褐',
}

const ZHI_XING_BAD = new Set(['建', '破', '危', '闭'])
const ZHI_XING_GOOD = new Set(['开', '成', '定'])

export interface DailyFortune {
  personalized: boolean
  star: number
  summary: string
  luckyColor: string
  direction: string
  tip: string
  notes: string[]
}

/** 温和参考型运势（方案 A）：不做绝对化预测，文案仅作参考 */
export function dailyFortune(result: PaipanResult | null, almanac: AlmanacInfo): DailyFortune {
  let star = 3
  const notes: string[] = []
  let summary = '今日黄历整体平稳，宜忌仅供参考。'
  const personalized = Boolean(result)

  if (result) {
    const dayGan = almanac.ganZhi[0]
    const masterGan = result.pillars.day.gan
    const shiShen = shiShenOf(dayGan, masterGan)
    const dayWu = GAN_WUXING[dayGan]
    const masterWu = GAN_WUXING[masterGan]

    if (dayWu === masterWu) {
      notes.push('今日五行与日主比和，节奏平稳')
    } else if (SHENG[dayWu] === masterWu || SHENG[masterWu] === dayWu) {
      star += 1
      notes.push('今日五行相生，气机较顺')
    } else {
      star -= 1
      notes.push('今日五行相克，宜稳不宜急')
    }

    if (ZHI_XING_BAD.has(almanac.zhiXing)) star -= 1
    if (ZHI_XING_GOOD.has(almanac.zhiXing)) star += 1

    if (almanac.chongShengXiao === result.shengXiao) {
      star -= 1
      notes.push(`今日冲${almanac.chongShengXiao}，属${result.shengXiao}的你多留意沟通与出行`)
    }

    summary = `${SHI_SHEN_SUMMARY[shiShen] ?? '今天按部就班，稳中有进。'}${almanac.zhiXing ? `（${almanac.zhiXing}日）` : ''}`
  }

  const luckyColor = LUCKY_COLOR[GAN_WUXING[almanac.ganZhi[0]]] ?? '—'
  const direction = almanac.positionFu || '—'
  const tip = [almanac.pengZu ? `彭祖百忌：${almanac.pengZu}` : '', almanac.chongDesc ? `冲煞：${almanac.chongDesc} · 煞${almanac.sha}` : '']
    .filter(Boolean)
    .join('；')

  return {
    personalized,
    star: Math.max(1, Math.min(5, star)),
    summary,
    luckyColor,
    direction,
    tip,
    notes,
  }
}
