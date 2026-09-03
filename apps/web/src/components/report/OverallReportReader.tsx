import { Link } from 'react-router-dom'
import type { OverallSavedReport } from '@/services/reportApi'
import { NarrativeTimeline } from './NarrativeTimeline'

const ORDINALS = ['第一年', '第二年', '第三年', '第四年', '第五年']

export function OverallReportReader({ report }: { report: OverallSavedReport }) {
  return (
    <main className="report-reader__paper overall-reader">
      <header className="report-reader__masthead">
        <div>
          <span>综合命书 · 通俗版</span>
          <small>{report.subject} · 四个方面逐年比较</small>
        </div>
        <i aria-hidden="true">总</i>
      </header>

      <section className="report-reader__thesis" aria-labelledby="overall-thesis">
        <span>三年总看</span>
        <h1 id="overall-thesis">{report.content.thesis}</h1>
        <p className="overall-reader__summary">{report.content.summary}</p>
      </section>

      <NarrativeTimeline timeline={report.content.timeline} />

      <div className="report-reader__years">
        {report.content.years.map((year, index) => (
          <article className="report-year overall-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header>
                <div>
                  <small>最值得关注 · {year.primaryLabel}</small>
                  <h2>{year.year} · {year.ganZhi}</h2>
                </div>
                <em>{ORDINALS[index] ?? `第${index + 1}年`}</em>
              </header>

              <h3 className="overall-year__headline">{year.headline}</h3>
              <p className="report-year__verdict">{year.verdict}</p>

              {year.linkage ? (
                <section className="overall-year__linkage">
                  <h3>为什么先看这件事</h3>
                  <p>{year.linkage}</p>
                </section>
              ) : null}

              <section className="overall-year__dimensions" aria-label={`${year.year}年四个方面`}>
                {year.dimensions.map((dimension) => {
                  const priority = dimension.code === year.primaryCode
                    ? 'primary'
                    : dimension.code === year.secondaryCode ? 'secondary' : 'regular'
                  return (
                    <article className={`overall-dimension is-${priority}`} key={dimension.code}>
                      <header>
                        <span aria-hidden="true">
                          {priority === 'primary' ? '先' : priority === 'secondary' ? '次' : '看'}
                        </span>
                        <div>
                          <h3>{dimension.label}</h3>
                          <small>{dimension.stance}</small>
                        </div>
                      </header>
                      <p>{dimension.judgment}</p>
                    </article>
                  )
                })}
              </section>

              <section className="overall-year__priority">
                <h3>这一年先处理什么</h3>
                <p>{year.priorityIssue}</p>
              </section>

              <section className="report-year__actions">
                <h3>这一年，先做两件事</h3>
                <ol>{year.actions.map((action, actionIndex) => (
                  <li key={`${year.year}-${actionIndex}`}>{action}</li>
                ))}</ol>
              </section>

              <p className="report-year__boundary"><b>出现下面的情况，就要调整：</b>{year.changeCondition}</p>
              <p className="overall-year__transition">{year.transition}</p>
            </div>
          </article>
        ))}
      </div>

      <section className="report-reader__route">
        <span>行动顺序</span>
        <h2>接下来三年，按这个顺序做</h2>
        <ol>{report.content.route.map((action, index) => (
          <li key={`route-${index}`}>{action}</li>
        ))}</ol>
      </section>

      <p className="overall-reader__note">{report.content.readingNote}</p>
      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
