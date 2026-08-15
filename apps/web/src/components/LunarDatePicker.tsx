import { useMemo, useState } from 'react'
import { buildYears, pad2, timeToShichen } from '@/lib/datePicker'
import {
  lunarMonthDayCount,
  lunarText,
  lunarToSolarDateTime,
  lunarYearLeapMonth,
  solarText,
  solarToLunar,
  type LunarDateValue,
} from '@/lib/lunarInput'
import { Wheel } from './DateTimePicker'

interface LunarDatePickerProps {
  value: string
  onChange: (solarIso: string) => void
}

export function LunarDatePicker({ value, onChange }: LunarDatePickerProps) {
  const [open, setOpen] = useState(false)
  const [temp, setTemp] = useState<LunarDateValue>(() => solarToLunar(value))

  const years = useMemo(() => buildYears().map(String), [])
  const hours = useMemo(() => Array.from({ length: 24 }, (_, i) => pad2(i)), [])
  const minutes = useMemo(() => Array.from({ length: 60 }, (_, i) => pad2(i)), [])

  const leapMonth = lunarYearLeapMonth(temp.year)
  const monthOptions = useMemo(() => {
    const opts: string[] = []
    for (let m = 1; m <= 12; m += 1) {
      if (leapMonth === m) opts.push(`闰${m}月`)
      opts.push(`${m}月`)
    }
    return opts
  }, [leapMonth])

  const dayCount = lunarMonthDayCount(temp.year, temp.month, temp.leap)
  const days = useMemo(
    () => Array.from({ length: dayCount }, (_, i) => String(i + 1)),
    [dayCount],
  )

  const setMonth = (label: string) => {
    if (label.startsWith('闰')) {
      setTemp((t) => ({ ...t, month: Number(label.slice(1, -1)), leap: true }))
    } else {
      setTemp((t) => ({ ...t, month: Number(label.slice(0, -1)), leap: false }))
    }
  }

  const confirm = () => {
    onChange(lunarToSolarDateTime(temp))
    setOpen(false)
  }

  return (
    <>
      <button type="button" className="input-box picker-field" onClick={() => setOpen(true)} aria-label="选择农历出生时间">
        <span>农历 {lunarText(temp)} {pad2(temp.hour)}:{pad2(temp.minute)}</span>
        <span className="shichen">{timeToShichen(temp.hour)}时</span>
      </button>

      {open && (
        <div className="picker-mask" onClick={() => setOpen(false)}>
          <div className="picker-panel" onClick={(e) => e.stopPropagation()}>
            <div className="picker-head">
              <button type="button" onClick={() => setOpen(false)}>
                取消
              </button>
              <span>农历出生时间</span>
              <button type="button" onClick={confirm}>
                确定
              </button>
            </div>
            <div className="wheels">
              <Wheel
                options={years}
                value={String(temp.year)}
                label="年"
                onChange={(v) => setTemp((t) => ({ ...t, year: Number(v) }))}
              />
              <Wheel
                options={monthOptions}
                value={`${temp.leap ? '闰' : ''}${temp.month}月`}
                label="月"
                onChange={setMonth}
              />
              <Wheel
                options={days}
                value={String(temp.day)}
                label="日"
                onChange={(v) => setTemp((t) => ({ ...t, day: Number(v) }))}
              />
              <Wheel
                options={hours}
                value={pad2(temp.hour)}
                label="时"
                onChange={(v) => setTemp((t) => ({ ...t, hour: Number(v) }))}
              />
              <Wheel
                options={minutes}
                value={pad2(temp.minute)}
                label="分"
                onChange={(v) => setTemp((t) => ({ ...t, minute: Number(v) }))}
              />
              <div className="picker-highlight" />
            </div>
            <div className="picker-hint">
              农历 {lunarText(temp)} · 对应公历 {solarText(lunarToSolarDateTime(temp))}
            </div>
          </div>
        </div>
      )}
    </>
  )
}
