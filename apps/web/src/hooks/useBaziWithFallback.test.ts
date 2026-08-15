import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useBaziStore } from '@/store/useBaziStore'
import { useBaziWithFallback } from './useBaziWithFallback'
import { useHistory } from './useHistory'

vi.mock('./useHistory', () => ({
  useHistory: vi.fn(),
}))

const mockUseHistory = vi.mocked(useHistory)
const record = {
  id: 1,
  request: { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false },
  result: { lunarText: '一九九五年闰八月十四' } as never,
  createdAt: '2026-08-16T00:00:00',
}

describe('useBaziWithFallback 刷新兜底（回归：历史加载中不跳转）', () => {
  beforeEach(() => {
    useBaziStore.getState().clear()
  })

  it('历史加载中：loading=true 且不返回结果（防止页面立即跳转）', () => {
    mockUseHistory.mockReturnValue({ data: [], isPending: true, isLoading: true } as never)
    const { result } = renderHook(() => useBaziWithFallback())
    expect(result.current.loading).toBe(true)
    expect(result.current.result).toBeNull()
  })

  it('历史加载完成：latest 兜底写回 store 并返回', async () => {
    mockUseHistory.mockReturnValue({ data: [record], isPending: false, isLoading: false } as never)
    const { result } = renderHook(() => useBaziWithFallback())
    await waitFor(() => expect(useBaziStore.getState().result).not.toBeNull())
    expect(result.current.loading).toBe(false)
    expect(result.current.result).toBe(record.result)
    expect(result.current.request).toBe(record.request)
  })

  it('当前会话结果优先于历史', () => {
    const sessionResult = { lunarText: '会话结果' } as never
    act(() => useBaziStore.getState().setResult(record.request, sessionResult))
    mockUseHistory.mockReturnValue({ data: [record], isPending: false, isLoading: false } as never)
    const { result } = renderHook(() => useBaziWithFallback())
    expect(result.current.result).toBe(sessionResult)
  })
})
