import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { DateTimePicker } from './DateTimePicker'

describe('DateTimePicker', () => {
  it('展示已选时间与时辰，确定后回传原值', () => {
    const onChange = vi.fn()
    render(<DateTimePicker value="1995-10-08T14:30" onChange={onChange} />)

    expect(screen.getByText('未时')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('选择出生时间'))
    expect(screen.getByText('出生时间')).toBeInTheDocument()

    fireEvent.click(screen.getByText('确定'))
    expect(onChange).toHaveBeenCalledWith('1995-10-08T14:30')
  })

  it('取消不触发变更', () => {
    const onChange = vi.fn()
    render(<DateTimePicker value="1995-10-08T14:30" onChange={onChange} />)

    fireEvent.click(screen.getByLabelText('选择出生时间'))
    fireEvent.click(screen.getByText('取消'))
    expect(onChange).not.toHaveBeenCalled()
  })
})
