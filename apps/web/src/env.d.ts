/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_MODE?: 'mock' | 'http'
  /** 浏览器实际请求的 API 根地址，默认 /api。 */
  readonly VITE_API_BASE_URL?: string
  /** 仅用于 Vite 开发代理的后端地址。 */
  readonly VITE_API_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
