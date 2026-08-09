import type { PaipanRequest, PaipanResult } from '@/types/bazi'
import { adjustRequestForTrueSolar, enrichResult } from '@/lib/enrichResult'
import { useAuthStore } from '@/store/useAuthStore'
import type { BaziApi } from './baziApi'

/** 上线实现：调用后端 REST API（契约见 contracts/openapi.yaml） */
export class HttpBaziApi implements BaziApi {
  async paipan(req: PaipanRequest): Promise<PaipanResult> {
    const { request, trueSolar } = adjustRequestForTrueSolar(req)
    const token = useAuthStore.getState().token
    const res = await fetch('/api/v1/records', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify(request),
    })
    if (res.status === 401) {
      useAuthStore.getState().clear()
      throw new Error('登录已过期，请重新登录')
    }
    if (!res.ok) {
      throw new Error(`排盘请求失败（HTTP ${res.status}）`)
    }
    const result = enrichResult((await res.json()) as PaipanResult)
    if (trueSolar) {
      result.trueSolar = trueSolar
    }
    return result
  }
}
