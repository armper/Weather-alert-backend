import { formatTemperature, formatPercentOrNA } from '../../../lib/formatting'
import type { WeatherCondition } from '../../../types'

interface DailyForecastStripProps {
  items: WeatherCondition[]
  unit?: 'F' | 'C'
}

function resolveDisplayTime(item: WeatherCondition): string | undefined {
  return item.onset ?? item.timestamp
}

function formatDayLabel(item: WeatherCondition): string {
  const displayTime = resolveDisplayTime(item)
  if (!displayTime) {
    return '—'
  }
  const date = new Date(displayTime)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return date.toLocaleDateString(undefined, { weekday: 'short' })
}

function buildUniqueDailyItems(items: WeatherCondition[]): WeatherCondition[] {
  const grouped = new Map<string, WeatherCondition[]>()

  for (const item of items) {
    const displayTime = resolveDisplayTime(item)
    if (!displayTime) {
      continue
    }

    const date = new Date(displayTime)
    if (Number.isNaN(date.getTime())) {
      continue
    }

    const dayKey = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    const existing = grouped.get(dayKey) ?? []
    existing.push(item)
    grouped.set(dayKey, existing)
  }

  return Array.from(grouped.values())
    .map((group) => group.find((item) => !/(tonight|overnight|night)/i.test(item.headline ?? '')) ?? group[0])
    .slice(0, 7)
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
  const displayItems = buildUniqueDailyItems(items)

  if (displayItems.length === 0) {
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
        {displayItems.map((item) => (
          <div key={item.id} className="daily-forecast-day">
            <span className="daily-forecast-day-label">{formatDayLabel(item)}</span>
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
