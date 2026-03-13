import { formatTemperature, formatPercentOrNA } from '../../../lib/formatting'
import type { WeatherCondition } from '../../../types'

interface HourlyForecastStripProps {
  items: WeatherCondition[]
  unit?: 'F' | 'C'
}

function resolveDisplayTime(item: WeatherCondition): string | undefined {
  return item.onset ?? item.timestamp
}

function formatHourLabel(item: WeatherCondition): string {
  const displayTime = resolveDisplayTime(item)
  if (!displayTime) {
    return '—'
  }
  const date = new Date(displayTime)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return date.toLocaleTimeString(undefined, { hour: 'numeric' })
}

export function HourlyForecastStrip({ items, unit = 'F' }: HourlyForecastStripProps) {
  if (items.length === 0) {
    return null
  }

  return (
    <div className="hourly-forecast-strip">
      <p className="eyebrow hourly-forecast-eyebrow">Next 24 hours</p>
      <div className="hourly-forecast-scroll">
        {items.slice(0, 24).map((item) => (
          <div key={item.id} className="hourly-forecast-slot">
            <span className="hourly-forecast-time">{formatHourLabel(item)}</span>
            <span className="hourly-forecast-temp">{formatTemperature(item.temperature, unit)}</span>
            <span className="hourly-forecast-rain">{formatPercentOrNA(item.precipitationProbability)}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
