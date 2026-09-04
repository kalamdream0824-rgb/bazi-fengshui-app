import type { PaipanRequest, MembershipInfo, OrderInfo } from '@/types/bazi'
import {
  ReportApiError,
  type CreateReportOptions,
  type ReportEditionCode,
  type ReportTopicCode,
  type SavedReport,
} from './reportApi'
import { authFetch } from './http'
import { apiUrl } from './apiConfig'

function isHttpMode(): boolean {
  return import.meta.env.VITE_API_MODE === 'http'
}

async function readError(res: Response, fallback: string): Promise<Error> {
  const data = (await res.json().catch(() => null)) as { code?: string; message?: string } | null
  return new ReportApiError(data?.code || `HTTP_${res.status}`, data?.message || fallback)
}

export interface ReportCheckoutInfo {
  orderId: number
  reportId: number
  topic: ReportTopicCode
  edition: ReportEditionCode
  amountCents: number
  status: 'pending'
}

/** 创建会员套餐订单（金额由后端套餐常量定价） */
export async function createOrder(plan: string): Promise<OrderInfo> {
  if (!isHttpMode()) {
    throw new Error('联调模式不支持购买')
  }
  const res = await authFetch(apiUrl('/v1/orders'), {
    method: 'POST',
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
  const res = await authFetch(apiUrl(`/v1/pay/mock-success/${orderId}`), {
    method: 'POST',
  })
  if (!res.ok) {
    throw await readError(res, '支付失败')
  }
  return (await res.json()) as MembershipInfo
}

/** Generates a locked report and a server-priced pending order without exposing report content. */
export async function prepareReportCheckout(
  request: PaipanRequest,
  topic: ReportTopicCode,
  edition: ReportEditionCode,
  options: CreateReportOptions = {},
): Promise<ReportCheckoutInfo> {
  if (!isHttpMode()) {
    throw new Error('联调模式不支持单份购买')
  }
  const payload = topic === 'career' && options.careerContext
    ? { request, topic, edition, careerContext: options.careerContext }
    : topic === 'relationship' && options.relationshipContext
      ? { request, topic, edition, relationshipContext: options.relationshipContext }
      : { request, topic, edition }
  const res = await authFetch(apiUrl('/v1/reports/checkout'), {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  if (!res.ok) {
    throw await readError(res, '命书暂未生成成功，本次未付款')
  }
  return (await res.json()) as ReportCheckoutInfo
}

/** Local acceptance payment: unlocks exactly the report linked to this order. */
export async function mockPayReportCheckout(orderId: number): Promise<SavedReport> {
  if (!isHttpMode()) {
    throw new Error('联调模式不支持单份支付')
  }
  const res = await authFetch(apiUrl(`/v1/reports/checkout/${orderId}/mock-pay`), {
    method: 'POST',
  })
  if (!res.ok) {
    throw await readError(res, '支付未完成，命书尚未解锁')
  }
  return (await res.json()) as SavedReport
}
