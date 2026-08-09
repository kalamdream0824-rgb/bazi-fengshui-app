import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { FooterNote } from '@/components/FooterNote'
import { RecordRow } from '@/components/RecordRow'
import { SegControl } from '@/components/SegControl'
import { useHistory } from '@/hooks/useHistory'
import { getGanZhiFor } from '@/lib/baziMapper'
import { useBaziStore } from '@/store/useBaziStore'
import { useToastStore } from '@/store/useToastStore'

const TODAY = getGanZhiFor(0)
const TOMORROW = getGanZhiFor(1)

const QUICK = [
  { to: '/input', label: '八字排盘' },
  { to: '/comp', label: '八字合婚' },
  { to: '/daily', label: '每日运势' },
  { to: '/report', label: '命书' },
]

export function HomePage() {
  const navigate = useNavigate()
  const setResult = useBaziStore((s) => s.setResult)
  const toast = useToastStore((s) => s.show)
  const { data: records = [] } = useHistory()
  const recent = records.slice(0, 3)
  const [day, setDay] = useState<'today' | 'tomorrow'>('today')
  const info = day === 'today' ? TODAY : TOMORROW

  return (
    <>
      <div className="top">
        <div className="slot" />
        <div className="title">
          <span className="seal">命</span>
          八字排盘
        </div>
        <button className="icon-btn" aria-label="设置" onClick={() => toast('设置功能规划中')}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
        </button>
      </div>

      <div className="hero">
        <div className="date">{day === 'today' ? '今日' : '明日'} · {info.dateText}</div>
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
        {day === 'today' ? (
          <div>
            <span className="chip">宜 祭祀</span>
            <span className="chip">宜 出行</span>
            <span className="chip no">忌 动土</span>
            <span className="chip no">忌 安葬</span>
          </div>
        ) : (
          <div>
            <span className="chip">宜 会友</span>
            <span className="chip">宜 沐浴</span>
            <span className="chip no">忌 开市</span>
            <span className="chip no">忌 求医</span>
          </div>
        )}
      </div>

      <Card>
        <CardTitle hint="八点网格 · 等宽入口">快捷功能</CardTitle>
        <div className="quick-grid">
          {QUICK.map((item) => (
            <Link key={item.to} to={item.to} className="opt">
              {item.label}
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
          <RecordRow title="暂无排盘记录" subtitle="去排一次，查看你的四柱八字" onClick={() => void navigate('/input')} />
        )}
      </Card>

      <FooterNote>排盘数据仅供传统文化研究参考</FooterNote>
    </>
  )
}
