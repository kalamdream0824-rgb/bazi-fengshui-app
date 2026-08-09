import { MemoryRouter } from 'react-router-dom'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EmptyState } from './EmptyState'
import { FooterNote } from './FooterNote'
import { RecordRow } from './RecordRow'

describe('FooterNote', () => {
  it('渲染说明文字', () => {
    render(<FooterNote>仅供传统文化研究参考</FooterNote>)
    expect(screen.getByText('仅供传统文化研究参考')).toHaveClass('footer-note')
  })
})

describe('EmptyState', () => {
  it('渲染标题与引导链接', () => {
    render(
      <MemoryRouter>
        <EmptyState title="暂无数据" linkTo="/input" linkText="去排盘" />
      </MemoryRouter>,
    )
    expect(screen.getByText('暂无数据')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '去排盘' })).toHaveAttribute('href', '/input')
  })
})

describe('RecordRow', () => {
  it('点击触发回调并显示箭头', () => {
    const onClick = vi.fn()
    render(<RecordRow title="一九九五年闰八月十四" subtitle="1995-10-08 · 生肖 猪" onClick={onClick} />)
    fireEvent.click(screen.getByText('一九九五年闰八月十四'))
    expect(onClick).toHaveBeenCalled()
  })

  it('自定义 action 替代箭头', () => {
    render(<RecordRow title="标题" subtitle="副标题" action={<button type="button">删除</button>} />)
    expect(screen.getByRole('button', { name: '删除' })).toBeInTheDocument()
    expect(screen.queryByText('›')).not.toBeInTheDocument()
  })
})
