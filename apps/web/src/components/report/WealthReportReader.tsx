import { Link } from 'react-router-dom'
import type { WealthPathSummary, WealthV2SavedReport } from '@/services/reportApi'

const ORDINALS = ['第一年', '第二年', '第三年', '第四年', '第五年']

function ordinal(index: number) {
  return ORDINALS[index] ?? `第${index + 1}年`
}

function pathByCode(report: WealthV2SavedReport, code: string): WealthPathSummary | undefined {
  return report.content.paths.find((path) => path.code === code)
}

export function WealthReportReader({ report }: { report: WealthV2SavedReport }) {
  const primary = pathByCode(report, report.content.primaryPathCode)
  const secondary = pathByCode(report, report.content.secondaryPathCode)

  return (
    <main className="report-reader__paper wealth-reader">
      <header className="report-reader__masthead">
        <div>
          <span>财富命书 · 通俗版</span>
          <small>{report.subject} · 未来三年</small>
        </div>
        <i aria-hidden="true">财</i>
      </header>

      <section className="report-reader__thesis" aria-labelledby="wealth-report-thesis">
        <span>三年总断</span>
        <h1 id="wealth-report-thesis">{report.content.thesis}</h1>
        <p className="wealth-reader__summary">{report.content.summary}</p>
      </section>

      <section className="wealth-reader__directions" aria-labelledby="wealth-directions-title">
        <h2 id="wealth-directions-title">先看你的钱路</h2>
        <div>
          <article className="money-direction is-primary">
            <span>主要钱路</span>
            <strong>{primary?.label ?? '主要收入方向'}</strong>
            <p>{primary?.judgment ?? report.content.summary}</p>
          </article>
          <article className="money-direction is-secondary">
            <span>辅助钱路</span>
            <strong>{secondary?.label ?? '辅助收入方向'}</strong>
            <p>{secondary?.judgment ?? report.content.summary}</p>
          </article>
          <article className="money-direction is-risk">
            <span>最需留意</span>
            <strong>{report.content.mainRisk.label}</strong>
            <p>{report.content.mainRisk.judgment}</p>
          </article>
        </div>
      </section>

      <section className="wealth-reader__paths" aria-labelledby="wealth-paths-title">
        <header>
          <span>五路账册</span>
          <h2 id="wealth-paths-title">五条钱路逐项看</h2>
        </header>
        <div>
          {report.content.paths.map((path, index) => (
            <article className={`wealth-path is-${path.code}`} key={path.code}>
              <b aria-hidden="true">{index + 1}</b>
              <span>
                <small>{path.status}</small>
                <strong>{path.label}</strong>
                <p>{path.judgment}</p>
              </span>
            </article>
          ))}
        </div>
      </section>

      <div className="report-reader__years wealth-reader__years">
        {report.content.years.map((year, index) => (
          <article className="report-year wealth-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header>
                <div>
                  <small>{year.focus}</small>
                  <h2>{year.year} · {year.ganZhi}</h2>
                </div>
                <em>{ordinal(index)}</em>
              </header>

              <section className="wealth-year__income">
                <h3>钱主要从哪里来</h3>
                <p>{year.incomeSource}</p>
              </section>

              <section className="report-year__grid">
                <div>
                  <h3>钱能不能留下</h3>
                  <p>{year.retention}</p>
                </div>
                <aside>
                  <h3>主要限制</h3>
                  <p>{year.mainLimit}</p>
                </aside>
              </section>

              <section className="wealth-year__signals">
                <h3>现实里出现这些情况，就说明方向正在发生</h3>
                <ol>{year.realitySignals.map((signal) => <li key={signal}>{signal}</li>)}</ol>
              </section>

              <section className="report-year__actions">
                <h3>这一年，建议先做两件事</h3>
                <ol>{year.actions.map((action) => <li key={action}>{action}</li>)}</ol>
              </section>

              <p className="report-year__boundary"><b>下一年怎么看：</b>{year.transition}</p>
            </div>
          </article>
        ))}
      </div>

      <section className="report-reader__route">
        <span>未来三年，可以按这个顺序处理钱的问题</span>
        <ol>{report.content.route.map((action) => <li key={action}>{action}</li>)}</ol>
      </section>

      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
