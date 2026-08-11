import { useAuthStore } from '@/store/useAuthStore'

/** 统一 HTTP 请求：注入 Bearer token；401 时清除本地登录态并抛明确错误。 */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const token = useAuthStore.getState().token
  const res = await fetch(input, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })
  if (res.status === 401) {
    useAuthStore.getState().clear()
    throw new Error('登录已过期，请重新登录')
  }
  return res
}
