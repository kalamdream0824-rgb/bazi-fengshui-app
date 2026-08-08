import type { PaipanRequest, PaipanResult } from '@/types/bazi'
import { HttpBaziApi } from './httpBaziApi'
import { MockBaziApi } from './mockBaziApi'

export interface BaziApi {
  paipan(req: PaipanRequest): Promise<PaipanResult>
}

let instance: BaziApi | null = null

export function getBaziApi(): BaziApi {
  if (!instance) {
    instance = import.meta.env.VITE_API_MODE === 'http' ? new HttpBaziApi() : new MockBaziApi()
  }
  return instance
}
