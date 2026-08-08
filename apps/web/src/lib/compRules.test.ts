import { describe, expect, it } from 'vitest'
import { computeCompatibility, dayMasterRelation } from './compRules'
import { paipan } from './baziMapper'

describe('compRules.dayMasterRelation', () => {
  it('五行相生：木生火', () => {
    expect(dayMasterRelation('mu', 'huo')).toBe('相生')
  })

  it('五行相克：木克土', () => {
    expect(dayMasterRelation('mu', 'tu')).toBe('相克')
  })

  it('同类为比和', () => {
    expect(dayMasterRelation('jin', 'jin')).toBe('比和')
  })
})

describe('compRules.computeCompatibility', () => {
  it('水生木 → 中等偏上（75 分）', () => {
    const male = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const female = paipan({ gender: 'female', solarDateTime: '1996-03-18T10:00:00', trueSolarTime: false })

    const comp = computeCompatibility(male, female)

    expect(comp.shengXiaoRelation).toBe('—')
    expect(comp.dayMasterRelation).toBe('相生')
    expect(comp.score).toBe(75)
    expect(comp.grade).toBe('中等偏上')
    expect(comp.tips.some((t) => t.includes('相生'))).toBe(true)
  })

  it('猪虎六合识别', () => {
    const male = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const female = paipan({ gender: 'female', solarDateTime: '1998-05-20T10:00:00', trueSolarTime: false })

    const comp = computeCompatibility(male, female)

    expect(comp.shengXiaoRelation).toBe('六合')
  })

  it('评分结果始终在 0-100 之间', () => {
    const a = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const b = paipan({ gender: 'female', solarDateTime: '1990-01-01T23:30:00', trueSolarTime: false })
    const comp = computeCompatibility(a, b)
    expect(comp.score).toBeGreaterThanOrEqual(0)
    expect(comp.score).toBeLessThanOrEqual(100)
  })
})
