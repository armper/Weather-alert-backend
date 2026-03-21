import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { apiRequest, toErrorMessage } from '../api'
import { OverviewLocationSwitcher, type OverviewLocationSelection } from '../components/features/dashboard/OverviewLocationSwitcher'
import { formatDate, formatFriendlyLocation, formatPercentOrNA, formatRelativeTime, formatRelativeTimeCompact, formatTemperature } from '../lib/formatting'
import { resolveOfficialAlertVisual } from '../lib/officialAlerts'
import { resolveCriteriaTileIcon } from '../lib/ruleIcons'
import { resolveWeatherVisual } from '../lib/weatherVisuals'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useActionState, useAsyncState, useDataState, useNoticeState, useSessionState } from '../state/useAppState'
import type { AlertEvent } from '../types'
import type { WeatherCondition } from '../types'

interface MinimalForecastItem {
  id: string
  label: string
  temperatureLabel: string
  precipitationLabel: string
  icon: ReactNode
  toggleMode?: 'daily' | 'hourly'
}

interface CustomOverviewView {
  location: OverviewLocationSelection
  weather: WeatherCondition | null
  officialAlerts: WeatherCondition[]
  dailyForecast: WeatherCondition[]
  hourlyForecast: WeatherCondition[]
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

function resolveWeatherIcon(item: Partial<WeatherCondition>): ReactNode {
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

function resolveAlertRank(item: WeatherCondition): number {
  const { tone } = resolveOfficialAlertVisual(item)
  return (
    {
      extreme: 0,
      severe: 1,
      warning: 2,
      advisory: 3,
      notice: 4,
    } as const
  )[tone]
}

function sortOfficialAlerts(items: WeatherCondition[]): WeatherCondition[] {
  return [...items].sort((left, right) => {
    const rankCompare = resolveAlertRank(left) - resolveAlertRank(right)
    if (rankCompare !== 0) {
      return rankCompare
    }

    const leftTime = new Date(left.onset ?? left.expires ?? '').getTime()
    const rightTime = new Date(right.onset ?? right.expires ?? '').getTime()
    if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) {
      return (left.headline ?? left.eventType ?? left.id).localeCompare(right.headline ?? right.eventType ?? right.id)
    }
    if (Number.isNaN(leftTime)) {
      return 1
    }
    if (Number.isNaN(rightTime)) {
      return -1
    }
    return leftTime - rightTime
  })
}

function describeOfficialAlertTiming(item: WeatherCondition): string {
  if (item.expires) {
    return `Until ${formatDate(item.expires)}`
  }
  if (item.onset) {
    return `Started ${formatRelativeTime(item.onset)}`
  }
  return 'Issued by NOAA'
}

function resolveOfficialAlertTitle(item: WeatherCondition): string {
  return item.eventType?.trim() || item.headline?.trim() || 'Official NOAA alert'
}

function resolveOfficialAlertArea(item: WeatherCondition, fallbackLocation: string): string {
  return formatFriendlyLocation((item.location || fallbackLocation).replace(/\s*;\s*/g, ' • '))
}

function describeRecentAlertTiming(item: AlertEvent, now: number): string {
  const timestamp = item.sentAt ?? item.alertTime ?? item.acknowledgedAt ?? item.expiredAt
  return timestamp ? formatRelativeTimeCompact(timestamp, now) : 'recent'
}

function resolveRecentAlertTime(item: AlertEvent): number {
  const timestamp = new Date(item.sentAt ?? item.alertTime ?? '').getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

export function OverviewPage() {
  const { criteria, alerts, currentWeather, officialAlerts, dailyForecast, hourlyForecast } = useDataState()
  const { loadingData, busyAlertId } = useAsyncState()
  const { handleAcknowledgeAlert } = useActionState()
  const { token } = useSessionState()
  const { setNotice } = useNoticeState()
  const [now, setNow] = useState(() => new Date())
  const [dismissingAlertIds, setDismissingAlertIds] = useState<string[]>([])
  const [dismissedAlertIds, setDismissedAlertIds] = useState<string[]>([])
  const [expandedOfficialAlert, setExpandedOfficialAlert] = useState<WeatherCondition | null>(null)
  const [forecastMode, setForecastMode] = useState<'daily' | 'hourly'>('daily')
  const recentAlertRefs = useRef(new Map<string, HTMLButtonElement>())
  const previousRecentAlertPositions = useRef(new Map<string, DOMRect>())

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

  useEffect(() => {
    if (!expandedOfficialAlert) {
      return
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setExpandedOfficialAlert(null)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [expandedOfficialAlert])

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

        const [weather, nextOfficialAlerts, nextDailyForecast, nextHourlyForecast] = await Promise.all([
          apiRequest<WeatherCondition>(`/api/weather/conditions/current?latitude=${latitude}&longitude=${longitude}`, {
            token,
          }).catch(() => null),
          apiRequest<WeatherCondition[]>(`/api/weather/location?latitude=${latitude}&longitude=${longitude}`, {
            token,
          }).catch(() => [] as WeatherCondition[]),
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
          officialAlerts: sortOfficialAlerts(nextOfficialAlerts),
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
  const displayOfficialAlerts = useMemo(
    () => sortOfficialAlerts(usingCustomLocation ? customLocationView?.officialAlerts ?? [] : officialAlerts),
    [customLocationView, officialAlerts, usingCustomLocation],
  )
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
  const criteriaById = useMemo(() => new Map(criteria.map((item) => [item.id, item])), [criteria])
  const recentTriggeredAlerts = useMemo(
    () =>
      alerts
        .filter((item) => item.status !== 'ACKNOWLEDGED' && item.status !== 'EXPIRED')
        .filter((item) => {
          const alertTime = resolveRecentAlertTime(item)
          return alertTime > 0 && alertTime >= now.getTime() - 24 * 60 * 60 * 1000
        })
        .filter((item) => !dismissedAlertIds.includes(item.id))
        .slice()
        .sort((left, right) => resolveRecentAlertTime(right) - resolveRecentAlertTime(left))
        .slice(0, 7),
    [alerts, dismissedAlertIds, now],
  )
  const handleDismissRecentAlert = useCallback(
    async (alertId: string) => {
      if (dismissingAlertIds.includes(alertId) || dismissedAlertIds.includes(alertId)) {
        return
      }

      setDismissingAlertIds((current) => [...current, alertId])
      const acknowledgePromise = handleAcknowledgeAlert(alertId)
      await new Promise((resolve) => window.setTimeout(resolve, 240))
      setDismissedAlertIds((current) => (current.includes(alertId) ? current : [...current, alertId]))

      const acknowledged = await acknowledgePromise
      if (!acknowledged) {
        setDismissingAlertIds((current) => current.filter((id) => id !== alertId))
        setDismissedAlertIds((current) => current.filter((id) => id !== alertId))
        return
      }
      setDismissingAlertIds((current) => current.filter((id) => id !== alertId))
    },
    [dismissedAlertIds, dismissingAlertIds, handleAcknowledgeAlert],
  )

  const setRecentAlertRef = useCallback((alertId: string, node: HTMLButtonElement | null) => {
    if (node) {
      recentAlertRefs.current.set(alertId, node)
      return
    }
    recentAlertRefs.current.delete(alertId)
  }, [])

  useLayoutEffect(() => {
    const nextPositions = new Map<string, DOMRect>()

    recentTriggeredAlerts.forEach((item) => {
      const node = recentAlertRefs.current.get(item.id)
      if (!node) {
        return
      }

      const nextRect = node.getBoundingClientRect()
      nextPositions.set(item.id, nextRect)

      const previousRect = previousRecentAlertPositions.current.get(item.id)
      if (!previousRect) {
        return
      }

      const deltaY = previousRect.top - nextRect.top
      if (Math.abs(deltaY) < 1) {
        return
      }

      node.style.transition = 'none'
      node.style.transform = `translateY(${deltaY}px)`
      node.style.willChange = 'transform'

      requestAnimationFrame(() => {
        const liveNode = recentAlertRefs.current.get(item.id)
        if (!liveNode) {
          return
        }
        liveNode.style.transition = 'transform 220ms cubic-bezier(0.22, 1, 0.36, 1)'
        liveNode.style.transform = ''
      })

      window.setTimeout(() => {
        const liveNode = recentAlertRefs.current.get(item.id)
        if (!liveNode) {
          return
        }
        liveNode.style.transition = ''
        liveNode.style.willChange = ''
      }, 240)
    })

    previousRecentAlertPositions.current = nextPositions
  }, [recentTriggeredAlerts])
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
  const hasHourlyForecast = displayHourlyForecast.length > 0
  const activeForecastMode = forecastMode === 'hourly' && hasHourlyForecast ? 'hourly' : 'daily'

  const forecastItems = useMemo<MinimalForecastItem[]>(() => {
    const nextDailyItems = buildNextDailyItems(displayDailyForecast, 6)
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

    const todayEntry: MinimalForecastItem = {
      id: 'today',
      label: 'Today',
      temperatureLabel: formatTemperature(displayWeather?.temperature, 'F'),
      precipitationLabel: formatPercentOrNA(nowPrecipProb),
      icon: resolveWeatherIcon(displayWeather ?? {}),
      toggleMode: hasHourlyForecast ? 'hourly' : undefined,
    }

    if (activeForecastMode === 'hourly') {
      const hourlyEntries = displayHourlyForecast.slice(0, 10).map((item, index) => ({
        id: `hour-${item.id}`,
        label: index === 0 ? 'Now' : formatHourLabel(item),
        temperatureLabel: formatTemperature(item.temperature, 'F'),
        precipitationLabel: formatPercentOrNA(item.precipitationProbability),
        icon: resolveWeatherIcon(item),
      }))

      const weeklyToggleEntry: MinimalForecastItem = {
        id: 'weekly-toggle',
        label: '7 Days',
        temperatureLabel: formatTemperature(displayWeather?.temperature, 'F'),
        precipitationLabel: formatPercentOrNA(nowPrecipProb),
        icon: resolveWeatherIcon(displayWeather ?? {}),
        toggleMode: 'daily',
      }

      return [weeklyToggleEntry, ...hourlyEntries]
    }

    return [todayEntry, ...dailyEntries]
  }, [activeForecastMode, displayDailyForecast, displayHourlyForecast, displayWeather, hasHourlyForecast])
  const isForecastLoading = loadingData && !usingCustomLocation && displayDailyForecast.length === 0
  const forecastAriaLabel =
    activeForecastMode === 'hourly' ? 'Today and next hours forecast' : 'Today and next seven days forecast'

  return (
    <section className="page-stack overview-page-stack">
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

        {displayOfficialAlerts.length > 0 ? (
          <section className="overview-official-alerts" aria-label="Active alerts">
            <div className="overview-official-alerts-row">
              {displayOfficialAlerts.map((item) => {
                const visual = resolveOfficialAlertVisual(item)
                const title = resolveOfficialAlertTitle(item)
                const area = resolveOfficialAlertArea(item, activeLocation.detail || activeLocation.name)

                return (
                  <article
                    key={item.id}
                    className={`overview-official-alert-card overview-official-alert-card--official is-${visual.tone}`}
                    aria-label={`${title} for ${area}`}
                  >
                    <div className="overview-official-alert-icon" aria-hidden>
                      <span>{visual.icon}</span>
                    </div>
                    <div className="overview-official-alert-body">
                      <div className="overview-official-alert-meta">
                        <span className="overview-official-alert-timing">{describeOfficialAlertTiming(item)}</span>
                        <button
                          type="button"
                          className="overview-official-alert-more"
                          aria-label={`More details for ${title}`}
                          onClick={() => setExpandedOfficialAlert(item)}
                        >
                          ...
                        </button>
                      </div>
                      <h3>{title}</h3>
                      <p className="overview-official-alert-area">{area}</p>
                    </div>
                  </article>
                )
              })}
            </div>
          </section>
        ) : null}

        {recentTriggeredAlerts.length > 0 ? (
          <section className="overview-recent-alerts" aria-label="Recent alerts">
            <div className="overview-recent-alerts-row">
              {recentTriggeredAlerts.map((item) => {
                const criteriaItem = item.criteriaId ? criteriaById.get(item.criteriaId) : undefined
                const visual = resolveOfficialAlertVisual(item)
                const title = criteriaItem?.name?.trim() || item.eventType?.trim() || item.headline?.trim() || 'Alert'
                const area = formatFriendlyLocation(item.location || 'Selected area')
                const icon = criteriaItem ? resolveCriteriaTileIcon(criteriaItem) : visual.icon

                return (
                  <button
                    key={item.id}
                    ref={(node) => setRecentAlertRef(item.id, node)}
                    type="button"
                    className={`overview-official-alert-card is-${visual.tone}${dismissingAlertIds.includes(item.id) ? ' is-dismissing' : ''}`}
                    aria-label={`${title} in ${area}`}
                    disabled={busyAlertId === item.id || dismissingAlertIds.includes(item.id)}
                    onClick={() => void handleDismissRecentAlert(item.id)}
                  >
                    <div className="overview-official-alert-icon" aria-hidden>
                      <span>{icon}</span>
                    </div>
                    <div className="overview-official-alert-body">
                      <div className="overview-official-alert-meta overview-recent-alert-meta">
                        <span className="overview-official-alert-timing">{describeRecentAlertTiming(item, now.getTime())}</span>
                      </div>
                      <h3>{title}</h3>
                      <p className="overview-official-alert-area">{area}</p>
                    </div>
                  </button>
                )
              })}
            </div>
          </section>
        ) : null}

        <section
          className={`overview-minimal-forecast-strip${isForecastLoading ? ' is-loading' : ''}`}
          aria-label={forecastAriaLabel}
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
                item.toggleMode ? (
                  <button
                    key={item.id}
                    type="button"
                    className={`overview-minimal-forecast-item overview-minimal-forecast-item--toggle${
                      activeForecastMode === 'hourly' ? ' is-hourly-active' : ''
                    }`}
                    aria-label={
                      item.toggleMode === 'hourly' ? 'Show hourly forecast' : 'Show seven day forecast'
                    }
                    onClick={() => setForecastMode(item.toggleMode ?? 'daily')}
                  >
                    <p className="overview-minimal-forecast-label">{item.label}</p>
                    <p className="overview-minimal-forecast-temp">{item.temperatureLabel}</p>
                    <p className="overview-minimal-forecast-icon" aria-hidden>
                      {item.icon}
                    </p>
                    <p className="overview-minimal-forecast-precip">{item.precipitationLabel}</p>
                  </button>
                ) : (
                  <article key={item.id} className="overview-minimal-forecast-item">
                    <p className="overview-minimal-forecast-label">{item.label}</p>
                    <p className="overview-minimal-forecast-temp">{item.temperatureLabel}</p>
                    <p className="overview-minimal-forecast-icon" aria-hidden>
                      {item.icon}
                    </p>
                    <p className="overview-minimal-forecast-precip">{item.precipitationLabel}</p>
                  </article>
                )
              ))}
            </div>
          )}
        </section>
      </div>

      {expandedOfficialAlert ? (
        <div
          className="overview-location-dialog-backdrop overview-official-alert-dialog-backdrop"
          role="presentation"
          onClick={() => setExpandedOfficialAlert(null)}
        >
          <div
            className="overview-location-dialog overview-official-alert-dialog"
            role="dialog"
            aria-modal="true"
            aria-label={`${resolveOfficialAlertTitle(expandedOfficialAlert)} details`}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="overview-location-dialog-header overview-official-alert-dialog-header">
              <div>
                <p className="overview-official-alert-dialog-kicker">{describeOfficialAlertTiming(expandedOfficialAlert)}</p>
                <h2>{resolveOfficialAlertTitle(expandedOfficialAlert)}</h2>
                <p className="overview-location-dialog-copy">
                  {resolveOfficialAlertArea(expandedOfficialAlert, activeLocation.detail || activeLocation.name)}
                </p>
              </div>
              <button
                type="button"
                className="overview-official-alert-dialog-close"
                aria-label="Close alert details"
                onClick={() => setExpandedOfficialAlert(null)}
              >
                ✕
              </button>
            </div>

            <div className="overview-official-alert-dialog-content">
              {expandedOfficialAlert.headline?.trim() &&
              expandedOfficialAlert.headline.trim() !== resolveOfficialAlertTitle(expandedOfficialAlert) ? (
                <p className="overview-official-alert-dialog-lead">{expandedOfficialAlert.headline.trim()}</p>
              ) : null}
              {expandedOfficialAlert.description?.trim() ? (
                <p className="overview-official-alert-dialog-copy-block">{expandedOfficialAlert.description.trim()}</p>
              ) : null}
              <div className="overview-official-alert-dialog-meta">
                {expandedOfficialAlert.severity?.trim() ? (
                  <div>
                    <span>Severity</span>
                    <strong>{expandedOfficialAlert.severity.trim()}</strong>
                  </div>
                ) : null}
                {expandedOfficialAlert.onset ? (
                  <div>
                    <span>Starts</span>
                    <strong>{formatDate(expandedOfficialAlert.onset)}</strong>
                  </div>
                ) : null}
                {expandedOfficialAlert.expires ? (
                  <div>
                    <span>Ends</span>
                    <strong>{formatDate(expandedOfficialAlert.expires)}</strong>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
