import type { PaipanRequest, PaipanResult } from '@/types/bazi'
import { adjustRequestForTrueSolar, enrichResult } from '@/lib/enrichResult'
import type { BaziApi } from './baziApi'
import { authFetch } from './http'

/** 上线实现：调用后端 REST API（契约见 contracts/openapi.yaml） */
export class HttpBaziApi implements BaziApi {
  async paipan(req: PaipanRequest): Promise<PaipanResult> {
    const { request, trueSolar } = adjustRequestForTrueSolar(req)
    const res = await authFetch('/api/v1/records', {
      method: 'POST',
      body: JSON.stringify(request),
    })
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
