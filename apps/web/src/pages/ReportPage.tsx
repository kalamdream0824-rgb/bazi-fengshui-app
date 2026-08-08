import { TopBar } from '@/components/TopBar'

export function ReportPage() {
  return (
    <>
      <TopBar title="命书报告" />
      <div className="placeholder">
        命书模块开发中（M0.8）
        <br />
        将支持排盘报告预览与 PDF 导出
      </div>
      <div className="footer-note">命书内容仅供传统文化研究参考</div>
    </>
  )
}
