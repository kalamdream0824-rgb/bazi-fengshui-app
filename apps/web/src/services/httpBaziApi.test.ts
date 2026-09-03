import { afterEach, describe, expect, it, vi } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import type { PaipanRequest } from '@/types/bazi'
import { useAuthStore } from '@/store/useAuthStore'
import { HttpBaziApi } from './httpBaziApi'

describe('HttpBaziApi', () => {
  afterEach(() => {
    useAuthStore.getState().clear()
    vi.unstubAllGlobals()
  })

  it('把原始出生时间、地点和真太阳时开关交给后端统一计算', async () => {
    useAuthStore.getState().setAuth('record-token', 'tester')
    const request: PaipanRequest = {
      name: '林先生',
      gender: 'male',
      solarDateTime: '1995-10-08T13:05:00',
      birthPlace: '广东省 深圳市',
      trueSolarTime: true,
    }
    const response = paipan(request)
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(response), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await new HttpBaziApi().paipan(request)

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/records', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(request),
    }))
    expect(result.trueSolar).toEqual(response.trueSolar)
  })
})
