import { useCallback, useMemo, useRef, useState } from 'react'
import { apiRequest } from '../api'
import backgroundOverviewImage from '../assets/background-overview.png'
import {
  QUICK_START_PRESETS,
  getSensitivityForSituation,
  getSituationConfig,
  type QuickStartPreset,
  type RuleBuilderIcon,
} from '../lib/ruleBuilder'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useDataState, useSessionState } from '../state/useAppState'
import type { AlertCriteria } from '../types'

function resolveRuleEmoji(icon: RuleBuilderIcon): string {
  switch (icon) {
    case 'heat':
      return '🔥'
    case 'jacket':
      return '🧥'
    case 'rain':
      return '🌧️'
    case 'wind':
      return '💨'
    case 'humidity':
      return '💧'
    case 'dew':
      return '🌙'
    case 'river':
      return '🏞️'
    case 'flood':
      return '🌊'
    case 'alert':
      return '🚨'
    case 'sky':
      return '☀️'
    default:
      return '✨'
  }
}

const TILE_SUBTITLES: Record<string, string> = {
  'chilly-weather': 'Temperature below 60°F',
  'hot-day-ahead': 'Temperature above 90°F',
  'rain-coming': 'Rain chance above 75%',
  'windy-outside': 'Wind speed above 30 km/h',
  'very-humid': 'Humidity above 80%',
  'warm-muggy-night': 'Dew point above 68°F',
  'river-rising': 'River stage above 8 ft',
  'river-problem': 'Flood category: Action',
  'minor-flooding': 'Flood category: Minor',
}

function findMatchingCriteria(preset: QuickStartPreset, criteria: AlertCriteria[]): AlertCriteria | undefined {
  const situation = getSituationConfig(preset.situationId)
  if (!situation?.ruleType) return undefined
  const sensitivity = getSensitivityForSituation(preset.situationId, preset.sensitivityId)

  return criteria.find((c) => {
    switch (situation.ruleType) {
      case 'TEMP_BELOW':
        return c.temperatureDirection === 'BELOW' && c.temperatureThreshold === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'TEMP_ABOVE':
        return c.temperatureDirection === 'ABOVE' && c.temperatureThreshold === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'RAIN':
        return c.rainThreshold === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'WIND':
        return c.maxWindSpeed === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'HUMIDITY_ABOVE':
        return c.humidityDirection === 'ABOVE' && c.humidityThreshold === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'DEW_POINT_ABOVE':
        return c.dewPointDirection === 'ABOVE' && c.dewPointThreshold === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'RIVER_STAGE_ABOVE':
        return c.riverStageDirection === 'ABOVE' && c.riverStageThreshold === Number(sensitivity?.threshold ?? situation.defaultThreshold)
      case 'RIVER_FLOOD_CATEGORY':
        return c.riverFloodCategoryThreshold === sensitivity?.floodCategory
      default:
        return false
    }
  })
}

function buildPresetPayload(preset: QuickStartPreset, userId: string): Record<string, unknown> {
  const situation = getSituationConfig(preset.situationId)
  if (!situation?.ruleType) return {}
  const sensitivity = getSensitivityForSituation(preset.situationId, preset.sensitivityId)
  const threshold = Number(sensitivity?.threshold ?? situation.defaultThreshold ?? '0')

  const payload: Record<string, unknown> = {
    userId,
    name: preset.title,
    location: 'Orlando',
    latitude: Number(DEFAULT_LAT),
    longitude: Number(DEFAULT_LON),
    radiusKm: 80,
    monitorCurrent: situation.defaultMonitorCurrent ?? true,
    monitorForecast: situation.defaultMonitorForecast ?? true,
    forecastWindowHours: Number(preset.forecastWindowHours ?? situation.defaultForecastWindowHours ?? '24'),
    temperatureUnit: situation.defaultTemperatureUnit ?? 'F',
    oncePerEvent: situation.defaultOncePerEvent ?? true,
    rearmWindowMinutes: Number(preset.rearmWindowMinutes ?? '240'),
  }

  switch (situation.ruleType) {
    case 'TEMP_BELOW':
      payload.temperatureThreshold = threshold
      payload.temperatureDirection = 'BELOW'
      break
    case 'TEMP_ABOVE':
      payload.temperatureThreshold = threshold
      payload.temperatureDirection = 'ABOVE'
      break
    case 'RAIN':
      payload.rainThreshold = threshold
      payload.rainThresholdType = 'PROBABILITY'
      break
    case 'WIND':
      payload.maxWindSpeed = threshold
      break
    case 'HUMIDITY_ABOVE':
      payload.humidityThreshold = threshold
      payload.humidityDirection = 'ABOVE'
      break
    case 'DEW_POINT_ABOVE':
      payload.dewPointThreshold = threshold
      payload.dewPointDirection = 'ABOVE'
      break
    case 'RIVER_STAGE_ABOVE':
      payload.riverStageThreshold = threshold
      payload.riverStageDirection = 'ABOVE'
      break
    case 'RIVER_FLOOD_CATEGORY':
      payload.riverFloodCategoryThreshold = sensitivity?.floodCategory ?? 'ACTION'
      break
  }

  return payload
}

export function RulesPage() {
  const { token, me, refresh } = useSessionState()
  const { criteria } = useDataState()
  const [busy, setBusy] = useState<Set<string>>(() => new Set())
  const [optimistic, setOptimistic] = useState<Set<string>>(() => new Set())
  const abortControllers = useRef(new Map<string, AbortController>())

  const enabledMap = useMemo(() => {
    const map = new Map<string, string>()
    for (const preset of QUICK_START_PRESETS) {
      const match = findMatchingCriteria(preset, criteria)
      if (match) {
        map.set(preset.id, match.id)
      }
    }
    return map
  }, [criteria])

  const toggle = useCallback(
    async (preset: QuickStartPreset) => {
      if (!token || !me) return

      // If already busy, abort the in-flight request and revert
      const existing = abortControllers.current.get(preset.id)
      if (existing) {
        existing.abort()
        abortControllers.current.delete(preset.id)
        setBusy((prev) => { const next = new Set(prev); next.delete(preset.id); return next })
        setOptimistic((prev) => { const next = new Set(prev); next.delete(preset.id); return next })
        return
      }

      const controller = new AbortController()
      abortControllers.current.set(preset.id, controller)
      setBusy((prev) => new Set(prev).add(preset.id))
      setOptimistic((prev) => new Set(prev).add(preset.id))

      try {
        const existingId = enabledMap.get(preset.id)
        if (existingId) {
          await apiRequest<void>(`/api/criteria/${existingId}`, { method: 'DELETE', token, signal: controller.signal })
        } else {
          const payload = buildPresetPayload(preset, me.id)
          await apiRequest<AlertCriteria>('/api/criteria', { method: 'POST', token, body: payload, signal: controller.signal })
        }
        // Refresh criteria list from server
        await refresh()
      } catch {
        // Silent — aborted or network error
      } finally {
        abortControllers.current.delete(preset.id)
        setBusy((prev) => { const next = new Set(prev); next.delete(preset.id); return next })
        setOptimistic((prev) => { const next = new Set(prev); next.delete(preset.id); return next })
      }
    },
    [token, me, enabledMap, refresh],
  )

  return (
    <section className="page-stack rules-page-fresh">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={backgroundOverviewImage} alt="" />
      </div>

      <div className="rules-page-content">
        <p className="rules-page-subtitle">Tap a tile to enable an alert</p>

        <div className="rules-tile-grid">
          {QUICK_START_PRESETS.map((preset) => {
            const actualEnabled = enabledMap.has(preset.id)
            const isEnabled = optimistic.has(preset.id) ? !actualEnabled : actualEnabled
            const isBusy = busy.has(preset.id)
            return (
              <button
                key={preset.id}
                className={`rules-tile${isEnabled ? ' is-enabled' : ''}${isBusy ? ' is-busy' : ''}`}
                type="button"
                aria-pressed={isEnabled}
                onClick={() => toggle(preset)}
              >
                <span className="rules-tile-icon">{resolveRuleEmoji(preset.icon)}</span>
                <span className="rules-tile-name">{preset.title}</span>
                <span className="rules-tile-desc">
                  {TILE_SUBTITLES[preset.id] ?? preset.description}
                </span>
              </button>
            )
          })}
        </div>
      </div>
    </section>
  )
}
