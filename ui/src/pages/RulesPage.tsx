import { Suspense, lazy, useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { apiRequest } from '../api'
import { buildCriteriaPayload, defaultThreshold, describeCriteria } from '../lib/criteria'
import { formatFriendlyLocation } from '../lib/formatting'
import { resolveCriteriaTileEmoji, resolveRuleEmoji } from '../lib/ruleIcons'
import {
  QUICK_START_PRESETS,
  SIMPLE_SITUATIONS,
  getSensitivityForSituation,
  getSituationForRuleType,
  getSituationConfig,
  resolveMatchingSensitivityId,
  type QuickStartPreset,
  type SimpleSituationConfig,
} from '../lib/ruleBuilder'
import { resolveWeatherVisual } from '../lib/weatherVisuals'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import type { RuleType } from '../state/types'
import { useAsyncState, useDataState, useFormState, useSessionState } from '../state/useAppState'
import type { AlertCriteria } from '../types'

type ModalStatus = 'idle' | 'saving' | 'success' | 'error'

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

const LocationPickerMap = lazy(() =>
  import('../components/maps/LocationPickerMap').then((module) => ({ default: module.LocationPickerMap })),
)

function resolveCriteriaRuleType(criteria: AlertCriteria): RuleType | null {
  if (criteria.temperatureThreshold != null && criteria.temperatureDirection) {
    return criteria.temperatureDirection === 'BELOW' ? 'TEMP_BELOW' : 'TEMP_ABOVE'
  }
  if (criteria.rainThreshold != null) {
    return 'RAIN'
  }
  if (criteria.maxWindSpeed != null) {
    return 'WIND'
  }
  if (criteria.humidityThreshold != null && criteria.humidityDirection) {
    return criteria.humidityDirection === 'BELOW' ? 'HUMIDITY_BELOW' : 'HUMIDITY_ABOVE'
  }
  if (criteria.dewPointThreshold != null && criteria.dewPointDirection) {
    return criteria.dewPointDirection === 'BELOW' ? 'DEW_POINT_BELOW' : 'DEW_POINT_ABOVE'
  }
  if (criteria.windGustThreshold != null) {
    return 'WIND_GUST'
  }
  if (criteria.skyCoverThreshold != null && criteria.skyCoverDirection) {
    return criteria.skyCoverDirection === 'BELOW' ? 'SKY_COVER_BELOW' : 'SKY_COVER_ABOVE'
  }
  if (criteria.riverStageThreshold != null && criteria.riverStageDirection) {
    return criteria.riverStageDirection === 'BELOW' ? 'RIVER_STAGE_BELOW' : 'RIVER_STAGE_ABOVE'
  }
  if (criteria.riverFloodCategoryThreshold) {
    return 'RIVER_FLOOD_CATEGORY'
  }
  return null
}

function resolveCriteriaThreshold(criteria: AlertCriteria, ruleType: RuleType): string {
  switch (ruleType) {
    case 'TEMP_ABOVE':
    case 'TEMP_BELOW':
      return String(criteria.temperatureThreshold ?? '')
    case 'RAIN':
      return String(criteria.rainThreshold ?? '')
    case 'WIND':
      return String(criteria.maxWindSpeed ?? '')
    case 'HUMIDITY_ABOVE':
    case 'HUMIDITY_BELOW':
      return String(criteria.humidityThreshold ?? '')
    case 'DEW_POINT_ABOVE':
    case 'DEW_POINT_BELOW':
      return String(criteria.dewPointThreshold ?? '')
    case 'WIND_GUST':
      return String(criteria.windGustThreshold ?? '')
    case 'SKY_COVER_ABOVE':
    case 'SKY_COVER_BELOW':
      return String(criteria.skyCoverThreshold ?? '')
    case 'RIVER_STAGE_ABOVE':
    case 'RIVER_STAGE_BELOW':
      return String(criteria.riverStageThreshold ?? '')
    case 'RIVER_FLOOD_CATEGORY':
      return ''
  }
}

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
    if ((c.name ?? '').trim() !== preset.title) {
      return false
    }
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
  const { criteria, currentWeather } = useDataState()
  const { criteriaForm, setCriteriaForm } = useFormState()
  const { canSubmitCriteria } = useAsyncState()

  const rulesBackground = resolveWeatherVisual(currentWeather ?? {}).backgroundImage

  const [pendingState, setPendingState] = useState<Map<string, boolean>>(() => new Map())
  const abortControllers = useRef(new Map<string, AbortController>())
  const [modalSituation, setModalSituation] = useState<SimpleSituationConfig | null>(null)
  const [selectedSensitivity, setSelectedSensitivity] = useState<string>('')
  const [modalStatus, setModalStatus] = useState<ModalStatus>('idle')
  const [editingCriteria, setEditingCriteria] = useState<AlertCriteria | null>(null)
  const [editingEnabled, setEditingEnabled] = useState(true)
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

  const customRules = useMemo(() => {
    const presetRuleIds = new Set(enabledMap.values())
    return criteria
      .filter((item) => !presetRuleIds.has(item.id))
      .sort((left, right) => {
        const leftTime = new Date(left.createdAt ?? '').getTime()
        const rightTime = new Date(right.createdAt ?? '').getTime()
        if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) {
          return (left.name ?? '').localeCompare(right.name ?? '')
        }
        if (Number.isNaN(leftTime)) {
          return 1
        }
        if (Number.isNaN(rightTime)) {
          return -1
        }
        return rightTime - leftTime
      })
  }, [criteria, enabledMap])

  const toggle = useCallback(
    async (preset: QuickStartPreset) => {
      if (!token || !me) return

      // If already busy, abort the in-flight request and revert
      const existing = abortControllers.current.get(preset.id)
      if (existing) {
        existing.abort()
        abortControllers.current.delete(preset.id)
        setPendingState((prev) => { const next = new Map(prev); next.delete(preset.id); return next })
        return
      }

      const isCurrentlyEnabled = enabledMap.has(preset.id)
      const targetState = !isCurrentlyEnabled

      const controller = new AbortController()
      abortControllers.current.set(preset.id, controller)
      setPendingState((prev) => new Map(prev).set(preset.id, targetState))

      try {
        if (isCurrentlyEnabled) {
          await apiRequest<void>(`/api/criteria/${enabledMap.get(preset.id)}`, { method: 'DELETE', token, signal: controller.signal })
        } else {
          const payload = buildPresetPayload(preset, me.id)
          await apiRequest<AlertCriteria>('/api/criteria', { method: 'POST', token, body: payload, signal: controller.signal })
        }
        await refresh()
      } catch {
        // Silent — aborted or network error
      } finally {
        abortControllers.current.delete(preset.id)
        setPendingState((prev) => { const next = new Map(prev); next.delete(preset.id); return next })
      }
    },
    [token, me, enabledMap, refresh],
  )

  function openCategory(situation: SimpleSituationConfig) {
    if (!situation.ruleType) return
    const defaultSens = situation.sensitivityOptions[1]?.id ?? situation.sensitivityOptions[0]?.id ?? ''
    setSelectedSensitivity(defaultSens)
    setEditingCriteria(null)
    setEditingEnabled(true)
    const sens = situation.sensitivityOptions.find((s) => s.id === defaultSens)
    setCriteriaForm((f) => ({
      ...f,
      ruleType: situation.ruleType as RuleType,
      threshold: sens?.threshold ?? situation.defaultThreshold ?? defaultThreshold(situation.ruleType as RuleType),
      riverFloodCategoryThreshold: (sens?.floodCategory ?? situation.defaultFloodCategory ?? 'ACTION') as 'ACTION' | 'MINOR' | 'MODERATE' | 'MAJOR',
      name: '',
    }))
    setModalStatus('idle')
    setModalSituation(situation)
  }

  function openCustomRuleEditor(criteria: AlertCriteria) {
    const ruleType = resolveCriteriaRuleType(criteria)
    if (!ruleType) return
    const situationId = getSituationForRuleType(ruleType)
    const situation = getSituationConfig(situationId)
    if (!situation?.ruleType) return

    const threshold = resolveCriteriaThreshold(criteria, ruleType)
    const floodCategory = criteria.riverFloodCategoryThreshold ?? situation.defaultFloodCategory ?? 'ACTION'

    setEditingCriteria(criteria)
    setEditingEnabled(criteria.enabled !== false)
    setSelectedSensitivity(resolveMatchingSensitivityId(situationId, threshold, floodCategory))
    setCriteriaForm((f) => ({
      ...f,
      name: criteria.name ?? '',
      location: criteria.location ?? 'Orlando',
      latitude: String(criteria.latitude ?? DEFAULT_LAT),
      longitude: String(criteria.longitude ?? DEFAULT_LON),
      threshold,
      ruleType,
      temperatureUnit: criteria.temperatureUnit ?? situation.defaultTemperatureUnit ?? 'F',
      riverGaugeId: criteria.riverGaugeId ?? '',
      riverFloodCategoryThreshold: floodCategory,
      gaugeSearchRadiusKm: String(criteria.radiusKm ?? f.gaugeSearchRadiusKm ?? '80'),
      monitorCurrent: criteria.monitorCurrent ?? situation.defaultMonitorCurrent ?? true,
      monitorForecast: criteria.monitorForecast ?? situation.defaultMonitorForecast ?? true,
      forecastWindowHours: String(criteria.forecastWindowHours ?? situation.defaultForecastWindowHours ?? '24'),
      oncePerEvent: criteria.oncePerEvent ?? situation.defaultOncePerEvent ?? true,
      rearmWindowMinutes: String(criteria.rearmWindowMinutes ?? '240'),
    }))
    setModalStatus('idle')
    setModalSituation(situation)
  }

  function closeModal() {
    setModalSituation(null)
    setModalStatus('idle')
    setEditingCriteria(null)
    setEditingEnabled(true)
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
  const modalLatitude = Number.isFinite(Number(criteriaForm.latitude)) ? Number(criteriaForm.latitude) : Number(DEFAULT_LAT)
  const modalLongitude = Number.isFinite(Number(criteriaForm.longitude)) ? Number(criteriaForm.longitude) : Number(DEFAULT_LON)

  return (
    <section className="page-stack rules-page-fresh">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={rulesBackground} alt="" />
      </div>

      <div className="rules-page-content">
        <div className="rules-tile-grid">
          {QUICK_START_PRESETS.map((preset) => {
            const isEnabled = pendingState.has(preset.id) ? pendingState.get(preset.id)! : enabledMap.has(preset.id)
            return (
              <button
                key={preset.id}
                className={`rules-tile${isEnabled ? ' is-enabled' : ''}`}
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

        {customRules.length > 0 ? (
          <section className="rules-custom-saved-section" aria-label="Custom rules">
            <div className="rules-custom-divider rules-custom-saved-divider">
              <span>custom rules</span>
            </div>
            <div className="rules-custom-saved-grid">
              {customRules.map((item) => (
                <button
                  key={item.id}
                  className={`rules-tile rules-custom-rule-tile${item.enabled === false ? ' is-muted' : ' is-enabled'}`}
                  type="button"
                  onClick={() => openCustomRuleEditor(item)}
                  aria-label={`Edit ${item.name?.trim() || 'custom alert'}`}
                >
                  <div className="rules-custom-rule-top">
                    <span className="rules-tile-icon">{resolveCriteriaTileEmoji(item)}</span>
                  </div>
                  <span className="rules-tile-name">{item.name?.trim() || 'Custom alert'}</span>
                  <span className="rules-custom-rule-location">{formatFriendlyLocation(item.location)}</span>
                  <span className="rules-tile-desc rules-custom-rule-condition">{describeCriteria(item)}</span>
                </button>
              ))}
            </div>
          </section>
        ) : null}

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
          <div className={`rules-modal rules-modal--builder${modalStatus === 'success' ? ' is-success' : ''}${modalStatus === 'error' ? ' is-error' : ''}`} role="dialog" aria-label={`${editingCriteria ? 'Edit' : 'Create'} ${modalSituation.title} alert`}>
            <div className="rules-modal-header">
              <span className="rules-modal-icon">{resolveRuleEmoji(modalSituation.icon)}</span>
              <h2 className="rules-modal-title">{editingCriteria ? `Edit ${modalSituation.title}` : modalSituation.title}</h2>
              <button type="button" className="rules-modal-close" aria-label="Close" onClick={closeModal}>✕</button>
            </div>

            <p className="rules-modal-desc">{editingCriteria ? 'Update your saved custom rule.' : modalSituation.description}</p>

            {editingCriteria ? (
              <div className="rules-status-row" aria-label="Rule status">
                <button
                  type="button"
                  className={`rules-chip${editingEnabled ? ' is-active' : ''}`}
                  onClick={() => setEditingEnabled(true)}
                >
                  active
                </button>
                <button
                  type="button"
                  className={`rules-chip${!editingEnabled ? ' is-active rules-chip-muted' : ' rules-chip-muted'}`}
                  onClick={() => setEditingEnabled(false)}
                >
                  paused
                </button>
              </div>
            ) : null}

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
              onSubmit={async (e: FormEvent<HTMLFormElement>) => {
                e.preventDefault()
                if (!token || !me) return
                setModalStatus('saving')
                try {
                  const payload = buildCriteriaPayload(criteriaForm, me.id)
                  if (editingCriteria) {
                    await apiRequest<AlertCriteria>(`/api/criteria/${editingCriteria.id}`, {
                      method: 'PUT',
                      token,
                      body: {
                        ...payload,
                        enabled: editingEnabled,
                      },
                    })
                  } else {
                    await apiRequest<AlertCriteria>('/api/criteria', { method: 'POST', token, body: payload })
                  }
                  setModalStatus('success')
                  await refresh()
                  setTimeout(() => closeModal(), 900)
                } catch {
                  setModalStatus('error')
                }
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

              <div className="rules-form-map">
                <Suspense fallback={<div className="rules-form-map-loading" />}>
                  <LocationPickerMap
                    location={criteriaForm.location}
                    latitude={modalLatitude}
                    longitude={modalLongitude}
                    onSelect={(selection) =>
                      setCriteriaForm((f) => ({
                        ...f,
                        location: selection.location,
                        latitude: String(selection.latitude),
                        longitude: String(selection.longitude),
                      }))
                    }
                  />
                </Suspense>
              </div>

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
                disabled={!canSubmitCriteria || modalStatus === 'saving' || modalStatus === 'success'}
              >
                {modalStatus === 'saving'
                  ? (editingCriteria ? 'Saving...' : 'Creating...')
                  : modalStatus === 'success'
                    ? (editingCriteria ? 'Saved!' : 'Created!')
                    : modalStatus === 'error'
                      ? 'Try Again'
                      : (editingCriteria ? 'Save changes' : 'Create Alert')}
              </button>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  )
}
