import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it } from 'vitest'
import App from './App'

function renderAt(path: string) {
  window.history.pushState({}, '', path)
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>,
  )
}

afterEach(() => {
  window.history.pushState({}, '', '/')
})

describe('底部导航显示范围', () => {
  it('主页面与「我的」二级页（会员中心/历史）保留底部导航', async () => {
    for (const path of ['/profile', '/membership', '/history']) {
      const { unmount } = renderAt(path)
      expect(await screen.findByLabelText('底部导航')).toBeInTheDocument()
      unmount()
    }
  })

  it('命书报告作为沉浸阅读页，保留返回但不显示底部导航', async () => {
    renderAt('/report')
    expect(await screen.findByLabelText('返回')).toBeInTheDocument()
    expect(screen.queryByLabelText('底部导航')).not.toBeInTheDocument()
  })

  it('登录页不显示底部导航', async () => {
    renderAt('/auth')
    expect(screen.queryByLabelText('底部导航')).not.toBeInTheDocument()
  })
})
