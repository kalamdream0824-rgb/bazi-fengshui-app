import type { Pillar, PillarKey } from '@/types/bazi'

/**
 * 常用神煞推算（A2）
 *
 * 查法来源：《三命通会》《渊海子平》通行口径。流派间存在差异，本实现取通行查法并在此公示：
 * - 以年支、日支三合局查：桃花 / 驿马 / 华盖 / 劫煞 / 亡神 / 将星
 * - 以日干查地支：羊刃 / 禄神 / 天乙贵人 / 文昌 / 红艳 / 流霞 / 金舆
 * - 以日柱查：魁罡 / 十恶大败 / 阴阳差错 / 孤鸾煞
 * - 以年支查：孤辰 / 寡宿
 * - 空亡：以日柱旬空，看四支是否落空
 */

const SANHE_GROUP: Record<string, string> = {
  申: '申子辰',
  子: '申子辰',
  辰: '申子辰',
  寅: '寅午戌',
  午: '寅午戌',
  戌: '寅午戌',
  巳: '巳酉丑',
  酉: '巳酉丑',
  丑: '巳酉丑',
  亥: '亥卯未',
  卯: '亥卯未',
  未: '亥卯未',
}

const GROUP_SHEN_SHA: Record<string, Record<string, string>> = {
  申子辰: { 桃花: '酉', 驿马: '寅', 华盖: '辰', 劫煞: '巳', 亡神: '亥', 将星: '子' },
  寅午戌: { 桃花: '卯', 驿马: '申', 华盖: '戌', 劫煞: '亥', 亡神: '巳', 将星: '午' },
  巳酉丑: { 桃花: '午', 驿马: '亥', 华盖: '丑', 劫煞: '寅', 亡神: '申', 将星: '酉' },
  亥卯未: { 桃花: '子', 驿马: '巳', 华盖: '未', 劫煞: '申', 亡神: '寅', 将星: '卯' },
}

/** 日干 → 羊刃地支 */
const YANG_REN: Record<string, string> = {
  甲: '卯',
  乙: '辰',
  丙: '午',
  丁: '未',
  戊: '午',
  己: '未',
  庚: '酉',
  辛: '戌',
  壬: '子',
  癸: '丑',
}
/** 日干 → 禄神地支 */
const LU_SHEN: Record<string, string> = {
  甲: '寅',
  乙: '卯',
  丙: '巳',
  丁: '午',
  戊: '巳',
  己: '午',
  庚: '申',
  辛: '酉',
  壬: '亥',
  癸: '子',
}
/** 日干 → 天乙贵人地支（甲戊庚牛羊，乙己鼠猴乡，丙丁猪鸡位，壬癸兔蛇藏，六辛逢马虎） */
const TIANYI: Record<string, string[]> = {
  甲: ['丑', '未'],
  戊: ['丑', '未'],
  庚: ['丑', '未'],
  乙: ['子', '申'],
  己: ['子', '申'],
  丙: ['亥', '酉'],
  丁: ['亥', '酉'],
  壬: ['卯', '巳'],
  癸: ['卯', '巳'],
  辛: ['午', '寅'],
}
/** 日干 → 文昌地支 */
const WENCHANG: Record<string, string> = {
  甲: '巳',
  乙: '午',
  丙: '申',
  丁: '酉',
  戊: '申',
  己: '酉',
  庚: '亥',
  辛: '子',
  壬: '寅',
  癸: '卯',
}
/** 日干 → 红艳地支 */
const HONGYAN: Record<string, string> = {
  甲: '午',
  乙: '申',
  丙: '寅',
  丁: '未',
  戊: '辰',
  己: '卯',
  庚: '戌',
  辛: '酉',
  壬: '子',
  癸: '亥',
}
/** 日干 → 流霞地支 */
const LIUXIA: Record<string, string> = {
  甲: '酉',
  乙: '戌',
  丙: '未',
  丁: '申',
  戊: '巳',
  己: '午',
  庚: '辰',
  辛: '卯',
  壬: '寅',
  癸: '亥',
}
/** 日干 → 金舆地支 */
const JINYU: Record<string, string> = {
  甲: '辰',
  乙: '巳',
  丙: '未',
  戊: '未',
  丁: '申',
  己: '申',
  庚: '戌',
  辛: '亥',
  壬: '丑',
  癸: '寅',
}
/** 年支 → 孤辰 */
const GUCHEN: Record<string, string> = {
  亥: '寅',
  子: '寅',
  丑: '寅',
  寅: '巳',
  卯: '巳',
  辰: '巳',
  巳: '申',
  午: '申',
  未: '申',
  申: '亥',
  酉: '亥',
  戌: '亥',
}
/** 年支 → 寡宿 */
const GUASU: Record<string, string> = {
  亥: '戌',
  子: '戌',
  丑: '戌',
  寅: '丑',
  卯: '丑',
  辰: '丑',
  巳: '辰',
  午: '辰',
  未: '辰',
  申: '未',
  酉: '未',
  戌: '未',
}

const KUI_GANG = new Set(['庚辰', '庚戌', '壬辰', '戊戌'])
const SHIE_DA_BAI = new Set(['甲辰', '乙巳', '壬申', '丙申', '丁亥', '庚辰', '戊戌', '癸亥', '辛巳', '己丑'])
const YINYANG_CHACUO = new Set([
  '丙子',
  '丁丑',
  '戊寅',
  '辛卯',
  '壬辰',
  '癸巳',
  '丙午',
  '丁未',
  '戊申',
  '辛酉',
  '壬戌',
  '癸亥',
])
const GU_LUAN = new Set(['乙巳', '丁巳', '辛亥', '戊申', '戊午', '壬子', '丙午'])

const KEYS: PillarKey[] = ['year', 'month', 'day', 'time']

export function computeShenSha(pillars: Record<PillarKey, Pillar>, dayXunKong: string): Record<PillarKey, string[]> {
  const result: Record<PillarKey, string[]> = { year: [], month: [], day: [], time: [] }
  const zhis = KEYS.map((k) => pillars[k].zhi)

  const add = (name: string, zhi: string) => {
    const idx = zhis.indexOf(zhi)
    if (idx >= 0 && !result[KEYS[idx]].includes(name)) {
      result[KEYS[idx]].push(name)
    }
  }

  const yearZhi = pillars.year.zhi
  const dayZhi = pillars.day.zhi
  const dayGan = pillars.day.gan
  const dayGanZhi = `${dayGan}${dayZhi}`

  // 以年支、日支三合局查
  for (const anchor of [yearZhi, dayZhi]) {
    const table = GROUP_SHEN_SHA[SANHE_GROUP[anchor]]
    if (table) {
      for (const [name, zhi] of Object.entries(table)) add(name, zhi)
    }
  }

  // 空亡：日柱旬空
  for (const k of dayXunKong) add('空亡', k)

  // 日干查地支
  add('羊刃', YANG_REN[dayGan])
  add('禄神', LU_SHEN[dayGan])
  for (const z of TIANYI[dayGan] ?? []) add('天乙贵人', z)
  add('文昌', WENCHANG[dayGan])
  add('红艳', HONGYAN[dayGan])
  add('流霞', LIUXIA[dayGan])
  add('金舆', JINYU[dayGan])

  // 年支查孤辰寡宿
  add('孤辰', GUCHEN[yearZhi])
  add('寡宿', GUASU[yearZhi])

  // 日柱神煞
  if (KUI_GANG.has(dayGanZhi)) add('魁罡', dayZhi)
  if (SHIE_DA_BAI.has(dayGanZhi)) add('十恶大败', dayZhi)
  if (YINYANG_CHACUO.has(dayGanZhi)) add('阴阳差错', dayZhi)
  if (GU_LUAN.has(dayGanZhi)) add('孤鸾煞', dayZhi)

  return result
}

export interface ShenShaRef {
  yearZhi: string
  dayZhi: string
  dayGan: string
  dayXunKong: string
}

/**
 * 大运/流年神煞：以大运或流年干支对照命局原局推算（口径见设计文档第 13 节）。
 * 入参 ganZhi 形如 "甲申"（前一位天干、后一位地支）。
 */
export function computeExternalShenSha(ref: ShenShaRef, ganZhi: string): string[] {
  const externalZhi = ganZhi[1]
  const hits: string[] = []

  const addHit = (name: string) => {
    if (!hits.includes(name)) hits.push(name)
  }

  for (const anchor of [ref.yearZhi, ref.dayZhi]) {
    const table = GROUP_SHEN_SHA[SANHE_GROUP[anchor]]
    if (table) {
      for (const [name, target] of Object.entries(table)) {
        if (target === externalZhi) addHit(name)
      }
    }
  }

  if (ref.dayXunKong.includes(externalZhi)) addHit('空亡')

  if (YANG_REN[ref.dayGan] === externalZhi) addHit('羊刃')
  if (LU_SHEN[ref.dayGan] === externalZhi) addHit('禄神')
  if ((TIANYI[ref.dayGan] ?? []).includes(externalZhi)) addHit('天乙贵人')
  if (WENCHANG[ref.dayGan] === externalZhi) addHit('文昌')
  if (HONGYAN[ref.dayGan] === externalZhi) addHit('红艳')
  if (LIUXIA[ref.dayGan] === externalZhi) addHit('流霞')
  if (JINYU[ref.dayGan] === externalZhi) addHit('金舆')
  if (GUCHEN[ref.yearZhi] === externalZhi) addHit('孤辰')
  if (GUASU[ref.yearZhi] === externalZhi) addHit('寡宿')

  return hits
}
