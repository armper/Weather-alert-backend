import { Droplets } from 'lucide-react'
import { renderAppIcon } from '../../../lib/appIcons'
import { formatTemperature, formatPercentOrNA } from '../../../lib/formatting'
import { resolveWeatherVisual } from '../../../lib/weatherVisuals'
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

function resolveWeatherLabel(item: WeatherCondition): string {
  return resolveWeatherVisual(item).label
}

export function HourlyForecastStrip({ items, unit = 'F' }: HourlyForecastStripProps) {
  if (items.length === 0) {
    return null
  }

  return (
    <article className="panel hourly-forecast-panel">
      <div className="panel-title-row">
        <div>
          <p className="eyebrow">Coming Up</p>
          <h2>Hourly Forecast</h2>
        </div>
      </div>
      <div className="hourly-forecast-scroll">
        {items.slice(0, 24).map((item) => (
          <div key={item.id} className="hourly-forecast-slot">
            <span className="hourly-forecast-time">{formatHourLabel(item)}</span>
            <span className="hourly-forecast-icon" role="img" aria-label={resolveWeatherLabel(item)}>{resolveWeatherVisual(item).icon}</span>
            <span className="hourly-forecast-temp">{formatTemperature(item.temperature, unit)}</span>
            <span className="hourly-forecast-rain">
              <span className="forecast-precip-icon" aria-hidden>{renderAppIcon(Droplets, 'app-icon-glyph forecast-precip-glyph')}</span>
              <span>{formatPercentOrNA(item.precipitationProbability)}</span>
            </span>
          </div>
        ))}
      </div>
    </article>
  )
}
