import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { computeWuxingDetail } from './wuxing'

describe('computeWuxingDetail 五行明细', () => {
  it('本气计数 = 天干 + 地支本气（共 8）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const detail = computeWuxingDetail(result)
    expect(detail.reduce((sum, d) => sum + d.stemCount, 0)).toBe(8)
  })

  it('含藏干加权计入藏干（本气1/中气0.5/余气0.3）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const detail = computeWuxingDetail(result)
    const jin = detail.find((d) => d.key === 'jin')!
    const shui = detail.find((d) => d.key === 'shui')!
    // 天干无金：金加权=酉辛+申庚两个本气（2），水加权=天干壬1+亥壬1+申壬0.5=2.5
    expect(jin.weightedCount).toBe(jin.stemCount)
    expect(shui.weightedCount).toBeGreaterThan(shui.stemCount)
    expect(shui.weightedCount).toBe(2.5)
  })

  it('缺失五行正确标记（2024-02-04 15:00 无火）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '2024-02-04T15:00:00', trueSolarTime: false })
    const detail = computeWuxingDetail(result)
    expect(detail.find((d) => d.key === 'huo')!.missing).toBe(true)
    expect(detail.filter((d) => d.missing)).toHaveLength(1)
  })
})
