import { useToastStore } from '@/store/useToastStore'

export function Toast() {
  const message = useToastStore((s) => s.message)
  return <div className={`toast ${message ? 'show' : ''}`}>{message}</div>
}
