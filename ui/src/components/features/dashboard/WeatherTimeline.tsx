import type { OverviewTimelineItem } from '../../../lib/overviewDashboard'

interface WeatherTimelineProps {
  items: OverviewTimelineItem[]
}

export function WeatherTimeline({ items }: WeatherTimelineProps) {
  return (
    <article className="panel overview-timeline-panel">
      <div className="panel-title-row">
        <div>
          <p className="eyebrow">Weather Timeline</p>
          <h2>Weather Timeline</h2>
        </div>
      </div>

      <div className="overview-timeline-list">
        {items.map((item) => (
          <div key={item.id} className="overview-timeline-item">
            <div className="overview-timeline-time">{item.timeLabel}</div>
            <div className="overview-timeline-marker" aria-hidden>
              {item.icon}
            </div>
            <div className="overview-timeline-copy">
              <strong>{item.title}</strong>
              <span>{item.description}</span>
            </div>
          </div>
        ))}
      </div>
    </article>
  )
}
