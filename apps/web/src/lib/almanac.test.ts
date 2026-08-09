import { describe, expect, it } from 'vitest'
import { getAlmanac } from './almanac'

describe('almanac', () => {
  it('固定日期（2026-08-09 乙卯日）返回真实宜忌与冲煞', () => {
    const alm = getAlmanac(new Date(2026, 7, 9, 12, 0, 0))
    expect(alm.ganZhi).toBe('乙卯')
    expect(alm.yi.length).toBeGreaterThan(0)
    expect(alm.ji.length).toBeGreaterThan(0)
    expect(alm.chongDesc).not.toBe('')
    expect(alm.sha).not.toBe('')
  })
})
