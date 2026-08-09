import { describe, expect, it } from 'vitest'
import { useAuthStore } from './useAuthStore'

describe('useAuthStore', () => {
  it('登录状态持久化与清除', () => {
    useAuthStore.getState().setAuth('token-123', 'tester')
    expect(useAuthStore.getState().token).toBe('token-123')
    expect(window.localStorage.getItem('bazi-auth')).toContain('tester')

    useAuthStore.getState().clear()
    expect(useAuthStore.getState().token).toBeNull()
    expect(window.localStorage.getItem('bazi-auth')).toBeNull()
  })
})
