import { Link } from 'react-router-dom'
import type {
  RelationshipDimensionSummary,
  RelationshipV1SavedReport,
} from '@/services/reportApi'
import { NarrativeTimeline } from './NarrativeTimeline'
import { AnnualActionGuideCard } from './AnnualActionGuide'
import './RelationshipReportReader.css'

const ORDINALS = ['第一年', '第二年', '第三年', '第四年', '第五年']
const HORIZON_LABELS: Record<number, string> = { 2: '二年', 3: '三年', 4: '四年', 5: '五年' }

function dimensionByCode(report: RelationshipV1SavedReport, code: string) {
  return report.content.dimensions.find((dimension) => dimension.code === code)
}

function FocusCard({
  kind,
  dimension,
}: {
  kind: 'primary' | 'secondary'
  dimension?: RelationshipDimensionSummary
}) {
  return (
    <article className={`relationship-focus-card is-${kind}`}>
      <span>{kind === 'primary' ? '主要关注' : '次要关注'}</span>
      <strong>{dimension?.label ?? '关系重点'}</strong>
      <p>{dimension?.judgment ?? '结合逐年内容继续查看。'}</p>
    </article>
  )
}

export function RelationshipReportReader({ report }: { report: RelationshipV1SavedReport }) {
  const primary = dimensionByCode(report, report.content.primaryDimensionCode)
  const secondary = dimensionByCode(report, report.content.secondaryDimensionCode)
  const horizon = HORIZON_LABELS[report.content.horizonYears] ?? `${report.content.horizonYears}年`

  return (
    <main className="report-reader__paper relationship-reader">
      <header className="report-reader__masthead">
        <div>
          <span>感情命书 · 通俗版</span>
          <small>{report.subject} · {report.content.relationshipStatusLabel} · 未来{horizon}</small>
        </div>
        <i aria-hidden="true">缘</i>
      </header>

      <section className="report-reader__thesis relationship-reader__thesis" aria-labelledby="relationship-report-thesis">
        <span>{horizon}总断</span>
        <h1 id="relationship-report-thesis">{report.content.thesis}</h1>
        <p>{report.content.summary}</p>
      </section>

      <NarrativeTimeline timeline={report.content.timeline} />

      <section className="relationship-reader__focus" aria-label={`${horizon}关系重点`}>
        <FocusCard kind="primary" dimension={primary} />
        <FocusCard kind="secondary" dimension={secondary} />
        {report.content.mainRisk && (
          <article className="relationship-focus-card is-risk">
            <span>需要留意</span>
            <strong>{report.content.mainRisk.label}</strong>
            <p>{report.content.mainRisk.judgment}</p>
          </article>
        )}
      </section>

      <section className="relationship-reader__dimensions" aria-labelledby="relationship-dimensions-title">
        <header>
          <span>关系五面</span>
          <h2 id="relationship-dimensions-title">五个方面逐项看</h2>
        </header>
        <div>
          {report.content.dimensions.map((dimension, index) => (
            <article key={dimension.code}>
              <b aria-hidden="true">{index + 1}</b>
              <span>
                <small>{dimension.status}</small>
                <strong>{dimension.label}</strong>
                <p>{dimension.judgment}</p>
              </span>
            </article>
          ))}
        </div>
      </section>

      <div className="report-reader__years relationship-reader__years">
        {report.content.years.map((year, index) => (
          <article className="report-year relationship-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header>
                <div>
                  <small>{year.focus}</small>
                  <h2>{year.year} · {year.ganZhi}</h2>
                </div>
                <em>{ORDINALS[index] ?? `第${index + 1}年`}</em>
              </header>

              <section className="relationship-year__judgment">
                <h3>这一年最值得关注</h3>
                <p>{year.judgment}</p>
              </section>

              {year.mainLimit && (
                <aside className="relationship-year__limit">
                  <h3>需要留意的事</h3>
                  <p>{year.mainLimit}</p>
                </aside>
              )}

              <section className="relationship-year__signals">
                <h3>现实中可以留意</h3>
                <ol>{year.realitySignals.map((signal) => <li key={signal}>{signal}</li>)}</ol>
              </section>

              {year.actionGuide ? (
                <AnnualActionGuideCard guide={year.actionGuide} />
              ) : (
                <section className="report-year__actions relationship-year__actions">
                  <h3>可以怎么做</h3>
                  <ol>{year.actions.map((action) => <li key={action}>{action}</li>)}</ol>
                </section>
              )}

              <p className="report-year__boundary">
                <b>{index + 1 < report.content.years.length ? '下一年怎么看：' : '阅读提醒：'}</b>
                {year.transition}
              </p>
            </div>
          </article>
        ))}
      </div>

      <details className="relationship-reader__evidence">
        <summary>查看计算依据</summary>
        <div>{report.content.evidenceKeys.map((key) => <code key={key}>{key}</code>)}</div>
      </details>

      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
