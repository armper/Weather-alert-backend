import type { AlertConsoleSummary } from '../../../lib/alertConsole'

interface MonitoringBannerProps {
  summary: AlertConsoleSummary
}

export function MonitoringBanner({ summary }: MonitoringBannerProps) {
  return (
    <section className={`monitoring-banner${summary.allClear ? ' is-clear' : ' is-alerting'}`}>
      <div className="monitoring-banner-header">
        <div>
          <p className="eyebrow">Monitoring status</p>
          <h3>Watching {summary.watchLocation}</h3>
        </div>
        <div className="monitoring-banner-stats">
          <span className={`badge ${summary.allClear ? '' : 'is-live'}`}>
            {summary.allClear ? 'All clear' : `${summary.activeCount} active alert${summary.activeCount === 1 ? '' : 's'}`}
          </span>
          <span className="monitoring-freshness">{summary.freshnessLabel}</span>
        </div>
      </div>

      <div className="monitoring-banner-copy">
        <div className="monitoring-banner-state">
          <strong>{summary.allClear ? 'No active alerts' : 'Weather event in progress'}</strong>
          {summary.allClear && summary.calmStreakLabel ? <span>Calm streak: {summary.calmStreakLabel}</span> : null}
          {summary.allClear && summary.lastAlertLabel ? <span>{summary.lastAlertLabel}</span> : null}
        </div>

        {summary.watchContext.length > 0 ? (
          <div className="monitoring-watch-context">
            <span className="monitoring-watch-label">Watching {summary.watchLocation} for:</span>
            <div className="monitoring-watch-chips">
              {summary.watchContext.map((item) => (
                <span key={item} className="metric-pill monitoring-watch-chip">
                  {item}
                </span>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    </section>
  )
}
