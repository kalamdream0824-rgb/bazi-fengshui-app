import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { TopBar } from '@/components/TopBar'
import { listReports, reportHeadline, type SavedReport } from '@/services/reportApi'

const TOPIC_META = {
  career: { label: '事业运势', seal: '业' },
  wealth: { label: '财富运势', seal: '财' },
  overall: { label: '综合运势', seal: '总' },
  relationship: { label: '感情运势', seal: '缘' },
} as const

export function ReportLibraryPage() {
  const [reports, setReports] = useState<SavedReport[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    listReports()
      .then(setReports)
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : '命书列表读取失败'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="report-library">
      <TopBar title="我的命书" tone="night" />
      <main>
        <header className="report-library__intro">
          <span>藏书阁</span>
          <h1>已生成的命书</h1>
          <p>内容按生成时的版本保存，之后可以随时回来查看。</p>
        </header>

        {loading ? <p className="report-library__state">正在整理命书…</p> : null}
        {error ? <p className="report-library__state">{error}</p> : null}
        {!loading && !error && reports.length === 0 ? (
          <section className="report-library__empty">
            <b aria-hidden="true">空</b>
            <h2>书架还是空的</h2>
            <p>先生成一份命书，正文会自动保存到这里。</p>
            <Link to="/report">去生成命书</Link>
          </section>
        ) : null}

        <div className="report-library__list">
          {reports.map((report) => (
            <Link className="report-library__item" key={report.id} to={`/reports/${report.id}`}>
              <span className="report-library__seal" aria-hidden="true">{TOPIC_META[report.topic].seal}</span>
              <span className="report-library__copy">
                <small>{report.subject} · {TOPIC_META[report.topic].label} · {report.edition === 'plain' ? '通俗版' : '专业版'}</small>
                <strong>{reportHeadline(report)}</strong>
                <time>{new Date(report.generatedAt).toLocaleDateString('zh-CN')}</time>
              </span>
              <i aria-hidden="true">›</i>
            </Link>
          ))}
        </div>

        {!loading && !error && reports.length > 0 ? <Link className="report-library__create" to="/report">再生成一份</Link> : null}
      </main>
    </div>
  )
}
