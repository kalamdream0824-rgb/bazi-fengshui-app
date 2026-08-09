import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { DaYunList } from '@/components/DaYunList'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { GAN_WUXING, WUXING_LABEL } from '@/lib/wuxing'
import { useBaziStore } from '@/store/useBaziStore'

type Panel = 'yun' | 'liu' | 'ss' | 'shensha'

export function ProPage() {
  const result = useBaziStore((s) => s.result)
  const [panel, setPanel] = useState<Panel>('yun')

  if (!result) {
    return <Navigate to="/input" replace />
  }

  const currentDaYun = result.daYun.find((d) => d.isCurrent)

  return (
    <>
      <TopBar title="专业细盘" />

      <div style={{ margin: '4px 14px 10px', fontSize: 12, color: 'var(--ink-3)', display: 'flex', justifyContent: 'space-between' }}>
        <span>当前大运 <b style={{ color: 'var(--ink)' }}>{currentDaYun?.ganZhi ?? '—'}</b></span>
        <span>{result.currentYearGanZhi}</span>
      </div>

      <SegControl
        className="four"
        options={[
          { label: '大运', value: 'yun' },
          { label: '流年', value: 'liu' },
          { label: '十神', value: 'ss' },
          { label: '神煞', value: 'shensha' },
        ]}
        value={panel}
        onChange={(v) => setPanel(v as Panel)}
      />

      {panel === 'yun' && (
        <Card>
          <CardTitle hint="十年一运">大运</CardTitle>
          <DaYunList daYun={result.daYun} />
        </Card>
      )}

      {panel === 'liu' && (
        <>
          <Card>
            <CardTitle hint="丙午 · 示例">2026 流年</CardTitle>
            <div style={{ padding: '0 16px 14px', fontSize: 12, color: 'var(--ink-2)', lineHeight: 1.9 }}>
              流年解读将在后端实现后输出（当前为示例占位）。今日干支：{result.currentYearGanZhi}。
            </div>
          </Card>
          <Card>
            <CardTitle hint="2027 · 示例">明年预告</CardTitle>
            <div style={{ padding: '0 16px 14px', fontSize: 12, color: 'var(--ink-2)', lineHeight: 1.9 }}>
              丁未年正财与日主相合（示例），感情与合作机会增多，事业压力与机遇并存。
            </div>
          </Card>
        </>
      )}

      {panel === 'ss' && (
        <Card>
          <CardTitle hint="以日主为原点">十神分布</CardTitle>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, padding: '4px 16px 16px' }}>
            {(['year', 'month', 'day', 'time'] as const).map((key) => {
              const p = result.pillars[key]
              return (
                <div key={key} style={{ textAlign: 'center', border: '1px solid var(--line)', borderRadius: 10, padding: '8px 2px', background: '#faf5e8' }}>
                  <div className="meta-line">{key === 'year' ? '年' : key === 'month' ? '月' : key === 'day' ? '日' : '时'}柱 · {p.gan}{p.zhi}</div>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 700, marginTop: 4 }}>
                    {p.shiShen}
                  </div>
                  <div className="meta-line" style={{ marginTop: 4 }}>{WUXING_LABEL[GAN_WUXING[p.gan]]}</div>
                </div>
              )
            })}
          </div>
        </Card>
      )}

      {panel === 'shensha' && (
        <>
          <Card>
            <CardTitle hint="大运/流年干支对照原局 · 口径见设计文档">流年神煞（2026 · {result.currentLiuNian?.ganZhi ?? '—'}）</CardTitle>
            <div style={{ padding: '0 16px 14px', fontSize: 13 }}>
              {result.currentLiuNian?.shenSha.join('·') || '—'}
            </div>
          </Card>
          <Card>
            <CardTitle hint="大运干支对照原局">大运神煞</CardTitle>
            {result.daYun.map((d) => (
              <div className="row" key={d.ganZhi}>
                <span className="k">{d.ganZhi}</span>
                <span className="v">{d.shenSha.join('·') || '—'}</span>
              </div>
            ))}
          </Card>
        </>
      )}

      <FooterNote>大运流年内容以实际算法输出为准</FooterNote>
    </>
  )
}
