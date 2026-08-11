import { describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { explain, explainBlock, SHISHEN_TIP, TERM_TIPS } from './explainer'

const result = paipan({ gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })

const FORBIDDEN = ['一定', '必定', '注定', '发财', '离婚', '能活', '克夫', '克妻']

describe('explainer 新人解读引擎', () => {
  it('生成总览与六块解读', () => {
    const ex = explain(result)
    expect(ex.overview.length).toBeGreaterThanOrEqual(2)
    expect(ex.overview[0]).toContain('日主')
    expect(ex.overview[0]).toContain(result.pillars.day.gan)
    expect(ex.blocks.map((b) => b.key)).toEqual(['daymaster', 'pillars', 'shishen', 'shensha', 'dayun', 'liunian'])
  })

  it('文案保持温和参考、不含断言词', () => {
    const ex = explain(result)
    const allText = [...ex.overview, ...ex.blocks.flatMap((b) => b.points.map((p) => p.text))].join('')
    expect(allText).toMatch(/参考|传统命理/)
    for (const word of FORBIDDEN) {
      expect(allText).not.toContain(word)
    }
  })

  it('日主与十神块引用真实排盘数据', () => {
    const ex = explain(result)
    const dayMaster = ex.blocks.find((b) => b.key === 'daymaster')!
    expect(dayMaster.points[0].text).toContain(result.pillars.day.gan)
    const shiShen = ex.blocks.find((b) => b.key === 'shishen')!
    expect(shiShen.points.some((p) => p.label.includes(result.pillars.day.shiShen))).toBe(true)
  })

  it('出现的神煞都能给出参考词条', () => {
    const ex = explain(result)
    const shenSha = ex.blocks.find((b) => b.key === 'shensha')!
    for (const p of shenSha.points) {
      expect(p.text).toMatch(/传统|参考/)
    }
  })

  it('术语词库覆盖核心术语与十神', () => {
    for (const term of ['天干', '地支', '五行', '日主', '大运', '流年', '主星', '藏干', '纳音', '星运', '神煞']) {
      expect(TERM_TIPS[term]).toBeTruthy()
    }
    expect(SHISHEN_TIP['正财']).toBeTruthy()
    expect(Object.keys(SHISHEN_TIP)).toHaveLength(10)
  })

  it('explainBlock 按 key 取块', () => {
    const ex = explain(result)
    expect(explainBlock(ex, 'dayun')?.key).toBe('dayun')
    expect(explainBlock(ex, 'dayun')!.points.length).toBeGreaterThan(0)
  })
})
