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
    taiYuan: string
    mingGong: string
    shenGong: string
    pillars: Record<PillarKey, { ganZhi: string; shiShen: string; hideGanShiShen: string[]; ziZuo: string }>
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
      expect(result.taiYuan).toBe(fixture.expected.taiYuan)
      expect(result.mingGong).toBe(fixture.expected.mingGong)
      expect(result.shenGong).toBe(fixture.expected.shenGong)

      for (const key of ['year', 'month', 'day', 'time'] as const) {
        expect(`${result.pillars[key].gan}${result.pillars[key].zhi}`, `${key} 柱干支`).toBe(
          fixture.expected.pillars[key].ganZhi,
        )
        expect(result.pillars[key].shiShen, `${key} 柱十神`).toBe(fixture.expected.pillars[key].shiShen)
        expect(result.pillars[key].hideGan.map((h) => h.shiShen), `${key} 柱副星`).toEqual(
          fixture.expected.pillars[key].hideGanShiShen,
        )
        expect(result.pillars[key].ziZuo, `${key} 柱自坐`).toBe(fixture.expected.pillars[key].ziZuo)
      }
    })
  }
})

describe('baziMapper.paipan 结构完整性', () => {
  it('四柱含副星（藏干十神）且顺序与藏干一致', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(result.pillars.year.hideGan.map((h) => h.gan)).toEqual(['壬', '甲'])
    expect(result.pillars.year.hideGan.map((h) => h.shiShen)).toEqual(['比肩', '食神'])
    expect(result.pillars.time.hideGan.map((h) => h.shiShen)).toEqual(['正官', '正财', '伤官'])
  })

  it('返回胎元/命宫/身宫及纳音', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(result.taiYuan).toBe('丙子')
    expect(result.taiYuanNaYin).toBe('涧下水')
    expect(result.mingGong).toBe('己丑')
    expect(result.mingGongNaYin).toBe('霹雳火')
    expect(result.shenGong).toBe('辛巳')
    expect(result.shenGongNaYin).toBe('白蜡金')
  })

  it('四柱旬空完整且日柱旬空不含日支', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(result.pillars.year.xunKong).toBe('申酉')
    expect(result.pillars.month.xunKong).toBe('午未')
    expect(result.pillars.day.xunKong).toBe('戌亥')
    expect(result.pillars.time.xunKong).toBe('寅卯')
    expect(result.pillars.day.xunKong).not.toContain(result.pillars.day.zhi)
  })

  it('自坐以本柱天干为太极点（与星运日干太极点口径不同）', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(result.pillars.year.ziZuo).toBe('死')
    expect(result.pillars.month.ziZuo).toBe('绝')
    expect(result.pillars.day.ziZuo).toBe('长生')
    expect(result.pillars.time.ziZuo).toBe('冠带')
    // 同年柱：星运以日干壬查亥支为临官，自坐以年干乙查亥支为死——证明太极点不同
    expect(result.pillars.year.diShi).toBe('临官')
    expect(result.pillars.year.ziZuo).not.toBe(result.pillars.year.diShi)
  })

  it('自坐与排盘软件示例一致：戊寅丙辰丁未乙巳 → 长生 冠带 冠带 沐浴', () => {
    const result = paipan({ gender: 'female', solarDateTime: '1998-04-30T09:20:00', trueSolarTime: false })
    expect(result.pillars.year.ziZuo).toBe('长生')
    expect(result.pillars.month.ziZuo).toBe('冠带')
    expect(result.pillars.day.ziZuo).toBe('冠带')
    expect(result.pillars.time.ziZuo).toBe('沐浴')
  })

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
    expect(withTrueSolar.trueSolar?.boundaryChanged).toBe(true)
    expect(withTrueSolar.trueSolar?.originalShichen).toBe('未')
    expect(withTrueSolar.trueSolar?.adjustedShichen).toBe('午')
  })

  it('真太阳时未跨边界时 boundaryChanged 为 false', () => {
    const result = paipan({
      gender: 'male',
      solarDateTime: '1995-10-08T14:30:00',
      trueSolarTime: true,
      birthPlace: '广东省 深圳市',
    })
    expect(result.trueSolar?.boundaryChanged).toBe(false)
  })

  it('大运神煞与当年流年神煞已计算', () => {
    const result = paipan({ gender: 'male', solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
    expect(result.daYun[0].shenSha).toEqual(expect.arrayContaining(['劫煞']))
    expect(result.currentLiuNian?.ganZhi).toBeTruthy()
    expect(Array.isArray(result.currentLiuNian?.shenSha)).toBe(true)
  })
})
