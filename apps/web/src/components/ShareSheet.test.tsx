import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ShareSheet } from './ShareSheet'

describe('ShareSheet', () => {
  it('渲染图片预览与操作按钮，关闭回调生效', () => {
    const onClose = vi.fn()
    render(<ShareSheet open imageUrl="data:image/jpeg;base64,xx" text="文案" filename="share.jpg" onClose={onClose} />)
    expect(screen.getByAltText('排盘分享卡片')).toHaveAttribute('src', 'data:image/jpeg;base64,xx')
    expect(screen.getByText('保存图片')).toBeInTheDocument()
    expect(screen.getByText('复制文案')).toBeInTheDocument()
    fireEvent.click(screen.getByText('关闭'))
    expect(onClose).toHaveBeenCalled()
  })
})
