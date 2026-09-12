import { Link } from 'react-router-dom'
import type { RelationshipSingleSavedReport } from '@/services/reportApi'
import { NarrativeTimeline } from './NarrativeTimeline'
import { AnnualActionGuideCard } from './AnnualActionGuide'
import './RelationshipReportReader.css'
import './RelationshipSingleReportReader.css'

export function RelationshipSingleReportReader({ report }: { report: RelationshipSingleSavedReport }) {
  const content = report.content
  return (
    <main className="report-reader__paper relationship-reader relationship-single-reader">
      <header className="report-reader__masthead">
        <div>
          <span>感情命书 · 通俗版</span>
          <small>{report.subject} · 单身或尚未确定关系</small>
          <small>{content.currentYear} 年重点｜{content.outlookYear} 年简短参考</small>
        </div>
        <i aria-hidden="true">缘</i>
      </header>

      <section className="report-reader__thesis relationship-reader__thesis" aria-labelledby="single-thesis">
        <span>先说结论 · {content.currentYear}</span>
        <h1 id="single-thesis">{content.thesis}</h1>
        <p>{content.summary}</p>
      </section>

      <NarrativeTimeline timeline={content.timeline} />

      <div className="relationship-single-reader__current" aria-label={`${content.currentYear}年详细解读`}>
        {content.sections.map((section) => (
          <section className="relationship-single-section" key={section.id} aria-labelledby={`single-${section.id}`}>
            <h2 id={`single-${section.id}`}>{section.title}</h2>
            {section.paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
            {section.signals.length > 0 && (
              <ul aria-label="相处时可以留意">
                {section.signals.map((signal) => <li key={signal}>{signal}</li>)}
              </ul>
            )}
          </section>
        ))}
      </div>

      {content.actionGuide && <AnnualActionGuideCard guide={content.actionGuide} />}

      <aside className="relationship-single-reader__outlook" aria-labelledby="single-outlook">
        <span>明年参考</span>
        <h2 id="single-outlook">{content.outlookYear} 年 · 简短参考</h2>
        {content.outlook.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
      </aside>

      <p className="relationship-single-reader__note">{content.readingNote}</p>
      <details className="relationship-reader__evidence">
        <summary>查看计算依据</summary>
        <div>{content.evidenceKeys.map((key) => <code key={key}>{key}</code>)}</div>
      </details>
      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}
