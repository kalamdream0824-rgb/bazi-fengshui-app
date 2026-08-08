import { Navigate } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { DaYunList } from '@/components/DaYunList'
import { TopBar } from '@/components/TopBar'
import { useBaziStore } from '@/store/useBaziStore'

export function ProPage() {
  const result = useBaziStore((s) => s.result)

  if (!result) {
    return <Navigate to="/input" replace />
  }

  return (
    <>
      <TopBar title="专业细盘" />

      <Card>
        <CardTitle hint="十年一运">大运</CardTitle>
        <DaYunList daYun={result.daYun} />
      </Card>

      <Card>
        <CardTitle hint="丙午 · 示例">2026 流年</CardTitle>
        <div style={{ padding: '0 16px 14px', fontSize: 12, color: 'var(--ink-2)', lineHeight: 1.9 }}>
          流年解读将在后端实现后输出（当前为示例占位）。今日干支：{result.currentYearGanZhi}。
        </div>
      </Card>

      <div className="footer-note">大运流年内容以实际算法输出为准</div>
    </>
  )
}
