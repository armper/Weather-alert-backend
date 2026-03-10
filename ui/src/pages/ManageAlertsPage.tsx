import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'
import { AriaSelect } from '../components/ui/AriaSelect'
import { formatRelativeTime } from '../lib/formatting'
import { useAppState } from '../state/useAppState'
import type { AlertCriteria, AlertEvent, WeatherCondition } from '../types'

type SortMode = 'attention' | 'lastTriggered' | 'location' | 'name'

const SORT_OPTIONS = [
  { id: 'attention', label: 'Attention first' },
  { id: 'lastTriggered', label: 'Latest triggered' },
  { id: 'location', label: 'Location' },
  { id: 'name', label: 'Alert name' },
] as const

export function ManageAlertsPage() {
  const { alerts, busyCriteriaId, criteria, currentWeather, handleDeleteCriteria, handleToggleCriteriaEnabled } =
    useAppState()
  const [sortMode, setSortMode] = useState<SortMode>('attention')
  const activeCount = criteria.filter((item) => item.enabled !== false).length
  const pausedCount = criteria.length - activeCount

  const latestAlertByCriteria = useMemo(() => {
    const latest = new Map<string, AlertEvent>()
    for (const alert of alerts) {
      if (!alert.criteriaId) {
        continue
      }

      const existing = latest.get(alert.criteriaId)
      if (!existing) {
        latest.set(alert.criteriaId, alert)
        continue
      }

      const existingTime = new Date(existing.alertTime ?? 0).getTime()
      const nextTime = new Date(alert.alertTime ?? 0).getTime()
      if (nextTime >= existingTime) {
        latest.set(alert.criteriaId, alert)
      }
    }
    return latest
  }, [alerts])

  const groupedCriteria = useMemo(() => {
    const enriched = criteria.map((item) => buildCriteriaViewModel(item, latestAlertByCriteria.get(item.id), currentWeather))
    return [
      {
        key: 'active',
        title: 'Active Now',
        description: 'Rules currently watching for changing conditions.',
        items: enriched
          .filter((item) => item.criteria.enabled !== false)
          .sort((left, right) => compareCriteriaViewModel(left, right, sortMode)),
      },
      {
        key: 'paused',
        title: 'Paused',
        description: 'Rules saved for later but not currently monitoring.',
        items: enriched
          .filter((item) => item.criteria.enabled === false)
          .sort((left, right) => compareCriteriaViewModel(left, right, sortMode)),
      },
    ].filter((group) => group.items.length > 0)
  }, [criteria, currentWeather, latestAlertByCriteria, sortMode])

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>My Alerts</h2>
          <span className="badge">{criteria.length} total</span>
        </div>
        <div className="manage-alerts-toolbar">
          <div className="manage-alerts-badges">
            <span className="badge">{activeCount} active</span>
            <span className="badge">{pausedCount} paused</span>
            <span className="badge">{latestAlertByCriteria.size} triggered before</span>
          </div>
          {criteria.length > 1 ? (
            <AriaSelect
              label="Sort alerts"
              buttonClassName="aria-select-trigger"
              popoverClassName="aria-select-popover"
              listBoxClassName="aria-select-listbox"
              selectedKey={sortMode}
              options={SORT_OPTIONS.map((option) => ({ ...option }))}
              onSelectionChange={(value) => setSortMode(value as SortMode)}
            />
          ) : null}
        </div>
        {criteria.length === 0 ? (
          <div className="empty-state-panel">
            <h3>No alerts yet</h3>
            <p className="muted">
              Create your first rule and it will show up here with status, recent activity, and trigger proximity.
            </p>
            <div className="button-row empty-state-actions">
              <Link to="/app/rules#create-custom-alert" className="primary overview-action-link">
                New alert
              </Link>
            </div>
          </div>
        ) : (
          <div className="stack">
            {groupedCriteria.map((group) => (
              <section key={group.key} className="criteria-section">
                <div className="criteria-section-header">
                  <div>
                    <h3>{group.title}</h3>
                    <p className="muted small">{group.description}</p>
                  </div>
                  <span className="badge">{group.items.length}</span>
                </div>
                <div className="criteria-grid">
                  {group.items.map((item) => (
                    <CriteriaCard
                      key={item.criteria.id}
                      criteria={item.criteria}
                      busy={busyCriteriaId === item.criteria.id}
                      lastTriggeredLabel={item.lastTriggeredLabel}
                      onDelete={(criteriaId) => void handleDeleteCriteria(criteriaId)}
                      onToggleEnabled={(criteriaId, enabled) => void handleToggleCriteriaEnabled(criteriaId, enabled)}
                      severityLabel={item.severityLabel}
                      severityTone={item.severityTone}
                    />
                  ))}
                </div>
              </section>
            ))}
          </div>
        )}
      </article>
    </section>
  )
}

interface CriteriaViewModel {
  criteria: AlertCriteria
  lastTriggeredLabel?: string
  lastTriggeredTime: number
  severityLabel: string
  severityPriority: number
  severityTone: 'calm' | 'critical' | 'muted' | 'warning'
}

function buildCriteriaViewModel(
  criteria: AlertCriteria,
  latestAlert: AlertEvent | undefined,
  currentWeather: WeatherCondition | null,
): CriteriaViewModel {
  const currentSignal = describeCurrentSignal(criteria, currentWeather)
  return {
    criteria,
    lastTriggeredLabel: latestAlert?.alertTime ? formatRelativeTime(latestAlert.alertTime) : undefined,
    lastTriggeredTime: latestAlert?.alertTime ? new Date(latestAlert.alertTime).getTime() : 0,
    severityLabel: currentSignal.label,
    severityPriority: currentSignal.priority,
    severityTone: currentSignal.tone,
  }
}

function compareCriteriaViewModel(left: CriteriaViewModel, right: CriteriaViewModel, sortMode: SortMode) {
  switch (sortMode) {
    case 'lastTriggered':
      return right.lastTriggeredTime - left.lastTriggeredTime || compareByName(left.criteria, right.criteria)
    case 'location':
      return compareByLocation(left.criteria, right.criteria) || compareByName(left.criteria, right.criteria)
    case 'name':
      return compareByName(left.criteria, right.criteria)
    case 'attention':
    default:
      return (
        right.severityPriority - left.severityPriority ||
        right.lastTriggeredTime - left.lastTriggeredTime ||
        compareByName(left.criteria, right.criteria)
      )
  }
}

function compareByLocation(left: AlertCriteria, right: AlertCriteria) {
  return (left.location ?? '').localeCompare(right.location ?? '', undefined, { sensitivity: 'base' })
}

function compareByName(left: AlertCriteria, right: AlertCriteria) {
  return (left.name ?? '').localeCompare(right.name ?? '', undefined, { sensitivity: 'base' })
}

function describeCurrentSignal(criteria: AlertCriteria, currentWeather: WeatherCondition | null) {
  const gap = resolveAlertGap(criteria, currentWeather)
  if (gap === null) {
    return { label: 'Monitoring', priority: 1, tone: 'muted' as const }
  }
  if (gap >= 0) {
    return { label: 'Triggering now', priority: 4, tone: 'critical' as const }
  }
  if (gap >= -resolveNearTriggerDistance(criteria)) {
    return { label: 'Close to trigger', priority: 3, tone: 'warning' as const }
  }
  return { label: 'Stable', priority: 2, tone: 'calm' as const }
}

function resolveAlertGap(criteria: AlertCriteria, currentWeather: WeatherCondition | null) {
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
    const currentRank = resolveFloodCategoryRank(
      currentWeather.riverForecastCategory ?? currentWeather.riverObservedCategory ?? undefined,
    )
    return currentRank == null || thresholdRank == null ? null : currentRank - thresholdRank
  }

  return null
}

function resolveNearTriggerDistance(criteria: AlertCriteria) {
  if (criteria.temperatureThreshold != null || criteria.dewPointThreshold != null) {
    return 5
  }
  if (criteria.maxWindSpeed != null || criteria.windGustThreshold != null) {
    return 8
  }
  if (criteria.riverStageThreshold != null) {
    return 1
  }
  if (criteria.riverFloodCategoryThreshold) {
    return 1
  }
  return 10
}

function convertTemperatureForRule(value: number | undefined, unit: 'C' | 'F') {
  if (value == null || Number.isNaN(value)) {
    return null
  }
  if (unit === 'C') {
    return value
  }
  return (value * 9) / 5 + 32
}

function resolveFloodCategoryRank(category?: string) {
  if (!category) {
    return null
  }

  switch (category.toUpperCase()) {
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
