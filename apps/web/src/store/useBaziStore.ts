import { create } from 'zustand'
import type { PaipanRequest, PaipanResult } from '@/types/bazi'

interface BaziState {
  request: PaipanRequest | null
  result: PaipanResult | null
  setResult: (request: PaipanRequest, result: PaipanResult) => void
  clear: () => void
}

export const useBaziStore = create<BaziState>((set) => ({
  request: null,
  result: null,
  setResult: (request, result) => set({ request, result }),
  clear: () => set({ request: null, result: null }),
}))
