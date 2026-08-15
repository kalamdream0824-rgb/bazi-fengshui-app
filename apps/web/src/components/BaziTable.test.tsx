import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { BaziTable } from './BaziTable'

const result = paipan({ gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })

describe('BaziTable 四柱表', () => {
  it('渲染副星行（藏干十神）', () => {
    render(<BaziTable pillars={result.pillars} />)
    expect(screen.getByText('副星')).toBeInTheDocument()
    expect(screen.getAllByText('比肩').length).toBeGreaterThan(0)
    expect(screen.getByText('食神')).toBeInTheDocument()
    expect(screen.getByText('七杀')).toBeInTheDocument()
  })

  it('渲染空亡行并高亮日柱旬空命中的落空支', () => {
    const { container } = render(<BaziTable pillars={result.pillars} />)
    expect(screen.getByText('空亡')).toBeInTheDocument()
    expect(screen.getByText('申酉')).toBeInTheDocument()
    expect(screen.getByText('戌亥')).toBeInTheDocument()
    const hits = container.querySelectorAll('.kong.hit')
    // 1995-10-08 壬申日旬空戌亥，四支亥/酉/申/未中仅年支亥落空
    expect(hits).toHaveLength(1)
    expect(hits[0].textContent).toBe('申酉')
  })

  it('点击副星标签可查看释义', async () => {
    render(<BaziTable pillars={result.pillars} />)
    fireEvent.click(screen.getByText('副星'))
    expect(screen.getByText(/藏干对应日主的十神/)).toBeInTheDocument()
  })

  it('旧快照藏干缺副星字段时显示占位', () => {
    const legacyPillars = {
      ...result.pillars,
      year: {
        ...result.pillars.year,
        hideGan: result.pillars.year.hideGan.map((h) => ({ gan: h.gan, wuxing: h.wuxing })),
      },
    } as unknown as typeof result.pillars
    render(<BaziTable pillars={legacyPillars} />)
    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
  })
})
