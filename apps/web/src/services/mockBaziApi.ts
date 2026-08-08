import { paipan } from '@/lib/baziMapper'
import type { PaipanRequest, PaipanResult } from '@/types/bazi'
import type { BaziApi } from './baziApi'

/** 开发期实现：本地 lunar-javascript 真算，模拟网络延迟 */
export class MockBaziApi implements BaziApi {
  async paipan(req: PaipanRequest): Promise<PaipanResult> {
    await new Promise((resolve) => setTimeout(resolve, 220))
    return paipan(req)
  }
}
