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

  it('展示后端真太阳时审计信息并突出跨时辰变化', () => {
    const result = paipan(request)
    result.trueSolar = {
      original: '1995-10-08T13:05',
      adjusted: '1995-10-08T12:53',
      offsetMinutes: -24,
      eotMinutes: 12.7,
      longitude: 114.05,
      originalShichen: '未',
      adjustedShichen: '午',
      boundaryChanged: true,
    }
    vi.mocked(useBaziWithFallback).mockReturnValue({ request, result, loading: false })

    render(
      <MemoryRouter initialEntries={['/chart']}>
        <Routes>
          <Route path="/chart" element={<ChartPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('真太阳时校正')).toBeInTheDocument()
    expect(screen.getByText(/原时间 1995-10-08 13:05 → 校正后 1995-10-08 12:53/)).toBeInTheDocument()
    expect(screen.getByText(/经度 114.05°E/)).toHaveTextContent('合计约 -11.3 分钟')
    expect(screen.getByText(/时辰由未时变为午时/)).toHaveTextContent('本命盘及命书已按校正后时间计算')
  })
})
