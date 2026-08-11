import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@/store/useAuthStore'
import { useToastStore } from '@/store/useToastStore'
import { MembershipPage } from './MembershipPage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <MembershipPage />
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

describe('MembershipPage 会员中心', () => {
  beforeEach(() => {
    useAuthStore.getState().setAuth('token-1', 'tester')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
    useAuthStore.getState().clear()
    useToastStore.setState({ message: null })
  })

  it('http 模式：选套餐 → 创建订单 → 模拟支付 → 会员即时开通', async () => {
    vi.stubEnv('VITE_API_MODE', 'http')
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

    fireEvent.click(screen.getByText('30 天'))
    fireEvent.click(screen.getByText('立即开通'))

    expect(await screen.findByText('会员 · member_1m')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/pay/mock-success/101', expect.objectContaining({ method: 'POST' }))
  })

  it('mock 模式：点击购买仅提示演示模式，不发请求', async () => {
    vi.stubEnv('VITE_API_MODE', 'mock')
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    renderPage()
    fireEvent.click(screen.getByText('立即开通'))

    expect(useToastStore.getState().message).toContain('演示模式')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('未登录：显示登录引导', async () => {
    vi.stubEnv('VITE_API_MODE', 'http')
    useAuthStore.getState().clear()
    renderPage()
    expect(await screen.findByText('登录后可用')).toBeInTheDocument()
  })

  it('购买时 401：清除本地登录态并提示重新登录', async () => {
    vi.stubEnv('VITE_API_MODE', 'http')
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url === '/api/v1/me') {
        return ok({ username: 'tester', plan: null, memberExpireAt: null, isMember: false })
      }
      return new Response(JSON.stringify({ code: 'UNAUTHORIZED', message: '未登录或登录已过期' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    renderPage()
    expect(await screen.findByText('未开通会员')).toBeInTheDocument()

    fireEvent.click(screen.getByText('立即开通'))

    expect(await screen.findByText('登录后可用')).toBeInTheDocument()
    expect(useToastStore.getState().message).toContain('登录已过期')
    expect(useAuthStore.getState().token).toBeNull()
  })
})
