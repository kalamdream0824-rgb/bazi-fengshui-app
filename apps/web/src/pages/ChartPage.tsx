import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Button, ButtonRow } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { BaziTable } from '@/components/BaziTable'
import { FooterNote } from '@/components/FooterNote'
import { ShareSheet } from '@/components/ShareSheet'
import { TopBar } from '@/components/TopBar'
import { WuxingBar } from '@/components/WuxingBar'
import { prepareShare, type PreparedShare } from '@/lib/share'
import { useBaziStore } from '@/store/useBaziStore'

export function ChartPage() {
  const navigate = useNavigate()
  const request = useBaziStore((s) => s.request)
  const result = useBaziStore((s) => s.result)
  const [share, setShare] = useState<PreparedShare | null>(null)

  if (!request || !result) {
    return <Navigate to="/input" replace />
  }

  const genderLabel = request.gender === 'male' ? '乾造' : '坤造'

  return (
    <>
      <TopBar
        title="排盘结果"
        onBack={() => navigate('/input')}
        right={
          <button
            className="icon-btn"
            aria-label="分享"
            onClick={async () => {
              setShare(await prepareShare(request, result))
            }}
          >
            <svg
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4" />
            </svg>
          </button>
        }
      />

      <Card>
        <div style={{ padding: '13px 16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontWeight: 600 }}>
              {request.name || '示例'} · {genderLabel}
            </div>
            <div className="meta-line" style={{ marginTop: 3 }}>
              农历 {result.lunarText} · {result.timeZhi}时
            </div>
            {result.trueSolar ? (
              <div className="meta-line" style={{ marginTop: 3 }}>
                真太阳时 {result.trueSolar.original} → {result.trueSolar.adjusted}
                （经度 {result.trueSolar.longitude}°E）
              </div>
            ) : null}
            {result.trueSolar?.boundaryChanged ? (
              <div className="meta-line" style={{ marginTop: 3, color: 'var(--red)' }}>
                校正后时辰由 {result.trueSolar.originalShichen} 时变为 {result.trueSolar.adjustedShichen}{' '}
                时，排盘已按校正后时辰计算
              </div>
            ) : null}
          </div>
          <span
            className="chip"
            style={{
              background: 'rgba(184,144,74,.12)',
              borderColor: 'rgba(184,144,74,.35)',
              color: '#8a6a2f',
              margin: 0,
            }}
          >
            生肖 {result.shengXiao}
          </span>
        </div>
      </Card>

      <div className="seg two" style={{ margin: '0 14px 12px', padding: 0 }}>
        <button type="button" className="opt active">
          基本排盘
        </button>
        <button type="button" className="opt" onClick={() => navigate('/chart/pro')}>
          专业细盘
        </button>
      </div>

      <BaziTable pillars={result.pillars} />

      <Card>
        <CardTitle hint="本气计数">五行占比</CardTitle>
        <WuxingBar wuXing={result.wuXing} />
      </Card>

      <ButtonRow>
        <Button variant="primary" onClick={() => navigate('/report')}>
          导出命书
        </Button>
        <Button onClick={() => navigate('/comp')}>合婚</Button>
        <Button onClick={() => navigate('/daily')}>每日运势</Button>
      </ButtonRow>
      <ShareSheet
        open={Boolean(share)}
        imageUrl={share?.imageUrl ?? ''}
        text={share?.text ?? ''}
        filename={`排盘-${request.name || '示例'}.jpg`}
        onClose={() => setShare(null)}
      />
      <FooterNote>排盘数据仅供传统文化研究参考</FooterNote>
    </>
  )
}
