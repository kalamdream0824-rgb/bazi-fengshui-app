import { MemoryRouter } from 'react-router-dom'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DailyPage } from './DailyPage'

describe('DailyPage', () => {
  it('切换明日后，宜忌与指引内容同步更新', () => {
    render(
      <MemoryRouter>
        <DailyPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('今日宜忌')).toBeInTheDocument()
    expect(screen.getByText('动土')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '明日' }))

    expect(screen.getByText('明日宜忌')).toBeInTheDocument()
    expect(screen.getByText('明日指引')).toBeInTheDocument()
    expect(screen.getByText('开市')).toBeInTheDocument()
    expect(screen.queryByText('动土')).not.toBeInTheDocument()
  })
})
