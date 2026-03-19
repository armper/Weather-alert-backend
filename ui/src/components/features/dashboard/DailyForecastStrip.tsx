import { Droplets } from 'lucide-react'
import { renderAppIcon } from '../../../lib/appIcons'
import { formatTemperature, formatPercentOrNA } from '../../../lib/formatting'
import { resolveWeatherVisual } from '../../../lib/weatherVisuals'
import type { WeatherCondition } from '../../../types'

interface DailyForecastStripProps {
  items: WeatherCondition[]
  unit?: 'F' | 'C'
}

function resolveDisplayTime(item: WeatherCondition): string | undefined {
  return item.onset ?? item.timestamp
}

function isToday(item: WeatherCondition, now: Date): boolean {
  const displayTime = resolveDisplayTime(item)
  if (!displayTime) return false
  const date = new Date(displayTime)
  if (Number.isNaN(date.getTime())) return false
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  )
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

function resolveWeatherLabel(item: WeatherCondition): string {
  return resolveWeatherVisual(item).label
}

export function DailyForecastStrip({ items, unit = 'F' }: DailyForecastStripProps) {
  const displayItems = buildUniqueDailyItems(items)
  const now = new Date()

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
        {displayItems.map((item) => {
          const today = isToday(item, now)
          return (
            <div key={item.id} className={`daily-forecast-day${today ? ' daily-forecast-day--today' : ''}`}>
              <span className="daily-forecast-day-label">{today ? 'Today' : formatDayLabel(item)}</span>
              <span className="daily-forecast-day-icon" role="img" aria-label={resolveWeatherLabel(item)}>
                {resolveWeatherVisual(item).icon}
              </span>
              <span className="daily-forecast-day-temp">{formatTemperature(item.temperature, unit)}</span>
              <span className="daily-forecast-day-rain">
                <span className="forecast-precip-icon" aria-hidden>{renderAppIcon(Droplets, 'app-icon-glyph forecast-precip-glyph')}</span>
                <span>{formatPercentOrNA(item.precipitationProbability)}</span>
              </span>
            </div>
          )
        })}
      </div>
    </article>
  )
}
