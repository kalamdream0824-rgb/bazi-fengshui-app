import { describe, expect, it } from 'vitest'
import { adjustRequestForTrueSolar, enrichResult } from './enrichResult'
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
})

describe('adjustRequestForTrueSolar', () => {
  it('深圳 13:05 提交校正后时间并携带边界标记', () => {
    const req = {
      gender: 'male' as const,
      solarDateTime: '1995-10-08T13:05:00',
      trueSolarTime: true,
      birthPlace: '广东省 深圳市',
    }
    const { request, trueSolar } = adjustRequestForTrueSolar(req)
    expect(request.trueSolarTime).toBe(false)
    expect(request.solarDateTime).toContain('12:54')
    expect(trueSolar?.boundaryChanged).toBe(true)
    expect(trueSolar?.originalShichen).toBe('未')
    expect(trueSolar?.adjustedShichen).toBe('午')
  })

  it('未开启真太阳时原样提交', () => {
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    const { request, trueSolar } = adjustRequestForTrueSolar(req)
    expect(request.solarDateTime).toBe('1995-10-08T14:30:00')
    expect(trueSolar).toBeUndefined()
  })
})
