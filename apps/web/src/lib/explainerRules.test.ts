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
      b.points.filter((p) => /旺衰|自坐|藏干|大运主题|十神组合|神煞落宫/.test(p.label)),
    )
    expect(combo.length).toBeGreaterThanOrEqual(5)

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

  it('十神组合检测：1995-10-08 命中伤官配印与食神生财', () => {
    const ex = explain(result)
    const combo = ex.blocks.find((b) => b.key === 'shishen')!.points.find((p) => p.label === '十神组合')!
    expect(combo.text).toContain('伤官配印')
    expect(combo.text).toContain('食神生财')
    expect(combo.text).toMatch(/传统称/)
    expect(combo.text).toContain('仅供参考')
  })

  it('神煞落宫：按柱序输出前 2 个', () => {
    const ex = explain(result)
    const palace = ex.blocks.find((b) => b.key === 'shensha')!.points.find((p) => p.label === '神煞落宫')!
    expect(palace.text).toMatch(/「亡神」落在年柱/)
    expect(palace.text).toMatch(/仅供参考/)
  })

  it('胎元命宫身宫进解读：引用真实数据与词条', () => {
    const ex = explain(result)
    const point = ex.blocks.find((b) => b.key === 'pillars')!.points.find((p) => p.label.includes('胎元'))!
    expect(point.text).toContain('丙子')
    expect(point.text).toContain('己丑')
    expect(point.text).toContain('辛巳')
    expect(point.text).toMatch(/传统/)
    expect(point.text).toContain('仅供参考')
  })

  it('流年冲合：输出冲/合或无明显冲合，且引用流年地支', () => {
    const ex = explain(result)
    const point = ex.blocks.find((b) => b.key === 'liunian')!.points.find((p) => p.label.includes('流年冲合'))!
    expect(point.text).toContain(result.currentLiuNian!.ganZhi[1])
    expect(point.text).toMatch(/相冲|相合|无明显冲合/)
    expect(point.text).toContain('仅供参考')
  })

  it('组合点按块取前 2，不刷屏', () => {
    for (const block of ['daymaster', 'pillars', 'dayun'] as const) {
      expect(matchedRules(result, block).length).toBeLessThanOrEqual(2)
    }
  })
})
