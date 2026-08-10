import { MemoryRouter } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DayPickerPage } from './DayPickerPage'

describe('DayPickerPage', () => {
  it('渲染事项选择与入口', () => {
    render(
      <MemoryRouter>
        <DayPickerPage />
      </MemoryRouter>,
    )
    expect(screen.getByText('选择事项')).toBeInTheDocument()
    expect(screen.getByText('嫁娶')).toBeInTheDocument()
    expect(screen.getByText('动土')).toBeInTheDocument()
  })
})
