import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { buildReportHtml, reportFileName } from './reportPdf'

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

  it('文件名包含姓名与日期', () => {
    expect(reportFileName(request, result)).toBe('命书-测试-1995-10-08.pdf')
  })
})
