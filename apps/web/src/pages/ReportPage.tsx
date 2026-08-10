import { useState } from 'react'
import { Button, ButtonRow } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { EmptyState } from '@/components/EmptyState'
import { FooterNote } from '@/components/FooterNote'
import { TopBar } from '@/components/TopBar'
import { GAN_WUXING, WUXING_LABEL } from '@/lib/wuxing'
import { exportMingshuPdf } from '@/lib/reportPdf'
import { useBaziWithFallback } from '@/hooks/useBaziWithFallback'
import { useToastStore } from '@/store/useToastStore'

const TOC = [
  ['壹', '命盘概览', 'P.01'],
  ['贰', '五行与用神', 'P.03'],
  ['叁', '大运流年', 'P.05'],
  ['肆', '十神详析', 'P.08'],
  ['伍', '综合建议', 'P.11'],
] as const

export function ReportPage() {
  const toast = useToastStore((s) => s.show)
  const [exporting, setExporting] = useState(false)
  const { request, result } = useBaziWithFallback()

  const handleExport = async () => {
    if (!request || !result || exporting) return
    setExporting(true)
    try {
      await exportMingshuPdf(request, result)
      toast('命书 PDF 已生成')
    } catch {
      toast('生成失败，请重试')
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <TopBar title="命书报告" />

      {!result ? (
        <>
          <EmptyState title="还没有命盘数据" hint="先去排一次盘" linkTo="/input" linkText="先去排一次盘" />
          <FooterNote>命书内容仅供传统文化研究参考</FooterNote>
        </>
      ) : (
        <>
          <div className="hero" style={{ padding: '24px 22px', position: 'relative' }}>
            <div
              style={{
                position: 'absolute',
                right: 18,
                top: 18,
                width: 32,
                height: 32,
                border: '2px solid rgba(255,246,232,.85)',
                borderRadius: '5px 9px 5px 9px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 15,
                transform: 'rotate(5deg)',
              }}
            >
              命
            </div>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 30, fontWeight: 700, letterSpacing: 14 }}>
              命 书
            </div>
            <div style={{ marginTop: 12, fontSize: 12, opacity: 0.9 }}>
              {request?.name || '示例'} · <b>{request?.gender === 'male' ? '乾造' : '坤造'}</b>
            </div>
            <div style={{ marginTop: 4, fontSize: 11, opacity: 0.75 }}>{result.lunarText} · 共 12 页（示例）</div>
          </div>

          <Card>
            <CardTitle>目录</CardTitle>
            {TOC.map(([num, title, page]) => (
              <div className="row" key={num}>
                <span className="k">
                  <span style={{ color: 'var(--red)', fontWeight: 700, marginRight: 6 }}>{num}</span>
                  {title}
                </span>
                <span className="v">{page}</span>
              </div>
            ))}
          </Card>

          <Card>
            <CardTitle hint="真实排盘数据">样张预览 · 命盘概览</CardTitle>
            <div
              style={{
                margin: '0 16px 14px',
                fontSize: 12,
                color: 'var(--ink-2)',
                lineHeight: 1.9,
                background: '#faf5e8',
                borderRadius: 10,
                padding: '10px 12px',
              }}
            >
              日主{result.pillars.day.gan}（{WUXING_LABEL[GAN_WUXING[result.pillars.day.gan]]}）， 四柱{' '}
              {result.pillars.year.gan}
              {result.pillars.year.zhi} {result.pillars.month.gan}
              {result.pillars.month.zhi} {result.pillars.day.gan}
              {result.pillars.day.zhi} {result.pillars.time.gan}
              {result.pillars.time.zhi}， 日柱纳音 {result.pillars.day.naYin}。完整解读见正式命书（示例版式）。
            </div>
          </Card>

          <div style={{ textAlign: 'center', fontSize: 12, color: 'var(--ink-3)', margin: '0 14px 10px' }}>
            深度版命书 <b style={{ color: 'var(--red)', fontSize: 16 }}>¥9.9</b>（示例定价，待定）
          </div>
          <ButtonRow>
            <Button variant="primary" onClick={handleExport} disabled={exporting}>
              {exporting ? '生成中…' : '导出 PDF'}
            </Button>
            <Button onClick={() => toast('分享功能规划中')}>分享</Button>
            <Button onClick={() => window.history.back()}>返回</Button>
          </ButtonRow>
          <FooterNote>命书内容仅供传统文化研究参考</FooterNote>
        </>
      )}
    </>
  )
}
