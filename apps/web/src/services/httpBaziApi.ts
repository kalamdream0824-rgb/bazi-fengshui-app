import type { PaipanRequest, PaipanResult } from '@/types/bazi'
import type { BaziApi } from './baziApi'

/** 上线实现：调用后端 REST API（契约见 contracts/openapi.yaml） */
export class HttpBaziApi implements BaziApi {
  async paipan(req: PaipanRequest): Promise<PaipanResult> {
    const res = await fetch('/api/v1/records', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    })
    if (!res.ok) {
      throw new Error(`排盘请求失败（HTTP ${res.status}）`)
    }
    return (await res.json()) as PaipanResult
  }
}
