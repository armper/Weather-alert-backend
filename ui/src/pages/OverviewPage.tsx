import { useEffect, useMemo, useState } from 'react'
import backgroundOverviewImage from '../assets/background-overview.png'
import { formatFriendlyLocation, formatPercentOrNA, formatTemperature } from '../lib/formatting'
import { useDataState } from '../state/useAppState'
import type { WeatherCondition } from '../types'

interface MinimalForecastItem {
  id: string
  label: string
  temperatureLabel: string
  precipitationLabel: string
  icon: string
}

function resolveDisplayTime(item: WeatherCondition): string | undefined {
  return item.onset ?? item.timestamp
}

function resolveWeatherIcon(item: Partial<WeatherCondition>): string {
  if ((item.probabilityOfThunder ?? 0) > 30) {
    return '⛈️'
  }
  if ((item.precipitationProbability ?? 0) > 65) {
    return '🌧️'
  }
  if ((item.precipitationProbability ?? 0) > 35) {
    return '🌦️'
  }
  if ((item.skyCover ?? 0) > 75) {
    return '☁️'
  }
  if ((item.skyCover ?? 0) > 40) {
    return '⛅'
  }
  return '☀️'
}

function buildNextDailyItems(items: WeatherCondition[], count: number): WeatherCondition[] {
  const now = new Date()
  const todayKey = `${now.getFullYear()}-${now.getMonth()}-${now.getDate()}`
  const grouped = new Map<string, WeatherCondition>()

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
    if (dayKey === todayKey || grouped.has(dayKey)) {
      continue
    }

    grouped.set(dayKey, item)
  }

  return Array.from(grouped.values()).slice(0, count)
}

export function OverviewPage() {
  const { currentWeather, dailyForecast } = useDataState()
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(new Date()), 60_000)
    return () => {
      window.clearInterval(intervalId)
    }
  }, [])

  const locationLabel = formatFriendlyLocation(currentWeather?.location ?? 'Orlando')
  const temperatureLabel =
    currentWeather?.temperature != null ? formatTemperature(currentWeather.temperature, 'F') : '--'
  const greetingLabel = useMemo(() => {
    const hour = now.getHours()
    if (hour < 12) {
      return 'Good morning!'
    }
    if (hour < 18) {
      return 'Good afternoon!'
    }
    return 'Good evening!'
  }, [now])
  const forecastItems = useMemo<MinimalForecastItem[]>(() => {
    const nextDailyItems = buildNextDailyItems(dailyForecast, 7)
    const dailyEntries = nextDailyItems.map((item, index) => {
      const displayTime = resolveDisplayTime(item)
      const dayDate = displayTime ? new Date(displayTime) : null
      const dayLabel =
        dayDate && !Number.isNaN(dayDate.getTime())
          ? dayDate.toLocaleDateString(undefined, { weekday: 'short' })
          : `Day ${index + 1}`

      return {
        id: `day-${item.id}`,
        label: dayLabel,
        temperatureLabel: formatTemperature(item.temperature, 'F'),
        precipitationLabel: formatPercentOrNA(item.precipitationProbability),
        icon: resolveWeatherIcon(item),
      }
    })

    const nowEntry: MinimalForecastItem = {
      id: 'now',
      label: 'Now',
      temperatureLabel: formatTemperature(currentWeather?.temperature, 'F'),
      precipitationLabel: formatPercentOrNA(currentWeather?.precipitationProbability),
      icon: resolveWeatherIcon(currentWeather ?? {}),
    }

    return [nowEntry, ...dailyEntries]
  }, [currentWeather, dailyForecast])

  return (
    <section className="page-stack overview-page-stack">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={backgroundOverviewImage} alt="" />
      </div>

      <div className="overview-page-content overview-page-content-fresh">
        <p className="overview-minimal-greeting" aria-live="polite">
          {greetingLabel}
        </p>

        <div className="overview-minimal-readout" aria-live="polite">
          <p className="overview-minimal-location">{locationLabel}</p>
          <p className="overview-minimal-temperature">{temperatureLabel}</p>
        </div>

        <section className="overview-minimal-forecast-strip" aria-label="Now and next seven days forecast">
          <div className="overview-minimal-forecast-row">
            {forecastItems.map((item) => (
              <article key={item.id} className="overview-minimal-forecast-item">
                <p className="overview-minimal-forecast-label">{item.label}</p>
                <p className="overview-minimal-forecast-temp">{item.temperatureLabel}</p>
                <p className="overview-minimal-forecast-icon" aria-hidden>
                  {item.icon}
                </p>
                <p className="overview-minimal-forecast-precip">{item.precipitationLabel}</p>
              </article>
            ))}
          </div>
        </section>
      </div>
    </section>
  )
}
