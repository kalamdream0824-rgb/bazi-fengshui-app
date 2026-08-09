import type { ReactNode } from 'react'

interface RecordRowProps {
  title: string
  subtitle: string
  onClick?: () => void
  action?: ReactNode
}

export function RecordRow({ title, subtitle, onClick, action }: RecordRowProps) {
  return (
    <div className="row" onClick={onClick}>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontWeight: 600 }}>{title}</div>
        <div className="meta-line" style={{ marginTop: 3 }}>
          {subtitle}
        </div>
      </div>
      {action ?? <span className="arrow">›</span>}
    </div>
  )
}
