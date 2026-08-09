import { create } from 'zustand'

const STORAGE_KEY = 'bazi-auth'

interface AuthState {
  token: string | null
  username: string | null
  setAuth: (token: string, username: string) => void
  clear: () => void
}

function load(): Pick<AuthState, 'token' | 'username'> {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return { token: null, username: null }
    const parsed = JSON.parse(raw) as { token?: string; username?: string }
    return { token: parsed.token ?? null, username: parsed.username ?? null }
  } catch {
    return { token: null, username: null }
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  ...load(),
  setAuth: (token, username) => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, username }))
    } catch {
      /* localStorage 不可用时静默降级 */
    }
    set({ token, username })
  },
  clear: () => {
    try {
      window.localStorage.removeItem(STORAGE_KEY)
    } catch {
      /* ignore */
    }
    set({ token: null, username: null })
  },
}))
