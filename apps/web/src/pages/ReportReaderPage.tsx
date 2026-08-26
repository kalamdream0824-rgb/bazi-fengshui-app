import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { CareerReportReader } from '@/components/report/CareerReportReader'
import { WealthReportReader } from '@/components/report/WealthReportReader'
import { RelationshipReportReader } from '@/components/report/RelationshipReportReader'
import { TopBar } from '@/components/TopBar'
import {
  getReport,
  isRelationshipV1Report,
  isWealthV2Report,
  type SavedReport,
} from '@/services/reportApi'

export function ReportReaderPage() {
  const { id } = useParams()
  const reportId = Number(id)
  const invalidReportId = !Number.isInteger(reportId)
  const [report, setReport] = useState<SavedReport | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (invalidReportId) return
    getReport(reportId).then(setReport).catch((reason: unknown) => {
      setError(reason instanceof Error ? reason.message : '命书读取失败')
    })
  }, [invalidReportId, reportId])

  return (
    <div className="report-reader">
      <TopBar title="命书正文" tone="night" />
      {invalidReportId || error ? (
        <main className="report-reader__state">
          <p>{invalidReportId ? '命书编号无效' : error}</p>
          <Link to="/reports">返回我的命书</Link>
        </main>
      ) : !report ? (
        <main className="report-reader__state">正在展开命书…</main>
      ) : isRelationshipV1Report(report) ? (
        <RelationshipReportReader report={report} />
      ) : isWealthV2Report(report) ? (
        <WealthReportReader report={report} />
      ) : (
        <CareerReportReader report={report} />
      )}
    </div>
  )
}
