import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { SettingsPage } from './SettingsPage'

function renderPage() {
  return render(
    <MemoryRouter>
      <SettingsPage />
    </MemoryRouter>,
  )
}

describe('SettingsPage 关于与合规', () => {
  it('渲染标题与四块内容', () => {
    renderPage()
    expect(screen.getByText('设置')).toBeInTheDocument()
    expect(screen.getByText('关于与合规')).toBeInTheDocument()
    expect(screen.getByText('解读口径')).toBeInTheDocument()
    expect(screen.getByText('免责声明')).toBeInTheDocument()
    expect(screen.getByText('数据与隐私')).toBeInTheDocument()
  })

  it('包含合规关键词与版本', () => {
    renderPage()
    const body = document.body.textContent ?? ''
    expect(body).toContain('仅供娱乐与参考')
    expect(body).toContain('不构成任何人生、医疗、投资或法律建议')
    expect(body).toContain('版本 v0.36')
  })
})
