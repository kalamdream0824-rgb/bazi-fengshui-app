import { REGIONS } from '@/data/regions'

interface RegionSelectProps {
  value: string
  onChange: (value: string) => void
}

export function RegionSelect({ value, onChange }: RegionSelectProps) {
  const parts = value ? value.split(' ') : ['', '']
  const province = parts[0] ?? ''
  const city = parts[1] ?? ''
  const cities = REGIONS.find((r) => r.province === province)?.cities ?? []

  return (
    <div className="region-row">
      <select className="input-box" value={province} aria-label="省份" onChange={(e) => onChange(e.target.value || '')}>
        <option value="">请选择省份</option>
        {REGIONS.map((r) => (
          <option key={r.province} value={r.province}>
            {r.province}
          </option>
        ))}
      </select>
      <select
        className="input-box"
        value={city}
        aria-label="城市"
        disabled={!province}
        onChange={(e) => onChange(e.target.value ? `${province} ${e.target.value}` : province)}
      >
        <option value="">{province ? '请选择城市' : '请先选择省份'}</option>
        {cities.map((c) => (
          <option key={c} value={c}>
            {c}
          </option>
        ))}
      </select>
    </div>
  )
}
