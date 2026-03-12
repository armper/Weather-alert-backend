import { describeCriteria } from './criteria'
import { formatFriendlyLocation, formatNumber, formatPercent, formatRelativeTime, formatTemperature, formatWind } from './formatting'
import type { AlertCriteria, AlertEvent, WeatherCondition } from '../types'

const DAY_IN_MS = 24 * 60 * 60 * 1000

export type AlertLifecycleState = 'triggered' | 'acknowledged' | 'resolved' | 'archived'

export interface AlertMetricChip {
  icon: string
  label: string
}

export interface AlertConsoleItem {
  key: string
  alert: AlertEvent
  criteria?: AlertCriteria
  duplicateCount: number
  lifecycleState: AlertLifecycleState
  title: string
  summary: string
  semanticLabel: string
  metrics: AlertMetricChip[]
  sortTime: number
  startedAt?: string
  resolvedAt?: string
  durationLabel?: string
  statusTimestampLabel: string
  secondaryTimestampLabel?: string
  locationLabel: string
}

export interface AlertConsoleSummary {
  watchLocation: string
  freshnessLabel: string
  activeCount: number
  allClear: boolean
  calmStreakLabel?: string
  lastAlertLabel?: string
  watchContext: string[]
  activeAlerts: AlertConsoleItem[]
  recentAlerts: AlertConsoleItem[]
  alertHistory: AlertConsoleItem[]
}

export function buildAlertConsoleSummary(
  criteria: AlertCriteria[],
  alerts: AlertEvent[],
  currentWeather: WeatherCondition | null,
  now = Date.now(),
): AlertConsoleSummary {
  const enabledCriteria = criteria.filter((item) => item.enabled !== false)
  const watchLocation = formatFriendlyLocation(
    enabledCriteria[0]?.location ?? criteria[0]?.location ?? currentWeather?.location,
  )
  const watchContext = enabledCriteria.map((item) => describeCriteria(item))
  const groupedItems = groupAlertEvents(alerts, criteria, now)
  const activeAlerts = groupedItems
    .filter((item) => item.lifecycleState === 'triggered' || item.lifecycleState === 'acknowledged')
    .sort((left, right) => right.sortTime - left.sortTime)
  const recentAlerts = groupedItems
    .filter((item) => item.lifecycleState === 'resolved')
    .sort((left, right) => right.sortTime - left.sortTime)
  const alertHistory = groupedItems
    .filter((item) => item.lifecycleState === 'archived')
    .sort((left, right) => right.sortTime - left.sortTime)
  const lastAlert = [...groupedItems].sort((left, right) => right.sortTime - left.sortTime)[0]

  return {
    watchLocation,
    freshnessLabel: currentWeather?.timestamp ? `Weather updated ${formatRelativeTime(currentWeather.timestamp)}` : 'Weather update pending',
    activeCount: activeAlerts.length,
    allClear: activeAlerts.length === 0,
    calmStreakLabel: activeAlerts.length === 0 ? buildCalmStreakLabel(enabledCriteria, groupedItems, now) : undefined,
    lastAlertLabel: lastAlert ? buildLastAlertLabel(lastAlert) : undefined,
    watchContext,
    activeAlerts,
    recentAlerts,
    alertHistory,
  }
}

function groupAlertEvents(alerts: AlertEvent[], criteria: AlertCriteria[], now: number): AlertConsoleItem[] {
  const criteriaById = new Map(criteria.map((item) => [item.id, item]))
  const grouped = new Map<string, { alert: AlertEvent; duplicateCount: number }>()

  for (const alert of alerts) {
    const key = [
      alert.criteriaId ?? '',
      alert.eventKey ?? '',
      alert.headline ?? '',
      alert.reason ?? '',
      alert.location ?? '',
    ].join('|')

    const current = grouped.get(key)
    if (!current) {
      grouped.set(key, { alert, duplicateCount: 1 })
      continue
    }

    current.duplicateCount += 1
    if (resolveSortTime(alert) >= resolveSortTime(current.alert)) {
      current.alert = alert
    }
  }

  return Array.from(grouped.entries()).map(([key, value]) => {
    const criteriaItem = value.alert.criteriaId ? criteriaById.get(value.alert.criteriaId) : undefined
    return toConsoleItem(key, value.alert, criteriaItem, value.duplicateCount, now)
  })
}

function toConsoleItem(
  key: string,
  alert: AlertEvent,
  criteria: AlertCriteria | undefined,
  duplicateCount: number,
  now: number,
): AlertConsoleItem {
  const lifecycleState = deriveLifecycleState(alert, now)
  const startedAt = alert.alertTime
  const resolvedAt = resolveResolvedAt(alert)
  const statusTimestampLabel = buildStatusTimestampLabel(lifecycleState, alert)
  const secondaryTimestampLabel = resolvedAt ? `Ended ${formatRelativeTime(resolvedAt)}` : undefined

  return {
    key,
    alert,
    criteria,
    duplicateCount,
    lifecycleState,
    title: buildAlertTitle(criteria),
    summary: buildAlertSummary(alert, criteria),
    semanticLabel: buildSemanticLabel(lifecycleState),
    metrics: buildMetricChips(alert),
    sortTime: resolveSortTime(alert),
    startedAt,
    resolvedAt,
    durationLabel: startedAt && resolvedAt ? formatDurationLabel(startedAt, resolvedAt) : undefined,
    statusTimestampLabel,
    secondaryTimestampLabel,
    locationLabel: formatFriendlyLocation(alert.location ?? criteria?.location),
  }
}

export function deriveLifecycleState(alert: AlertEvent, now = Date.now()): AlertLifecycleState {
  const resolvedAt = resolveResolvedAt(alert)
  if (resolvedAt) {
    return now - new Date(resolvedAt).getTime() >= DAY_IN_MS ? 'archived' : 'resolved'
  }
  if ((alert.status ?? '').toUpperCase() === 'ACKNOWLEDGED' || alert.acknowledgedAt) {
    return 'acknowledged'
  }
  return 'triggered'
}

function resolveResolvedAt(alert: AlertEvent): string | undefined {
  if ((alert.status ?? '').toUpperCase() === 'EXPIRED') {
    return alert.expiredAt ?? alert.sentAt ?? alert.alertTime
  }
  return alert.expiredAt
}

function resolveSortTime(alert: AlertEvent): number {
  return new Date(resolveResolvedAt(alert) ?? alert.acknowledgedAt ?? alert.sentAt ?? alert.alertTime ?? 0).getTime()
}

function buildAlertTitle(criteria: AlertCriteria | undefined): string {
  if (criteria?.name?.trim()) {
    return criteria.name.trim()
  }
  if (criteria?.temperatureThreshold != null && criteria.temperatureDirection === 'ABOVE') {
    return 'Heat Alert'
  }
  if (criteria?.temperatureThreshold != null && criteria.temperatureDirection === 'BELOW') {
    return 'Cold Alert'
  }
  if (criteria?.rainThreshold != null) {
    return 'Rain Alert'
  }
  if (criteria?.windGustThreshold != null || criteria?.maxWindSpeed != null) {
    return 'Wind Alert'
  }
  if (criteria?.humidityThreshold != null) {
    return 'Humidity Alert'
  }
  if (criteria?.riverStageThreshold != null || criteria?.riverFloodCategoryThreshold) {
    return 'River Alert'
  }
  return 'Weather Event'
}

function buildAlertSummary(alert: AlertEvent, criteria: AlertCriteria | undefined): string {
  if (criteria?.temperatureThreshold != null && criteria.temperatureDirection) {
    return criteria.temperatureDirection === 'ABOVE'
      ? `Temperature exceeded ${formatNumber(criteria.temperatureThreshold)}°${criteria.temperatureUnit ?? 'F'}`
      : `Temperature dropped below ${formatNumber(criteria.temperatureThreshold)}°${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria?.rainThreshold != null) {
    return criteria.rainThresholdType === 'AMOUNT'
      ? `Rainfall exceeded ${formatNumber(criteria.rainThreshold)} mm`
      : `Rain chance exceeded ${formatNumber(criteria.rainThreshold)}%`
  }
  if (criteria?.maxWindSpeed != null) {
    return `Wind speed exceeded ${formatNumber(criteria.maxWindSpeed)} km/h`
  }
  if (criteria?.windGustThreshold != null) {
    return `Wind gust exceeded ${formatNumber(criteria.windGustThreshold)} km/h`
  }
  if (criteria?.humidityThreshold != null && criteria.humidityDirection) {
    return criteria.humidityDirection === 'ABOVE'
      ? `Humidity reached ${formatNumber(criteria.humidityThreshold)}%`
      : `Humidity dropped below ${formatNumber(criteria.humidityThreshold)}%`
  }
  if (criteria?.dewPointThreshold != null && criteria.dewPointDirection) {
    return criteria.dewPointDirection === 'ABOVE'
      ? `Dew point reached ${formatNumber(criteria.dewPointThreshold)}°${criteria.temperatureUnit ?? 'F'}`
      : `Dew point dropped below ${formatNumber(criteria.dewPointThreshold)}°${criteria.temperatureUnit ?? 'F'}`
  }
  if (criteria?.skyCoverThreshold != null && criteria.skyCoverDirection) {
    return criteria.skyCoverDirection === 'ABOVE'
      ? `Sky cover exceeded ${formatNumber(criteria.skyCoverThreshold)}%`
      : `Sky cover dropped below ${formatNumber(criteria.skyCoverThreshold)}%`
  }
  if (criteria?.riverStageThreshold != null && criteria.riverStageDirection) {
    return criteria.riverStageDirection === 'ABOVE'
      ? `River stage exceeded ${formatNumber(criteria.riverStageThreshold)} ft`
      : `River stage dropped below ${formatNumber(criteria.riverStageThreshold)} ft`
  }
  if (criteria?.riverFloodCategoryThreshold) {
    return `River reached ${criteria.riverFloodCategoryThreshold.toLowerCase()} flood stage`
  }
  if (alert.reason?.startsWith('Matched ') && alert.reason.includes(':')) {
    const detail = alert.reason.split(':').slice(1).join(':').trim()
    if (detail) {
      return `${detail} condition detected`
    }
  }
  if (alert.reason?.trim()) {
    return alert.reason.trim()
  }
  return 'Conditions matched the current watch'
}

function buildSemanticLabel(lifecycleState: AlertLifecycleState): string {
  switch (lifecycleState) {
    case 'acknowledged':
      return 'Acknowledged'
    case 'resolved':
      return 'Resolved'
    case 'archived':
      return 'Archived'
    case 'triggered':
    default:
      return 'Triggered'
  }
}

function buildMetricChips(alert: AlertEvent): AlertMetricChip[] {
  return [
    alert.conditionTemperatureC != null ? { icon: '🌡', label: formatTemperature(alert.conditionTemperatureC, 'F') } : null,
    alert.conditionHumidity != null ? { icon: '💧', label: formatPercent(alert.conditionHumidity) } : null,
    alert.conditionPrecipitationProbability != null
      ? { icon: '🌧', label: formatPercent(alert.conditionPrecipitationProbability) }
      : null,
    alert.conditionWindGust != null ? { icon: '🌬', label: formatWind(alert.conditionWindGust) } : null,
    alert.conditionRiverObservedStage != null
      ? { icon: '🌊', label: `${formatNumber(alert.conditionRiverObservedStage)} ${alert.conditionRiverStageUnit ?? 'ft'}` }
      : null,
  ].filter((value): value is AlertMetricChip => Boolean(value))
}

function buildStatusTimestampLabel(lifecycleState: AlertLifecycleState, alert: AlertEvent): string {
  const triggeredAt = alert.alertTime
  if (lifecycleState === 'archived' || lifecycleState === 'resolved') {
    return triggeredAt ? `Triggered ${formatRelativeTime(triggeredAt)}` : 'Resolved'
  }
  if (lifecycleState === 'acknowledged') {
    return alert.acknowledgedAt ? `Acknowledged ${formatRelativeTime(alert.acknowledgedAt)}` : 'Acknowledged'
  }
  return triggeredAt ? `Triggered ${formatRelativeTime(triggeredAt)}` : 'Triggered just now'
}

function formatDurationLabel(startedAt: string, resolvedAt: string): string {
  const durationMs = new Date(resolvedAt).getTime() - new Date(startedAt).getTime()
  if (!Number.isFinite(durationMs) || durationMs <= 0) {
    return ''
  }

  const minutes = Math.round(durationMs / (60 * 1000))
  if (minutes < 60) {
    return `Duration ${minutes} min`
  }
  const hours = Math.round(minutes / 60)
  if (hours < 48) {
    return `Duration ${hours} hr`
  }
  const days = Math.round(hours / 24)
  return `Duration ${days} day${days === 1 ? '' : 's'}`
}

function buildCalmStreakLabel(criteria: AlertCriteria[], items: AlertConsoleItem[], now: number): string | undefined {
  const lastResolved = items
    .filter((item) => item.lifecycleState === 'resolved' || item.lifecycleState === 'archived')
    .map((item) => item.resolvedAt)
    .filter((value): value is string => Boolean(value))
    .sort((left, right) => new Date(right).getTime() - new Date(left).getTime())[0]

  const oldestWatchCreatedAt = criteria
    .map((item) => item.createdAt)
    .filter((value): value is string => Boolean(value))
    .sort((left, right) => new Date(left).getTime() - new Date(right).getTime())[0]

  const startAt = lastResolved ?? oldestWatchCreatedAt
  if (!startAt) {
    return undefined
  }

  const elapsedMs = now - new Date(startAt).getTime()
  if (!Number.isFinite(elapsedMs) || elapsedMs < 0) {
    return undefined
  }
  if (elapsedMs < 60 * 60 * 1000) {
    const minutes = Math.max(1, Math.round(elapsedMs / (60 * 1000)))
    return `${minutes} min`
  }
  if (elapsedMs < DAY_IN_MS) {
    const hours = Math.max(1, Math.round(elapsedMs / (60 * 60 * 1000)))
    return `${hours} hr`
  }
  const days = Math.max(1, Math.round(elapsedMs / DAY_IN_MS))
  return `${days} day${days === 1 ? '' : 's'}`
}

function buildLastAlertLabel(item: AlertConsoleItem): string {
  if (item.resolvedAt) {
    return `Last alert resolved ${formatRelativeTime(item.resolvedAt)}`
  }
  if (item.startedAt) {
    return `Last alert triggered ${formatRelativeTime(item.startedAt)}`
  }
  return 'Last alert just updated'
}
