import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiRequest, toErrorMessage } from '../api'
import { OverviewLocationSwitcher, type OverviewLocationSelection } from '../components/features/dashboard/OverviewLocationSwitcher'
import { formatFriendlyLocation, formatPercentOrNA, formatTemperature } from '../lib/formatting'
import { resolveWeatherVisual } from '../lib/weatherVisuals'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useAsyncState, useDataState, useNoticeState, useSessionState } from '../state/useAppState'
import type { WeatherCondition } from '../types'

interface MinimalForecastItem {
  id: string
  label: string
  temperatureLabel: string
  precipitationLabel: string
  icon: string
}

interface CustomOverviewView {
  location: OverviewLocationSelection
  weather: WeatherCondition | null
  dailyForecast: WeatherCondition[]
  hourlyForecast: WeatherCondition[]
}

function resolveDisplayTime(item: WeatherCondition): string | undefined {
  return item.onset ?? item.timestamp
}

function resolveWeatherIcon(item: Partial<WeatherCondition>): string {
  return resolveWeatherVisual(item).icon
}

function buildWeatherVisualSource(
  current: WeatherCondition | null,
  hourly: WeatherCondition[],
  daily: WeatherCondition[],
): Partial<WeatherCondition> {
  const hourlyLead = hourly[0]
  const dailyLead = daily[0]

  return {
    headline: current?.headline ?? hourlyLead?.headline ?? dailyLead?.headline,
    eventType: current?.eventType ?? hourlyLead?.eventType ?? dailyLead?.eventType,
    precipitationProbability:
      current?.precipitationProbability ?? hourlyLead?.precipitationProbability ?? dailyLead?.precipitationProbability,
    precipitationAmount:
      current?.precipitationAmount ?? hourlyLead?.precipitationAmount ?? dailyLead?.precipitationAmount,
    snowfallAmount:
      current?.snowfallAmount ?? hourlyLead?.snowfallAmount ?? dailyLead?.snowfallAmount,
    iceAccumulation:
      current?.iceAccumulation ?? hourlyLead?.iceAccumulation ?? dailyLead?.iceAccumulation,
    probabilityOfThunder:
      current?.probabilityOfThunder ?? hourlyLead?.probabilityOfThunder ?? dailyLead?.probabilityOfThunder,
    skyCover: current?.skyCover ?? hourlyLead?.skyCover ?? dailyLead?.skyCover,
  }
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

function areLocationsEquivalent(left: OverviewLocationSelection, right: OverviewLocationSelection): boolean {
  return Math.abs(left.latitude - right.latitude) < 0.0001 && Math.abs(left.longitude - right.longitude) < 0.0001
}

export function OverviewPage() {
  const { criteria, currentWeather, dailyForecast, hourlyForecast } = useDataState()
  const { loadingData } = useAsyncState()
  const { token } = useSessionState()
  const { setNotice } = useNoticeState()
  const [now, setNow] = useState(() => new Date())

  const monitoringLocation = useMemo<OverviewLocationSelection>(() => {
    const monitoredCriteria = criteria[0]

    return {
      name: monitoredCriteria?.location?.trim() || currentWeather?.location?.trim() || 'Orlando',
      latitude: Number(monitoredCriteria?.latitude ?? DEFAULT_LAT),
      longitude: Number(monitoredCriteria?.longitude ?? DEFAULT_LON),
      detail: monitoredCriteria?.location?.trim() || currentWeather?.location?.trim() || 'Orlando',
    }
  }, [criteria, currentWeather?.location])

  const [customLocationView, setCustomLocationView] = useState<CustomOverviewView | null>(null)

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(new Date()), 60_000)
    return () => {
      window.clearInterval(intervalId)
    }
  }, [])

  const handleSaveLocation = useCallback(
    async (selection: OverviewLocationSelection) => {
      if (!token) {
        return
      }

      if (areLocationsEquivalent(selection, monitoringLocation)) {
        setCustomLocationView(null)
        return
      }

      try {
        const latitude = encodeURIComponent(String(selection.latitude))
        const longitude = encodeURIComponent(String(selection.longitude))

        const [weather, nextDailyForecast, nextHourlyForecast] = await Promise.all([
          apiRequest<WeatherCondition>(`/api/weather/conditions/current?latitude=${latitude}&longitude=${longitude}`, {
            token,
          }).catch(() => null),
          apiRequest<WeatherCondition[]>(`/api/weather/conditions/daily?latitude=${latitude}&longitude=${longitude}`, {
            token,
          }).catch(() => [] as WeatherCondition[]),
          apiRequest<WeatherCondition[]>(
            `/api/weather/conditions/forecast?latitude=${latitude}&longitude=${longitude}&hours=24`,
            {
              token,
            },
          ).catch(() => [] as WeatherCondition[]),
        ])

        const resolvedName = weather?.location?.trim() || selection.name

        setCustomLocationView({
          location: {
            ...selection,
            name: resolvedName,
            detail: weather?.location?.trim() || selection.detail,
          },
          weather,
          dailyForecast: nextDailyForecast,
          hourlyForecast: nextHourlyForecast,
        })
      } catch (error) {
        setNotice({
          kind: 'error',
          text: toErrorMessage(error),
        })
      }
    },
    [monitoringLocation, setNotice, token],
  )

  const usingCustomLocation =
    customLocationView != null && !areLocationsEquivalent(customLocationView.location, monitoringLocation)
  const activeLocation = usingCustomLocation ? customLocationView.location : monitoringLocation
  const displayWeather = usingCustomLocation ? customLocationView.weather : currentWeather
  const displayDailyForecast = usingCustomLocation ? customLocationView.dailyForecast : dailyForecast
  const displayHourlyForecast = usingCustomLocation ? customLocationView.hourlyForecast : hourlyForecast
  const weatherVisualSource = useMemo(
    () => buildWeatherVisualSource(displayWeather, displayHourlyForecast, displayDailyForecast),
    [displayDailyForecast, displayHourlyForecast, displayWeather],
  )

  const locationLabel = formatFriendlyLocation(activeLocation.name || displayWeather?.location || 'Orlando')
  const temperatureLabel =
    displayWeather?.temperature != null ? formatTemperature(displayWeather.temperature, 'F') : '--'
  const weatherVisual = resolveWeatherVisual(weatherVisualSource)
  const conditionIcon = weatherVisual.icon
  const conditionLabel = weatherVisual.label
  const backgroundImage = weatherVisual.backgroundImage
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
    const nextDailyItems = buildNextDailyItems(displayDailyForecast, 7)
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
      displayWeather?.precipitationProbability ?? displayHourlyForecast[0]?.precipitationProbability

    const nowEntry: MinimalForecastItem = {
      id: 'now',
      label: 'Now',
      temperatureLabel: formatTemperature(displayWeather?.temperature, 'F'),
      precipitationLabel: formatPercentOrNA(nowPrecipProb),
      icon: resolveWeatherIcon(displayWeather ?? {}),
    }

    return [nowEntry, ...dailyEntries]
  }, [displayDailyForecast, displayHourlyForecast, displayWeather])
  const isForecastLoading = loadingData && !usingCustomLocation && displayDailyForecast.length === 0

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
          <div className="overview-minimal-location-row">
            <p className="overview-minimal-location">{locationLabel}</p>
            <OverviewLocationSwitcher
              activeLocation={activeLocation}
              monitoringLocation={monitoringLocation}
              onSaveLocation={handleSaveLocation}
            />
          </div>
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
