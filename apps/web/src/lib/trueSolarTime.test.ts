import { describe, expect, it } from 'vitest'
import { equationOfTimeMinutes, formatAdjusted, longitudeOf, trueSolarTime } from './trueSolarTime'

describe('trueSolarTime', () => {
  it('城市经度解析：深圳市 114.05，成都市 104.07', () => {
    expect(longitudeOf('广东省 深圳市')).toBe(114.05)
    expect(longitudeOf('四川省 成都市')).toBe(104.07)
  })

  it('城市缺失时回退到省会经度', () => {
    expect(longitudeOf('青海省')).toBe(101.78)
  })

  it('无法解析时返回 null', () => {
    expect(longitudeOf('火星')).toBeNull()
  })

  it('均时差近似值：8 月约 -5 分钟，10 月约 +13 分钟', () => {
    const aug = equationOfTimeMinutes(new Date(2026, 7, 8))
    const oct = equationOfTimeMinutes(new Date(1995, 9, 8))
    expect(aug).toBeGreaterThan(-7)
    expect(aug).toBeLessThan(-3)
    expect(oct).toBeGreaterThan(11)
    expect(oct).toBeLessThan(16)
  })

  it('深圳 13:05 校正后进入午时（12:54 左右）', () => {
    const base = new Date(1995, 9, 8, 13, 5, 0)
    const r = trueSolarTime(base, 114.05)
    expect(formatAdjusted(r.adjusted).slice(11)).toBe('12:54')
    expect(r.offsetMinutes).toBeCloseTo(-23.8, 1)
  })
})
