import { useQuery, useQueryClient } from '@tanstack/react-query'
import { listCloudRecords } from '@/services/cloudSync'
import { listHistory } from '@/services/historyStore'
import { useAuthStore } from '@/store/useAuthStore'

const KEY = ['history'] as const

export function useHistory() {
  const queryClient = useQueryClient()
  const token = useAuthStore((s) => s.token)
  const query = useQuery({
    queryKey: KEY,
    queryFn: () => (import.meta.env.VITE_API_MODE === 'http' && token ? listCloudRecords() : listHistory()),
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: KEY })
  return { ...query, invalidate }
}
