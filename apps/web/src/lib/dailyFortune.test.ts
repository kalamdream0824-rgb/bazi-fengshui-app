import { describe, expect, it } from 'vitest'
import { getAlmanac } from './almanac'
import { paipan } from './baziMapper'
import { dailyFortune, shiShenOf } from './dailyFortune'

const ALM = getAlmanac(new Date(2026, 7, 9, 12, 0, 0))
const REQ = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }

describe('dailyFortune', () => {
  it('十神映射：壬日主对丁→正财、乙→伤官、壬→比肩', () => {
    expect(shiShenOf('丁', '壬')).toBe('正财')
    expect(shiShenOf('乙', '壬')).toBe('伤官')
    expect(shiShenOf('壬', '壬')).toBe('比肩')
  })

  it('确定性：同输入同输出', () => {
    const result = paipan(REQ)
    expect(JSON.stringify(dailyFortune(result, ALM))).toBe(JSON.stringify(dailyFortune(result, ALM)))
  })

  it('星级始终在 1-5 之间', () => {
    const result = paipan(REQ)
    const f = dailyFortune(result, ALM)
    expect(f.star).toBeGreaterThanOrEqual(1)
    expect(f.star).toBeLessThanOrEqual(5)
  })

  it('有命盘生成个性化运势，无命盘仅黄历参考', () => {
    const result = paipan(REQ)
    expect(dailyFortune(result, ALM).personalized).toBe(true)
    expect(dailyFortune(null, ALM).personalized).toBe(false)
  })
})
