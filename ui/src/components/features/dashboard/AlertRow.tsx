import type { AlertEvent } from '../../../types'
import { formatDate, formatRelativeTime, formatStatusLabel } from '../../../lib/formatting'

interface AlertRowProps {
  alert: AlertEvent
  busy: boolean
  duplicateCount?: number
  onAcknowledge: (alertId: string) => void
}

const STATUS_ICON: Record<string, string> = {
  pending: '…',
  sent: '✉',
  acknowledged: '✓',
  expired: '⌛',
}

export function AlertRow({ alert, busy, duplicateCount = 1, onAcknowledge }: AlertRowProps) {
  const status = (alert.status ?? 'PENDING').toLowerCase()
  const statusLabel = formatStatusLabel(alert.status ?? 'PENDING')
  const icon = STATUS_ICON[status] ?? '•'

  return (
    <article className="alert-row">
      <div>
        <p className="alert-row-title">
          {alert.headline ?? 'Triggered alert'}
          {duplicateCount > 1 ? <span className="badge duplicate-badge">x{duplicateCount}</span> : null}
        </p>
        <p className="muted small">{alert.reason ?? 'Rule matched'}</p>
        <p className="muted small" title={formatDate(alert.alertTime)}>
          {formatRelativeTime(alert.alertTime)}
        </p>
      </div>
      <div className="alert-row-actions">
        <span className={`delivery-indicator status-${status}`} title={statusLabel}>
          {icon}
        </span>
        {alert.status === 'SENT' ? (
          <button className="primary" disabled={busy} onClick={() => onAcknowledge(alert.id)}>
            {busy ? 'Saving...' : 'Acknowledge'}
          </button>
        ) : null}
      </div>
    </article>
  )
}
