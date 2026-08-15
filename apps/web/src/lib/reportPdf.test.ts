import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { buildReportHtml, reportFileName } from './reportPdf'
import type { PaipanResult } from '@/types/bazi'

describe('reportPdf', () => {
  const request = { name: '测试', gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
  const result = paipan(request)

  it('封面与三个页面内容完整', () => {
    const html = buildReportHtml(request, result)
    expect(html).toContain('命 书')
    expect(html).toContain('一、命盘概览')
    expect(html).toContain('二、大运与参考')
    expect(html).toContain('仅供传统文化研究参考')
    expect(html.match(/class="page"/g)?.length).toBe(3)
  })

  it('四柱与真太阳时信息写入', () => {
    const html = buildReportHtml(request, result)
    expect(html).toContain('乙亥')
    expect(html).toContain('壬申')
    expect(html).toContain('剑锋金')
    expect(html).toContain('甲申')
  })

  it('副星/空亡行与胎元命宫身宫信息写入', () => {
    const html = buildReportHtml(request, result)
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
    const html = buildReportHtml(request, legacy)
    expect(html).not.toContain('胎元 undefined')
    expect(html).toContain('二、大运与参考')
  })

  it('文件名包含姓名与日期', () => {
    expect(reportFileName(request, result)).toBe('命书-测试-1995-10-08.pdf')
  })
})
