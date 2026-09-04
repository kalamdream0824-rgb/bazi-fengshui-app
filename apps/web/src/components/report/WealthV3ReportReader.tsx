import { Link } from 'react-router-dom'
import type { WealthPathV3, WealthV3SavedReport } from '@/services/reportApi'
import { NarrativeTimeline } from './NarrativeTimeline'

const LEGACY_PATH_LABELS: Record<WealthPathV3, string> = {
  stable_income: '稳定收入',
  skill_income: '靠能力赚钱',
  project_income: '项目和额外收入',
  cooperation_income: '合作带来的收入',
  retention: '把钱留下',
}

const MONEY_STATE_LABELS: Record<WealthPathV3, string> = {
  stable_income: '进账稳定',
  skill_income: '投入回报',
  project_income: '到账节奏',
  cooperation_income: '资金责任',
  retention: '收支结余',
}

export function WealthV3ReportReader({ report }: { report: WealthV3SavedReport }) {
  const usesMoneyStateCopy = report.content.copyVersion === 'wealth-plain-v3.4'
  const pathLabels = usesMoneyStateCopy ? MONEY_STATE_LABELS : LEGACY_PATH_LABELS

  return (
    <main className="report-reader__paper wealth-reader wealth-v3-reader">
      <header className="report-reader__masthead">
        <div>
          <span>财富命书 · 通俗版</span>
          <small>{report.subject} · {report.content.asOf.slice(0, 4)} 年起三年</small>
        </div>
        <i aria-hidden="true">财</i>
      </header>

      <section className="report-reader__thesis" aria-labelledby="wealth-v3-thesis">
        <span>三年总览</span>
        <h1 id="wealth-v3-thesis">{report.content.thesis.text}</h1>
        <p className="wealth-reader__summary">{report.content.summary.text}</p>
      </section>

      <NarrativeTimeline timeline={report.content.timeline} />

      <section className="wealth-reader__paths" aria-labelledby="wealth-v3-paths">
        <header>
          <span>{usesMoneyStateCopy ? '资金状态账册' : '五路账册'}</span>
          <h2 id="wealth-v3-paths">{usesMoneyStateCopy ? '五项资金状态逐项看' : '五条钱路逐项看'}</h2>
        </header>
        <div>
          {report.content.pathSummaries.map((path, index) => (
            <article className={`wealth-path is-${path.path}`} key={path.path}>
              <b aria-hidden="true">{index + 1}</b>
              <span>
                <strong>{pathLabels[path.path]}</strong>
                <p>{path.reading.text}</p>
              </span>
            </article>
          ))}
        </div>
      </section>

      {report.content.riskSummary ? (
        <section className="wealth-v3-risk-summary">
          <h2>三年里需要留意</h2>
          <p>{report.content.riskSummary.text}</p>
        </section>
      ) : null}

      <div className="report-reader__years wealth-reader__years">
        {report.content.years.map((year, index) => (
          <article className="report-year wealth-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header><h2>{year.year} · {year.ganZhi}</h2></header>
              <p className="report-year__verdict">{year.overview.text}</p>
              {year.focus.state === 'tied' ? (
                <section className="wealth-v3-year__focus is-tied">
                  <h3>可以一起关注的方向</h3>
                  <p>{year.focus.primaryCandidates.map((path) => pathLabels[path]).join('、')}</p>
                </section>
              ) : year.focus.state === 'leading' ? (
                <section className="wealth-v3-year__focus is-leading">
                  <h3>相对更值得关注的方向</h3>
                  <p>{pathLabels[year.focus.primaryCandidates[0]]}</p>
                </section>
              ) : null}
              <section className="wealth-year__income">
                <h3>{usesMoneyStateCopy ? '这一年的资金变化' : '这一年的收入方向'}</h3>
                {year.income.map((block) => <p key={block.id}>{block.text}</p>)}
              </section>
              <section className="wealth-v3-year__retention">
                <h3>钱能不能留下</h3>
                <p>{year.retention.text}</p>
              </section>
              {year.risk ? (
                <section className="wealth-v3-year__risk">
                  <h3>需要留意的一项</h3>
                  <p>{year.risk.reading.text}</p>
                </section>
              ) : null}
              {year.observations.length > 0 ? (
                <section className="wealth-v3-year__observations">
                  <h3>可以留意的现实情况</h3>
                  <ul>{year.observations.map((block) => <li key={block.id}>{block.text}</li>)}</ul>
                </section>
              ) : null}
              {year.actions.length > 0 ? (
                <section className="wealth-v3-year__actions">
                  <h3>可以先做这些事</h3>
                  <ol>{year.actions.map((block) => <li key={block.id}>{block.text}</li>)}</ol>
                </section>
              ) : null}
              {year.comparison ? (
                <section className="wealth-v3-year__comparison">
                  <h3>和 {year.comparison.toYear} 年相比</h3>
                  <p>{year.comparison.reading.text}</p>
                </section>
              ) : null}
            </div>
          </article>
        ))}
      </div>

      {report.content.route.length > 0 ? (
        <section className="report-reader__route wealth-v3-reader__route">
          <h2>接下来三年，可以这样安排</h2>
          <ol>{report.content.route.map((block) => <li key={block.id}>{block.text}</li>)}</ol>
        </section>
      ) : null}

      <p className="wealth-v3-reader__note">{report.content.readingNote.text}</p>

      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
