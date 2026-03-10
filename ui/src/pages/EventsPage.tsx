import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { AlertRow } from '../components/features/dashboard/AlertRow'
import { AriaButton } from '../components/ui/AriaButton'

export function EventsPage() {
  const { alerts, busyAlertId, loadingData, handleAcknowledgeAlert, handleAcknowledgeAllAlerts } = useAppState()
  const groupedAlerts = useMemo(() => {
    const grouped = new Map<string, { key: string; count: number; alert: (typeof alerts)[number] }>()
    for (const alert of alerts) {
      const key = [
        alert.criteriaId ?? '',
        alert.eventKey ?? '',
        alert.headline ?? '',
        alert.reason ?? '',
        alert.status ?? 'PENDING',
      ].join('|')

      const current = grouped.get(key)
      if (!current) {
        grouped.set(key, { key, count: 1, alert })
        continue
      }

      current.count += 1
      const currentTime = new Date(current.alert.alertTime ?? 0).getTime()
      const nextTime = new Date(alert.alertTime ?? 0).getTime()
      if (nextTime >= currentTime) {
        current.alert = alert
      }
    }
    return Array.from(grouped.values()).sort(
      (a, b) => new Date(b.alert.alertTime ?? 0).getTime() - new Date(a.alert.alertTime ?? 0).getTime(),
    )
  }, [alerts])

  const sentCount = groupedAlerts.filter((entry) => entry.alert.status === 'SENT').length

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Triggered Alerts</h2>
          <div className="alert-toolbar">
            <span className="badge">{groupedAlerts.length} events</span>
            {sentCount > 0 ? (
              <AriaButton
                className="primary button-inline"
                isDisabled={loadingData}
                onPress={() => void handleAcknowledgeAllAlerts()}
              >
                Mark all as acknowledged
              </AriaButton>
            ) : null}
          </div>
        </div>
        {groupedAlerts.length === 0 ? (
          <div className="empty-state-panel">
            <h3>No triggered alerts yet</h3>
            <p className="muted">
              When a rule fires, this page becomes your recent alert timeline with delivery and acknowledgement status.
            </p>
            <div className="button-row empty-state-actions">
              <Link to="/app/rules#create-custom-alert" className="primary overview-action-link">
                New alert
              </Link>
              <Link to="/app/alerts" className="ghost overview-action-link">
                Review my alerts
              </Link>
            </div>
            <div className="empty-state-list">
              <p>Use forecast monitoring for early warnings.</p>
              <p>Keep one or two broad alerts active for your main area.</p>
              <p>Return here to acknowledge alerts once weather starts changing.</p>
            </div>
          </div>
        ) : (
          <div className="events-list-wrap">
            <div className="alert-list">
              {groupedAlerts.map((entry) => (
                <AlertRow
                  key={entry.key}
                  alert={entry.alert}
                  duplicateCount={entry.count}
                  busy={busyAlertId === entry.alert.id}
                  onAcknowledge={(alertId) => void handleAcknowledgeAlert(alertId)}
                />
              ))}
            </div>
          </div>
        )}
      </article>
    </section>
  )
}
