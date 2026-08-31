import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@/store/useAuthStore'
import { mockPayReportCheckout, prepareReportCheckout } from './payApi'

const request = {
  name: '林先生',
  gender: 'male' as const,
  solarDateTime: '1995-10-08T14:30:00',
  birthPlace: '上海',
  trueSolarTime: false,
}

function ok(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('payApi report checkout', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'http')
    useAuthStore.getState().setAuth('report-pay-token', 'tester')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
    useAuthStore.getState().clear()
  })

  it('按关系状态创建服务端定价的锁定命书订单', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({
      orderId: 81,
      reportId: 28,
      topic: 'relationship',
      edition: 'plain',
      amountCents: 690,
      status: 'pending',
    }))
    vi.stubGlobal('fetch', fetchMock)

    const checkout = await prepareReportCheckout(request, 'relationship', 'plain', {
      relationshipContext: { status: 'dating' },
    })

    expect(checkout.amountCents).toBe(690)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/reports/checkout', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        request,
        topic: 'relationship',
        edition: 'plain',
        relationshipContext: { status: 'dating' },
      }),
    }))
  })

  it('支付确认只解锁订单关联的同一份命书', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({
      id: 28,
      subject: '林先生',
      topic: 'wealth',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'wealth-narrative-v3',
      content: {},
      createdAt: '2026-08-30T16:00:00',
      generatedAt: '2026-08-30T16:00:00',
    }))
    vi.stubGlobal('fetch', fetchMock)

    const report = await mockPayReportCheckout(81)

    expect(report.id).toBe(28)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/reports/checkout/81/mock-pay',
      expect.objectContaining({ method: 'POST' }),
    )
  })
})
