import { LunarUtil } from 'lunar-javascript'
import { timeToShichen } from './datePicker'
import { ziZuoOf } from './baziMapper'
import { computeExternalShenSha, computeShenSha, type ShenShaRef } from './shenSha'
import { formatAdjusted, longitudeOf, trueSolarTime } from './trueSolarTime'
import type { PaipanRequest, PaipanResult, TrueSolarInfo } from '@/types/bazi'

/** 后端第一版不实现神煞，由前端同一套规则补充（口径与 Mock 一致） */
export function enrichResult(result: PaipanResult): PaipanResult {
  const shenSha = computeShenSha(result.pillars, result.pillars.day.xunKong)
  for (const key of ['year', 'month', 'day', 'time'] as const) {
    result.pillars[key].shenSha = shenSha[key]
  }

  const ref: ShenShaRef = {
    yearZhi: result.pillars.year.zhi,
    dayZhi: result.pillars.day.zhi,
    dayGan: result.pillars.day.gan,
    dayXunKong: result.pillars.day.xunKong,
  }
  result.daYun.forEach((d) => {
    d.shenSha = computeExternalShenSha(ref, d.ganZhi)
    if (!d.naYin) {
      d.naYin = LunarUtil.NAYIN[d.ganZhi] ?? ''
    }
    if (!d.shiShen) {
      d.shiShen = LunarUtil.SHI_SHEN[result.pillars.day.gan + d.ganZhi[0]] ?? ''
    }
    if (!d.starFortune) {
      d.starFortune = ziZuoOf(result.pillars.day.gan, d.ganZhi[1])
    }
  })
  if (result.currentLiuNian) {
    result.currentLiuNian.shenSha = computeExternalShenSha(ref, result.currentLiuNian.ganZhi)
  }
  result.liuNianList?.forEach((ln) => {
    ln.shenSha = computeExternalShenSha(ref, ln.ganZhi)
    if (!ln.naYin) {
      ln.naYin = LunarUtil.NAYIN[ln.ganZhi] ?? ''
    }
    if (!ln.shiShen) {
      ln.shiShen = LunarUtil.SHI_SHEN[result.pillars.day.gan + ln.ganZhi[0]] ?? ''
    }
    if (!ln.starFortune) {
      ln.starFortune = ziZuoOf(result.pillars.day.gan, ln.ganZhi[1])
    }
  })
  return result
}

/** 后端第一版不实现真太阳时：前端校正后提交，并把校正信息挂到结果上 */
export function adjustRequestForTrueSolar(req: PaipanRequest): { request: PaipanRequest; trueSolar?: TrueSolarInfo } {
  if (!req.trueSolarTime || !req.birthPlace) {
    return { request: req }
  }
  const lng = longitudeOf(req.birthPlace)
  if (lng == null) {
    return { request: req }
  }

  const [datePart, timePart = '00:00'] = req.solarDateTime.split('T')
  const [y, m, d] = datePart.split('-').map(Number)
  const [hh, mm] = timePart.split(':').map(Number)
  const base = new Date(y, (m || 1) - 1, d || 1, hh || 0, mm || 0, 0)
  const r = trueSolarTime(base, lng)

  const originalShichen = timeToShichen(base.getHours())
  const adjustedShichen = timeToShichen(r.adjusted.getHours())
  return {
    request: {
      ...req,
      solarDateTime: formatAdjusted(r.adjusted),
      trueSolarTime: false,
    },
    trueSolar: {
      original: req.solarDateTime.slice(0, 16),
      adjusted: formatAdjusted(r.adjusted),
      offsetMinutes: Math.round(r.offsetMinutes),
      eotMinutes: Math.round(r.eotMinutes * 10) / 10,
      longitude: lng,
      originalShichen,
      adjustedShichen,
      boundaryChanged: originalShichen !== adjustedShichen,
    },
  }
}
