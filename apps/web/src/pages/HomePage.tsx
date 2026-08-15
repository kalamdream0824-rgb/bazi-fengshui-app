import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { FooterNote } from '@/components/FooterNote'
import { RecordRow } from '@/components/RecordRow'
import { SegControl } from '@/components/SegControl'
import { useHistory } from '@/hooks/useHistory'
import { getAlmanacFor } from '@/lib/almanac'
import { getGanZhiFor } from '@/lib/baziMapper'
import { useBaziStore } from '@/store/useBaziStore'

const TODAY = getGanZhiFor(0)
const TOMORROW = getGanZhiFor(1)
const ALM_TODAY = getAlmanacFor(0)
const ALM_TOMORROW = getAlmanacFor(1)

const QUICK = [
  { to: '/input', glyph: '排', label: '八字排盘' },
  { to: '/comp', glyph: '合', label: '八字合婚' },
  { to: '/daily', glyph: '运', label: '每日运势' },
  { to: '/day-picker', glyph: '择', label: '择日' },
]

export function HomePage() {
  const navigate = useNavigate()
  const setResult = useBaziStore((s) => s.setResult)
  const { data: records = [] } = useHistory()
  const recent = records.slice(0, 3)
  const [day, setDay] = useState<'today' | 'tomorrow'>('today')
  const info = day === 'today' ? TODAY : TOMORROW
  const alm = day === 'today' ? ALM_TODAY : ALM_TOMORROW

  return (
    <>
      <div className="top">
        <div className="slot" />
        <div className="title">
          <span className="seal">命</span>
          八字排盘
        </div>
        <button className="icon-btn" aria-label="设置" onClick={() => void navigate('/settings')}>
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
        </button>
      </div>

      <div className="hero">
        <div className="date">
          {day === 'today' ? '今日' : '明日'} · {info.dateText}
        </div>
        <div className="gz">{info.ganZhi}</div>
        <SegControl
          two
          className="hero-seg"
          options={[
            { label: '今日', value: 'today' },
            { label: '明日', value: 'tomorrow' },
          ]}
          value={day}
          onChange={(v) => setDay(v as 'today' | 'tomorrow')}
        />
        <div>
          {alm.yi.slice(0, 4).map((t) => (
            <span key={t} className="chip">
              宜 {t}
            </span>
          ))}
          {alm.ji.slice(0, 3).map((t) => (
            <span key={t} className="chip no">
              忌 {t}
            </span>
          ))}
        </div>
        <div className="meta-line" style={{ marginTop: 8, color: 'rgba(255,246,232,.85)' }}>
          冲煞：{alm.chongDesc} · 煞{alm.sha}
        </div>
      </div>

      <Card>
        <CardTitle hint="选一件事开始">起盘</CardTitle>
        <div className="quick-grid">
          {QUICK.map((item) => (
            <Link key={item.to} to={item.to} className="opt">
              <span className="q-glyph">{item.glyph}</span>
              <span className="q-t">{item.label}</span>
            </Link>
          ))}
        </div>
      </Card>

      <Card>
        <CardTitle>最近排盘</CardTitle>
        {recent.length > 0 ? (
          recent.map((record) => (
            <RecordRow
              key={record.id}
              title={record.result.lunarText}
              subtitle={`${record.result.solarText} · 生肖 ${record.result.shengXiao}`}
              onClick={() => {
                setResult(record.request, record.result)
                void navigate('/chart')
              }}
            />
          ))
        ) : (
          <RecordRow
            title="暂无排盘记录"
            subtitle="去排一次，查看你的四柱八字"
            onClick={() => void navigate('/input')}
          />
        )}
      </Card>

      <FooterNote>排盘数据仅供传统文化研究参考</FooterNote>
    </>
  )
}
