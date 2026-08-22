import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={['card', className].filter(Boolean).join(' ')}>{children}</div>
}

export function CardTitle({ children, hint }: { children: ReactNode; hint?: string }) {
  return (
    <div className="sec-title">
      {children}
      {hint ? <small>{hint}</small> : null}
    </div>
  )
}
