import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { useBaziStore } from '@/store/useBaziStore'
import { ReportPage } from './ReportPage'

const request = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ReportPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ReportPage', () => {
  it('左侧章节按钮切换为对应的真实命盘摘要', () => {
    useBaziStore.getState().setResult(request, paipan(request))
    renderPage()

    expect(screen.getByRole('button', { name: '第壹章：命盘概览' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('button', { name: '生成命书' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '第贰章：五行与用神' }))
    expect(screen.getByRole('heading', { name: '五行与用神' })).toBeInTheDocument()
    expect(screen.getByLabelText('五行分布')).toBeInTheDocument()
    expect(screen.getByText(/暂不作确定性推断/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '第叁章：大运流年' }))
    expect(screen.getByRole('heading', { name: '大运流年' })).toBeInTheDocument()
    expect(screen.getByText('今年流年')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '第肆章：十神详析' }))
    expect(screen.getByRole('heading', { name: '十神详析' })).toBeInTheDocument()
    expect(screen.getByText('日主十神')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '第伍章：综合建议' }))
    expect(screen.getByRole('heading', { name: '综合建议' })).toBeInTheDocument()
    expect(screen.getByText('传统文化研究参考')).toBeInTheDocument()
  })

  afterEach(() => useBaziStore.getState().clear())
})
