import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

interface TopBarProps {
  title: string
  onBack?: () => void
  right?: ReactNode
  tone?: 'default' | 'night'
}

export function TopBar({ title, onBack, right, tone = 'default' }: TopBarProps) {
  const navigate = useNavigate()
  return (
    <div className={`top ${tone === 'night' ? 'top-night' : ''}`}>
      <button className="icon-btn" aria-label="返回" onClick={onBack ?? (() => navigate(-1))}>
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div className="title">
        <span className="seal">命</span>
        {title}
      </div>
      <div className="slot">{right}</div>
    </div>
  )
}
