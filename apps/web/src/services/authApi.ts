import type { AuthResponse } from '@/types/bazi'

function isHttpMode(): boolean {
  return import.meta.env.VITE_API_MODE === 'http'
}

async function postAuth(path: string, username: string, password: string): Promise<AuthResponse> {
  if (!isHttpMode()) {
    // 开发期（Mock）模拟登录，不接后端
    return { token: 'mock-token', username }
  }
  const res = await fetch(`/api/v1/auth${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) {
    const data = (await res.json().catch(() => null)) as { message?: string } | null
    throw new Error(data?.message || (path === '/login' ? '登录失败' : '注册失败'))
  }
  return (await res.json()) as AuthResponse
}

export function register(username: string, password: string): Promise<AuthResponse> {
  return postAuth('/register', username, password)
}

export function login(username: string, password: string): Promise<AuthResponse> {
  return postAuth('/login', username, password)
}
