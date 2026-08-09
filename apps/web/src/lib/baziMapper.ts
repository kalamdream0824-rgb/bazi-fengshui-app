import { Lunar, Solar } from 'lunar-javascript'
import type { DaYun, PaipanRequest, PaipanResult, Pillar, PillarKey, WuxingKey } from '@/types/bazi'
import { computeShenSha } from './shenSha'
import { formatAdjusted, longitudeOf, trueSolarTime } from './trueSolarTime'
import { GAN_WUXING, ZHI_WUXING } from './wuxing'

const YUN_GENDER = { male: 1, female: 0 } as const

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
  naYin: string,
  diShi: string,
  xunKong: string,
): Pillar {
  return {
    label,
    gan,
    zhi,
    shiShen,
    hideGan: hideGan.map((g) => ({ gan: g, wuxing: wuxingOfGan(g) })),
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
      }
    }
  }

  const solar = Solar.fromYmdHms(solarDate.getFullYear(), solarDate.getMonth() + 1, solarDate.getDate(), solarDate.getHours(), solarDate.getMinutes(), 0)
  const lunar = solar.getLunar()
  const ec = lunar.getEightChar()

  const now = new Date()
  const today = Lunar.fromDate(now)

  const pillars: PaipanResult['pillars'] = {
    year: toPillar('year', ec.getYearGan(), ec.getYearZhi(), ec.getYearShiShenGan(), ec.getYearHideGan(), ec.getYearNaYin(), ec.getYearDiShi(), ec.getYearXunKong()),
    month: toPillar('month', ec.getMonthGan(), ec.getMonthZhi(), ec.getMonthShiShenGan(), ec.getMonthHideGan(), ec.getMonthNaYin(), ec.getMonthDiShi(), ec.getMonthXunKong()),
    day: toPillar('day', ec.getDayGan(), ec.getDayZhi(), '日主', ec.getDayHideGan(), ec.getDayNaYin(), ec.getDayDiShi(), ec.getDayXunKong()),
    time: toPillar('time', ec.getTimeGan(), ec.getTimeZhi(), ec.getTimeShiShenGan(), ec.getTimeHideGan(), ec.getTimeNaYin(), ec.getTimeDiShi(), ec.getTimeXunKong()),
  }

  const shenSha = computeShenSha(pillars, ec.getDayXunKong())
  for (const key of ['year', 'month', 'day', 'time'] as const) {
    pillars[key].shenSha = shenSha[key]
  }

  const wuXing: PaipanResult['wuXing'] = { jin: 0, mu: 0, shui: 0, huo: 0, tu: 0 }
  for (const key of ['year', 'month', 'day', 'time'] as const) {
    wuXing[wuxingOfGan(pillars[key].gan)] += 1
    wuXing[wuxingOfZhi(pillars[key].zhi)] += 1
  }

  const yun = ec.getYun(YUN_GENDER[req.gender])
  const daYun: DaYun[] = yun
    .getDaYun()
    .filter((d) => d.getGanZhi())
    .slice(0, 8)
    .map((d) => {
      const startYear = d.getStartYear()
      return {
        ageRange: `${d.getStartAge()} - ${d.getStartAge() + 10} 岁`,
        ganZhi: d.getGanZhi(),
        yearRange: `${startYear} - ${startYear + 10}`,
        isCurrent: startYear <= now.getFullYear() && now.getFullYear() < startYear + 10,
      }
    })

  return {
    solarText: solar.toYmdHms().slice(0, 16),
    lunarText: lunar.toString(),
    shengXiao: lunar.getYearShengXiao(),
    timeZhi: lunar.getTimeZhi(),
    pillars,
    wuXing,
    daYun,
    currentYearGanZhi: `${today.getYearInGanZhiExact()}年 ${today.getMonthInGanZhiExact()}月 ${today.getDayInGanZhiExact()}日`,
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
