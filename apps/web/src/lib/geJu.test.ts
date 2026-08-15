import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { computeGeJu, computeWangShuai, wuxingRelation } from './geJu'

describe('wuxingRelation 五行关系', () => {
  it('以木日主为基准：水印 / 木比 / 火食伤 / 土财 / 金官杀', () => {
    expect(wuxingRelation('mu', 'shui')).toBe('yin')
    expect(wuxingRelation('mu', 'mu')).toBe('bi')
    expect(wuxingRelation('mu', 'huo')).toBe('shishen')
    expect(wuxingRelation('mu', 'tu')).toBe('cai')
    expect(wuxingRelation('mu', 'jin')).toBe('guansha')
  })
})

describe('computeGeJu 月令格局', () => {
  it('1995-10-08 男命：酉藏辛不透 → 取本气正印 → 正印格', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const geJu = computeGeJu(result)
    expect(geJu).toMatchObject({ monthGanZhi: '乙酉', gan: '辛', shiShen: '正印', name: '正印格', trans: false })
  })

  it('2024-02-04 17:00 男命：寅藏甲透于年干 → 七杀格（透干）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '2024-02-04T17:00:00', trueSolarTime: false })
    const geJu = computeGeJu(result)
    expect(geJu).toMatchObject({ monthGanZhi: '丙寅', gan: '甲', shiShen: '七杀', name: '七杀格', trans: true })
  })

  it('1996-03-18 女命：卯藏乙不透 → 劫财本气 → 月劫格', () => {
    const result = paipan({ gender: 'female', solarDateTime: '1996-03-18T10:00:00', trueSolarTime: false })
    expect(computeGeJu(result)).toMatchObject({ name: '月劫格', shiShen: '劫财', trans: false })
  })
})

describe('computeWangShuai 日主旺衰粗判', () => {
  it('1995-10-08 男命：得令+得地 → 4 分偏强', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const ws = computeWangShuai(result)
    expect(ws.score).toBe(4)
    expect(ws.level).toBe('偏强')
    expect(ws.items.some((i) => i.label === '得令' && i.score === 2)).toBe(true)
  })

  it('2024-02-04 17:00 男命：不得令但得地 → 2.5 分中和', () => {
    const result = paipan({ gender: 'male', solarDateTime: '2024-02-04T17:00:00', trueSolarTime: false })
    const ws = computeWangShuai(result)
    expect(ws.score).toBe(2.5)
    expect(ws.level).toBe('中和')
  })

  it('1990-01-01 23:30 男命：月干比劫+1 → 3 分中和', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1990-01-01T23:30:00', trueSolarTime: false })
    const ws = computeWangShuai(result)
    expect(ws.score).toBe(3)
    expect(ws.level).toBe('中和')
    expect(ws.items.some((i) => i.label === '月干' && i.score === 1)).toBe(true)
  })
})
