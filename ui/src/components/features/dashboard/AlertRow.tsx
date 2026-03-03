import type { AlertEvent } from '../../../types'
import { formatDate } from '../../../lib/formatting'

interface AlertRowProps {
  alert: AlertEvent
  busy: boolean
  onAcknowledge: (alertId: string) => void
}

export function AlertRow({ alert, busy, onAcknowledge }: AlertRowProps) {
  return (
    <article className="alert-row">
      <div>
        <p className="alert-row-title">{alert.headline ?? 'Triggered alert'}</p>
        <p className="muted small">{alert.reason ?? 'Rule matched'}</p>
        <p className="muted small">{formatDate(alert.alertTime)}</p>
      </div>
      <div className="alert-row-actions">
        <span className={`status-chip status-${(alert.status ?? 'PENDING').toLowerCase()}`}>{alert.status ?? 'PENDING'}</span>
        {alert.status === 'SENT' ? (
          <button className="ghost" disabled={busy} onClick={() => onAcknowledge(alert.id)}>
            {busy ? 'Saving...' : 'Acknowledge'}
          </button>
        ) : null}
      </div>
    </article>
  )
}
