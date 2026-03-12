import { deriveLifecycleState } from './alertConsole'
import { describeCriteria, resolveCriteriaMarkerIcon } from './criteria'
import { formatFriendlyLocation, formatRelativeTime } from './formatting'
import type { AlertCriteria, AlertEvent, WeatherCondition } from '../types'

export type RuleSortMode = 'attention' | 'recentlyTriggered' | 'alphabetical' | 'location'
export type RuleMonitoringState = 'Monitoring' | 'Stable' | 'Triggered' | 'Paused'
export type RuleTone = 'critical' | 'warning' | 'calm' | 'muted'
export type RuleEmphasis = 'critical' | 'recent' | 'quiet' | 'paused'

export interface RuleMapGroupItem {
  criteriaId: string
  ruleName: string
  triggerCondition: string
  monitoringState: RuleMonitoringState
  monitoringTone: RuleTone
  icon: string
}

export interface RuleLocationGroup {
  id: string
  latitude: number
  longitude: number
  radiusKm: number
  locationLabel: string
  locationNames: string[]
  statusTone: RuleTone
  statusLabel: string
  icon: string
  ruleCount: number
  triggeredCount: number
  rules: RuleMapGroupItem[]
}

export interface RuleHeartbeatSummary {
  activeRules: number
  pausedRules: number
  triggeredRules: number
  recentRules: number
  monitoredLocationsLabel: string
  freshnessLabel: string
  systemStateLabel: string
  allClear: boolean
}

export interface RuleViewModel {
  criteria: AlertCriteria
  ruleName: string
  triggerCondition: string
  locationLabel: string
  locationName: string
  statusLabel: RuleMonitoringState
  statusTone: RuleTone
  statusDetail: string
  emphasis: RuleEmphasis
  lastAlertLabel: string
  lastAlertTime: number
  priority: number
  icon: string
  latitude?: number
  longitude?: number
  radiusKm?: number
  locationGroupId?: string
}

export interface RuleDashboardSummary {
  heartbeat: RuleHeartbeatSummary
  activeRules: RuleViewModel[]
  pausedRules: RuleViewModel[]
  locationGroups: RuleLocationGroup[]
}

const RECENT_WINDOW_MS = 24 * 60 * 60 * 1000
const DEFAULT_RADIUS_KM = 8

export function buildRuleDashboardSummary(
  criteria: AlertCriteria[],
  alerts: AlertEvent[],
  currentWeather: WeatherCondition | null,
  now = Date.now(),
): RuleDashboardSummary {
  const latestAlertByCriteria = new Map<string, AlertEvent>()
  const activeAlertByCriteria = new Map<string, AlertEvent>()

  for (const alert of alerts) {
    if (!alert.criteriaId) {
      continue
    }
    const currentLatest = latestAlertByCriteria.get(alert.criteriaId)
    if (!currentLatest || resolveAlertTime(alert) >= resolveAlertTime(currentLatest)) {
      latestAlertByCriteria.set(alert.criteriaId, alert)
    }

    const lifecycle = deriveLifecycleState(alert, now)
    if (lifecycle === 'triggered' || lifecycle === 'acknowledged') {
      const currentActive = activeAlertByCriteria.get(alert.criteriaId)
      if (!currentActive || resolveAlertTime(alert) >= resolveAlertTime(currentActive)) {
        activeAlertByCriteria.set(alert.criteriaId, alert)
      }
    }
  }

  const rules = criteria.map((item) =>
    buildRuleViewModel(item, latestAlertByCriteria.get(item.id), activeAlertByCriteria.get(item.id), currentWeather, now),
  )
  const locationGroups = buildRuleLocationGroups(rules)
  const groupIdByCriteriaId = new Map<string, string>()

  for (const group of locationGroups) {
    for (const rule of group.rules) {
      groupIdByCriteriaId.set(rule.criteriaId, group.id)
    }
  }

  const hydratedRules = rules.map((item) => ({
    ...item,
    locationGroupId: groupIdByCriteriaId.get(item.criteria.id),
  }))

  const activeRules = hydratedRules
    .filter((item) => item.criteria.enabled !== false)
    .sort((left, right) => compareRuleViewModel(left, right, 'attention'))
  const pausedRules = hydratedRules
    .filter((item) => item.criteria.enabled === false)
    .sort((left, right) => compareRuleViewModel(left, right, 'alphabetical'))
  const uniqueLocations = Array.from(
    new Set(
      criteria
        .filter((item) => item.enabled !== false)
        .map((item) => formatFriendlyLocation(item.location))
        .filter((value) => value && value !== 'Selected area'),
    ),
  )

  return {
    heartbeat: {
      activeRules: activeRules.length,
      pausedRules: pausedRules.length,
      triggeredRules: activeRules.filter((item) => item.statusLabel === 'Triggered').length,
      recentRules: activeRules.filter((item) => item.emphasis === 'recent').length,
      monitoredLocationsLabel: summarizeLocations(uniqueLocations),
      freshnessLabel: resolveMonitoringFreshnessLabel(currentWeather),
      systemStateLabel:
        activeRules.filter((item) => item.statusLabel === 'Triggered').length > 0
          ? `${activeRules.filter((item) => item.statusLabel === 'Triggered').length} rule currently triggered`
          : 'All clear',
      allClear: activeRules.every((item) => item.statusLabel !== 'Triggered'),
    },
    activeRules,
    pausedRules,
    locationGroups,
  }
}

export function compareRuleViewModel(left: RuleViewModel, right: RuleViewModel, sortMode: RuleSortMode) {
  switch (sortMode) {
    case 'recentlyTriggered':
      return right.lastAlertTime - left.lastAlertTime || compareByName(left.criteria, right.criteria)
    case 'alphabetical':
      return compareByName(left.criteria, right.criteria)
    case 'location':
      return compareByLocation(left.criteria, right.criteria) || compareByName(left.criteria, right.criteria)
    case 'attention':
    default:
      return right.priority - left.priority || right.lastAlertTime - left.lastAlertTime || compareByName(left.criteria, right.criteria)
  }
}

function buildRuleViewModel(
  criteria: AlertCriteria,
  latestAlert: AlertEvent | undefined,
  activeAlert: AlertEvent | undefined,
  currentWeather: WeatherCondition | null,
  now: number,
): RuleViewModel {
  const enabled = criteria.enabled !== false
  const recentActivity = latestAlert ? now - resolveAlertTime(latestAlert) <= RECENT_WINDOW_MS : false
  const currentSignal = describeCurrentSignal(criteria, currentWeather)

  let statusLabel: RuleMonitoringState
  let statusTone: RuleTone
  let statusDetail: string
  let emphasis: RuleEmphasis
  let priority: number

  if (!enabled) {
    statusLabel = 'Paused'
    statusTone = 'muted'
    statusDetail = 'Monitoring is paused until you turn this rule back on.'
    emphasis = 'paused'
    priority = 0
  } else if (activeAlert) {
    statusLabel = 'Triggered'
    statusTone = 'critical'
    statusDetail = 'A live weather event is currently matching this rule.'
    emphasis = 'critical'
    priority = 4
  } else if (currentSignal === null) {
    statusLabel = 'Monitoring'
    statusTone = recentActivity ? 'warning' : 'muted'
    statusDetail = 'Waiting for the next matching condition. Live rule checks are still running.'
    emphasis = recentActivity ? 'recent' : 'quiet'
    priority = recentActivity ? 3 : 2
  } else {
    statusLabel = 'Stable'
    statusTone = recentActivity ? 'warning' : 'calm'
    statusDetail = 'Conditions are currently safe for this rule.'
    emphasis = recentActivity ? 'recent' : 'quiet'
    priority = recentActivity ? 3 : 1
  }

  return {
    criteria,
    ruleName: criteria.name?.trim() || 'Custom rule',
    triggerCondition: describeCriteria(criteria),
    locationName: formatFriendlyLocation(criteria.location),
    locationLabel: `Watching: ${formatFriendlyLocation(criteria.location)}`,
    statusLabel,
    statusTone,
    statusDetail,
    emphasis,
    lastAlertLabel: buildLastAlertLabel(latestAlert),
    lastAlertTime: latestAlert ? resolveAlertTime(latestAlert) : 0,
    priority,
    icon: resolveCriteriaMarkerIcon(criteria),
    latitude: criteria.latitude,
    longitude: criteria.longitude,
    radiusKm: criteria.radiusKm ?? DEFAULT_RADIUS_KM,
  }
}

function buildLastAlertLabel(alert: AlertEvent | undefined): string {
  if (!alert) {
    return 'No alerts sent yet'
  }
  if (alert.sentAt) {
    return `Last alert sent ${formatRelativeTime(alert.sentAt)}`
  }
  if (alert.alertTime) {
    return `Last alert: ${formatRelativeTime(alert.alertTime)}`
  }
  return 'Alert history available'
}

function resolveMonitoringFreshnessLabel(currentWeather: WeatherCondition | null): string {
  if (currentWeather?.timestamp) {
    return `Forecast updated ${formatRelativeTime(currentWeather.timestamp)}`
  }
  // TODO: replace this fallback with dedicated backend polling metadata once refresh timestamps are exposed separately.
  return 'Forecast refresh pending'
}

function summarizeLocations(locations: string[]): string {
  if (locations.length === 0) {
    return 'Watching your saved areas'
  }
  if (locations.length === 1) {
    return `Watching ${locations[0]}`
  }
  if (locations.length === 2) {
    return `Watching ${locations[0]} and ${locations[1]}`
  }
  return `Watching ${locations[0]}, ${locations[1]}, and ${locations.length - 2} more`
}

function resolveAlertTime(alert: AlertEvent): number {
  return new Date(alert.sentAt ?? alert.alertTime ?? 0).getTime()
}

function compareByLocation(left: AlertCriteria, right: AlertCriteria) {
  return (left.location ?? '').localeCompare(right.location ?? '', undefined, { sensitivity: 'base' })
}

function compareByName(left: AlertCriteria, right: AlertCriteria) {
  return (left.name ?? '').localeCompare(right.name ?? '', undefined, { sensitivity: 'base' })
}

function describeCurrentSignal(criteria: AlertCriteria, currentWeather: WeatherCondition | null): number | null {
  if (!currentWeather) {
    return null
  }

  if (criteria.temperatureThreshold != null && criteria.temperatureDirection) {
    const currentTemperature = convertTemperatureForRule(currentWeather.temperature, criteria.temperatureUnit ?? 'F')
    return currentTemperature == null
      ? null
      : criteria.temperatureDirection === 'ABOVE'
        ? currentTemperature - criteria.temperatureThreshold
        : criteria.temperatureThreshold - currentTemperature
  }

  if (criteria.maxWindSpeed != null) {
    return currentWeather.windSpeed == null ? null : currentWeather.windSpeed - criteria.maxWindSpeed
  }

  if (criteria.rainThreshold != null) {
    return currentWeather.precipitationProbability == null ? null : currentWeather.precipitationProbability - criteria.rainThreshold
  }

  if (criteria.humidityThreshold != null && criteria.humidityDirection) {
    return currentWeather.humidity == null
      ? null
      : criteria.humidityDirection === 'ABOVE'
        ? currentWeather.humidity - criteria.humidityThreshold
        : criteria.humidityThreshold - currentWeather.humidity
  }

  if (criteria.dewPointThreshold != null && criteria.dewPointDirection) {
    const currentDewPoint = convertTemperatureForRule(currentWeather.dewPoint, criteria.temperatureUnit ?? 'F')
    return currentDewPoint == null
      ? null
      : criteria.dewPointDirection === 'ABOVE'
        ? currentDewPoint - criteria.dewPointThreshold
        : criteria.dewPointThreshold - currentDewPoint
  }

  if (criteria.windGustThreshold != null) {
    return currentWeather.windGust == null ? null : currentWeather.windGust - criteria.windGustThreshold
  }

  if (criteria.skyCoverThreshold != null && criteria.skyCoverDirection) {
    return currentWeather.skyCover == null
      ? null
      : criteria.skyCoverDirection === 'ABOVE'
        ? currentWeather.skyCover - criteria.skyCoverThreshold
        : criteria.skyCoverThreshold - currentWeather.skyCover
  }

  if (criteria.riverStageThreshold != null && criteria.riverStageDirection) {
    const currentStage = currentWeather.riverObservedStage ?? currentWeather.riverForecastStage
    return currentStage == null
      ? null
      : criteria.riverStageDirection === 'ABOVE'
        ? currentStage - criteria.riverStageThreshold
        : criteria.riverStageThreshold - currentStage
  }

  if (criteria.riverFloodCategoryThreshold) {
    const thresholdRank = resolveFloodCategoryRank(criteria.riverFloodCategoryThreshold)
    const currentRank = resolveFloodCategoryRank(currentWeather.riverForecastCategory ?? currentWeather.riverObservedCategory ?? undefined)
    return currentRank == null || thresholdRank == null ? null : currentRank - thresholdRank
  }

  return null
}

function convertTemperatureForRule(celsius: number | undefined, targetUnit: 'F' | 'C') {
  if (celsius == null || Number.isNaN(celsius)) {
    return null
  }
  return targetUnit === 'C' ? celsius : (celsius * 9) / 5 + 32
}

function resolveFloodCategoryRank(category?: string) {
  switch ((category ?? '').toUpperCase()) {
    case 'ACTION':
      return 1
    case 'MINOR':
      return 2
    case 'MODERATE':
      return 3
    case 'MAJOR':
      return 4
    default:
      return null
  }
}

function buildRuleLocationGroups(rules: RuleViewModel[]): RuleLocationGroup[] {
  interface MutableGroup {
    latitude: number
    longitude: number
    radiusKm: number
    locationNames: Set<string>
    rules: RuleViewModel[]
  }

  const mutableGroups: MutableGroup[] = []

  for (const rule of rules) {
    if (rule.latitude == null || rule.longitude == null) {
      continue
    }

    const targetGroup = mutableGroups.find((group) =>
      isWithinGroupingDistance(rule.latitude!, rule.longitude!, group.latitude, group.longitude, groupingThresholdKm(rule, group)),
    )

    if (!targetGroup) {
      mutableGroups.push({
        latitude: rule.latitude,
        longitude: rule.longitude,
        radiusKm: rule.radiusKm ?? DEFAULT_RADIUS_KM,
        locationNames: new Set([rule.locationName]),
        rules: [rule],
      })
      continue
    }

    targetGroup.rules.push(rule)
    targetGroup.locationNames.add(rule.locationName)
    targetGroup.latitude = average(targetGroup.rules.map((item) => item.latitude ?? targetGroup.latitude))
    targetGroup.longitude = average(targetGroup.rules.map((item) => item.longitude ?? targetGroup.longitude))
    targetGroup.radiusKm = Math.max(targetGroup.radiusKm, rule.radiusKm ?? DEFAULT_RADIUS_KM)
  }

  return mutableGroups.map((group, index) => {
    const rulesByPriority = [...group.rules].sort(
      (left, right) =>
        toneSeverity(right.statusTone) - toneSeverity(left.statusTone) ||
        right.priority - left.priority ||
        right.lastAlertTime - left.lastAlertTime,
    )
    const locationNames = Array.from(group.locationNames)
    const primaryRule = rulesByPriority[0]
    const triggeredCount = group.rules.filter((item) => item.statusLabel === 'Triggered').length

    return {
      id: `rule-location-${index + 1}`,
      latitude: group.latitude,
      longitude: group.longitude,
      radiusKm: group.radiusKm,
      locationLabel: summarizeGroupLocation(locationNames),
      locationNames,
      statusTone: resolveGroupTone(group.rules),
      statusLabel:
        triggeredCount > 0
          ? `${triggeredCount} rule${triggeredCount === 1 ? '' : 's'} triggered`
          : `${group.rules.filter((item) => item.criteria.enabled !== false).length} rule${
              group.rules.filter((item) => item.criteria.enabled !== false).length === 1 ? '' : 's'
            } monitoring`,
      icon: primaryRule?.icon ?? '◉',
      ruleCount: group.rules.length,
      triggeredCount,
      rules: rulesByPriority.map((item) => ({
        criteriaId: item.criteria.id,
        ruleName: item.ruleName,
        triggerCondition: item.triggerCondition,
        monitoringState: item.statusLabel,
        monitoringTone: item.statusTone,
        icon: item.icon,
      })),
    }
  })
}

function summarizeGroupLocation(locationNames: string[]) {
  if (locationNames.length === 0) {
    return 'Saved watch area'
  }
  if (locationNames.length === 1) {
    return locationNames[0]
  }
  if (locationNames.length === 2) {
    return `${locationNames[0]} and ${locationNames[1]}`
  }
  return `${locationNames[0]} area +${locationNames.length - 1}`
}

function resolveGroupTone(rules: RuleViewModel[]): RuleTone {
  if (rules.some((item) => item.statusTone === 'critical')) {
    return 'critical'
  }
  if (rules.some((item) => item.statusTone === 'warning')) {
    return 'warning'
  }
  if (rules.some((item) => item.statusTone === 'calm')) {
    return 'calm'
  }
  return 'muted'
}

function groupingThresholdKm(rule: RuleViewModel, group: { radiusKm: number }) {
  const baseline = ((rule.radiusKm ?? DEFAULT_RADIUS_KM) + group.radiusKm) / 2
  return Math.max(5, Math.min(14, baseline))
}

function average(values: number[]) {
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

function toneSeverity(tone: RuleTone) {
  switch (tone) {
    case 'critical':
      return 3
    case 'warning':
      return 2
    case 'calm':
      return 1
    case 'muted':
    default:
      return 0
  }
}

function isWithinGroupingDistance(leftLat: number, leftLon: number, rightLat: number, rightLon: number, thresholdKm: number) {
  const earthRadiusKm = 6371
  const toRadians = (value: number) => (value * Math.PI) / 180
  const latitudeDelta = toRadians(rightLat - leftLat)
  const longitudeDelta = toRadians(rightLon - leftLon)
  const a =
    Math.sin(latitudeDelta / 2) ** 2 +
    Math.cos(toRadians(leftLat)) * Math.cos(toRadians(rightLat)) * Math.sin(longitudeDelta / 2) ** 2
  const distanceKm = 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return distanceKm <= thresholdKm
}
