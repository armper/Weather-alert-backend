import { Link } from 'react-router-dom'
import { formatRelativeTimeCompact } from '../../../lib/formatting'
import type { OverviewActivityItem } from '../../../lib/overviewDashboard'

interface RecentActivityFeedProps {
  items: OverviewActivityItem[]
  calmLabel?: string
  now: number
}

export function RecentActivityFeed({ items, calmLabel, now }: RecentActivityFeedProps) {
  return (
    <article className="panel overview-activity-panel">
      <div className="panel-title-row">
        <div>
          <p className="eyebrow">Alerts</p>
          <h2>Recent alerts</h2>
        </div>
      </div>

      {calmLabel ? (
        <div className="overview-calm-banner">
          <strong>Calm conditions</strong>
          <span>{calmLabel}</span>
        </div>
      ) : null}

      {items.length === 0 ? (
        <p className="muted">No recent alert activity yet.</p>
      ) : (
        <div className="overview-activity-list">
          {items.map((item) => (
            <Link key={item.id} to={item.href} className="overview-activity-item">
              <span className="overview-activity-icon" aria-hidden>
                {item.icon}
              </span>
              <div className="overview-activity-copy">
                <strong>{item.title}</strong>
                <span>{item.description}</span>
              </div>
              <span className="overview-activity-time">
                {item.timestamp ? formatRelativeTimeCompact(item.timestamp, now) : 'recent'}
              </span>
            </Link>
          ))}
        </div>
      )}

      <Link to="/app/events" className="auth-link overview-activity-link">
        View all alerts
      </Link>
    </article>
  )
}
