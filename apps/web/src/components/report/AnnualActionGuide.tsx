export interface AnnualActionGuideCopy {
  problem: string
  action: string
  expectedChange: string
  checkTiming: string
  successSignal: string
  adjustmentCondition: string
  fallbackAction: string
}

export function AnnualActionGuideCard({ guide }: { guide: AnnualActionGuideCopy }) {
  return (
    <section className="annual-action-guide wealth-action-guide" aria-label="年度行动闭环">
      <header className="wealth-action-guide__header">
        <span>行 · 验 · 调</span>
        <h3>做一件能看到结果的事</h3>
      </header>
      <div className="wealth-action-guide__problem">
        <b>当前要解决</b>
        <p>{guide.problem}</p>
      </div>
      <div className="wealth-action-guide__steps">
        <article data-step="行">
          <h3>现在怎么做</h3>
          <p>{guide.action}</p>
        </article>
        <article data-step="果">
          <h3>这样做，可能看到什么变化</h3>
          <p>{guide.expectedChange}</p>
        </article>
        <article data-step="验">
          <h3>什么时候检查</h3>
          <p>{guide.checkTiming}</p>
        </article>
        <article data-step="成">
          <h3>什么情况说明有效</h3>
          <p>{guide.successSignal}</p>
        </article>
      </div>
      <div className="wealth-action-guide__fallback">
        <h3>没有改善时怎么调整</h3>
        <p>{guide.adjustmentCondition}</p>
        <strong>{guide.fallbackAction}</strong>
      </div>
    </section>
  )
}
