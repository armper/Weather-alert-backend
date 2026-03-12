import type { AlertConsoleItem } from '../../../lib/alertConsole'
import { formatDate } from '../../../lib/formatting'
import { AriaButton } from '../../ui/AriaButton'

interface ActiveAlertCardProps {
  item: AlertConsoleItem
  busy: boolean
  onAcknowledge: (alertId: string) => void
}

export function ActiveAlertCard({ item, busy, onAcknowledge }: ActiveAlertCardProps) {
  const isAcknowledged = item.lifecycleState === 'acknowledged'

  return (
    <article className={`active-alert-card${isAcknowledged ? ' is-acknowledged' : ''}`}>
      <div className="active-alert-card-header">
        <div>
          <p className="active-alert-kicker">{item.semanticLabel}</p>
          <h3>{item.title}</h3>
        </div>
        <div className="active-alert-status-stack">
          <span className={`badge ${isAcknowledged ? 'is-muted' : 'is-live'}`}>{item.semanticLabel}</span>
          {item.duplicateCount > 1 ? <span className="badge is-muted">x{item.duplicateCount}</span> : null}
        </div>
      </div>

      <p className="active-alert-summary">{item.summary}</p>

      <div className="active-alert-meta">
        <span title={item.startedAt ? formatDate(item.startedAt) : undefined}>{item.statusTimestampLabel}</span>
        <span>{item.locationLabel}</span>
        {item.secondaryTimestampLabel ? (
          <span title={item.resolvedAt ? formatDate(item.resolvedAt) : undefined}>{item.secondaryTimestampLabel}</span>
        ) : null}
        {item.durationLabel ? <span>{item.durationLabel}</span> : null}
      </div>

      {item.metrics.length > 0 ? (
        <div className="alert-metrics">
          {item.metrics.map((metric) => (
            <span key={`${metric.icon}-${metric.label}`} className="metric-pill">
              <span aria-hidden>{metric.icon}</span>
              <span>{metric.label}</span>
            </span>
          ))}
        </div>
      ) : null}

      {item.alert.status === 'SENT' ? (
        <div className="active-alert-actions">
          <AriaButton className={isAcknowledged ? 'ghost button-inline' : 'primary button-inline'} isDisabled={busy} onPress={() => onAcknowledge(item.alert.id)}>
            {busy ? 'Saving...' : 'Acknowledge'}
          </AriaButton>
        </div>
      ) : null}
    </article>
  )
}
