import { useAppState } from '../state/useAppState'
import { AlertRow } from '../components/features/dashboard/AlertRow'

export function EventsPage() {
  const { alerts, busyAlertId, handleAcknowledgeAlert } = useAppState()

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Triggered Alerts</h2>
          <span className="badge">{alerts.length} events</span>
        </div>
        {alerts.length === 0 ? (
          <p className="muted">No triggered events yet.</p>
        ) : (
          <div className="alert-list">
            {alerts.map((alert) => (
              <AlertRow
                key={alert.id}
                alert={alert}
                busy={busyAlertId === alert.id}
                onAcknowledge={(alertId) => void handleAcknowledgeAlert(alertId)}
              />
            ))}
          </div>
        )}
      </article>
    </section>
  )
}
