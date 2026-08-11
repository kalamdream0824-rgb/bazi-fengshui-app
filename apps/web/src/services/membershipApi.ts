import type { MembershipInfo } from '@/types/bazi'
import { authFetch } from './http'

function isHttpMode(): boolean {
  return import.meta.env.VITE_API_MODE === 'http'
}

export async function getMe(): Promise<MembershipInfo> {
  if (!isHttpMode()) {
    return { username: '', plan: null, memberExpireAt: null, isMember: false }
  }
  const res = await authFetch('/api/v1/me')
  if (!res.ok) {
    throw new Error('获取会员信息失败')
  }
  return (await res.json()) as MembershipInfo
}

export async function redeemCode(code: string): Promise<MembershipInfo> {
  if (!isHttpMode()) {
    throw new Error('联调模式不支持兑换')
  }
  const res = await authFetch('/api/v1/redeem', {
    method: 'POST',
    body: JSON.stringify({ code }),
  })
  if (!res.ok) {
    const data = (await res.json().catch(() => null)) as { message?: string } | null
    throw new Error(data?.message || '兑换失败')
  }
  return (await res.json()) as MembershipInfo
}
