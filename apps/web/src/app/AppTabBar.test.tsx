import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import App from './App'

function renderAt(path: string) {
  window.history.pushState({}, '', path)
  return render(<App />)
}

afterEach(() => {
  window.history.pushState({}, '', '/')
})

describe('底部导航显示范围', () => {
  it('主页面与「我的」二级页（会员中心/历史/命书）保留底部导航', async () => {
    for (const path of ['/profile', '/membership', '/history', '/report']) {
      const { unmount } = renderAt(path)
      expect(await screen.findByLabelText('底部导航')).toBeInTheDocument()
      unmount()
    }
  })

  it('登录页不显示底部导航', async () => {
    renderAt('/auth')
    expect(screen.queryByLabelText('底部导航')).not.toBeInTheDocument()
  })
})
