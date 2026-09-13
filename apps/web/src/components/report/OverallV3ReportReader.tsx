import { Link } from 'react-router-dom'
import type { OverallV3SavedReport } from '@/services/reportApi'
import { AnnualActionGuideCard } from './AnnualActionGuide'
import { NarrativeTimeline } from './NarrativeTimeline'

const ORDINALS = ['第一年', '第二年', '第三年', '第四年', '第五年']

export function OverallV3ReportReader({ report }: { report: OverallV3SavedReport }) {
  return (
    <main className="report-reader__paper overall-v3-reader">
      <header className="report-reader__masthead">
        <div>
          <span>综合命书 · 通俗版</span>
          <small>{report.subject} · 年度决策总览</small>
        </div>
        <i aria-hidden="true">总</i>
      </header>

      <section className="report-reader__thesis" aria-labelledby="overall-v3-thesis">
        <span>三年主线</span>
        <h1 id="overall-v3-thesis">{report.content.thesis}</h1>
        <p className="overall-reader__summary">{report.content.summary}</p>
      </section>

      <NarrativeTimeline timeline={report.content.timeline} />

      <div className="report-reader__years overall-v3-reader__years">
        {report.content.years.map((year, index) => (
          <article className="report-year overall-v3-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header>
                <div>
                  <small>这一年的优先顺序</small>
                  <h2>{year.year}</h2>
                </div>
                <em>{ORDINALS[index] ?? `第${index + 1}年`}</em>
              </header>

              <h3 className="overall-year__headline">{year.headline}</h3>

              <section className="overall-v3-year__decision" aria-label={`${year.year}年主次主题`}>
                <div className="is-primary">
                  <span>主主题</span>
                  <strong>{year.primaryLabel}</strong>
                  <small>先处理</small>
                </div>
                <i aria-hidden="true">牵动</i>
                <div className="is-secondary">
                  <span>次主题</span>
                  <strong>{year.secondaryLabel}</strong>
                  <small>同时守住</small>
                </div>
              </section>

              <section className="overall-year__linkage overall-v3-year__linkage">
                <h3>为什么这样排序</h3>
                <p>{year.linkage}</p>
              </section>

              <AnnualActionGuideCard guide={year.actionGuide} />

              <section className="overall-v3-year__observations">
                <header>
                  <span>兼看</span>
                  <h3>另外两个方面</h3>
                </header>
                <ul aria-label={`${year.year}年另外两个方面`}>
                  {year.observations.map((observation) => (
                    <li key={observation.topicCode}>
                      <div>
                        <strong>{observation.topicLabel}</strong>
                        <small>{observation.stance}</small>
                      </div>
                      <p>{observation.note}</p>
                    </li>
                  ))}
                </ul>
              </section>

              <p className="overall-year__transition">{year.transition}</p>
            </div>
          </article>
        ))}
      </div>

      <p className="overall-reader__note">{report.content.readingNote}</p>
      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
