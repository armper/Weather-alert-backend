import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { apiRequest } from '../api'
import backgroundOverviewImage from '../assets/background-overview.png'
import { defaultThreshold } from '../lib/criteria'
import {
  QUICK_START_PRESETS,
  SIMPLE_SITUATIONS,
  getSensitivityForSituation,
  getSituationConfig,
  type QuickStartPreset,
  type RuleBuilderIcon,
  type SimpleSituationConfig,
} from '../lib/ruleBuilder'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import type { RuleType } from '../state/types'
import { useActionState, useAsyncState, useDataState, useFormState, useSessionState } from '../state/useAppState'
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
  'river-problem': 'Flood watch level reached',
  'minor-flooding': 'Minor flooding detected',
}

const FLOOD_CATEGORY_LABELS: Record<string, string> = {
  ACTION: 'Watch level',
  MINOR: 'Minor flooding',
  MODERATE: 'Moderate flooding',
  MAJOR: 'Major flooding',
}

const CUSTOM_CATEGORIES = SIMPLE_SITUATIONS.filter((s) => s.id !== 'CUSTOM')

function thresholdUnit(ruleType: RuleType): string {
  switch (ruleType) {
    case 'TEMP_ABOVE':
    case 'TEMP_BELOW':
    case 'DEW_POINT_ABOVE':
    case 'DEW_POINT_BELOW':
      return '°F'
    case 'RAIN':
    case 'HUMIDITY_ABOVE':
    case 'HUMIDITY_BELOW':
    case 'SKY_COVER_ABOVE':
    case 'SKY_COVER_BELOW':
      return '%'
    case 'WIND':
    case 'WIND_GUST':
      return 'km/h'
    case 'RIVER_STAGE_ABOVE':
    case 'RIVER_STAGE_BELOW':
      return 'ft'
    default:
      return ''
  }
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
  const { criteriaForm, setCriteriaForm } = useFormState()
  const { handleCreateCriteria } = useActionState()
  const { canSubmitCriteria, savingCriteria } = useAsyncState()
  const [busy, setBusy] = useState<Set<string>>(() => new Set())
  const [optimistic, setOptimistic] = useState<Set<string>>(() => new Set())
  const abortControllers = useRef(new Map<string, AbortController>())
  const [modalSituation, setModalSituation] = useState<SimpleSituationConfig | null>(null)
  const [selectedSensitivity, setSelectedSensitivity] = useState<string>('')
  const backdropRef = useRef<HTMLDivElement>(null)

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

  function openCategory(situation: SimpleSituationConfig) {
    if (!situation.ruleType) return
    const defaultSens = situation.sensitivityOptions[1]?.id ?? situation.sensitivityOptions[0]?.id ?? ''
    setSelectedSensitivity(defaultSens)
    const sens = situation.sensitivityOptions.find((s) => s.id === defaultSens)
    setCriteriaForm((f) => ({
      ...f,
      ruleType: situation.ruleType as RuleType,
      threshold: sens?.threshold ?? situation.defaultThreshold ?? defaultThreshold(situation.ruleType as RuleType),
      riverFloodCategoryThreshold: (sens?.floodCategory ?? situation.defaultFloodCategory ?? 'ACTION') as 'ACTION' | 'MINOR' | 'MODERATE' | 'MAJOR',
      name: '',
    }))
    setModalSituation(situation)
  }

  function closeModal() {
    setModalSituation(null)
  }

  function handleSensitivityPick(sensId: string) {
    if (!modalSituation) return
    setSelectedSensitivity(sensId)
    const sens = modalSituation.sensitivityOptions.find((s) => s.id === sensId)
    if (sens?.custom) return // keep current threshold for custom
    if (sens?.floodCategory) {
      setCriteriaForm((f) => ({
        ...f,
        riverFloodCategoryThreshold: sens.floodCategory as 'ACTION' | 'MINOR' | 'MODERATE' | 'MAJOR',
      }))
    } else if (sens?.threshold) {
      setCriteriaForm((f) => ({ ...f, threshold: sens.threshold! }))
    }
  }

  // Close modal on Escape
  useEffect(() => {
    if (!modalSituation) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') closeModal()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [modalSituation])

  const isCustomSensitivity = modalSituation?.sensitivityOptions.find((s) => s.id === selectedSensitivity)?.custom ?? false

  return (
    <section className="page-stack rules-page-fresh">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={backgroundOverviewImage} alt="" />
      </div>

      <div className="rules-page-content">
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

        {/* ─── Custom Alert Builder ─── */}
        <div className="rules-custom-divider">
          <span>or build your own</span>
        </div>

        <div className="rules-tile-grid rules-category-grid">
          {CUSTOM_CATEGORIES.map((situation) => (
            <button
              key={situation.id}
              className="rules-tile rules-category-tile"
              type="button"
              onClick={() => openCategory(situation)}
            >
              <span className="rules-tile-icon">{resolveRuleEmoji(situation.icon)}</span>
              <span className="rules-tile-name">{situation.title}</span>
            </button>
          ))}
        </div>
      </div>

      {/* ─── Modal ─── */}
      {modalSituation ? (
        <div
          className="rules-modal-backdrop"
          ref={backdropRef}
          onClick={(e) => { if (e.target === backdropRef.current) closeModal() }}
        >
          <div className="rules-modal" role="dialog" aria-label={`Create ${modalSituation.title} alert`}>
            <div className="rules-modal-header">
              <span className="rules-modal-icon">{resolveRuleEmoji(modalSituation.icon)}</span>
              <h2 className="rules-modal-title">{modalSituation.title}</h2>
              <button type="button" className="rules-modal-close" aria-label="Close" onClick={closeModal}>✕</button>
            </div>

            <p className="rules-modal-desc">{modalSituation.description}</p>

            {modalSituation.sensitivityOptions.length > 0 ? (
              <div className="rules-sensitivity-chips">
                {modalSituation.sensitivityOptions.map((sens) => (
                  <button
                    key={sens.id}
                    type="button"
                    className={`rules-chip${selectedSensitivity === sens.id ? ' is-active' : ''}`}
                    onClick={() => handleSensitivityPick(sens.id)}
                  >
                    {sens.label}
                  </button>
                ))}
              </div>
            ) : null}

            <form
              className="rules-modal-form"
              onSubmit={async (e) => {
                const result = await handleCreateCriteria(e)
                if (result && 'ok' in result && result.ok) closeModal()
              }}
            >
              {modalSituation.ruleType === 'RIVER_FLOOD_CATEGORY' && !isCustomSensitivity ? null : modalSituation.ruleType === 'RIVER_FLOOD_CATEGORY' ? (
                <select
                  className="auth-login-input rules-custom-select"
                  aria-label="Flood level"
                  value={criteriaForm.riverFloodCategoryThreshold}
                  onChange={(e) =>
                    setCriteriaForm((f) => ({
                      ...f,
                      riverFloodCategoryThreshold: e.target.value as 'ACTION' | 'MINOR' | 'MODERATE' | 'MAJOR',
                    }))
                  }
                >
                  {Object.entries(FLOOD_CATEGORY_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              ) : isCustomSensitivity ? (
                <div className="rules-custom-threshold-row">
                  <input
                    className="auth-login-input"
                    type="number"
                    inputMode="decimal"
                    aria-label="Threshold"
                    placeholder="Threshold"
                    value={criteriaForm.threshold}
                    onChange={(e) => setCriteriaForm((f) => ({ ...f, threshold: e.target.value }))}
                  />
                  <span className="rules-custom-unit">{thresholdUnit(modalSituation.ruleType as RuleType)}</span>
                </div>
              ) : null}

              <input
                className="auth-login-input"
                type="text"
                aria-label="Location name"
                placeholder="Location (e.g. Orlando)"
                value={criteriaForm.location}
                onChange={(e) => setCriteriaForm((f) => ({ ...f, location: e.target.value }))}
              />

              <input
                className="auth-login-input"
                type="text"
                aria-label="Alert name"
                placeholder="Alert name (optional)"
                value={criteriaForm.name}
                onChange={(e) => setCriteriaForm((f) => ({ ...f, name: e.target.value }))}
              />

              <button
                type="submit"
                className="action-bubble action-bubble-wide action-bubble-accent"
                disabled={!canSubmitCriteria || savingCriteria}
              >
                {savingCriteria ? 'Creating...' : 'Create Alert'}
              </button>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  )
}
