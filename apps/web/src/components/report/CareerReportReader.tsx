import { Link } from 'react-router-dom'
import type { CareerV4SavedReport, LegacySavedReport } from '@/services/reportApi'
import { AnnualActionGuideCard } from './AnnualActionGuide'
import { NarrativeTimeline } from './NarrativeTimeline'

const TOPIC_META = {
  career: { title: '事业命书', seal: '业' },
  wealth: { title: '财富命书', seal: '财' },
  overall: { title: '综合命书', seal: '总' },
  relationship: { title: '感情命书', seal: '缘' },
} as const

const ORDINALS = ['第一年', '第二年', '第三年', '第四年', '第五年']

function ordinal(index: number) {
  return ORDINALS[index] ?? `第${index + 1}年`
}

export function CareerReportReader({ report }: { report: LegacySavedReport | CareerV4SavedReport }) {
  return (
    <main className="report-reader__paper">
      <header className="report-reader__masthead">
        <div>
          <span>{TOPIC_META[report.topic].title} · {report.edition === 'plain' ? '通俗版' : '专业版'}</span>
          <small>{report.subject} · {report.content.contextSummary}</small>
        </div>
        <i aria-hidden="true">{TOPIC_META[report.topic].seal}</i>
      </header>

      <section className="report-reader__thesis" aria-labelledby="report-thesis">
        <span>两年总断</span>
        <h1 id="report-thesis">{report.content.thesis}</h1>
      </section>

      <NarrativeTimeline timeline={report.content.timeline} />

      <div className="report-reader__years">
        {report.content.years.map((year, index) => (
          <article className="report-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header>
                <div>
                  <small>{year.stage} · {year.headline}</small>
                  <h2>{year.year} · {year.ganZhi}</h2>
                </div>
                <em>{ordinal(index)}</em>
              </header>
              <p className="report-year__verdict">{year.verdict}</p>

              <section className={`report-year__grid${year.actionGuide ? ' is-action-guide' : ''}`}>
                <div>
                  <h3>为什么这么判断</h3>
                  <ol>{year.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ol>
                </div>
                {!year.actionGuide && <aside>
                  <h3>眼下最需要解决的问题</h3>
                  <p>{year.obstacle}</p>
                </aside>}
              </section>

              {year.actionGuide ? <AnnualActionGuideCard guide={year.actionGuide} /> : <section className="report-year__actions">
                <h3>这一年，建议先做两件事</h3>
                <ol>{year.actions.map((action) => <li key={action}>{action}</li>)}</ol>
              </section>}

              {!year.actionGuide && <p className="report-year__boundary"><b>出现下面的情况，就要调整：</b>{year.changeCondition}</p>}

              {report.edition === 'professional' && (
                <details className="report-year__evidence">
                  <summary>专业依据 <small>置信度 {year.confidence}</small></summary>
                  <div>
                    {year.evidence.map((item) => (
                      <p key={item.key}><code>{item.key}</code><b>{item.label}</b>{item.value}</p>
                    ))}
                    {year.counterEvidence.length > 0 && (
                      <p><b>反向证据：</b>{year.counterEvidence.map((item) => item.label).join('、')}</p>
                    )}
                  </div>
                </details>
              )}
            </div>
          </article>
        ))}
      </div>

      <section className="report-reader__route">
        <span>接下来两年，可以先做这三件事</span>
        <ol>{report.content.route.map((action) => <li key={action}>{action}</li>)}</ol>
      </section>

      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
