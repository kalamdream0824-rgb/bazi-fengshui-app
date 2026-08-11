import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { FooterNote } from '@/components/FooterNote'
import { RecordRow } from '@/components/RecordRow'
import { TopBar } from '@/components/TopBar'
import { getMe, redeemCode } from '@/services/membershipApi'
import { createOrder, mockPay } from '@/services/payApi'
import { useAuthStore } from '@/store/useAuthStore'
import { useToastStore } from '@/store/useToastStore'
import type { MembershipInfo } from '@/types/bazi'

const PLANS = [
  { code: 'member_1m', label: '30 天', price: '¥29.9' },
  { code: 'member_3m', label: '90 天', price: '¥68', tag: '推荐' },
] as const

export function MembershipPage() {
  const navigate = useNavigate()
  const toast = useToastStore((s) => s.show)
  const token = useAuthStore((s) => s.token)
  const [membership, setMembership] = useState<MembershipInfo | null>(null)
  const [plan, setPlan] = useState<string>('member_3m')
  const [paying, setPaying] = useState(false)
  const [code, setCode] = useState('')
  const [redeeming, setRedeeming] = useState(false)

  const isHttp = import.meta.env.VITE_API_MODE === 'http'

  useEffect(() => {
    if (isHttp && token) {
      getMe()
        .then(setMembership)
        .catch(() => undefined)
    }
  }, [isHttp, token])

  const handleBuy = async () => {
    if (!isHttp) {
      toast('当前为演示模式（mock），请以 http 联调模式体验模拟支付')
      return
    }
    setPaying(true)
    try {
      const order = await createOrder(plan)
      const info = await mockPay(order.id)
      setMembership(info)
      toast('支付成功，会员已开通')
    } catch (err) {
      toast((err as Error).message || '支付失败')
    } finally {
      setPaying(false)
    }
  }

  const handleRedeem = async () => {
    if (!isHttp) {
      toast('当前为演示模式（mock），http 联调模式可用兑换码')
      return
    }
    if (!code.trim()) {
      toast('请输入兑换码')
      return
    }
    setRedeeming(true)
    try {
      const info = await redeemCode(code.trim())
      setMembership(info)
      setCode('')
      toast('兑换成功')
    } catch (err) {
      toast((err as Error).message || '兑换失败')
    } finally {
      setRedeeming(false)
    }
  }

  if (!token) {
    return (
      <>
        <TopBar title="会员中心" />
        <Card>
          <CardTitle>会员中心</CardTitle>
          <RecordRow title="登录后可用" subtitle="登录后可查看会员状态与套餐" onClick={() => navigate('/auth')} />
        </Card>
        <FooterNote>仅供传统文化研究参考</FooterNote>
      </>
    )
  }

  return (
    <>
      <TopBar title="会员中心" />

      <Card>
        <CardTitle hint={isHttp ? undefined : '演示模式'}>会员状态</CardTitle>
        <RecordRow
          title={membership?.isMember ? `会员 · ${membership?.plan ?? ''}` : '未开通会员'}
          subtitle={
            membership?.isMember && membership.memberExpireAt
              ? `到期时间 ${new Date(membership.memberExpireAt).toLocaleDateString('zh-CN')}`
              : isHttp
                ? '选择套餐，支付后即时开通'
                : 'mock 演示模式，不产生真实数据'
          }
        />
      </Card>

      <Card>
        <CardTitle hint="会员期间解锁全部功能">选择套餐</CardTitle>
        <div className="plan-grid">
          {PLANS.map((p) => (
            <button
              key={p.code}
              type="button"
              className={`plan-card${plan === p.code ? ' active' : ''}`}
              onClick={() => setPlan(p.code)}
            >
              <span className="plan-label">
                {p.label}
                {'tag' in p && p.tag ? <em className="plan-tag">{p.tag}</em> : null}
              </span>
              <span className="plan-price">{p.price}</span>
            </button>
          ))}
        </div>
        <div style={{ padding: '12px 16px 14px' }}>
          <Button block onClick={handleBuy} disabled={paying}>
            {paying ? '支付中…' : membership?.isMember ? '续费' : '立即开通'}
          </Button>
        </div>
      </Card>

      <Card>
        <CardTitle>兑换码</CardTitle>
        <div className="field" style={{ paddingTop: 4 }}>
          <input className="input-box" value={code} placeholder="输入兑换码" onChange={(e) => setCode(e.target.value)} />
        </div>
        <div style={{ padding: '0 16px 14px' }}>
          <Button block onClick={handleRedeem} disabled={redeeming}>
            {redeeming ? '兑换中…' : '兑换'}
          </Button>
        </div>
      </Card>

      <FooterNote>会员为虚拟商品演示，仅供传统文化研究参考</FooterNote>
    </>
  )
}
