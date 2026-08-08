import { Link } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { getTodayGanZhi } from '@/lib/baziMapper'
import { useBaziStore } from '@/store/useBaziStore'
import { useToastStore } from '@/store/useToastStore'

const TODAY = getTodayGanZhi()

const QUICK = [
  { to: '/input', label: '八字排盘' },
  { to: '/comp', label: '八字合婚' },
  { to: '/daily', label: '每日运势' },
  { to: '/report', label: '命书' },
]

export function HomePage() {
  const result = useBaziStore((s) => s.result)
  const toast = useToastStore((s) => s.show)

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
        <div className="date">今日 · {new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })}</div>
        <div className="gz">{TODAY}</div>
        <div>
          <span className="chip">宜 祭祀</span>
          <span className="chip">宜 出行</span>
          <span className="chip no">忌 动土</span>
          <span className="chip no">忌 安葬</span>
        </div>
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
        {result ? (
          <Link className="row" to="/chart">
            <div>
              <div style={{ fontWeight: 600 }}>最近一次 · {result.lunarText}</div>
              <div className="meta-line" style={{ marginTop: 3 }}>
                {result.solarText} · {result.shengXiao}年
              </div>
            </div>
            <span className="arrow">›</span>
          </Link>
        ) : (
          <Link className="row" to="/input">
            <div>
              <div style={{ fontWeight: 600 }}>暂无排盘记录</div>
              <div className="meta-line" style={{ marginTop: 3 }}>去排一次，查看你的四柱八字</div>
            </div>
            <span className="arrow">›</span>
          </Link>
        )}
      </Card>

      <div className="footer-note">排盘数据仅供传统文化研究参考</div>
    </>
  )
}
