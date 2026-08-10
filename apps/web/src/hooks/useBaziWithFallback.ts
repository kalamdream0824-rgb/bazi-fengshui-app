import { useEffect } from 'react'
import { useHistory } from './useHistory'
import type { HistoryRecord } from '@/services/historyStore'
import { useBaziStore } from '@/store/useBaziStore'

export function useLatestRecord(): HistoryRecord | null {
  const { data: records = [] } = useHistory()
  return records[0] ?? null
}

/** 排盘数据：优先当前会话结果，刷新后回退到最新历史记录 */
export function useBaziWithFallback() {
  const request = useBaziStore((s) => s.request)
  const result = useBaziStore((s) => s.result)
  const setResult = useBaziStore((s) => s.setResult)
  const latest = useLatestRecord()

  useEffect(() => {
    if (!result && latest) {
      setResult(latest.request, latest.result)
    }
  }, [result, latest, setResult])

  return { request: request ?? latest?.request ?? null, result: result ?? latest?.result ?? null }
}
