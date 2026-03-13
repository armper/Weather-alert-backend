import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { ActiveAlertCard } from '../components/features/dashboard/ActiveAlertCard'
import { AlertTimelineItem } from '../components/features/dashboard/AlertTimelineItem'
import { MonitoringBanner } from '../components/features/dashboard/MonitoringBanner'
import { NwsProductsPanel } from '../components/features/dashboard/NwsProductsPanel'
import { AriaButton } from '../components/ui/AriaButton'
import { buildAlertConsoleSummary } from '../lib/alertConsole'

export function EventsPage() {
  const { alerts, busyAlertId, criteria, currentWeather, loadingData, nwsProducts, handleAcknowledgeAlert, handleAcknowledgeAllAlerts } =
    useAppState()
  const summary = useMemo(
    () => buildAlertConsoleSummary(criteria, alerts, currentWeather),
    [criteria, alerts, currentWeather],
  )
  const sentCount = summary.activeAlerts.filter((entry) => entry.alert.status === 'SENT').length

  return (
    <section className="page-stack">
      <article className="panel">
        <MonitoringBanner summary={summary} />

        <div className="panel-title-row">
          <h2>Triggered Alerts</h2>
          <div className="alert-toolbar">
            <span className="badge">
              {summary.activeAlerts.length + summary.recentAlerts.length + summary.alertHistory.length} events
            </span>
            {sentCount > 0 ? (
              <AriaButton
                className="primary button-inline"
                isDisabled={loadingData}
                onPress={() => void handleAcknowledgeAllAlerts()}
              >
                Acknowledge active alerts
              </AriaButton>
            ) : null}
          </div>
        </div>

        <section className="console-section">
          <div className="console-section-header">
            <div>
              <h3>Active Alerts</h3>
              <p className="muted small">Weather events currently in play for this watch area.</p>
            </div>
            <span className={`badge ${summary.allClear ? '' : 'is-live'}`}>
              {summary.activeAlerts.length} active
            </span>
          </div>

          {summary.activeAlerts.length === 0 ? (
            <div className="empty-state-panel empty-state-clear">
              <h3>All clear</h3>
              <p className="muted">No active alerts for {summary.watchLocation}. The system is still watching in the background.</p>
              {summary.calmStreakLabel ? <p className="small muted">Calm streak: {summary.calmStreakLabel}</p> : null}
              {summary.lastAlertLabel ? <p className="small muted">{summary.lastAlertLabel}</p> : null}
              <div className="button-row empty-state-actions">
                <Link to="/app/rules#create-custom-alert" className="primary overview-action-link">
                  New alert
                </Link>
                <Link to="/app/alerts" className="ghost overview-action-link">
                  Review my alerts
                </Link>
              </div>
            </div>
          ) : (
            <div className="active-alert-grid">
              {summary.activeAlerts.map((item) => (
                <ActiveAlertCard
                  key={item.key}
                  item={item}
                  busy={busyAlertId === item.alert.id}
                  onAcknowledge={(alertId) => void handleAcknowledgeAlert(alertId)}
                />
              ))}
            </div>
          )}
        </section>

        {summary.recentAlerts.length > 0 ? (
          <details className="console-collapsible">
            <summary className="console-collapsible-summary">
              <span>Recent Alerts</span>
              <span className="badge">{summary.recentAlerts.length}</span>
            </summary>
            <div className="timeline-list">
              {summary.recentAlerts.map((item) => (
                <AlertTimelineItem key={item.key} item={item} />
              ))}
            </div>
          </details>
        ) : null}

        {summary.alertHistory.length > 0 ? (
          <details className="console-collapsible history-collapsible">
            <summary className="console-collapsible-summary">
              <span>Alert History</span>
              <span className="badge is-muted">{summary.alertHistory.length}</span>
            </summary>
            <div className="timeline-list compact">
              {summary.alertHistory.map((item) => (
                <AlertTimelineItem key={item.key} item={item} />
              ))}
            </div>
          </details>
        ) : null}

        {nwsProducts.length > 0 ? <NwsProductsPanel products={nwsProducts} /> : null}
      </article>
    </section>
  )
}
