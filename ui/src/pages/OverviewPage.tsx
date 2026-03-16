import { useEffect, useMemo, useState } from 'react'
import backgroundOverviewImage from '../assets/background-overview.png'
import backgroundRainImage from '../assets/background-rain.png'
import backgroundThunderstormImage from '../assets/background-thunderstorm.png'
import { formatFriendlyLocation, formatPercentOrNA, formatTemperature } from '../lib/formatting'
import { useAsyncState, useDataState } from '../state/useAppState'
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

function resolveFromHeadline(headline?: string): { icon: string; label: string } | null {
  if (!headline) return null
  const h = headline.toLowerCase()
  if (h.includes('snow') || h.includes('blizzard') || h.includes('ice') || h.includes('sleet') || h.includes('freezing'))
    return { icon: '❄️', label: 'Snow' }
  if (h.includes('thunder') || h.includes('tstms'))
    return { icon: '⛈️', label: 'Thunderstorms' }
  if (h.includes('rain') || h.includes('drizzle') || h.includes('shower'))
    return { icon: '🌧️', label: 'Rainy' }
  if (h.includes('fog') || h.includes('mist') || h.includes('haze'))
    return { icon: '🌫️', label: 'Foggy' }
  if (h.includes('overcast'))
    return { icon: '☁️', label: 'Cloudy' }
  if (h.includes('cloud') || h.includes('mostly cloudy') || h.includes('broken'))
    return { icon: '☁️', label: 'Cloudy' }
  if (h.includes('partly') || h.includes('few clouds') || h.includes('scattered') || h.includes('mostly sunny') || h.includes('mostly clear'))
    return { icon: '⛅', label: 'Partly cloudy' }
  if (h.includes('fair') || h.includes('sunny') || h.includes('clear') || h.includes('hot'))
    return { icon: '☀️', label: 'Sunny' }
  return null
}

function resolveFromNumeric(item: Partial<WeatherCondition>): { icon: string; label: string } {
  if ((item.iceAccumulation ?? 0) > 0 || (item.snowfallAmount ?? 0) > 0)
    return { icon: '❄️', label: 'Snow' }
  if ((item.probabilityOfThunder ?? 0) > 30)
    return { icon: '⛈️', label: 'Thunderstorms' }
  if ((item.precipitationAmount ?? 0) > 0 || (item.precipitationProbability ?? 0) > 65)
    return { icon: '🌧️', label: 'Rainy' }
  if ((item.precipitationProbability ?? 0) > 35)
    return { icon: '🌦️', label: 'Chance of rain' }
  if ((item.skyCover ?? 0) > 75)
    return { icon: '☁️', label: 'Cloudy' }
  if ((item.skyCover ?? 0) > 40)
    return { icon: '⛅', label: 'Partly cloudy' }
  return { icon: '☀️', label: 'Sunny' }
}

function resolveCondition(item: Partial<WeatherCondition>): { icon: string; label: string } {
  return resolveFromHeadline(item.headline) ?? resolveFromNumeric(item)
}

function resolveWeatherIcon(item: Partial<WeatherCondition>): string {
  return resolveCondition(item).icon
}

function resolveConditionLabel(item: Partial<WeatherCondition>): string {
  return resolveCondition(item).label
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
  const { currentWeather, dailyForecast, hourlyForecast } = useDataState()
  const { loadingData } = useAsyncState()
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
  const conditionIcon = resolveWeatherIcon(currentWeather ?? {})
  const conditionLabel = resolveConditionLabel(currentWeather ?? {})
  const backgroundImage =
    conditionLabel === 'Thunderstorms'
      ? backgroundThunderstormImage
      : conditionLabel === 'Rainy'
        ? backgroundRainImage
        : backgroundOverviewImage
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

    const nowPrecipProb =
      currentWeather?.precipitationProbability ?? hourlyForecast[0]?.precipitationProbability

    const nowEntry: MinimalForecastItem = {
      id: 'now',
      label: 'Now',
      temperatureLabel: formatTemperature(currentWeather?.temperature, 'F'),
      precipitationLabel: formatPercentOrNA(nowPrecipProb),
      icon: resolveWeatherIcon(currentWeather ?? {}),
    }

    return [nowEntry, ...dailyEntries]
  }, [currentWeather, dailyForecast, hourlyForecast])
  const isForecastLoading = loadingData && dailyForecast.length === 0

  return (
    <section className="page-stack overview-page-stack">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={backgroundImage} alt="" />
      </div>

      <div className="overview-page-content overview-page-content-fresh">
        <p className="overview-minimal-greeting" aria-live="polite">
          {greetingLabel}
        </p>

        <div className="overview-minimal-readout" aria-live="polite">
          <p className="overview-minimal-location">{locationLabel}</p>
          <p className="overview-minimal-temperature">{temperatureLabel}</p>
          <p className="overview-minimal-condition" title={conditionLabel}>
            <span className="overview-minimal-condition-icon">{conditionIcon}</span>
          </p>
        </div>

        <section
          className={`overview-minimal-forecast-strip${isForecastLoading ? ' is-loading' : ''}`}
          aria-label="Now and next seven days forecast"
        >
          {isForecastLoading ? (
            <div className="overview-minimal-forecast-loading-glow" role="status" aria-label="Loading seven day forecast" aria-hidden>
              <span />
              <span />
              <span />
            </div>
          ) : (
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
          )}
        </section>
      </div>
    </section>
  )
}
