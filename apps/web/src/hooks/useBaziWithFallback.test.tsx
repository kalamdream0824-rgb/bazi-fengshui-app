import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { addHistory, clearHistory } from '@/services/historyStore'
import { useAuthStore } from '@/store/useAuthStore'
import { useBaziWithFallback } from './useBaziWithFallback'

function Probe() {
  const { result } = useBaziWithFallback()
  return <div>{result ? result.lunarText : 'none'}</div>
}

function renderProbe() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <Probe />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('useBaziWithFallback', () => {
  it('无历史时返回空', async () => {
    useAuthStore.getState().clear()
    await clearHistory()
    renderProbe()
    expect(await screen.findByText('none')).toBeInTheDocument()
  })

  it('刷新后有历史记录时回退到最新命盘', async () => {
    useAuthStore.getState().clear()
    await clearHistory()
    const req = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
    await addHistory(req, paipan(req))
    renderProbe()
    expect(await screen.findByText('一九九五年闰八月十四')).toBeInTheDocument()
  })
})
