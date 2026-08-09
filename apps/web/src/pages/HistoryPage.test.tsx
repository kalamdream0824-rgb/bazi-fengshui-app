import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { addHistory, clearHistory } from '@/services/historyStore'
import { HistoryPage } from './HistoryPage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('HistoryPage', () => {
  it('空态引导去排盘', async () => {
    await clearHistory()
    renderPage()
    expect(await screen.findByText(/暂无排盘记录/)).toBeInTheDocument()
  })

  it('有记录时展示列表与导出按钮', async () => {
    await clearHistory()
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    await addHistory(req, paipan(req))
    renderPage()
    expect(await screen.findByText('一九九五年闰八月十四')).toBeInTheDocument()
    expect(screen.getByText('导出全部')).toBeInTheDocument()
  })
})
