import { useEffect, useMemo, useRef, useState } from 'react'
import {
  buildYears,
  daysInMonth,
  formatDateTime,
  pad2,
  parseInput,
  timeToShichen,
  type DateTimeValue,
} from '@/lib/datePicker'

const ITEM_HEIGHT = 36
const WHEEL_HEIGHT = 180

interface WheelProps {
  options: string[]
  value: string
  label: string
  onChange: (value: string) => void
}

function Wheel({ options, value, label, onChange }: WheelProps) {
  const ref = useRef<HTMLDivElement>(null)
  const index = Math.max(options.indexOf(value), 0)

  useEffect(() => {
    const el = ref.current
    if (el) el.scrollTop = index * ITEM_HEIGHT
  }, [index])

  const handleScroll = () => {
    const el = ref.current
    if (!el) return
    const i = Math.min(Math.max(Math.round(el.scrollTop / ITEM_HEIGHT), 0), options.length - 1)
    const next = options[i]
    if (next && next !== value) onChange(next)
  }

  return (
    <div className="wheel-col">
      <div className="wheel" ref={ref} onScroll={handleScroll}>
        <div style={{ height: (WHEEL_HEIGHT - ITEM_HEIGHT) / 2 }} />
        {options.map((opt) => (
          <div key={opt} className={`wheel-item ${opt === value ? 'active' : ''}`} style={{ height: ITEM_HEIGHT }}>
            {opt}
          </div>
        ))}
        <div style={{ height: (WHEEL_HEIGHT - ITEM_HEIGHT) / 2 }} />
      </div>
      <div className="wheel-label">{label}</div>
    </div>
  )
}

interface DateTimePickerProps {
  value: string
  onChange: (value: string) => void
}

export function DateTimePicker({ value, onChange }: DateTimePickerProps) {
  const [open, setOpen] = useState(false)
  const [temp, setTemp] = useState<DateTimeValue>(() => parseInput(value))

  const years = useMemo(() => buildYears().map(String), [])
  const months = useMemo(() => Array.from({ length: 12 }, (_, i) => String(i + 1)), [])
  const days = useMemo(
    () => Array.from({ length: daysInMonth(temp.y, temp.m) }, (_, i) => String(i + 1)),
    [temp.y, temp.m],
  )
  const hours = useMemo(() => Array.from({ length: 24 }, (_, i) => pad2(i)), [])
  const minutes = useMemo(() => Array.from({ length: 60 }, (_, i) => pad2(i)), [])

  const openSheet = () => {
    setTemp(parseInput(value))
    setOpen(true)
  }

  const confirm = () => {
    onChange(formatDateTime(temp))
    setOpen(false)
  }

  const parsed = parseInput(value)

  return (
    <>
      <button type="button" className="input-box picker-field" onClick={openSheet} aria-label="选择出生时间">
        <span>
          {parsed.y}-{pad2(parsed.m)}-{pad2(parsed.d)} {pad2(parsed.h)}:{pad2(parsed.min)}
        </span>
        <span className="shichen">{timeToShichen(parsed.h)}时</span>
      </button>

      {open && (
        <div className="picker-mask" onClick={() => setOpen(false)}>
          <div className="picker-panel" onClick={(e) => e.stopPropagation()}>
            <div className="picker-head">
              <button type="button" onClick={() => setOpen(false)}>
                取消
              </button>
              <span>出生时间</span>
              <button type="button" onClick={confirm}>
                确定
              </button>
            </div>
            <div className="wheels">
              <Wheel
                options={years}
                value={String(temp.y)}
                label="年"
                onChange={(v) => setTemp((t) => ({ ...t, y: Number(v) }))}
              />
              <Wheel
                options={months}
                value={String(temp.m)}
                label="月"
                onChange={(v) => setTemp((t) => ({ ...t, m: Number(v) }))}
              />
              <Wheel
                options={days}
                value={String(temp.d)}
                label="日"
                onChange={(v) => setTemp((t) => ({ ...t, d: Number(v) }))}
              />
              <Wheel
                options={hours}
                value={pad2(temp.h)}
                label="时"
                onChange={(v) => setTemp((t) => ({ ...t, h: Number(v) }))}
              />
              <Wheel
                options={minutes}
                value={pad2(temp.min)}
                label="分"
                onChange={(v) => setTemp((t) => ({ ...t, min: Number(v) }))}
              />
              <div className="picker-highlight" />
            </div>
            <div className="picker-hint">
              已选：{formatDateTime(temp)} · {timeToShichen(temp.h)}时
            </div>
          </div>
        </div>
      )}
    </>
  )
}
