interface SwitchProps {
  checked: boolean
  onChange: (checked: boolean) => void
  title: string
  desc?: string
}

export function Switch({ checked, onChange, title, desc }: SwitchProps) {
  return (
    <div className="switch-row">
      <div>
        <div className="t">{title}</div>
        {desc ? <div className="d">{desc}</div> : null}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        className={`sw ${checked ? 'on' : ''}`}
        onClick={() => onChange(!checked)}
      />
    </div>
  )
}
