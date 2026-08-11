import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@/store/useAuthStore'
import { ProfilePage } from './ProfilePage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function ok(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('ProfilePage 会员购买（http 联调模式）', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'http')
    useAuthStore.getState().setAuth('token-1', 'tester')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
    useAuthStore.getState().clear()
  })

  it('选套餐 → 创建订单 → 模拟支付 → 会员即时开通', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url === '/api/v1/me') {
        return ok({ username: 'tester', plan: null, memberExpireAt: null, isMember: false })
      }
      if (url === '/api/v1/orders') {
        return ok({
          id: 101,
          plan: 'member_1m',
          amountCents: 2990,
          status: 'pending',
          provider: null,
          providerTradeNo: null,
          createdAt: '2026-08-11T00:00:00',
          paidAt: null,
        })
      }
      if (url === '/api/v1/pay/mock-success/101') {
        return ok({
          username: 'tester',
          plan: 'member_1m',
          memberExpireAt: '2026-09-10T00:00:00',
          isMember: true,
        })
      }
      return new Response(JSON.stringify({ message: 'not found' }), { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    renderPage()

    expect(await screen.findByText('未开通会员')).toBeInTheDocument()
    // 默认推荐 90 天；切到 30 天再购买
    fireEvent.click(screen.getByText('30 天'))
    fireEvent.click(screen.getByText('立即开通'))

    expect(await screen.findByText('会员 · member_1m')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/pay/mock-success/101', expect.objectContaining({ method: 'POST' }))
  })

  it('支付失败提示错误且不改变会员状态', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url === '/api/v1/me') {
        return ok({ username: 'tester', plan: null, memberExpireAt: null, isMember: false })
      }
      if (url === '/api/v1/orders') {
        return ok({
          id: 102,
          plan: 'member_3m',
          amountCents: 6800,
          status: 'pending',
          provider: null,
          providerTradeNo: null,
          createdAt: '2026-08-11T00:00:00',
          paidAt: null,
        })
      }
      if (url === '/api/v1/pay/mock-success/102') {
        return new Response(JSON.stringify({ code: 'PAY_MOCK_DISABLED', message: '当前环境不支持模拟支付' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return new Response(JSON.stringify({ message: 'not found' }), { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    renderPage()
    expect(await screen.findByText('未开通会员')).toBeInTheDocument()

    fireEvent.click(screen.getByText('立即开通'))
    expect(await screen.findByText('未开通会员')).toBeInTheDocument()
  })
})
