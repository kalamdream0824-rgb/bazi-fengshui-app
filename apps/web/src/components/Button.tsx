import type { ButtonHTMLAttributes, ReactNode } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary'
  block?: boolean
  children: ReactNode
}

export function Button({ variant = 'secondary', block = false, className = '', children, ...rest }: ButtonProps) {
  const cls = ['btn', variant === 'primary' ? 'primary' : '', block ? 'block' : '', className].filter(Boolean).join(' ')
  return (
    <button type="button" className={cls} {...rest}>
      {children}
    </button>
  )
}

export function ButtonRow({ children }: { children: ReactNode }) {
  return <div className="row3">{children}</div>
}
