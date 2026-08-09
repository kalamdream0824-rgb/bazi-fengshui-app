import { MemoryRouter } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DailyPage } from './DailyPage'

describe('DailyPage', () => {
  it('展示真实黄历内容且无示例占位', () => {
    render(
      <MemoryRouter>
        <DailyPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('宜忌')).toBeInTheDocument()
    expect(screen.getByText(/幸运色/)).toBeInTheDocument()
    expect(screen.getByText('明日预告')).toBeInTheDocument()
    expect(screen.queryByText(/示例/)).not.toBeInTheDocument()
  })
})
