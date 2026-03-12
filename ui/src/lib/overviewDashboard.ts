import { deriveLifecycleState } from './alertConsole'
import { formatFriendlyLocation, formatRelativeTimeCompact } from './formatting'
import type { AlertCriteria, AlertEvent, WeatherCondition } from '../types'

export interface OverviewRuleChip {
  id: string
  icon: string
  label: string
  href: string
}

export interface OverviewActivityItem {
  id: string
  icon: string
  title: string
  description: string
  timestamp?: string
  href: string
}

export interface OverviewTimelineItem {
  id: string
  icon: string
  timeLabel: string
  title: string
  description: string
}

export interface OverviewDashboardSummary {
  monitoringLabel: string
  freshnessLabel: string
  watchRules: OverviewRuleChip[]
  recentActivity: OverviewActivityItem[]
  timeline: OverviewTimelineItem[]
  calmLabel?: string
  mapRuleCount: number
}

export function buildOverviewDashboardSummary(
  criteria: AlertCriteria[],
  alerts: AlertEvent[],
  currentWeather: WeatherCondition | null,
  now = Date.now(),
): OverviewDashboardSummary {
  const enabledCriteria = criteria.filter((item) => item.enabled !== false)
  const monitoringLabel = `Monitoring ${formatFriendlyLocation(
    enabledCriteria[0]?.location ?? criteria[0]?.location ?? currentWeather?.location,
  )}`
  const watchRules = enabledCriteria.slice(0, 6).map((item) => ({
    id: item.id,
    icon: resolveRuleIcon(item),
    label: describeOverviewRule(item),
    href: '/app/alerts',
  }))

  const recentActivity = buildRecentActivity(criteria, alerts, currentWeather, now)
  const timeline = buildTimeline(criteria, alerts, currentWeather, now)
  const latestAlertTime = alerts
    .map((item) => item.sentAt ?? item.alertTime ?? item.acknowledgedAt ?? item.expiredAt)
    .filter((value): value is string => Boolean(value))
    .sort((left, right) => new Date(right).getTime() - new Date(left).getTime())[0]

  return {
    monitoringLabel,
    freshnessLabel: currentWeather?.timestamp ? `Updated ${formatRelativeTimeCompact(currentWeather.timestamp, now)}` : 'Update pending',
    watchRules,
    recentActivity,
    timeline,
    calmLabel: latestAlertTime ? `No alerts triggered in the last ${formatRelativeTimeCompact(latestAlertTime, now).replace(' ago', '')}.` : 'No alerts triggered yet.',
    mapRuleCount: enabledCriteria.length,
  }
}

function buildRecentActivity(
  criteria: AlertCriteria[],
  alerts: AlertEvent[],
  currentWeather: WeatherCondition | null,
  now: number,
) {
  const criteriaById = new Map(criteria.map((item) => [item.id, item]))
  const alertItems: OverviewActivityItem[] = alerts
    .map((alert) => {
      const criteriaItem = alert.criteriaId ? criteriaById.get(alert.criteriaId) : undefined
      const lifecycle = deriveLifecycleState(alert, now)
      const timestamp = alert.expiredAt ?? alert.acknowledgedAt ?? alert.sentAt ?? alert.alertTime
      return {
        id: `alert-${alert.id}`,
        icon: resolveActivityIcon(lifecycle),
        title: criteriaItem?.name?.trim() || alert.headline?.trim() || 'Weather alert',
        description: describeActivity(lifecycle, alert, criteriaItem),
        timestamp,
        href: '/app/events',
      }
    })
    .sort((left, right) => new Date(right.timestamp ?? 0).getTime() - new Date(left.timestamp ?? 0).getTime())

  const ruleCreateItems: OverviewActivityItem[] = criteria
    .filter((item) => item.createdAt)
    .map((item) => ({
      id: `rule-${item.id}`,
      icon: '✨',
      title: item.name?.trim() || 'New alert rule',
      description: `Started monitoring ${formatFriendlyLocation(item.location)}`,
      timestamp: item.createdAt,
      href: '/app/alerts',
    }))
    .sort((left, right) => new Date(right.timestamp ?? 0).getTime() - new Date(left.timestamp ?? 0).getTime())

  const forecastItem =
    currentWeather?.headline && criteria.some((item) => item.monitorForecast)
      ? [
          {
            id: 'forecast-watch',
            icon: '🌤',
            title: 'Forecast watch active',
            description: currentWeather.headline,
            timestamp: currentWeather.timestamp,
            href: '/app/events',
          } satisfies OverviewActivityItem,
        ]
      : []

  return [...alertItems, ...ruleCreateItems, ...forecastItem]
    .sort((left, right) => new Date(right.timestamp ?? 0).getTime() - new Date(left.timestamp ?? 0).getTime())
    .slice(0, 6)
}

function buildTimeline(criteria: AlertCriteria[], alerts: AlertEvent[], currentWeather: WeatherCondition | null, now: number) {
  const enabledCriteria = criteria.filter((item) => item.enabled !== false)
  const recentAlertItems: OverviewTimelineItem[] = alerts
    .slice()
    .sort((left, right) => new Date(left.alertTime ?? left.sentAt ?? 0).getTime() - new Date(right.alertTime ?? right.sentAt ?? 0).getTime())
    .slice(-3)
    .map((alert) => ({
      id: `timeline-alert-${alert.id}`,
      icon: resolveActivityIcon(deriveLifecycleState(alert, now)),
      timeLabel: formatClockTime(alert.alertTime ?? alert.sentAt),
      title: alert.headline?.trim() || 'Alert event',
      description: alert.reason?.trim() || 'Weather change detected',
    }))

  const currentItem: OverviewTimelineItem[] = currentWeather
    ? [
        {
          id: 'timeline-now',
          icon: '📍',
          timeLabel: 'Now',
          title: currentWeather.headline?.trim() || 'Current conditions',
          description: `${formatTemperatureLabel(currentWeather)} ${currentWeather.headline?.trim() || 'conditions in view'}`.trim(),
        },
      ]
    : []

  const forecastItems: OverviewTimelineItem[] = enabledCriteria
    .filter((item) => item.monitorForecast)
    .slice(0, 3)
    .map((item) => ({
      id: `timeline-forecast-${item.id}`,
      icon: resolveRuleIcon(item),
      timeLabel: item.forecastWindowHours ? `Next ${item.forecastWindowHours}h` : 'Later',
      title: item.name?.trim() || describeOverviewRule(item),
      description: describeForecastWatch(item),
    }))

  const calmItem =
    recentAlertItems.length === 0
      ? [
          {
            id: 'timeline-calm',
            icon: '🕊',
            timeLabel: 'Now',
            title: 'Calm conditions',
            description: 'No alert conditions are currently active.',
          } satisfies OverviewTimelineItem,
        ]
      : []

  return [...recentAlertItems, ...currentItem, ...forecastItems, ...calmItem].slice(0, 7)
}

function resolveActivityIcon(lifecycle: ReturnType<typeof deriveLifecycleState>) {
  switch (lifecycle) {
    case 'acknowledged':
      return '👀'
    case 'resolved':
      return '✅'
    case 'archived':
      return '🕘'
    case 'triggered':
    default:
      return '⚠️'
  }
}

function describeActivity(lifecycle: ReturnType<typeof deriveLifecycleState>, alert: AlertEvent, criteria?: AlertCriteria) {
  switch (lifecycle) {
    case 'acknowledged':
      return `Acknowledged for ${formatFriendlyLocation(alert.location ?? criteria?.location)}`
    case 'resolved':
      return `Resolved at ${formatFriendlyLocation(alert.location ?? criteria?.location)}`
    case 'archived':
      return `Moved to history for ${formatFriendlyLocation(alert.location ?? criteria?.location)}`
    case 'triggered':
    default:
      return alert.reason?.trim() || `Triggered for ${formatFriendlyLocation(alert.location ?? criteria?.location)}`
  }
}

function resolveRuleIcon(criteria: AlertCriteria) {
  if (criteria.temperatureThreshold != null || criteria.dewPointThreshold != null) {
    return '🌡'
  }
  if (criteria.rainThreshold != null) {
    return '🌧'
  }
  if (criteria.maxWindSpeed != null || criteria.windGustThreshold != null) {
    return '🌬'
  }
  if (criteria.humidityThreshold != null) {
    return '💧'
  }
  if (criteria.skyCoverThreshold != null) {
    return '🌫'
  }
  if (criteria.riverStageThreshold != null || criteria.riverFloodCategoryThreshold) {
    return '🌊'
  }
  return '🔎'
}

function describeOverviewRule(criteria: AlertCriteria) {
  if (criteria.temperatureThreshold != null && criteria.temperatureDirection === 'ABOVE') {
    return `Heat above ${criteria.temperatureThreshold}°${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria.temperatureThreshold != null && criteria.temperatureDirection === 'BELOW') {
    return `Cold below ${criteria.temperatureThreshold}°${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria.humidityThreshold != null && criteria.humidityDirection === 'ABOVE') {
    return `Humidity above ${criteria.humidityThreshold}%`
  }
  if (criteria.humidityThreshold != null && criteria.humidityDirection === 'BELOW') {
    return `Humidity below ${criteria.humidityThreshold}%`
  }
  if (criteria.dewPointThreshold != null && criteria.dewPointDirection === 'ABOVE') {
    return `Dew point above ${criteria.dewPointThreshold}°${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria.dewPointThreshold != null && criteria.dewPointDirection === 'BELOW') {
    return `Dew point below ${criteria.dewPointThreshold}°${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria.rainThreshold != null) {
    return `Rain chance above ${criteria.rainThreshold}%`
  }
  if (criteria.maxWindSpeed != null) {
    return `Wind above ${criteria.maxWindSpeed} km/h`
  }
  if (criteria.windGustThreshold != null) {
    return `Wind gusts above ${criteria.windGustThreshold} km/h`
  }
  if (criteria.skyCoverThreshold != null && criteria.skyCoverDirection === 'BELOW') {
    return `Clear skies below ${criteria.skyCoverThreshold}% cloud cover`
  }
  if (criteria.skyCoverThreshold != null && criteria.skyCoverDirection === 'ABOVE') {
    return `Cloud cover above ${criteria.skyCoverThreshold}%`
  }
  if (criteria.riverStageThreshold != null && criteria.riverStageDirection === 'ABOVE') {
    return `River rising above ${criteria.riverStageThreshold} ft`
  }
  if (criteria.riverFloodCategoryThreshold) {
    return `Flood risk at ${criteria.riverFloodCategoryThreshold.toLowerCase()} stage`
  }
  return criteria.name?.trim() || 'Custom weather watch'
}

function describeForecastWatch(criteria: AlertCriteria) {
  if (criteria.monitorForecast) {
    return `Forecast signal for ${describeOverviewRule(criteria)}`
  }
  return `Monitoring ${formatFriendlyLocation(criteria.location)}`
}

function formatClockTime(value?: string) {
  if (!value) {
    return 'Earlier'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
}

function formatTemperatureLabel(currentWeather: WeatherCondition) {
  if (currentWeather.temperature == null) {
    return ''
  }
  const fahrenheit = Math.round((currentWeather.temperature * 9) / 5 + 32)
  return `${fahrenheit}°`
}
