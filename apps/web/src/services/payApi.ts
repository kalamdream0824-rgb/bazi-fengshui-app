import { useAuthStore } from '@/store/useAuthStore'
import type { MembershipInfo, OrderInfo } from '@/types/bazi'

function isHttpMode(): boolean {
  return import.meta.env.VITE_API_MODE === 'http'
}

function authHeaders(): Record<string, string> {
  const token = useAuthStore.getState().token
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function readError(res: Response, fallback: string): Promise<Error> {
  const data = (await res.json().catch(() => null)) as { message?: string } | null
  return new Error(data?.message || fallback)
}

/** 创建会员套餐订单（金额由后端套餐常量定价） */
export async function createOrder(plan: string): Promise<OrderInfo> {
  if (!isHttpMode()) {
    throw new Error('联调模式不支持购买')
  }
  const res = await fetch('/api/v1/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ plan }),
  })
  if (!res.ok) {
    throw await readError(res, '创建订单失败')
  }
  return (await res.json()) as OrderInfo
}

/** 模拟支付成功回调：幂等，重复调用不重复顺延到期 */
export async function mockPay(orderId: number): Promise<MembershipInfo> {
  if (!isHttpMode()) {
    throw new Error('联调模式不支持支付')
  }
  const res = await fetch(`/api/v1/pay/mock-success/${orderId}`, {
    method: 'POST',
    headers: authHeaders(),
  })
  if (!res.ok) {
    throw await readError(res, '支付失败')
  }
  return (await res.json()) as MembershipInfo
}
