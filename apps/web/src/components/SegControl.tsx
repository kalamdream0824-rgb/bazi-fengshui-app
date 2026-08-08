export interface SegOption {
  label: string
  value: string
}

interface SegControlProps {
  options: SegOption[]
  value: string
  onChange: (value: string) => void
  two?: boolean
  className?: string
}

export function SegControl({ options, value, onChange, two = false, className = '' }: SegControlProps) {
  const cls = ['seg', two ? 'two' : '', className].filter(Boolean).join(' ')
  return (
    <div className={cls}>
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          className={`opt ${value === opt.value ? 'active' : ''}`}
          onClick={() => onChange(opt.value)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}
