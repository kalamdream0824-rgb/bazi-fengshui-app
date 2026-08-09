import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { computeShenSha } from './shenSha'
import type { Pillar, PillarKey } from '@/types/bazi'

function fakePillar(gan: string, zhi: string): Pillar {
  return { label: 'day', gan, zhi, shiShen: '', hideGan: [], naYin: '', diShi: '', xunKong: '', shenSha: [] }
}

describe('shenSha.computeShenSha', () => {
  it('已知命例：1995-10-08（乙亥 乙酉 壬申 丁未）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })

    expect(result.pillars.year.shenSha).toEqual(expect.arrayContaining(['禄神', '空亡']))
    expect(result.pillars.month.shenSha).toEqual(expect.arrayContaining(['桃花']))
    expect(result.pillars.day.shenSha).toEqual(expect.arrayContaining(['劫煞']))
    expect(result.pillars.time.shenSha).toEqual(expect.arrayContaining(['华盖']))
  })

  it('魁罡：日柱庚辰', () => {
    const pillars: Record<PillarKey, Pillar> = {
      year: fakePillar('甲', '子'),
      month: fakePillar('乙', '丑'),
      day: fakePillar('庚', '辰'),
      time: fakePillar('丙', '寅'),
    }
    const result = computeShenSha(pillars, '')
    expect(result.day).toEqual(expect.arrayContaining(['魁罡']))
  })

  it('桃花：申子辰见酉', () => {
    const pillars: Record<PillarKey, Pillar> = {
      year: fakePillar('甲', '申'),
      month: fakePillar('乙', '酉'),
      day: fakePillar('丙', '子'),
      time: fakePillar('丁', '亥'),
    }
    const result = computeShenSha(pillars, '')
    expect(result.month).toEqual(expect.arrayContaining(['桃花']))
  })
})
