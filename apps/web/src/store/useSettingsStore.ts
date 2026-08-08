import { create } from 'zustand'

const STORAGE_KEY = 'bazi-settings'

interface SettingsState {
  trueSolarDefault: boolean
  toggleTrueSolarDefault: () => void
}

function load(): boolean {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as { trueSolarDefault?: boolean }).trueSolarDefault === true : false
  } catch {
    return false
  }
}

export const useSettingsStore = create<SettingsState>((set) => ({
  trueSolarDefault: load(),
  toggleTrueSolarDefault: () =>
    set((state) => {
      const next = !state.trueSolarDefault
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ trueSolarDefault: next }))
      } catch {
        /* localStorage 不可用时静默降级 */
      }
      return { trueSolarDefault: next }
    }),
}))
