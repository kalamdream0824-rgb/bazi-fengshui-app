import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { explain } from './explainer'
import { EXPLAIN_RULES, matchedRules } from './explainerRules'

const result = paipan({ gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })

describe('explainerRules 组合型解读（P0）', () => {
  it('旺衰/自坐/副星/大运十神四条规则全部命中', () => {
    for (const key of ['wangshuai', 'zizuo', 'fuxing', 'dayun-shishen']) {
      expect(EXPLAIN_RULES.find((r) => r.key === key)!.match(result), key).toBe(true)
    }
  })

  it('每盘至少 3 条组合型解读，且文本引用盘内证据', () => {
    const ex = explain(result)
    const combo = ex.blocks.flatMap((b) =>
      b.points.filter((p) => /旺衰|自坐|藏干|大运主题/.test(p.label)),
    )
    expect(combo.length).toBeGreaterThanOrEqual(3)

    const wangShuai = combo.find((p) => p.label.includes('旺衰'))!
    expect(wangShuai.text).toMatch(/分/)
    expect(wangShuai.text).toMatch(/传统称/)

    const ziZuo = combo.find((p) => p.label.includes('自坐'))!
    expect(ziZuo.text).toContain(result.pillars.day.zhi)

    const fuXing = combo.find((p) => p.label.includes('藏干'))!
    expect(fuXing.text).toContain('日支')

    const daYun = combo.find((p) => p.label.includes('大运主题'))!
    expect(daYun.text).toContain('比肩')
  })

  it('组合点按块取前 2，不刷屏', () => {
    for (const block of ['daymaster', 'pillars', 'dayun'] as const) {
      expect(matchedRules(result, block).length).toBeLessThanOrEqual(2)
    }
  })
})
