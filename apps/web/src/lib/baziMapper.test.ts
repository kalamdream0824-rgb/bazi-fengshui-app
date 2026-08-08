import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import type { PaipanRequest, PillarKey } from '@/types/bazi'

interface FixtureCase {
  id: string
  desc: string
  request: PaipanRequest
  expected: {
    lunarText: string
    shengXiao: string
    pillars: Record<PillarKey, { ganZhi: string; shiShen: string }>
  }
}

const fixtures: { cases: FixtureCase[] } = JSON.parse(
  readFileSync(resolve(process.cwd(), '../../contracts/fixtures/bazi-cases.json'), 'utf8'),
)

describe('baziMapper.paipan 对照夹具', () => {
  for (const fixture of fixtures.cases) {
    it(`${fixture.id}：${fixture.desc}`, () => {
      const result = paipan(fixture.request)

      expect(result.lunarText).toBe(fixture.expected.lunarText)
      expect(result.shengXiao).toBe(fixture.expected.shengXiao)

      for (const key of ['year', 'month', 'day', 'time'] as const) {
        expect(`${result.pillars[key].gan}${result.pillars[key].zhi}`, `${key} 柱干支`).toBe(
          fixture.expected.pillars[key].ganZhi,
        )
        expect(result.pillars[key].shiShen, `${key} 柱十神`).toBe(fixture.expected.pillars[key].shiShen)
      }
    })
  }
})

describe('baziMapper.paipan 结构完整性', () => {
  it('返回完整的五行计数（天干+地支本气，共 8）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const total = Object.values(result.wuXing).reduce((sum, n) => sum + n, 0)
    expect(total).toBe(8)
  })

  it('大运列表按起运排序且包含当前大运标记', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(result.daYun.length).toBeGreaterThan(0)
    expect(result.daYun.some((d) => d.isCurrent)).toBe(true)
    expect(result.daYun[0].ganZhi).toBe('甲申')
  })

  it('女命大运与男命不同（阴阳顺逆）', () => {
    const male = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    const female = paipan({ gender: 'female', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(male.daYun[0].ganZhi).not.toBe(female.daYun[0].ganZhi)
  })

  it('真太阳时校正改变时辰：深圳 13:05 由未时变为午时', () => {
    const withTrueSolar = paipan({
      gender: 'male',
      solarDateTime: '1995-10-08T13:05:00',
      trueSolarTime: true,
      birthPlace: '广东省 深圳市',
    })
    const without = paipan({ gender: 'male', solarDateTime: '1995-10-08T13:05:00', trueSolarTime: false })

    expect(without.pillars.time.gan + without.pillars.time.zhi).toBe('丁未')
    expect(withTrueSolar.pillars.time.gan + withTrueSolar.pillars.time.zhi).toBe('丙午')
    expect(withTrueSolar.trueSolar?.longitude).toBe(114.05)
    expect(withTrueSolar.trueSolar?.adjusted).toContain('12:54')
  })
})
