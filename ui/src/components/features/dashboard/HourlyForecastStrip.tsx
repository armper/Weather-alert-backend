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

function resolveWeatherIcon(item: WeatherCondition): string {
  if (item.probabilityOfThunder != null && item.probabilityOfThunder > 30) return '⛈'
  if ((item.precipitationProbability ?? 0) > 60) return '🌧'
  if ((item.precipitationProbability ?? 0) > 30) return '🌦'
  if ((item.skyCover ?? 0) > 75) return '☁️'
  if ((item.skyCover ?? 0) > 40) return '⛅'
  return '☀️'
}

function resolveWeatherLabel(item: WeatherCondition): string {
  if (item.probabilityOfThunder != null && item.probabilityOfThunder > 30) return 'Thunderstorm'
  if ((item.precipitationProbability ?? 0) > 60) return 'Rain'
  if ((item.precipitationProbability ?? 0) > 30) return 'Partly rainy'
  if ((item.skyCover ?? 0) > 75) return 'Cloudy'
  if ((item.skyCover ?? 0) > 40) return 'Partly cloudy'
  return 'Sunny'
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
            <span className="hourly-forecast-icon" role="img" aria-label={resolveWeatherLabel(item)}>{resolveWeatherIcon(item)}</span>
            <span className="hourly-forecast-temp">{formatTemperature(item.temperature, unit)}</span>
            <span className="hourly-forecast-rain">💧 {formatPercentOrNA(item.precipitationProbability)}</span>
          </div>
        ))}
      </div>
    </article>
  )
}
