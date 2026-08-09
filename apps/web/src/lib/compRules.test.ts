import { describe, expect, it } from 'vitest'
import { computeCompatibility, dayMasterRelation, nayinRelation, tianGanHe, zhiRelation } from './compRules'
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

describe('compRules 关系判定', () => {
  it('地支六合 / 三合 / 六冲', () => {
    expect(zhiRelation('子', '丑')).toBe('六合')
    expect(zhiRelation('申', '子')).toBe('三合')
    expect(zhiRelation('子', '午')).toBe('六冲')
    expect(zhiRelation('子', '寅')).toBe('—')
  })

  it('纳音五行生克：剑锋金 vs 大溪水 相生', () => {
    expect(nayinRelation('剑锋金', '大溪水')).toBe('相生')
    expect(nayinRelation('剑锋金', '炉中火')).toBe('相克')
  })

  it('天干五合：甲己合', () => {
    expect(tianGanHe('甲', '己')).toBe(true)
    expect(tianGanHe('甲', '乙')).toBe(false)
  })
})

describe('compRules.computeCompatibility', () => {
  it('水生木 + 纳音相生，但夫妻宫相冲 → 75 分（中等偏上）', () => {
    const male = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const female = paipan({ gender: 'female', solarDateTime: '1996-03-18T10:00:00', trueSolarTime: false })

    const comp = computeCompatibility(male, female)

    expect(comp.shengXiaoRelation).toBe('—')
    expect(comp.dayMasterRelation).toBe('相生')
    expect(comp.nayinRelation).toBe('相生')
    expect(comp.spousePalaceRelation).toBe('六冲')
    expect(comp.score).toBe(75)
    expect(comp.grade).toBe('中等偏上')
    expect(comp.tips.some((t) => t.includes('相生'))).toBe(true)
    expect(comp.tips.some((t) => t.includes('相冲'))).toBe(true)
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
