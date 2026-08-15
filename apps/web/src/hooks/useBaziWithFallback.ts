import { useEffect } from 'react'
import { useHistory } from './useHistory'
import { useBaziStore } from '@/store/useBaziStore'

/** 排盘数据：优先当前会话结果，刷新后回退到最新历史记录 */
export function useBaziWithFallback() {
  const request = useBaziStore((s) => s.request)
  const result = useBaziStore((s) => s.result)
  const setResult = useBaziStore((s) => s.setResult)
  const { data: records = [], isPending, isLoading } = useHistory()
  const latest = records[0] ?? null
  const loading = isPending || isLoading

  useEffect(() => {
    if (!result && latest) {
      setResult(latest.request, latest.result)
    }
  }, [result, latest, setResult])

  return {
    request: request ?? latest?.request ?? null,
    result: result ?? latest?.result ?? null,
    loading,
  }
}
