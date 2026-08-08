import { Link } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { Switch } from '@/components/Switch'
import { useBaziStore } from '@/store/useBaziStore'
import { useSettingsStore } from '@/store/useSettingsStore'
import { useToastStore } from '@/store/useToastStore'

export function ProfilePage() {
  const result = useBaziStore((s) => s.result)
  const toast = useToastStore((s) => s.show)
  const trueSolarDefault = useSettingsStore((s) => s.trueSolarDefault)
  const toggleTrueSolar = useSettingsStore((s) => s.toggleTrueSolarDefault)

  return (
    <>
      <div className="top">
        <div className="slot" />
        <div className="title">
          <span className="seal">命</span>
          我的
        </div>
        <button className="icon-btn" aria-label="设置" onClick={() => toast('设置功能规划中')}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
        </button>
      </div>

      <Card>
        <CardTitle>我的命盘</CardTitle>
        {result ? (
          <Link className="row" to="/chart">
            <div>
              <div style={{ fontWeight: 600 }}>{result.lunarText}</div>
              <div className="meta-line" style={{ marginTop: 3 }}>{result.solarText} · 生肖 {result.shengXiao}</div>
            </div>
            <span className="arrow">›</span>
          </Link>
        ) : (
          <Link className="row" to="/input">
            <div>
              <div style={{ fontWeight: 600 }}>还没有命盘</div>
              <div className="meta-line" style={{ marginTop: 3 }}>先去排一次盘</div>
            </div>
            <span className="arrow">›</span>
          </Link>
        )}
      </Card>

      <Card>
        <CardTitle>设置</CardTitle>
        <Switch checked={trueSolarDefault} onChange={toggleTrueSolar} title="真太阳时默认开启" desc="新排盘默认按出生地校正" />
        <div className="row" onClick={() => toast('排盘流派设置规划中')}>
          <span className="k">排盘流派</span>
          <span className="v">子平（示例）</span>
          <span className="arrow">›</span>
        </div>
      </Card>

      <Card>
        <CardTitle>其他</CardTitle>
        <div className="row" onClick={() => toast('会员开通流程规划中')}>
          <span className="k">会员中心</span>
          <span className="arrow">›</span>
        </div>
        <div className="row" onClick={() => toast('历史记录规划中')}>
          <span className="k">历史记录</span>
          <span className="arrow">›</span>
        </div>
        <Link className="row" to="/report">
          <span className="k">我的命书</span>
          <span className="arrow">›</span>
        </Link>
        <div className="row" onClick={() => toast('关于与合规声明规划中')}>
          <span className="k">关于与合规声明</span>
          <span className="arrow">›</span>
        </div>
      </Card>

      <div style={{ margin: '0 14px 12px', textAlign: 'center', fontSize: 13, color: 'var(--red)', background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 14, padding: '13px 0' }} onClick={() => toast('登录体系将在后端实现后接入')}>
        退出登录
      </div>
    </>
  )
}
