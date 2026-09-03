import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getBaziApi } from '@/services/baziApi'
import { useToastStore } from '@/store/useToastStore'
import { InputPage } from './InputPage'

vi.mock('@/services/baziApi', () => ({ getBaziApi: vi.fn() }))

describe('InputPage 真太阳时地点校验', () => {
  const paipan = vi.fn()

  beforeEach(() => {
    paipan.mockReset()
    vi.mocked(getBaziApi).mockReturnValue({ paipan })
    useToastStore.setState({ message: null })
  })

  it('只选择省份时不提交，要求继续选择城市', () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <InputPage />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    fireEvent.click(screen.getByRole('switch'))
    fireEvent.change(screen.getByLabelText('省份'), { target: { value: '广东省' } })
    fireEvent.click(screen.getByRole('button', { name: '开始排盘' }))

    expect(paipan).not.toHaveBeenCalled()
    expect(useToastStore.getState().message).toBe('开启真太阳时请完整选择出生省份和城市')
  })
})
