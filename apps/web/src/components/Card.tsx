import type { ReactNode } from 'react'

export function Card({ children }: { children: ReactNode }) {
  return <div className="card">{children}</div>
}

export function CardTitle({ children, hint }: { children: ReactNode; hint?: string }) {
  return (
    <div className="sec-title">
      {children}
      {hint ? <small>{hint}</small> : null}
    </div>
  )
}
