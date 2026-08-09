import { useQuery, useQueryClient } from '@tanstack/react-query'
import { listHistory } from '@/services/historyStore'

const KEY = ['history'] as const

export function useHistory() {
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: KEY, queryFn: listHistory })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: KEY })
  return { ...query, invalidate }
}
