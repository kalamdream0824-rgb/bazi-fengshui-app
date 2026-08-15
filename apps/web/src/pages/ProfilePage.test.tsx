import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { addHistory, clearHistory, listHistory } from '@/services/historyStore'
import { useAuthStore } from '@/store/useAuthStore'
import { useBaziStore } from '@/store/useBaziStore'
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

describe('ProfilePage 我的命盘', () => {
  it('无记录时显示引导', async () => {
    await clearHistory()
    renderPage()
    expect(await screen.findByText('还没有命盘')).toBeInTheDocument()
    expect(screen.getByText('会员状态')).toBeInTheDocument()
    expect(screen.getByText('登录后可用')).toBeInTheDocument()
  })

  it('有历史记录时显示最近命盘', async () => {
    await clearHistory()
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    await addHistory(req, paipan(req))
    renderPage()
    expect(await screen.findByText('一九九五年闰八月十四')).toBeInTheDocument()
  })

  it('退出登录清空本地历史与当前命盘', async () => {
    await clearHistory()
    useAuthStore.getState().setAuth('token-x', '测试用户')
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    const result = paipan(req)
    useBaziStore.getState().setResult(req, result)
    await addHistory(req, result)
    renderPage()
    expect(await screen.findByText('测试用户')).toBeInTheDocument()
    fireEvent.click(screen.getByText('退出登录'))
    await waitFor(async () => {
      expect(useAuthStore.getState().token).toBeNull()
      expect(useBaziStore.getState().result).toBeNull()
      expect(await listHistory()).toHaveLength(0)
    })
    expect(await screen.findByText('还没有命盘')).toBeInTheDocument()
  })
})
