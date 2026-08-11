import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { RecordRow } from '@/components/RecordRow'
import { Switch } from '@/components/Switch'
import { useHistory } from '@/hooks/useHistory'
import { getMe } from '@/services/membershipApi'
import { useAuthStore } from '@/store/useAuthStore'
import { useBaziStore } from '@/store/useBaziStore'
import { useSettingsStore } from '@/store/useSettingsStore'
import { useToastStore } from '@/store/useToastStore'
import type { MembershipInfo } from '@/types/bazi'

export function ProfilePage() {
  const navigate = useNavigate()
  const result = useBaziStore((s) => s.result)
  const setResult = useBaziStore((s) => s.setResult)
  const toast = useToastStore((s) => s.show)
  const trueSolarDefault = useSettingsStore((s) => s.trueSolarDefault)
  const toggleTrueSolar = useSettingsStore((s) => s.toggleTrueSolarDefault)
  const username = useAuthStore((s) => s.username)
  const clearAuth = useAuthStore((s) => s.clear)
  const token = useAuthStore((s) => s.token)
  const [membership, setMembership] = useState<MembershipInfo | null>(null)
  const { data: records = [] } = useHistory()
  const latest = records[0] ?? null
  const myResult = result ?? latest?.result ?? null

  useEffect(() => {
    if (import.meta.env.VITE_API_MODE === 'http' && token) {
      getMe()
        .then(setMembership)
        .catch(() => undefined)
    }
  }, [token])

  return (
    <>
      <div className="top">
        <div className="slot" />
        <div className="title">
          <span className="seal">命</span>
          我的
        </div>
        <button className="icon-btn" aria-label="设置" onClick={() => toast('设置功能规划中')}>
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

      <Card>
        <CardTitle>我的命盘</CardTitle>
        {myResult ? (
          <RecordRow
            title={myResult.lunarText}
            subtitle={`${myResult.solarText} · 生肖 ${myResult.shengXiao}`}
            onClick={() => {
              if (latest) {
                setResult(latest.request, latest.result)
              }
              navigate('/chart')
            }}
          />
        ) : (
          <RecordRow title="还没有命盘" subtitle="先去排一次盘" onClick={() => navigate('/input')} />
        )}
      </Card>

      <Card>
        <CardTitle>我的账号</CardTitle>
        {username ? (
          <>
            <RecordRow title={username} subtitle="已登录 · 排盘记录自动云同步" onClick={() => toast('账号信息规划中')} />
            <div
              style={{
                margin: '0 16px 14px',
                textAlign: 'center',
                fontSize: 13,
                color: 'var(--red)',
                background: 'var(--paper)',
                border: '1px solid var(--line)',
                borderRadius: 10,
                padding: '10px 0',
              }}
              onClick={() => {
                clearAuth()
                toast('已退出登录')
              }}
            >
              退出登录
            </div>
          </>
        ) : (
          <RecordRow title="未登录" subtitle="登录后可云同步排盘记录" onClick={() => navigate('/auth')} />
        )}
      </Card>

      <Card>
        <CardTitle>会员状态</CardTitle>
        {token ? (
          <Link className="row" to="/membership">
            <span className="k">会员中心</span>
            <span className="v">
              {import.meta.env.VITE_API_MODE === 'http' && membership?.isMember
                ? `会员 · 到期 ${new Date(membership.memberExpireAt ?? '').toLocaleDateString('zh-CN')}`
                : import.meta.env.VITE_API_MODE === 'http'
                  ? '未开通 · 查看套餐'
                  : '演示模式 · 查看会员方案'}
            </span>
            <span className="arrow">›</span>
          </Link>
        ) : (
          <RecordRow title="登录后可用" subtitle="登录后可查看会员状态" onClick={() => navigate('/auth')} />
        )}
      </Card>

      <Card>
        <CardTitle>设置</CardTitle>
        <Switch
          checked={trueSolarDefault}
          onChange={toggleTrueSolar}
          title="真太阳时默认开启"
          desc="新排盘默认按出生地校正"
        />
        <div className="row" onClick={() => toast('排盘流派设置规划中')}>
          <span className="k">排盘流派</span>
          <span className="v">子平（示例）</span>
          <span className="arrow">›</span>
        </div>
      </Card>

      <Card>
        <CardTitle>其他</CardTitle>
        <Link className="row" to="/history">
          <span className="k">历史记录</span>
          <span className="arrow">›</span>
        </Link>
        <Link className="row" to="/report">
          <span className="k">我的命书</span>
          <span className="arrow">›</span>
        </Link>
        <div className="row" onClick={() => toast('关于与合规声明规划中')}>
          <span className="k">关于与合规声明</span>
          <span className="arrow">›</span>
        </div>
      </Card>

    </>
  )
}
