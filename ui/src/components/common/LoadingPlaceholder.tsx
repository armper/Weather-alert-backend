interface LoadingPlaceholderProps {
  title: string
  copy?: string
  lineCount?: number
  className?: string
  compact?: boolean
}

export function LoadingPlaceholder({
  title,
  copy,
  lineCount = 2,
  className,
  compact = false,
}: LoadingPlaceholderProps) {
  return (
    <div
      className={`loading-placeholder${compact ? ' is-compact' : ''}${className ? ` ${className}` : ''}`}
      role="status"
      aria-live="polite"
    >
      <span className="loading-placeholder-spinner" aria-hidden />
      <div className="loading-placeholder-copy-block">
        <p className="loading-placeholder-title">{title}</p>
        {copy ? <p className="loading-placeholder-copy">{copy}</p> : null}
        <div className="loading-placeholder-lines" aria-hidden>
          {Array.from({ length: Math.max(lineCount, 1) }, (_, index) => (
            <span key={index} className="loading-placeholder-line" />
          ))}
        </div>
      </div>
    </div>
  )
}
