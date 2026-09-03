import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { NarrativeTimeline as NarrativeTimelineData } from '@/services/reportApi'
import { NarrativeTimeline } from './NarrativeTimeline'

const timeline: NarrativeTimelineData = {
  past: {
    year: 2025,
    headline: '去年的变化先用实际经历核对',
    checkpoints: [
      '回看去年是否经常临时接下额外任务。',
      '如果职责变多，成果有没有被清楚记录。',
    ],
    bridge: '这些经历会影响今年先处理哪件事。',
    evidenceKeys: ['career.2025.review'],
  },
  present: {
    year: 2026,
    headline: '今年先把已经承担的事情说清楚',
    judgment: '责任正在增加，但回报和权限还需要进一步确认。',
    priority: '先整理已经完成的成果，再和负责人确认职责范围。',
    evidenceKeys: ['career.2026.present'],
  },
  future: [
    {
      year: 2027,
      headline: '下一年再争取更明确的位置',
      action: '用今年留下的成果记录，争取正式授权或新的机会。',
      evidenceKeys: ['career.2027.future'],
    },
  ],
}

describe('NarrativeTimeline', () => {
  it('按去年回看、当下判断、未来行动的顺序显示具体年份', () => {
    render(<NarrativeTimeline timeline={timeline} />)

    const region = screen.getByRole('region', { name: '命书时间线' })
    const phases = within(region).getAllByTestId('narrative-timeline-phase')

    expect(phases.map((phase) => phase.dataset.phase)).toEqual(['past', 'present', 'future'])
    expect(within(phases[0]).getByText('去年回看')).toBeInTheDocument()
    expect(within(phases[0]).getByText('回看核对')).toBeInTheDocument()
    expect(within(phases[0]).getByText('2025')).toBeInTheDocument()
    expect(within(phases[1]).getByText('当下判断')).toBeInTheDocument()
    expect(within(phases[1]).getByText('2026')).toBeInTheDocument()
    expect(within(phases[2]).getByText('未来行动')).toBeInTheDocument()
    expect(within(phases[2]).getByText('2027')).toBeInTheDocument()
    expect(screen.queryByText('已发生')).not.toBeInTheDocument()
  })

  it('突出当下优先事项，并按年份列出未来行动', () => {
    render(<NarrativeTimeline timeline={timeline} />)

    expect(screen.getByText('眼下先做')).toBeInTheDocument()
    expect(screen.getByText(timeline.present.priority)).toBeInTheDocument()
    expect(screen.getByText(timeline.future[0].action)).toBeInTheDocument()
  })

  it('旧报告没有时间线时不渲染占位内容', () => {
    const { container } = render(<NarrativeTimeline />)

    expect(container).toBeEmptyDOMElement()
  })
})
