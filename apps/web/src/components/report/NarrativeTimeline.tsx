import type { NarrativeTimeline as NarrativeTimelineData } from '@/services/reportApi'

interface NarrativeTimelineProps {
  timeline?: NarrativeTimelineData | null
}

export function NarrativeTimeline({ timeline }: NarrativeTimelineProps) {
  if (!timeline) return null

  return (
    <section className="narrative-timeline" aria-label="命书时间线">
      <header className="narrative-timeline__intro">
        <span>命书脉络</span>
        <h2>从回看经历，到决定下一步</h2>
        <p>先核对去年的真实感受，再看眼下最该处理什么。</p>
      </header>

      <ol className="narrative-timeline__phases">
        <li className="narrative-timeline__phase is-past" data-phase="past" data-testid="narrative-timeline-phase">
          <header className="narrative-timeline__phase-heading">
            <span>去年回看</span>
            <time dateTime={String(timeline.past.year)}>{timeline.past.year}</time>
          </header>
          <b className="narrative-timeline__review-label">回看核对</b>
          <h3>{timeline.past.headline}</h3>
          <ul className="narrative-timeline__checkpoints">
            {timeline.past.checkpoints.map((checkpoint) => (
              <li key={checkpoint}>{checkpoint}</li>
            ))}
          </ul>
          <p className="narrative-timeline__bridge">{timeline.past.bridge}</p>
        </li>

        <li className="narrative-timeline__phase is-present" data-phase="present" data-testid="narrative-timeline-phase">
          <header className="narrative-timeline__phase-heading">
            <span>当下判断</span>
            <time dateTime={String(timeline.present.year)}>{timeline.present.year}</time>
          </header>
          <h3>{timeline.present.headline}</h3>
          <p className="narrative-timeline__judgment">{timeline.present.judgment}</p>
          <div className="narrative-timeline__priority">
            <span>眼下先做</span>
            <strong>{timeline.present.priority}</strong>
          </div>
        </li>

        <li className="narrative-timeline__phase is-future" data-phase="future" data-testid="narrative-timeline-phase">
          <header className="narrative-timeline__phase-heading">
            <span>未来行动</span>
            <small>按年安排</small>
          </header>
          <ol className="narrative-timeline__future-list">
            {timeline.future.map((step) => (
              <li key={step.year}>
                <time dateTime={String(step.year)}>{step.year}</time>
                <div>
                  <h3>{step.headline}</h3>
                  <p>{step.action}</p>
                </div>
              </li>
            ))}
          </ol>
        </li>
      </ol>
    </section>
  )
}
