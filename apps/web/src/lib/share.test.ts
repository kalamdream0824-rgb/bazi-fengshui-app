import { describe, expect, it } from 'vitest'
import { paipan } from './baziMapper'
import { buildShareCardHtml, buildShareText } from './share'

const REQ = { name: '测试', gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
const RESULT = paipan(REQ)

describe('share', () => {
  it('分享文案包含四柱与免责声明', () => {
    const text = buildShareText(REQ, RESULT)
    expect(text).toContain('乙亥')
    expect(text).toContain('壬申')
    expect(text).toContain('仅供传统文化研究参考')
  })

  it('分享卡片包含四柱、日主与免责声明', () => {
    const html = buildShareCardHtml(REQ, RESULT)
    expect(html).toContain('年柱')
    expect(html).toContain('乙亥')
    expect(html).toContain('剑锋金')
    expect(html).toContain('仅供传统文化研究参考')
  })
})
