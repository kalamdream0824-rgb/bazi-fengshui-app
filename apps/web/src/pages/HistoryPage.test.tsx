import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
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

  it('有记录时展示列表，导出/导入收进数据管理区', async () => {
    await clearHistory()
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    await addHistory(req, paipan(req))
    renderPage()
    expect(await screen.findByText('一九九五年闰八月十四')).toBeInTheDocument()
    expect(screen.getByText('数据管理：')).toBeInTheDocument()
    expect(screen.getByText('导出备份')).toBeInTheDocument()
    expect(screen.getByText('导入备份')).toBeInTheDocument()
  })

  it('清空全部：确认后本地记录清空回到空态', async () => {
    await clearHistory()
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    await addHistory(req, paipan(req))
    renderPage()
    expect(await screen.findByText('一九九五年闰八月十四')).toBeInTheDocument()

    vi.spyOn(window, 'confirm').mockReturnValue(true)
    fireEvent.click(screen.getByText('清空全部'))
    expect(await screen.findByText(/暂无排盘记录/)).toBeInTheDocument()
    expect(window.confirm).toHaveBeenCalled()
  })

  it('清空全部：取消确认时不清空', async () => {
    await clearHistory()
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    await addHistory(req, paipan(req))
    renderPage()
    await screen.findByText('一九九五年闰八月十四')

    vi.spyOn(window, 'confirm').mockReturnValue(false)
    fireEvent.click(screen.getByText('清空全部'))
    expect(screen.getByText('一九九五年闰八月十四')).toBeInTheDocument()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })
})
