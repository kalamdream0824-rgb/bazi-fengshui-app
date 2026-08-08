import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { RegionSelect } from './RegionSelect'

function Harness({ initial = '' }: { initial?: string }) {
  const [value, setValue] = useState(initial)
  return (
    <div>
      <span data-testid="value">{value}</span>
      <RegionSelect value={value} onChange={setValue} />
    </div>
  )
}

describe('RegionSelect', () => {
  it('选择省份后城市联动，输出「省 市」格式', () => {
    render(<Harness />)

    fireEvent.change(screen.getByLabelText('省份'), { target: { value: '广东省' } })
    expect(screen.getByTestId('value').textContent).toBe('广东省')

    fireEvent.change(screen.getByLabelText('城市'), { target: { value: '深圳市' } })
    expect(screen.getByTestId('value').textContent).toBe('广东省 深圳市')
  })

  it('未选省份时城市下拉禁用', () => {
    render(<Harness />)
    expect(screen.getByLabelText('城市')).toBeDisabled()
  })

  it('传入已选值可正确回显', () => {
    render(<Harness initial="浙江省 杭州市" />)
    expect((screen.getByLabelText('省份') as HTMLSelectElement).value).toBe('浙江省')
    expect((screen.getByLabelText('城市') as HTMLSelectElement).value).toBe('杭州市')
  })
})
