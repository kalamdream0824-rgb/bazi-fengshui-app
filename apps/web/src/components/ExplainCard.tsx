import { Card, CardTitle } from '@/components/Card'
import type { ChartExplanation, ExplainBlockKey } from '@/lib/explainer'

interface ExplainCardProps {
  explanation: ChartExplanation
  /** 传入时只渲染对应解读块（专业细盘按页签取用） */
  blockKey?: ExplainBlockKey
}

export function ExplainCard({ explanation, blockKey }: ExplainCardProps) {
  if (blockKey) {
    const block = explanation.blocks.find((b) => b.key === blockKey)
    if (!block) return null
    return (
      <Card>
        <CardTitle hint="仅供参考">{block.title}</CardTitle>
        <ExplainPoints points={block.points} />
      </Card>
    )
  }

  return (
    <Card>
      <CardTitle hint="新人解读 · 温和参考">读懂这张盘</CardTitle>
      <div className="explain-overview">
        {explanation.overview.map((s) => (
          <p key={s}>{s}</p>
        ))}
      </div>
      {explanation.blocks.map((b) => (
        <details className="explain-block" key={b.key}>
          <summary>{b.title}</summary>
          <ExplainPoints points={b.points} />
        </details>
      ))}
    </Card>
  )
}

function ExplainPoints({ points }: { points: ChartExplanation['blocks'][number]['points'] }) {
  return (
    <div className="explain">
      {points.map((p) => (
        <div className="explain-row" key={p.label}>
          <b className="explain-label">{p.label}</b>
          <span className="explain-text">{p.text}</span>
        </div>
      ))}
    </div>
  )
}
