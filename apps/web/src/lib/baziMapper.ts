import { Lunar, LunarUtil, Solar } from 'lunar-javascript'
import type { DaYun, LiuNianInfo, LiuNianItem, PaipanRequest, PaipanResult, Pillar, PillarKey, WuxingKey } from '@/types/bazi'
import { timeToShichen } from './datePicker'
import { computeExternalShenSha, computeShenSha, type ShenShaRef } from './shenSha'
import { formatAdjusted, longitudeOf, trueSolarTime } from './trueSolarTime'
import { GAN_WUXING, ZHI_WUXING } from './wuxing'

const YUN_GENDER = { male: 1, female: 0 } as const

/** 十二长生（与 lunar-javascript LunarUtil.CHANG_SHENG 同表同口径） */
const CHANG_SHENG_12 = ['长生', '沐浴', '冠带', '临官', '帝旺', '衰', '病', '死', '墓', '绝', '胎', '养']
/** 天干长生起点（地支 0-based 序：子0 丑1 … 亥11），与 lunar 库 CHANG_SHENG_OFFSET 一致 */
const CHANG_SHENG_OFFSET: Record<string, number> = { 甲: 1, 乙: 6, 丙: 10, 丁: 9, 戊: 10, 己: 9, 庚: 7, 辛: 0, 壬: 4, 癸: 3 }
const ZHI_INDEX: Record<string, number> = { 子: 0, 丑: 1, 寅: 2, 卯: 3, 辰: 4, 巳: 5, 午: 6, 未: 7, 申: 8, 酉: 9, 戌: 10, 亥: 11 }

/** 自坐：本柱天干为本柱地支的太极点，查十二长生（阳干顺行、阴干逆行） */
export function ziZuoOf(gan: string, zhi: string): string {
  const offset = CHANG_SHENG_OFFSET[gan] ?? 0
  const zhiIndex = ZHI_INDEX[zhi] ?? 0
  const yang = '甲丙戊庚壬'.includes(gan)
  let index = offset + (yang ? zhiIndex : -zhiIndex)
  if (index >= 12) index -= 12
  if (index < 0) index += 12
  return CHANG_SHENG_12[index]
}

function wuxingOfGan(gan: string): WuxingKey {
  return GAN_WUXING[gan] ?? 'tu'
}

function wuxingOfZhi(zhi: string): WuxingKey {
  return ZHI_WUXING[zhi] ?? 'tu'
}

function toPillar(
  label: PillarKey,
  gan: string,
  zhi: string,
  shiShen: string,
  hideGan: string[],
  hideGanShiShen: string[],
  naYin: string,
  diShi: string,
  xunKong: string,
): Pillar {
  return {
    label,
    gan,
    zhi,
    shiShen,
    ziZuo: ziZuoOf(gan, zhi),
    hideGan: hideGan.map((g, i) => ({
      gan: g,
      shiShen: hideGanShiShen[i] ?? '',
      wuxing: wuxingOfGan(g),
    })),
    naYin,
    diShi,
    xunKong,
    shenSha: [], // 神煞后端实现，前端展示；开发期占位
  }
}

export function paipan(req: PaipanRequest): PaipanResult {
  const [datePart, timePart = '00:00'] = req.solarDateTime.split('T')
  const [y, m, d] = datePart.split('-').map(Number)
  const [hh, mm] = timePart.split(':').map(Number)

  const base = new Date(y, (m || 1) - 1, d || 1, hh || 0, mm || 0, 0)
  let solarDate = base
  let trueSolar: PaipanResult['trueSolar']

  if (req.trueSolarTime && req.birthPlace) {
    const lng = longitudeOf(req.birthPlace)
    if (lng != null) {
      const r = trueSolarTime(base, lng)
      solarDate = r.adjusted
      trueSolar = {
        original: req.solarDateTime.slice(0, 16),
        adjusted: formatAdjusted(r.adjusted),
        offsetMinutes: Math.round(r.offsetMinutes),
        eotMinutes: Math.round(r.eotMinutes * 10) / 10,
        longitude: lng,
        originalShichen: timeToShichen(base.getHours()),
        adjustedShichen: timeToShichen(r.adjusted.getHours()),
        boundaryChanged: timeToShichen(base.getHours()) !== timeToShichen(r.adjusted.getHours()),
      }
    }
  }

  const solar = Solar.fromYmdHms(
    solarDate.getFullYear(),
    solarDate.getMonth() + 1,
    solarDate.getDate(),
    solarDate.getHours(),
    solarDate.getMinutes(),
    0,
  )
  const lunar = solar.getLunar()
  const ec = lunar.getEightChar()

  const now = new Date()
  const today = Lunar.fromDate(now)

  const pillars: PaipanResult['pillars'] = {
    year: toPillar(
      'year',
      ec.getYearGan(),
      ec.getYearZhi(),
      ec.getYearShiShenGan(),
      ec.getYearHideGan(),
      ec.getYearShiShenZhi(),
      ec.getYearNaYin(),
      ec.getYearDiShi(),
      ec.getYearXunKong(),
    ),
    month: toPillar(
      'month',
      ec.getMonthGan(),
      ec.getMonthZhi(),
      ec.getMonthShiShenGan(),
      ec.getMonthHideGan(),
      ec.getMonthShiShenZhi(),
      ec.getMonthNaYin(),
      ec.getMonthDiShi(),
      ec.getMonthXunKong(),
    ),
    day: toPillar(
      'day',
      ec.getDayGan(),
      ec.getDayZhi(),
      '日主',
      ec.getDayHideGan(),
      ec.getDayShiShenZhi(),
      ec.getDayNaYin(),
      ec.getDayDiShi(),
      ec.getDayXunKong(),
    ),
    time: toPillar(
      'time',
      ec.getTimeGan(),
      ec.getTimeZhi(),
      ec.getTimeShiShenGan(),
      ec.getTimeHideGan(),
      ec.getTimeShiShenZhi(),
      ec.getTimeNaYin(),
      ec.getTimeDiShi(),
      ec.getTimeXunKong(),
    ),
  }

  const shenSha = computeShenSha(pillars, ec.getDayXunKong())
  for (const key of ['year', 'month', 'day', 'time'] as const) {
    pillars[key].shenSha = shenSha[key]
  }

  const shenShaRef: ShenShaRef = {
    yearZhi: pillars.year.zhi,
    dayZhi: pillars.day.zhi,
    dayGan: pillars.day.gan,
    dayXunKong: ec.getDayXunKong(),
  }

  const wuXing: PaipanResult['wuXing'] = { jin: 0, mu: 0, shui: 0, huo: 0, tu: 0 }
  for (const key of ['year', 'month', 'day', 'time'] as const) {
    wuXing[wuxingOfGan(pillars[key].gan)] += 1
    wuXing[wuxingOfZhi(pillars[key].zhi)] += 1
  }

  const yun = ec.getYun(YUN_GENDER[req.gender])
  const dayGan = pillars.day.gan
  const daYun: DaYun[] = yun
    .getDaYun()
    .filter((d) => d.getGanZhi())
    .slice(0, 8)
    .map((d) => {
      const startYear = d.getStartYear()
      const ganZhi = d.getGanZhi()
      return {
        ageRange: `${d.getStartAge()} - ${d.getStartAge() + 10} 岁`,
        ganZhi,
        yearRange: `${startYear} - ${startYear + 10}`,
        isCurrent: startYear <= now.getFullYear() && now.getFullYear() < startYear + 10,
        naYin: LunarUtil.NAYIN[ganZhi] ?? '',
        xunKong: d.getXunKong(),
        shiShen: LunarUtil.SHI_SHEN[dayGan + ganZhi[0]] ?? '',
        shenSha: computeExternalShenSha(shenShaRef, ganZhi),
      }
    })

  const startSolar = yun.getStartSolar()
  const firstDaYun = yun.getDaYun().find((d) => d.getGanZhi())
  const liuNianList: LiuNianItem[] = firstDaYun
    ? firstDaYun.getLiuNian(10).map((ln) => {
        const ganZhi = ln.getGanZhi()
        return {
          year: ln.getYear(),
          age: ln.getAge(),
          ganZhi,
          naYin: LunarUtil.NAYIN[ganZhi] ?? '',
          shiShen: LunarUtil.SHI_SHEN[dayGan + ganZhi[0]] ?? '',
          shenSha: computeExternalShenSha(shenShaRef, ganZhi),
        }
      })
    : []

  const currentYearGanZhi = today.getYearInGanZhiExact()
  const currentLiuNian: LiuNianInfo = {
    ganZhi: currentYearGanZhi,
    shenSha: computeExternalShenSha(shenShaRef, currentYearGanZhi),
  }

  return {
    solarText: solar.toYmdHms().slice(0, 16),
    lunarText: lunar.toString(),
    shengXiao: lunar.getYearShengXiao(),
    timeZhi: lunar.getTimeZhi(),
    pillars,
    taiYuan: ec.getTaiYuan(),
    taiYuanNaYin: ec.getTaiYuanNaYin(),
    mingGong: ec.getMingGong(),
    mingGongNaYin: ec.getMingGongNaYin(),
    shenGong: ec.getShenGong(),
    shenGongNaYin: ec.getShenGongNaYin(),
    wuXing,
    daYun,
    yunStart: {
      year: startSolar.getYear(),
      month: startSolar.getMonth(),
      day: startSolar.getDay(),
      hour: startSolar.getHour(),
      forward: yun.isForward(),
    },
    liuNianList,
    currentYearGanZhi: `${currentYearGanZhi}年 ${today.getMonthInGanZhiExact()}月 ${today.getDayInGanZhiExact()}日`,
    currentLiuNian,
    trueSolar,
  }
}

export function getTodayGanZhi(): string {
  const today = Lunar.fromDate(new Date())
  return `${today.getYearInGanZhiExact()}年 ${today.getMonthInGanZhiExact()}月 ${today.getDayInGanZhiExact()}日`
}

export function getGanZhiFor(offsetDays: number): { ganZhi: string; dateText: string } {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const lunar = Lunar.fromDate(d)
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return {
    ganZhi: `${lunar.getYearInGanZhiExact()}年 ${lunar.getMonthInGanZhiExact()}月 ${lunar.getDayInGanZhiExact()}日`,
    dateText: `${d.getMonth() + 1}月${d.getDate()}日 ${week}`,
  }
}
