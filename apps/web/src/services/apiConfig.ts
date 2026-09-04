/**
 * API 服务地址。
 *
 * 默认使用同源 /api，便于通过 Vite 代理开发；部署前端到独立域名时，
 * 设置 VITE_API_BASE_URL，例如 https://api.example.com/api。
 */
const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

export const API_BASE_URL = (configuredBaseUrl || '/api').replace(/\/$/, '')

export function apiUrl(path: string): string {
  return `${API_BASE_URL}/${path.replace(/^\//, '')}`
}
