import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useBaziWithFallback } from '@/hooks/useBaziWithFallback'
import { paipan } from '@/lib/baziMapper'
import { ChartPage } from './ChartPage'

vi.mock('@/hooks/useBaziWithFallback', () => ({ useBaziWithFallback: vi.fn() }))

const request = {
  name: '林先生',
  gender: 'male' as const,
  solarDateTime: '1995-10-08T14:30:00',
  birthPlace: '上海',
  trueSolarTime: false,
}

describe('ChartPage', () => {
  beforeEach(() => {
    vi.mocked(useBaziWithFallback).mockReturnValue({
      request,
      result: paipan(request),
      loading: false,
    })
  })

  it('点击查看命书直接进入我的命书页面', () => {
    render(
      <MemoryRouter initialEntries={['/chart']}>
        <Routes>
          <Route path="/chart" element={<ChartPage />} />
          <Route path="/reports" element={<h1>我的命书页面</h1>} />
        </Routes>
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: '查看命书' }))

    expect(screen.getByRole('heading', { name: '我的命书页面' })).toBeInTheDocument()
  })
})
