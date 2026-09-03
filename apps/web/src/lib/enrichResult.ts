import { LunarUtil } from 'lunar-javascript'
import { ziZuoOf } from './baziMapper'
import { computeExternalShenSha, computeShenSha, type ShenShaRef } from './shenSha'
import type { PaipanResult } from '@/types/bazi'

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
    ln.liuYue?.forEach((ly) => {
      ly.shenSha = computeExternalShenSha(ref, ly.ganZhi)
      if (!ly.naYin) {
        ly.naYin = LunarUtil.NAYIN[ly.ganZhi] ?? ''
      }
      if (!ly.shiShen) {
        ly.shiShen = LunarUtil.SHI_SHEN[result.pillars.day.gan + ly.ganZhi[0]] ?? ''
      }
    })
  })
  result.xiaoYunList?.forEach((xy) => {
    xy.shenSha = computeExternalShenSha(ref, xy.ganZhi)
    if (!xy.naYin) {
      xy.naYin = LunarUtil.NAYIN[xy.ganZhi] ?? ''
    }
    if (!xy.shiShen) {
      xy.shiShen = LunarUtil.SHI_SHEN[result.pillars.day.gan + xy.ganZhi[0]] ?? ''
    }
  })
  result.currentYearLiuYue?.forEach((ly) => {
    ly.shenSha = computeExternalShenSha(ref, ly.ganZhi)
    if (!ly.naYin) {
      ly.naYin = LunarUtil.NAYIN[ly.ganZhi] ?? ''
    }
    if (!ly.shiShen) {
      ly.shiShen = LunarUtil.SHI_SHEN[result.pillars.day.gan + ly.ganZhi[0]] ?? ''
    }
  })
  return result
}
