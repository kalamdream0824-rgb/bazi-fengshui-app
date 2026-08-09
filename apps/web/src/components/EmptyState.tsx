import { Link } from 'react-router-dom'

interface EmptyStateProps {
  title: string
  hint?: string
  linkTo?: string
  linkText?: string
}

export function EmptyState({ title, hint, linkTo, linkText }: EmptyStateProps) {
  return (
    <div className="placeholder">
      {title}
      {hint ? (
        <>
          <br />
          {hint}
        </>
      ) : null}
      {linkTo && linkText ? (
        <>
          <br />
          <Link to={linkTo} style={{ color: 'var(--red)' }}>
            {linkText}
          </Link>
        </>
      ) : null}
    </div>
  )
}
