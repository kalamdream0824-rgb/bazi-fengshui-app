import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { explain } from '@/lib/explainer'
import { ExplainCard } from './ExplainCard'

const result = paipan({ gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false })
const explanation = explain(result)

describe('ExplainCard 解读卡', () => {
  it('完整模式：渲染总览与全部解读块', () => {
    render(<ExplainCard explanation={explanation} />)
    expect(screen.getByText('读懂这张盘')).toBeInTheDocument()
    expect(screen.getByText('日主与五行')).toBeInTheDocument()
    expect(screen.getByText('十神性格')).toBeInTheDocument()
  })

  it('单块模式：只渲染指定块（专业细盘页签用）', () => {
    render(<ExplainCard explanation={explanation} blockKey="dayun" />)
    expect(screen.getByText('当前大运')).toBeInTheDocument()
    expect(screen.queryByText('日主与五行')).not.toBeInTheDocument()
  })

  it('单块模式：块不存在时渲染为空', () => {
    const { container } = render(<ExplainCard explanation={{ overview: [], blocks: [] }} blockKey="dayun" />)
    expect(container.firstChild).toBeNull()
  })
})
