import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
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

describe('ProfilePage 会员状态入口', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
    useAuthStore.getState().clear()
  })

  it('未登录：显示登录引导', async () => {
    useAuthStore.getState().clear()
    renderPage()
    expect(await screen.findByText('登录后可用')).toBeInTheDocument()
  })

  it('mock 模式已登录：会员状态卡显示会员中心入口', async () => {
    vi.stubEnv('VITE_API_MODE', 'mock')
    useAuthStore.getState().setAuth('token-1', 'tester')
    renderPage()

    expect(await screen.findByText('会员中心')).toBeInTheDocument()
    expect(screen.getByText('演示模式 · 查看会员方案')).toBeInTheDocument()
    expect(screen.queryByText('登录后可用')).not.toBeInTheDocument()
  })

  it('http 模式已登录：会员状态卡显示套餐引导入口', async () => {
    vi.stubEnv('VITE_API_MODE', 'http')
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        new Response(JSON.stringify({ username: 'tester', plan: null, memberExpireAt: null, isMember: false }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    useAuthStore.getState().setAuth('token-1', 'tester')
    renderPage()

    expect(await screen.findByText('会员中心')).toBeInTheDocument()
    expect(screen.getByText('未开通 · 查看套餐')).toBeInTheDocument()
  })
})
