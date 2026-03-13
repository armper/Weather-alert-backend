import { formatTemperature, formatPercentOrNA } from '../../../lib/formatting'
import type { WeatherCondition } from '../../../types'

interface DailyForecastStripProps {
  items: WeatherCondition[]
  unit?: 'F' | 'C'
}

function formatDayLabel(timestamp?: string): string {
  if (!timestamp) {
    return '—'
  }
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return date.toLocaleDateString(undefined, { weekday: 'short' })
}

function resolveWeatherIcon(item: WeatherCondition): string {
  if (item.probabilityOfThunder != null && item.probabilityOfThunder > 30) {
    return '⛈'
  }
  if ((item.precipitationProbability ?? 0) > 60) {
    return '🌧'
  }
  if ((item.precipitationProbability ?? 0) > 30) {
    return '🌦'
  }
  if ((item.skyCover ?? 0) > 75) {
    return '☁️'
  }
  if ((item.skyCover ?? 0) > 40) {
    return '⛅'
  }
  return '☀️'
}

export function DailyForecastStrip({ items, unit = 'F' }: DailyForecastStripProps) {
  if (items.length === 0) {
    return null
  }

  return (
    <article className="panel daily-forecast-panel">
      <div className="panel-title-row">
        <div>
          <p className="eyebrow">7-Day Outlook</p>
          <h2>Weekly Forecast</h2>
        </div>
      </div>
      <div className="daily-forecast-strip">
        {items.slice(0, 7).map((item) => (
          <div key={item.id} className="daily-forecast-day">
            <span className="daily-forecast-day-label">{formatDayLabel(item.timestamp)}</span>
            <span className="daily-forecast-day-icon" aria-hidden>
              {resolveWeatherIcon(item)}
            </span>
            <span className="daily-forecast-day-temp">{formatTemperature(item.temperature, unit)}</span>
            <span className="daily-forecast-day-rain">{formatPercentOrNA(item.precipitationProbability)}</span>
          </div>
        ))}
      </div>
    </article>
  )
}
