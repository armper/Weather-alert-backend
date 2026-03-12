import type { AlertConsoleItem } from '../../../lib/alertConsole'
import { formatDate } from '../../../lib/formatting'

interface AlertTimelineItemProps {
  item: AlertConsoleItem
}

export function AlertTimelineItem({ item }: AlertTimelineItemProps) {
  return (
    <article className="alert-timeline-item">
      <div className="alert-timeline-main">
        <div>
          <p className="alert-timeline-title">{item.title}</p>
          <p className="muted small">{item.summary}</p>
        </div>
        <span className="badge is-muted">{item.semanticLabel}</span>
      </div>

      <div className="alert-timeline-meta">
        <span title={item.startedAt ? formatDate(item.startedAt) : undefined}>{item.statusTimestampLabel}</span>
        <span>{item.locationLabel}</span>
        {item.durationLabel ? <span>{item.durationLabel}</span> : null}
      </div>

      {item.metrics.length > 0 ? (
        <div className="alert-timeline-chips">
          {item.metrics.slice(0, 3).map((metric) => (
            <span key={`${metric.icon}-${metric.label}`} className="metric-pill">
              <span aria-hidden>{metric.icon}</span>
              <span>{metric.label}</span>
            </span>
          ))}
        </div>
      ) : null}
    </article>
  )
}
