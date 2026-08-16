import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { explain } from './explainer'
import { buildReportHtml, reportFileName } from './reportPdf'
import type { PaipanResult } from '@/types/bazi'

describe('reportPdf', () => {
  const request = { name: '测试', gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
  const result = paipan(request)
  const explanation = explain(result)

  it('封面与五章结构完整（6 页）', () => {
    const html = buildReportHtml(request, result, explanation)
    expect(html).toContain('命 书')
    expect(html).toContain('壹、命盘概览')
    expect(html).toContain('贰、五行与用神')
    expect(html).toContain('叁、大运流年')
    expect(html).toContain('肆、十神详析')
    expect(html).toContain('伍、综合建议')
    expect(html).toContain('仅供传统文化研究参考')
    expect(html.match(/class="page"/g)?.length).toBe(6)
  })

  it('组合型解读写入（档案/旺衰/大运/十神）', () => {
    const html = buildReportHtml(request, result, explanation)
    expect(html).toContain('档案：')
    expect(html).toContain('旺衰判定为')
    expect(html).toContain('当前大运解读')
    expect(html).toContain('十神性格')
    expect(html).toContain('神煞参考')
    expect(html).toContain('仅供娱乐与参考')
  })

  it('四柱与真太阳时信息写入', () => {
    const html = buildReportHtml(request, result, explanation)
    expect(html).toContain('乙亥')
    expect(html).toContain('壬申')
    expect(html).toContain('剑锋金')
    expect(html).toContain('甲申')
  })

  it('副星/空亡行与胎元命宫身宫信息写入', () => {
    const html = buildReportHtml(request, result, explanation)
    expect(html).toContain('比肩')
    expect(html).toContain('食神')
    expect(html).toContain('戌亥')
    expect(html).toContain('胎元 丙子（涧下水）')
    expect(html).toContain('命宫 己丑（霹雳火）')
    expect(html).toContain('身宫 辛巳（白蜡金）')
    expect(html).toContain('长生')
    expect(html).toContain('冠带')
  })

  it('旧快照（缺胎元/命宫/身宫字段）仍可导出且不渲染空字段', () => {
    const legacy = JSON.parse(
      JSON.stringify({
        ...result,
        taiYuan: undefined,
        taiYuanNaYin: undefined,
        mingGong: undefined,
        mingGongNaYin: undefined,
        shenGong: undefined,
        shenGongNaYin: undefined,
      }),
    ) as PaipanResult
    const html = buildReportHtml(request, legacy, explain(legacy))
    expect(html).not.toContain('胎元 undefined')
    expect(html).toContain('叁、大运流年')
  })

  it('文件名包含姓名与日期', () => {
    expect(reportFileName(request, result)).toBe('命书-测试-1995-10-08.pdf')
  })
})
