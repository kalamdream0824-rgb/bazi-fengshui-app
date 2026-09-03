import { describe, expect, it } from 'vitest'
import { enrichResult } from './enrichResult'
import { paipan } from './baziMapper'

describe('enrichResult', () => {
  it('补充四柱/大运/流年神煞（口径与 Mock 一致）', () => {
    const base = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const result = enrichResult({
      ...base,
      pillars: {
        year: { ...base.pillars.year, shenSha: [] },
        month: { ...base.pillars.month, shenSha: [] },
        day: { ...base.pillars.day, shenSha: [] },
        time: { ...base.pillars.time, shenSha: [] },
      },
      daYun: base.daYun.map((d) => ({ ...d, shenSha: [] })),
      currentLiuNian: base.currentLiuNian ? { ...base.currentLiuNian, shenSha: [] } : undefined,
    })

    expect(result.pillars.year.shenSha).toContain('禄神')
    expect(result.daYun[0].shenSha).toContain('劫煞')
    expect(Array.isArray(result.currentLiuNian?.shenSha)).toBe(true)
  })

  it('后端未返回大运/流年十神与神煞时由前端补齐（口径与 Mock 一致）', () => {
    const base = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const result = enrichResult({
      ...base,
      daYun: base.daYun.map((d) => ({ ...d, naYin: '', shiShen: '', starFortune: '', shenSha: [] })),
      liuNianList: base.liuNianList.map((ln) => ({
        ...ln,
        naYin: '',
        shiShen: '',
        starFortune: '',
        liuYue: ln.liuYue.map((ly) => ({ ...ly, naYin: '', shiShen: '', shenSha: [] })),
        shenSha: [],
      })),
      xiaoYunList: base.xiaoYunList.map((xy) => ({ ...xy, naYin: '', shiShen: '', shenSha: [] })),
      currentYearLiuYue: base.currentYearLiuYue.map((ly) => ({ ...ly, naYin: '', shiShen: '', shenSha: [] })),
    })

    expect(result.daYun[0].naYin).toBe('泉中水')
    expect(result.daYun[0].shiShen).toBe('食神')
    expect(result.daYun[0].starFortune).toBe('长生')
    expect(result.liuNianList[0].naYin).toBe('泉中水')
    expect(result.liuNianList[0].shiShen).toBe('伤官')
    expect(result.liuNianList[0].starFortune).toBe('沐浴')
    expect(result.liuNianList[0].xunKong).toBe('午未')
    expect(result.liuNianList[0].liuYue[0].naYin).toBe('城头土')
    expect(result.liuNianList[0].liuYue[0].shiShen).toBe('七杀')
    expect(result.xiaoYunList[0].naYin).toBe('山下火')
    expect(result.xiaoYunList[0].shiShen).toBe('偏财')
    expect(result.currentYearLiuYue[0].ganZhi).toBeTruthy()
    expect(result.currentYearLiuYue[0].shenSha.length).toBeGreaterThanOrEqual(0)
    expect(result.liuNianList[0].shenSha.length).toBeGreaterThanOrEqual(0)
  })
})
