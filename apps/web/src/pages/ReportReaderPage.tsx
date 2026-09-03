import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { CareerReportReader } from '@/components/report/CareerReportReader'
import { WealthReportReader } from '@/components/report/WealthReportReader'
import { WealthV3ReportReader } from '@/components/report/WealthV3ReportReader'
import { RelationshipReportReader } from '@/components/report/RelationshipReportReader'
import { RelationshipSingleReportReader } from '@/components/report/RelationshipSingleReportReader'
import { OverallReportReader } from '@/components/report/OverallReportReader'
import { TopBar } from '@/components/TopBar'
import {
  getReport,
  isCareerV4Report,
  isOverallReport,
  isLegacyReport,
  isRelationshipReport,
  isRelationshipSingleReport,
  isWealthV2Report,
  isWealthDetailedReport,
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
      ) : isOverallReport(report) ? (
        <OverallReportReader report={report} />
      ) : isRelationshipSingleReport(report) ? (
        <RelationshipSingleReportReader report={report} />
      ) : isRelationshipReport(report) ? (
        <RelationshipReportReader report={report} />
      ) : isWealthV2Report(report) ? (
        <WealthReportReader report={report} />
      ) : isWealthDetailedReport(report) ? (
        <WealthV3ReportReader report={report} />
      ) : isCareerV4Report(report) ? (
        <CareerReportReader report={report} />
      ) : isLegacyReport(report) ? (
        <CareerReportReader report={report} />
      ) : (
        <main className="report-reader__state">
          <h1>暂不支持读取这份命书</h1>
          <p>这份命书来自当前页面尚未支持的内容版本。</p>
          <Link to="/reports">返回我的命书</Link>
        </main>
      )}
    </div>
  )
}
