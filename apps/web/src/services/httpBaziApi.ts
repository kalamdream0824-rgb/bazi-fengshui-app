import type { PaipanRequest, PaipanResult } from '@/types/bazi'
import { enrichResult } from '@/lib/enrichResult'
import type { BaziApi } from './baziApi'
import { authFetch } from './http'
import { apiUrl } from './apiConfig'

/** 上线实现：调用后端 REST API（契约见 contracts/openapi.yaml） */
export class HttpBaziApi implements BaziApi {
  async paipan(req: PaipanRequest): Promise<PaipanResult> {
    const res = await authFetch(apiUrl('/v1/records'), {
      method: 'POST',
      body: JSON.stringify(req),
    })
    if (!res.ok) {
      throw new Error(`排盘请求失败（HTTP ${res.status}）`)
    }
    return enrichResult((await res.json()) as PaipanResult)
  }
}
