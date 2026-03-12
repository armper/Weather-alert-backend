import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Disclosure, DisclosurePanel } from 'react-aria-components'
import { useLocation } from 'react-router-dom'
import { apiRequest, toErrorMessage } from '../api'
import { buildAlertConsoleSummary } from '../lib/alertConsole'
import { RIVER_RULE_TYPES, RIVER_STAGE_RULE_TYPES, buildSuggestedAlertName, defaultThreshold } from '../lib/criteria'
import { formatFriendlyLocation, formatNumber, formatTemperature, formatWind } from '../lib/formatting'
import {
  QUICK_START_PRESETS,
  SIMPLE_SITUATIONS,
  filterAdvancedRuleGroups,
  getAdvancedRuleLabel,
  getSensitivityForSituation,
  getSituationConfig,
  getSituationForRuleType,
  resolveMatchingSensitivityId,
  type BuilderMode,
  type QuickStartPreset,
  type RuleBuilderIcon,
  type SimpleSituationConfig,
  type SimpleSituationId,
} from '../lib/ruleBuilder'
import { LocationPickerMap } from '../components/maps/LocationPickerMap'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { useAppState } from '../state/useAppState'
import { DEFAULT_LAT, DEFAULT_LON, initialCriteriaForm, type RuleType } from '../state/types'
import type { FloodCategory, WeatherCondition } from '../types'

interface RuleFormErrors {
  location?: string
  threshold?: string
  riverGaugeId?: string
  forecastWindowHours?: string
  rearmWindowMinutes?: string
  gaugeSearchRadiusKm?: string
  latitude?: string
  longitude?: string
}

type LocationMode = 'CITY' | 'MANUAL'

const TEMP_UNIT_OPTIONS = [
  { id: 'F', label: 'Fahrenheit' },
  { id: 'C', label: 'Celsius' },
]

const FLOOD_CATEGORY_OPTIONS = [
  { id: 'ACTION', label: 'Action stage' },
  { id: 'MINOR', label: 'Minor flood' },
  { id: 'MODERATE', label: 'Moderate flood' },
  { id: 'MAJOR', label: 'Major flood' },
]

const LOCATION_MODE_OPTIONS = [
  { id: 'CITY', label: 'City or place' },
  { id: 'MANUAL', label: 'Manual coordinates' },
]

export function RulesPage() {
  const {
    token,
    setNotice,
    alerts,
    criteria,
    criteriaForm,
    currentWeather,
    setCriteriaForm,
    canSubmitCriteria,
    savingCriteria,
    handleCreateCriteria,
  } = useAppState()
  const location = useLocation()

  const [formErrors, setFormErrors] = useState<RuleFormErrors>({})
  const [advancedExpanded, setAdvancedExpanded] = useState(false)
  const [locationMode, setLocationMode] = useState<LocationMode>('CITY')
  const [useCustomCoordinates, setUseCustomCoordinates] = useState(false)
  const [flashForm, setFlashForm] = useState(false)
  const [builderMode, setBuilderMode] = useState<BuilderMode>('SIMPLE')
  const [selectedPresetId, setSelectedPresetId] = useState<string | null>(null)
  const [selectedSituationId, setSelectedSituationId] = useState<SimpleSituationId>(() =>
    getSituationForRuleType(criteriaForm.ruleType),
  )
  const [selectedSensitivityId, setSelectedSensitivityId] = useState<string>(() =>
    resolveMatchingSensitivityId(
      getSituationForRuleType(criteriaForm.ruleType),
      criteriaForm.threshold,
      criteriaForm.riverFloodCategoryThreshold,
    ),
  )
  const [advancedSearchQuery, setAdvancedSearchQuery] = useState('')
  const [autoNameEnabled, setAutoNameEnabled] = useState(
    () => !criteriaForm.name.trim() || criteriaForm.name === initialCriteriaForm.name,
  )
  const [didSeedPrimaryLocation, setDidSeedPrimaryLocation] = useState(false)
  const [resolvingRiverGauge, setResolvingRiverGauge] = useState(false)
  const [resolvedRiverGauge, setResolvedRiverGauge] = useState<WeatherCondition | null>(null)

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'
  const isDewPointRule = criteriaForm.ruleType === 'DEW_POINT_ABOVE' || criteriaForm.ruleType === 'DEW_POINT_BELOW'
  const isTemperatureScaleRule = isTemperatureRule || isDewPointRule
  const isRiverRule = RIVER_RULE_TYPES.includes(criteriaForm.ruleType)
  const isRiverStageRule = RIVER_STAGE_RULE_TYPES.includes(criteriaForm.ruleType)
  const isRiverCategoryRule = criteriaForm.ruleType === 'RIVER_FLOOD_CATEGORY'
  const shouldShowCoordinateToggle = locationMode === 'MANUAL'
  const shouldUseCustomCoordinates = shouldShowCoordinateToggle && useCustomCoordinates
  const mapLatitude = Number(criteriaForm.latitude)
  const mapLongitude = Number(criteriaForm.longitude)
  const resolvedLatitude = Number.isNaN(mapLatitude) ? Number(DEFAULT_LAT) : mapLatitude
  const resolvedLongitude = Number.isNaN(mapLongitude) ? Number(DEFAULT_LON) : mapLongitude

  const primaryArea = useMemo(() => {
    const savedArea = criteria[0]
    if (savedArea?.location?.trim()) {
      return {
        location: savedArea.location.trim(),
        latitude: String(savedArea.latitude ?? criteriaForm.latitude ?? DEFAULT_LAT),
        longitude: String(savedArea.longitude ?? criteriaForm.longitude ?? DEFAULT_LON),
      }
    }

    if (currentWeather?.location?.trim()) {
      return {
        location: currentWeather.location.trim(),
        latitude: criteriaForm.latitude || DEFAULT_LAT,
        longitude: criteriaForm.longitude || DEFAULT_LON,
      }
    }

    return null
  }, [criteria, criteriaForm.latitude, criteriaForm.longitude, currentWeather])

  const selectedPreset = useMemo(
    () => QUICK_START_PRESETS.find((preset) => preset.id === selectedPresetId) ?? null,
    [selectedPresetId],
  )

  const selectedSituation = useMemo(
    () => getSituationConfig(selectedSituationId),
    [selectedSituationId],
  )

  const selectedSensitivity = useMemo(
    () => getSensitivityForSituation(selectedSituationId, selectedSensitivityId),
    [selectedSituationId, selectedSensitivityId],
  )

  const filteredAdvancedGroups = useMemo(
    () => filterAdvancedRuleGroups(advancedSearchQuery),
    [advancedSearchQuery],
  )

  const alertConsoleSummary = useMemo(
    () => buildAlertConsoleSummary(criteria, alerts, currentWeather),
    [criteria, alerts, currentWeather],
  )

  const suggestedName = useMemo(
    () => buildSuggestedAlertName(criteriaForm.ruleType, criteriaForm.location, criteriaForm.riverFloodCategoryThreshold),
    [criteriaForm.location, criteriaForm.riverFloodCategoryThreshold, criteriaForm.ruleType],
  )

  const resolvedAlertName = criteriaForm.name.trim() || suggestedName
  const previewLocation = formatFriendlyLocation(criteriaForm.location || primaryArea?.location)
  const previewChecks = describeMonitoringMode(criteriaForm.monitorCurrent, criteriaForm.monitorForecast)
  const previewCurrentSnapshot = buildCurrentSnapshotCopy(currentWeather)
  const previewChips = buildPreviewChips(currentWeather, criteriaForm.temperatureUnit)
  const previewAlertSentence = buildPreviewAlertSentence(criteriaForm)
  const thresholdHelp = buildThresholdHelp(criteriaForm)
  const advancedConditionLabel = getAdvancedRuleLabel(criteriaForm.ruleType)
  const showSuggestedNameAction = criteriaForm.name.trim() !== suggestedName
  const isSimpleCustomSensitivity = selectedSensitivity?.custom === true

  useEffect(() => {
    const targetId = location.hash.replace('#', '').trim()
    if (!targetId) {
      return
    }

    const target = document.getElementById(targetId)
    if (!target) {
      return
    }

    window.requestAnimationFrame(() => {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
      if (target instanceof HTMLElement) {
        target.focus()
      }
    })
  }, [location.hash])

  useEffect(() => {
    if (!flashForm) {
      return
    }
    const id = window.setTimeout(() => setFlashForm(false), 900)
    return () => window.clearTimeout(id)
  }, [flashForm])

  useEffect(() => {
    if (!isRiverRule) {
      setResolvedRiverGauge(null)
    }
  }, [isRiverRule])

  useEffect(() => {
    if (!resolvedRiverGauge) {
      return
    }
    setResolvedRiverGauge(null)
  }, [criteriaForm.location, criteriaForm.latitude, criteriaForm.longitude, resolvedRiverGauge])

  useEffect(() => {
    if (!resolvedRiverGauge?.riverGaugeId) {
      return
    }
    if (criteriaForm.riverGaugeId.trim().toUpperCase() === resolvedRiverGauge.riverGaugeId.toUpperCase()) {
      return
    }
    setResolvedRiverGauge(null)
  }, [criteriaForm.riverGaugeId, resolvedRiverGauge])

  useEffect(() => {
    if (didSeedPrimaryLocation || !primaryArea) {
      return
    }

    setCriteriaForm((state) => {
      const shouldSeedLocation = !state.location.trim() || state.location === initialCriteriaForm.location
      const shouldSeedLatitude = !state.latitude.trim() || state.latitude === initialCriteriaForm.latitude
      const shouldSeedLongitude = !state.longitude.trim() || state.longitude === initialCriteriaForm.longitude

      if (!shouldSeedLocation && !shouldSeedLatitude && !shouldSeedLongitude) {
        return state
      }

      return {
        ...state,
        location: shouldSeedLocation ? primaryArea.location : state.location,
        latitude: shouldSeedLatitude ? primaryArea.latitude : state.latitude,
        longitude: shouldSeedLongitude ? primaryArea.longitude : state.longitude,
      }
    })
    setDidSeedPrimaryLocation(true)
  }, [didSeedPrimaryLocation, primaryArea, setCriteriaForm])

  useEffect(() => {
    if (!autoNameEnabled || !suggestedName) {
      return
    }

    setCriteriaForm((state) => (state.name === suggestedName ? state : { ...state, name: suggestedName }))
  }, [autoNameEnabled, setCriteriaForm, suggestedName])

  function handleBuilderModeChange(nextMode: BuilderMode) {
    setBuilderMode(nextMode)

    if (nextMode === 'SIMPLE') {
      const nextSituationId = getSituationForRuleType(criteriaForm.ruleType)
      setSelectedSituationId(nextSituationId)
      setSelectedSensitivityId(
        resolveMatchingSensitivityId(nextSituationId, criteriaForm.threshold, criteriaForm.riverFloodCategoryThreshold),
      )
      return
    }

    setSelectedSituationId(getSituationForRuleType(criteriaForm.ruleType))
  }

  function handleSituationSelect(situationId: SimpleSituationId) {
    if (situationId === 'CUSTOM') {
      setSelectedSituationId('CUSTOM')
      handleBuilderModeChange('ADVANCED')
      return
    }

    const situation = getSituationConfig(situationId)
    if (!situation || !situation.ruleType) {
      return
    }

    const fallbackSensitivity = situation.sensitivityOptions[0]
    setBuilderMode('SIMPLE')
    setSelectedSituationId(situationId)
    setSelectedSensitivityId(fallbackSensitivity?.id ?? 'custom')
    setSelectedPresetId(null)
    setAutoNameEnabled(true)
    applySituationConfig(situation, fallbackSensitivity?.id ?? 'custom')
  }

  function handleSensitivitySelect(sensitivityId: string) {
    if (!selectedSituation || selectedSituation.id === 'CUSTOM') {
      return
    }

    setSelectedSensitivityId(sensitivityId)
    setSelectedPresetId(null)
    setAutoNameEnabled(true)
    applySituationConfig(selectedSituation, sensitivityId)
  }

  function applySituationConfig(situation: SimpleSituationConfig, sensitivityId: string) {
    const sensitivity = getSensitivityForSituation(situation.id, sensitivityId)
    setCriteriaForm((state) => ({
      ...state,
      name: '',
      ruleType: situation.ruleType ?? state.ruleType,
      threshold:
        sensitivity?.custom === true
          ? state.threshold || situation.defaultThreshold || defaultThreshold(situation.ruleType ?? state.ruleType)
          : sensitivity?.threshold ?? situation.defaultThreshold ?? state.threshold,
      temperatureUnit: situation.defaultTemperatureUnit ?? state.temperatureUnit,
      riverFloodCategoryThreshold:
        sensitivity?.custom === true
          ? state.riverFloodCategoryThreshold
          : sensitivity?.floodCategory ?? situation.defaultFloodCategory ?? state.riverFloodCategoryThreshold,
      monitorCurrent: situation.defaultMonitorCurrent ?? state.monitorCurrent,
      monitorForecast: situation.defaultMonitorForecast ?? state.monitorForecast,
      oncePerEvent: situation.defaultOncePerEvent ?? state.oncePerEvent,
      forecastWindowHours: situation.defaultForecastWindowHours ?? state.forecastWindowHours,
    }))
    setFormErrors((state) => ({
      ...state,
      threshold: undefined,
      riverGaugeId: undefined,
      gaugeSearchRadiusKm: undefined,
    }))
    setAdvancedExpanded(Boolean(isRiverLikeSituation(situation.id)))
    setFlashForm(true)
  }

  function applyQuickStartPreset(preset: QuickStartPreset) {
    const situation = getSituationConfig(preset.situationId)
    if (!situation) {
      return
    }

    setSelectedPresetId(preset.id)
    setBuilderMode('SIMPLE')
    setSelectedSituationId(preset.situationId)
    setSelectedSensitivityId(preset.sensitivityId)
    setAutoNameEnabled(true)
    applySituationConfig(situation, preset.sensitivityId)
    setCriteriaForm((state) => ({
      ...state,
      rearmWindowMinutes: preset.rearmWindowMinutes ?? state.rearmWindowMinutes,
      forecastWindowHours: preset.forecastWindowHours ?? state.forecastWindowHours,
    }))

    window.requestAnimationFrame(() => {
      document.getElementById('create-custom-alert')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  function handleRuleTypeChange(value: string) {
    const next = value as RuleType
    const nextIsRiverRule = RIVER_RULE_TYPES.includes(next)
    const nextSituation = getSituationForRuleType(next)

    setCriteriaForm((state) => ({
      ...state,
      ruleType: next,
      threshold: defaultThreshold(next),
      riverFloodCategoryThreshold:
        next === 'RIVER_FLOOD_CATEGORY' ? state.riverFloodCategoryThreshold || 'ACTION' : state.riverFloodCategoryThreshold,
      monitorCurrent: nextIsRiverRule ? true : state.monitorCurrent,
      monitorForecast: nextIsRiverRule ? true : state.monitorForecast,
    }))
    setSelectedSituationId(nextSituation)
    setSelectedSensitivityId(resolveMatchingSensitivityId(nextSituation, defaultThreshold(next), criteriaForm.riverFloodCategoryThreshold))
    setSelectedPresetId(null)
    setFormErrors((state) => ({
      ...state,
      threshold: undefined,
      riverGaugeId: undefined,
      gaugeSearchRadiusKm: undefined,
    }))
  }

  function handleLocationModeChange(value: string) {
    const nextMode = value as LocationMode
    setLocationMode(nextMode)
    if (nextMode === 'CITY') {
      setUseCustomCoordinates(false)
      setFormErrors((state) => ({
        ...state,
        latitude: undefined,
        longitude: undefined,
      }))
    }
  }

  function handleCustomCoordinatesToggle(value: boolean) {
    setUseCustomCoordinates(value)
    if (!value) {
      setFormErrors((state) => ({
        ...state,
        latitude: undefined,
        longitude: undefined,
      }))
    }
  }

  function applyPrimaryArea() {
    if (!primaryArea) {
      return
    }
    setCriteriaForm((state) => ({
      ...state,
      location: primaryArea.location,
      latitude: primaryArea.latitude,
      longitude: primaryArea.longitude,
    }))
    setFormErrors((state) => ({
      ...state,
      location: undefined,
      latitude: undefined,
      longitude: undefined,
    }))
    setDidSeedPrimaryLocation(true)
  }

  function handleAlertNameChange(value: string) {
    setCriteriaForm((state) => ({ ...state, name: value }))
    setAutoNameEnabled(value.trim() === '' || value.trim() === suggestedName)
  }

  function validateRuleForm(): RuleFormErrors {
    const errors: RuleFormErrors = {}

    if (!criteriaForm.location.trim()) {
      errors.location = 'Location is required.'
    }

    if (!isRiverCategoryRule) {
      const threshold = Number(criteriaForm.threshold)
      if (Number.isNaN(threshold)) {
        errors.threshold = 'Threshold must be a number.'
      } else {
        const thresholdError = validateThreshold(criteriaForm.ruleType, threshold, criteriaForm.temperatureUnit)
        if (thresholdError) {
          errors.threshold = thresholdError
        }
      }
    }

    if (isRiverRule && !criteriaForm.riverGaugeId.trim()) {
      errors.riverGaugeId = 'Find a nearby river before saving this alert.'
    }

    if (isRiverRule) {
      const searchRadius = Number(criteriaForm.gaugeSearchRadiusKm)
      if (!Number.isInteger(searchRadius) || searchRadius < 1 || searchRadius > 500) {
        errors.gaugeSearchRadiusKm = 'Use a whole number between 1 and 500.'
      }
    }

    const forecastWindow = Number(criteriaForm.forecastWindowHours)
    if (!Number.isInteger(forecastWindow) || forecastWindow <= 0) {
      errors.forecastWindowHours = 'Use a positive whole number.'
    } else if (forecastWindow > 168) {
      errors.forecastWindowHours = 'Use 168 hours or less.'
    }

    const rearmWindow = Number(criteriaForm.rearmWindowMinutes)
    if (!Number.isInteger(rearmWindow) || rearmWindow <= 0) {
      errors.rearmWindowMinutes = 'Use a positive whole number.'
    }

    if (shouldUseCustomCoordinates) {
      const latitude = Number(criteriaForm.latitude)
      if (Number.isNaN(latitude) || latitude < -90 || latitude > 90) {
        errors.latitude = 'Latitude must be between -90 and 90.'
      }

      const longitude = Number(criteriaForm.longitude)
      if (Number.isNaN(longitude) || longitude < -180 || longitude > 180) {
        errors.longitude = 'Longitude must be between -180 and 180.'
      }
    }

    return errors
  }

  function handleCreateAlertSubmit(event: FormEvent<HTMLFormElement>) {
    const errors = validateRuleForm()
    setFormErrors(errors)

    const hasAdvancedErrors = Boolean(
      errors.forecastWindowHours || errors.rearmWindowMinutes || errors.latitude || errors.longitude,
    )

    if (hasAdvancedErrors) {
      setAdvancedExpanded(true)
    }

    if (Object.keys(errors).length > 0) {
      event.preventDefault()
      return
    }

    void handleCreateCriteria(event)
  }

  async function handleResolveNearestGauge() {
    if (!token) {
      setNotice({ kind: 'error', text: 'Your session expired. Sign in again.' })
      return
    }

    const latitude = Number(criteriaForm.latitude)
    const longitude = Number(criteriaForm.longitude)
    const radiusKm = Number(criteriaForm.gaugeSearchRadiusKm)
    const nextErrors: RuleFormErrors = {}

    if (Number.isNaN(latitude) || latitude < -90 || latitude > 90) {
      nextErrors.latitude = 'Latitude must be between -90 and 90.'
    }

    if (Number.isNaN(longitude) || longitude < -180 || longitude > 180) {
      nextErrors.longitude = 'Longitude must be between -180 and 180.'
    }

    if (!Number.isInteger(radiusKm) || radiusKm < 1 || radiusKm > 500) {
      nextErrors.gaugeSearchRadiusKm = 'Use a whole number between 1 and 500.'
    }

    if (Object.keys(nextErrors).length > 0) {
      setFormErrors((state) => ({ ...state, ...nextErrors }))
      setAdvancedExpanded(true)
      return
    }

    setResolvingRiverGauge(true)
    setResolvedRiverGauge(null)
    setFormErrors((state) => ({
      ...state,
      riverGaugeId: undefined,
      gaugeSearchRadiusKm: undefined,
      latitude: undefined,
      longitude: undefined,
    }))

    const query = new URLSearchParams({
      latitude: String(latitude),
      longitude: String(longitude),
      radiusKm: String(radiusKm),
    })

    try {
      const [current, forecast] = await Promise.all([
        apiRequest<WeatherCondition | null>(`/api/weather/hydrology/current?${query.toString()}`, { token }).catch(() => null),
        apiRequest<WeatherCondition | null>(`/api/weather/hydrology/forecast?${query.toString()}`, { token }).catch(() => null),
      ])

      const merged = mergeRiverGaugeConditions(current, forecast)

      if (!merged?.riverGaugeId) {
        throw new Error('No nearby river was found for this spot. Try another point on the map or increase the search range in Advanced.')
      }

      setCriteriaForm((state) => ({
        ...state,
        riverGaugeId: merged.riverGaugeId ?? state.riverGaugeId,
      }))
      setResolvedRiverGauge(merged)
      setNotice({
        kind: 'success',
        text: merged.location ? `Connected this alert to ${merged.location}.` : 'Connected this alert to nearby river data.',
      })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setResolvingRiverGauge(false)
    }
  }

  function renderLocationStep(copy: ReactNode) {
    return (
      <section id="location-picker" tabIndex={-1} className="guided-step-card">
        <div className="guided-step-header">
          <div>
            <p className="eyebrow">Step 2</p>
            <h3>Choose the place</h3>
            <p className="muted small">{copy}</p>
          </div>
          {primaryArea ? (
            <AriaButton type="button" className="ghost button-inline" isDisabled={savingCriteria} onPress={applyPrimaryArea}>
              Use current area
            </AriaButton>
          ) : null}
        </div>

        <div className="location-picker-panel">
          <div className="location-picker-toolbar">
            <div>
              <p className="location-picker-label">Watching: {previewLocation}</p>
              <p className="muted small">{`${resolvedLatitude.toFixed(4)}, ${resolvedLongitude.toFixed(4)}`}</p>
            </div>
          </div>

          <LocationPickerMap
            location={criteriaForm.location}
            latitude={resolvedLatitude}
            longitude={resolvedLongitude}
            onSelect={({ location: selectedLocation, latitude, longitude }) => {
              setCriteriaForm((state) => ({
                ...state,
                location: selectedLocation,
                latitude: String(latitude),
                longitude: String(longitude),
              }))
              setFormErrors((state) => ({
                ...state,
                location: undefined,
                latitude: undefined,
                longitude: undefined,
              }))
            }}
          />

          {formErrors.location ? <p className="field-error">{formErrors.location}</p> : null}
        </div>
      </section>
    )
  }

  function renderThresholdControl() {
    if (isRiverCategoryRule) {
      return (
        <AriaSelect
          label="Flood risk level"
          buttonClassName="aria-select-trigger"
          popoverClassName="aria-select-popover"
          listBoxClassName="aria-select-listbox"
          selectedKey={criteriaForm.riverFloodCategoryThreshold}
          options={FLOOD_CATEGORY_OPTIONS}
          onSelectionChange={(value) =>
            setCriteriaForm((state) => ({
              ...state,
              riverFloodCategoryThreshold: value as FloodCategory,
            }))
          }
        />
      )
    }

    return (
      <div className="threshold-row">
        <AriaTextField
          label={isRiverStageRule ? 'Water level trigger' : 'Threshold'}
          inputClassName="aria-input"
          type="number"
          required
          value={criteriaForm.threshold}
          description={thresholdHelp}
          errorMessage={formErrors.threshold}
          onChange={(value) => setCriteriaForm((state) => ({ ...state, threshold: value }))}
        />

        {isTemperatureScaleRule ? (
          <AriaSelect
            label={isDewPointRule ? 'Unit' : 'Temperature unit'}
            buttonClassName="aria-select-trigger"
            popoverClassName="aria-select-popover"
            listBoxClassName="aria-select-listbox"
            selectedKey={criteriaForm.temperatureUnit}
            options={TEMP_UNIT_OPTIONS}
            onSelectionChange={(value) =>
              setCriteriaForm((state) => ({
                ...state,
                temperatureUnit: value as 'F' | 'C',
              }))
            }
          />
        ) : null}
      </div>
    )
  }

  return (
    <section className={`page-stack rules-page-stack${savingCriteria ? ' is-saving' : ''}`} aria-busy={savingCriteria}>
      {savingCriteria ? (
        <div className="rules-busy-overlay" role="status" aria-live="polite" aria-label="Saving alert">
          <div className="rules-busy-card">
            <span className="rules-busy-spinner" aria-hidden />
            <p className="rules-busy-title">Saving alert</p>
            <p className="muted small rules-busy-copy">This usually takes a few seconds.</p>
          </div>
        </div>
      ) : null}

      <article className="panel">
        <div className="panel-title-row">
          <div>
            <h2>Quick Start</h2>
            <p className="muted small">Pick a common situation, then tune the builder below for the exact place you want SkyPanda to watch.</p>
          </div>
          <span className="badge">{QUICK_START_PRESETS.length} presets</span>
        </div>

        <fieldset className="rules-fieldset-reset" disabled={savingCriteria}>
          <div className="preset-groups">
            <div className="easy-alert-grid">
              {QUICK_START_PRESETS.map((preset) => (
                <AriaButton
                  key={preset.id}
                  type="button"
                  className={`easy-alert-card${selectedPresetId === preset.id ? ' is-active' : ''}`}
                  isDisabled={savingCriteria}
                  onPress={() => applyQuickStartPreset(preset)}
                >
                  <div className="easy-alert-title-row">
                    <span aria-hidden className="easy-alert-icon-badge">
                      {renderPresetIcon(preset.icon)}
                    </span>
                    <div className="easy-alert-copy">
                      <p className="easy-alert-title">{preset.title}</p>
                      <p className="easy-alert-hint">{buildQuickStartHint(preset)}</p>
                    </div>
                  </div>
                  <p className="easy-alert-desc">{preset.description}</p>
                </AriaButton>
              ))}
            </div>
          </div>
        </fieldset>
      </article>

      <article id="create-custom-alert" tabIndex={-1} className={`panel custom-alert-section${flashForm ? ' field-flash' : ''}`}>
        <div className="panel-title-row">
          <div>
            <h2>New Alert</h2>
            <p className="muted small">Choose what matters, choose the place, tune the sensitivity, and let SkyPanda keep watch.</p>
          </div>
        </div>

        <fieldset className="rules-fieldset-reset" disabled={savingCriteria}>
          <div className="builder-mode-switch" role="tablist" aria-label="Alert builder mode">
            <button
              type="button"
              role="tab"
              aria-selected={builderMode === 'SIMPLE'}
              className={`builder-mode-tab${builderMode === 'SIMPLE' ? ' is-active' : ''}`}
              onClick={() => handleBuilderModeChange('SIMPLE')}
            >
              Simple
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={builderMode === 'ADVANCED'}
              className={`builder-mode-tab${builderMode === 'ADVANCED' ? ' is-active' : ''}`}
              onClick={() => handleBuilderModeChange('ADVANCED')}
            >
              Advanced
            </button>
          </div>

          <form className={`guided-rule-builder${builderMode === 'ADVANCED' ? ' is-advanced' : ''}`} onSubmit={handleCreateAlertSubmit} noValidate>
            {selectedPreset ? (
              <div className="preset-loaded-banner" role="status" aria-live="polite">
                <div>
                  <strong>{`Preset loaded: ${selectedPreset.title}`}</strong>
                  <p className="muted small">You can adjust it below. Presets are just starting points.</p>
                </div>
                <span className="badge">{buildQuickStartHint(selectedPreset)}</span>
              </div>
            ) : null}

            <div className="guided-rule-layout">
              <div className="guided-rule-main">
                {builderMode === 'SIMPLE' ? (
                  <>
                    <section className="guided-step-card">
                      <div className="guided-step-header">
                        <div>
                          <p className="eyebrow">Step 1</p>
                          <h3>Choose what to watch</h3>
                          <p className="muted small">Start with the kind of situation you care about. You can tune the details after.</p>
                        </div>
                      </div>

                      <div className="simple-situation-grid">
                        {SIMPLE_SITUATIONS.map((situation) => (
                          <AriaButton
                            key={situation.id}
                            type="button"
                            className={`simple-situation-card${selectedSituationId === situation.id ? ' is-active' : ''}`}
                            onPress={() => handleSituationSelect(situation.id)}
                          >
                            <div className="simple-situation-header">
                              <span aria-hidden className="easy-alert-icon-badge">
                                {renderPresetIcon(situation.icon)}
                              </span>
                              <div className="easy-alert-copy">
                                <p className="easy-alert-title">{situation.title}</p>
                                <p className="easy-alert-hint">{situation.helper}</p>
                              </div>
                            </div>
                            <p className="easy-alert-desc">{situation.description}</p>
                          </AriaButton>
                        ))}
                      </div>

                      {selectedSituation?.id === 'CUSTOM' ? (
                        <div className="simple-custom-note">
                          <strong>Custom situations use the full Advanced builder.</strong>
                          <p className="muted small">Switch to Advanced to choose the exact weather variable and threshold yourself.</p>
                        </div>
                      ) : null}
                    </section>

                    {renderLocationStep('Search for a place, use your current area, or drop a point directly on the map.')}

                    <section className="guided-step-card">
                      <div className="guided-step-header">
                        <div>
                          <p className="eyebrow">Step 3</p>
                          <h3>Choose sensitivity</h3>
                          <p className="muted small">
                            {selectedSituation?.sensitivityLabel
                              ? `${selectedSituation.sensitivityLabel}. Choose a preset first, then fine-tune only if you need to.`
                              : 'Choose how sensitive this alert should be.'}
                          </p>
                        </div>
                      </div>

                      {selectedSituation && selectedSituation.id !== 'CUSTOM' ? (
                        <>
                          <div className="simple-sensitivity-grid">
                            {selectedSituation.sensitivityOptions.map((option) => (
                              <AriaButton
                                key={option.id}
                                type="button"
                                className={`simple-sensitivity-card${selectedSensitivityId === option.id ? ' is-active' : ''}`}
                                onPress={() => handleSensitivitySelect(option.id)}
                              >
                                <strong>{option.label}</strong>
                                <span>{option.description}</span>
                              </AriaButton>
                            ))}
                          </div>

                          {isSimpleCustomSensitivity ? (
                            <div className="simple-custom-threshold-panel">{renderThresholdControl()}</div>
                          ) : (
                            <p className="muted small simple-threshold-summary">
                              {isRiverCategoryRule
                                ? `Using ${formatFloodCategoryLabel(criteriaForm.riverFloodCategoryThreshold)} sensitivity.`
                                : `Using ${formatThresholdForPreview(criteriaForm)} as the trigger threshold.`}
                            </p>
                          )}
                        </>
                      ) : (
                        <p className="muted small">Choose a situation above to unlock the simple sensitivity presets.</p>
                      )}

                      {isRiverRule ? renderRiverGaugePanel(criteriaForm, resolvedRiverGauge, resolvingRiverGauge, formErrors, handleResolveNearestGauge) : null}
                    </section>

                    <section className="guided-step-card">
                      <div className="guided-step-header">
                        <div>
                          <p className="eyebrow">Step 4</p>
                          <h3>Choose timing and notifications</h3>
                          <p className="muted small">Tell SkyPanda whether to watch current conditions, forecast conditions, or both.</p>
                        </div>
                      </div>

                      <div className="guided-toggle-groups">
                        <div className="guided-toggle-group">
                          <p className="guided-toggle-label">Alert for</p>
                          <div className="toggle-row toggle-row-wide">
                            <AriaSwitch
                              label="Current conditions"
                              isSelected={criteriaForm.monitorCurrent}
                              onChange={(value) => setCriteriaForm((state) => ({ ...state, monitorCurrent: value }))}
                            />
                            <AriaSwitch
                              label="Forecast conditions"
                              isSelected={criteriaForm.monitorForecast}
                              onChange={(value) => setCriteriaForm((state) => ({ ...state, monitorForecast: value }))}
                            />
                          </div>
                        </div>

                        <div className="guided-toggle-group">
                          <p className="guided-toggle-label">Notify</p>
                          <div className="toggle-row toggle-row-wide">
                            <AriaSwitch
                              label="Once per event"
                              isSelected={criteriaForm.oncePerEvent}
                              onChange={(value) => setCriteriaForm((state) => ({ ...state, oncePerEvent: value }))}
                            />
                          </div>
                        </div>
                      </div>
                    </section>
                  </>
                ) : (
                  <>
                    {renderLocationStep('Pick the place first, then choose the exact condition you want to monitor.')}

                    <section className="guided-step-card">
                      <div className="guided-step-header">
                        <div>
                          <p className="eyebrow">Step 2</p>
                          <h3>Choose the raw condition</h3>
                          <p className="muted small">Search by terms like heat, rain, flood, wind, humidity, or sky to get to the exact rule faster.</p>
                        </div>
                      </div>

                      <AriaTextField
                        label="Search conditions"
                        inputClassName="aria-input"
                        value={advancedSearchQuery}
                        placeholder="Search heat, rain, wind, flood..."
                        onChange={setAdvancedSearchQuery}
                      />

                      <div className="advanced-rule-groups">
                        {filteredAdvancedGroups.map((group) => (
                          <section key={group.id} className="advanced-rule-group">
                            <div className="advanced-rule-group-header">
                              <p className="preset-group-title">{group.label}</p>
                            </div>
                            <div className="advanced-rule-option-grid">
                              {group.options.map((option) => (
                                <AriaButton
                                  key={option.id}
                                  type="button"
                                  className={`advanced-rule-option${criteriaForm.ruleType === option.id ? ' is-active' : ''}`}
                                  onPress={() => handleRuleTypeChange(option.id)}
                                >
                                  <strong>{option.label}</strong>
                                  <span>{option.description}</span>
                                </AriaButton>
                              ))}
                            </div>
                          </section>
                        ))}
                        {filteredAdvancedGroups.length === 0 ? (
                          <p className="muted small">No matching conditions. Try broader terms like rain, flood, heat, or sky.</p>
                        ) : null}
                      </div>

                      {isRiverRule ? renderRiverGaugePanel(criteriaForm, resolvedRiverGauge, resolvingRiverGauge, formErrors, handleResolveNearestGauge) : null}
                    </section>

                    <section className="guided-step-card">
                      <div className="guided-step-header">
                        <div>
                          <p className="eyebrow">Step 3</p>
                          <h3>Tune the threshold</h3>
                          <p className="muted small">Edit the exact raw condition for this rule.</p>
                        </div>
                      </div>

                      {renderThresholdControl()}
                    </section>

                    <section className="guided-step-card">
                      <div className="guided-step-header">
                        <div>
                          <p className="eyebrow">Step 4</p>
                          <h3>Choose timing and notifications</h3>
                          <p className="muted small">Keep it simple here, then open Advanced settings below if you need more control.</p>
                        </div>
                      </div>

                      <div className="guided-toggle-groups">
                        <div className="guided-toggle-group">
                          <p className="guided-toggle-label">Alert for</p>
                          <div className="toggle-row toggle-row-wide">
                            <AriaSwitch
                              label="Current conditions"
                              isSelected={criteriaForm.monitorCurrent}
                              onChange={(value) => setCriteriaForm((state) => ({ ...state, monitorCurrent: value }))}
                            />
                            <AriaSwitch
                              label="Forecast conditions"
                              isSelected={criteriaForm.monitorForecast}
                              onChange={(value) => setCriteriaForm((state) => ({ ...state, monitorForecast: value }))}
                            />
                          </div>
                        </div>

                        <div className="guided-toggle-group">
                          <p className="guided-toggle-label">Notify</p>
                          <div className="toggle-row toggle-row-wide">
                            <AriaSwitch
                              label="Once per event"
                              isSelected={criteriaForm.oncePerEvent}
                              onChange={(value) => setCriteriaForm((state) => ({ ...state, oncePerEvent: value }))}
                            />
                          </div>
                        </div>
                      </div>
                    </section>
                  </>
                )}

                <Disclosure className="advanced-disclosure" isExpanded={advancedExpanded} onExpandedChange={setAdvancedExpanded}>
                  <AriaButton type="button" slot="trigger" className="advanced-trigger" aria-label="Advanced settings">
                    <span>Additional options</span>
                    <span className="advanced-chevron" aria-hidden>
                      ▾
                    </span>
                  </AriaButton>

                  <DisclosurePanel className="advanced-disclosure-panel">
                    <section className="advanced-section">
                      <h3>Delivery behavior</h3>
                      <div className="advanced-delivery-grid">
                        <AriaTextField
                          label="Minimum time between alerts (minutes)"
                          inputClassName="aria-input"
                          type="number"
                          value={criteriaForm.rearmWindowMinutes}
                          errorMessage={formErrors.rearmWindowMinutes}
                          onChange={(value) => setCriteriaForm((state) => ({ ...state, rearmWindowMinutes: value }))}
                        />

                        <AriaTextField
                          label="Look ahead (forecast hours)"
                          inputClassName="aria-input"
                          type="number"
                          value={criteriaForm.forecastWindowHours}
                          errorMessage={formErrors.forecastWindowHours}
                          onChange={(value) => setCriteriaForm((state) => ({ ...state, forecastWindowHours: value }))}
                        />
                      </div>
                    </section>

                    <section className="advanced-section">
                      <h3>Location details</h3>
                      <AriaSelect
                        label="Location mode"
                        buttonClassName="aria-select-trigger"
                        popoverClassName="aria-select-popover"
                        listBoxClassName="aria-select-listbox"
                        selectedKey={locationMode}
                        options={LOCATION_MODE_OPTIONS}
                        onSelectionChange={handleLocationModeChange}
                      />

                      {shouldShowCoordinateToggle ? (
                        <>
                          <AriaSwitch
                            label="Edit coordinates directly"
                            isSelected={useCustomCoordinates}
                            onChange={handleCustomCoordinatesToggle}
                          />

                          {shouldUseCustomCoordinates ? (
                            <div className="advanced-coordinate-grid">
                              <AriaTextField
                                label="Latitude"
                                inputClassName="aria-input"
                                type="number"
                                value={criteriaForm.latitude}
                                errorMessage={formErrors.latitude}
                                onChange={(value) => setCriteriaForm((state) => ({ ...state, latitude: value }))}
                              />
                              <AriaTextField
                                label="Longitude"
                                inputClassName="aria-input"
                                type="number"
                                value={criteriaForm.longitude}
                                errorMessage={formErrors.longitude}
                                onChange={(value) => setCriteriaForm((state) => ({ ...state, longitude: value }))}
                              />
                            </div>
                          ) : null}
                        </>
                      ) : null}
                    </section>

                    {isRiverRule ? (
                      <section className="advanced-section">
                        <h3>River source</h3>
                        <AriaTextField
                          label="River gauge id"
                          inputClassName="aria-input"
                          value={criteriaForm.riverGaugeId}
                          errorMessage={formErrors.riverGaugeId}
                          description="Use the nearby river helper above or enter a gauge id directly."
                          onChange={(value) => setCriteriaForm((state) => ({ ...state, riverGaugeId: value }))}
                        />

                        <AriaTextField
                          label="Gauge search radius (km)"
                          inputClassName="aria-input"
                          type="number"
                          value={criteriaForm.gaugeSearchRadiusKm}
                          errorMessage={formErrors.gaugeSearchRadiusKm}
                          description="Only used when SkyPanda searches for the nearest river gauge."
                          onChange={(value) => setCriteriaForm((state) => ({ ...state, gaugeSearchRadiusKm: value }))}
                        />

                        {criteriaForm.riverGaugeId ? (
                          <p className="muted small river-monitor-note">Current monitor: {criteriaForm.riverGaugeId}</p>
                        ) : null}
                      </section>
                    ) : null}
                  </DisclosurePanel>
                </Disclosure>
              </div>

              <aside className="guided-rule-aside">
                <section className={`sky-status-card${alertConsoleSummary.allClear ? ' is-calm' : ' is-live'}`}>
                  <div className="rule-preview-header">
                    <div>
                      <p className="eyebrow">Sky Status</p>
                      <h3>{alertConsoleSummary.allClear ? 'Stable' : 'Changing'}</h3>
                    </div>
                    <span className={`badge ${alertConsoleSummary.allClear ? '' : 'is-live'}`}>
                      {alertConsoleSummary.allClear ? 'All clear' : `${alertConsoleSummary.activeCount} active`}
                    </span>
                  </div>
                  <p className="rule-preview-copy">
                    {alertConsoleSummary.allClear
                      ? `${alertConsoleSummary.watchLocation} has been calm${alertConsoleSummary.calmStreakLabel ? ` for ${alertConsoleSummary.calmStreakLabel}` : ''}.`
                      : `${alertConsoleSummary.watchLocation} has active weather changes being tracked now.`}
                  </p>
                  <div className="rule-preview-pills">
                    <span className="metric-pill">{alertConsoleSummary.freshnessLabel}</span>
                    {alertConsoleSummary.lastAlertLabel ? <span className="metric-pill">{alertConsoleSummary.lastAlertLabel}</span> : null}
                  </div>
                </section>

                <section className="sample-alert-card">
                  <div className="rule-preview-header">
                    <div>
                      <p className="eyebrow">Live Preview</p>
                      <h3>{resolvedAlertName}</h3>
                    </div>
                    <span className="badge">{builderMode === 'SIMPLE' ? 'Simple setup' : 'Advanced setup'}</span>
                  </div>
                  <p className="sample-alert-message">{previewAlertSentence}</p>
                  <p className="sample-alert-copy">{`Watching ${previewLocation}. ${previewChecks}. ${
                    criteriaForm.oncePerEvent ? 'Notify once per event.' : `Repeat every ${criteriaForm.rearmWindowMinutes} minutes.`
                  }`}</p>
                  <div className="rule-preview-pills">
                    <span className="metric-pill">{`Condition ${builderMode === 'SIMPLE' ? selectedSituation?.title ?? advancedConditionLabel : advancedConditionLabel}`}</span>
                    <span className="metric-pill">{`Threshold ${formatThresholdForPreview(criteriaForm)}`}</span>
                    {builderMode === 'ADVANCED' ? <span className="metric-pill">{advancedConditionLabel}</span> : null}
                  </div>
                  {previewChips.length > 0 ? (
                    <div className="rule-preview-pills">
                      {previewChips.map((chip) => (
                        <span key={chip} className="metric-pill">
                          {chip}
                        </span>
                      ))}
                    </div>
                  ) : null}
                  {previewCurrentSnapshot ? <p className="sample-alert-note muted small">{previewCurrentSnapshot}</p> : null}
                </section>

                <section className="guided-step-card guided-step-card-compact">
                  <div className="guided-step-header">
                    <div>
                      <p className="eyebrow">Optional</p>
                      <h3>Alert name</h3>
                      <p className="muted small">SkyPanda can name this for you. Edit only if you want a custom label.</p>
                    </div>
                  </div>

                  <AriaTextField
                    label="Alert name"
                    inputClassName="aria-input"
                    value={criteriaForm.name}
                    placeholder={suggestedName}
                    onChange={handleAlertNameChange}
                  />

                  {showSuggestedNameAction ? (
                    <AriaButton
                      type="button"
                      className="ghost button-inline"
                      onPress={() => {
                        setAutoNameEnabled(true)
                        setCriteriaForm((state) => ({ ...state, name: suggestedName }))
                      }}
                    >
                      Use suggested name
                    </AriaButton>
                  ) : null}
                </section>

                <div className="guided-save-actions">
                  <button type="submit" className="primary" disabled={!canSubmitCriteria || savingCriteria}>
                    {savingCriteria ? 'Saving alert…' : 'Save alert'}
                  </button>
                  <p className="muted small">
                    {builderMode === 'SIMPLE'
                      ? 'A useful alert should take four choices or fewer.'
                      : 'Advanced mode preserves every raw rule type the backend supports.'}
                  </p>
                </div>
              </aside>
            </div>
          </form>
        </fieldset>
      </article>
    </section>
  )
}

function renderRiverGaugePanel(
  criteriaForm: typeof initialCriteriaForm,
  resolvedRiverGauge: WeatherCondition | null,
  resolvingRiverGauge: boolean,
  formErrors: RuleFormErrors,
  handleResolveNearestGauge: () => Promise<void>,
) {
  return (
    <section className="river-rule-panel">
      <div className="river-rule-header">
        <div>
          <h3>Connect the nearby river monitor</h3>
          <p className="muted small river-rule-copy">Pick the point on the map first, then let SkyPanda find the river gauge behind the scenes.</p>
        </div>
        <AriaButton type="button" className="button-inline river-helper-button" onPress={() => void handleResolveNearestGauge()}>
          {resolvingRiverGauge ? 'Finding nearby river...' : 'Find nearby river'}
        </AriaButton>
      </div>

      {!criteriaForm.riverGaugeId.trim() ? (
        <p className="muted small river-setup-hint">Choose the river point first. After that, this alert is ready to save.</p>
      ) : null}
      {formErrors.riverGaugeId ? <p className="field-error">{formErrors.riverGaugeId}</p> : null}

      {resolvedRiverGauge?.riverGaugeId ? (
        <div className="river-gauge-card">
          <div className="river-gauge-card-header">
            <p className="river-gauge-title">{resolvedRiverGauge.location || 'Nearby river selected'}</p>
            {resolvedRiverGauge.riverDistanceKm != null ? (
              <span className="badge">{formatNumber(resolvedRiverGauge.riverDistanceKm)} km away</span>
            ) : null}
          </div>

          <div className="river-gauge-metrics">
            {resolvedRiverGauge.riverObservedStage != null ? (
              <span className="metric-pill">Now: {formatStage(resolvedRiverGauge.riverObservedStage, resolvedRiverGauge.riverStageUnit)}</span>
            ) : null}
            {resolvedRiverGauge.riverForecastStage != null ? (
              <span className="metric-pill">Later: {formatStage(resolvedRiverGauge.riverForecastStage, resolvedRiverGauge.riverStageUnit)}</span>
            ) : null}
            {resolvedRiverGauge.riverActionStage != null ? (
              <span className="metric-pill">Early warning: {formatStage(resolvedRiverGauge.riverActionStage, resolvedRiverGauge.riverStageUnit)}</span>
            ) : null}
            {resolvedRiverGauge.riverFloodStage != null ? (
              <span className="metric-pill">Flood level: {formatStage(resolvedRiverGauge.riverFloodStage, resolvedRiverGauge.riverStageUnit)}</span>
            ) : null}
            {resolvedRiverGauge.riverObservedCategory ? (
              <span className="metric-pill">Current risk: {formatRiverCategoryLabel(resolvedRiverGauge.riverObservedCategory)}</span>
            ) : null}
            {resolvedRiverGauge.riverForecastCategory ? (
              <span className="metric-pill">Forecast risk: {formatRiverCategoryLabel(resolvedRiverGauge.riverForecastCategory)}</span>
            ) : null}
          </div>
        </div>
      ) : null}
    </section>
  )
}

function buildThresholdHelp(criteriaForm: typeof initialCriteriaForm) {
  switch (criteriaForm.ruleType) {
    case 'TEMP_BELOW':
      return `Alert when temperature drops below ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
    case 'TEMP_ABOVE':
      return `Alert when temperature rises above ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
    case 'WIND':
      return `Alert when sustained wind goes above ${criteriaForm.threshold || 'X'} km/h.`
    case 'RAIN':
      return `Alert when rain chance reaches ${criteriaForm.threshold || 'X'}% or higher.`
    case 'HUMIDITY_ABOVE':
      return `Alert when humidity reaches ${criteriaForm.threshold || 'X'}% or higher.`
    case 'HUMIDITY_BELOW':
      return `Alert when humidity drops below ${criteriaForm.threshold || 'X'}%.`
    case 'DEW_POINT_ABOVE':
      return `Alert when dew point reaches ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit} or higher.`
    case 'DEW_POINT_BELOW':
      return `Alert when dew point drops below ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
    case 'WIND_GUST':
      return `Alert when forecast wind gusts exceed ${criteriaForm.threshold || 'X'} km/h.`
    case 'SKY_COVER_ABOVE':
      return `Alert when sky cover reaches ${criteriaForm.threshold || 'X'}% or higher.`
    case 'SKY_COVER_BELOW':
      return `Alert when sky cover drops below ${criteriaForm.threshold || 'X'}%.`
    case 'RIVER_STAGE_ABOVE':
      return `Alert when the nearby river rises above ${criteriaForm.threshold || 'X'} ft.`
    case 'RIVER_STAGE_BELOW':
      return `Alert when the nearby river drops below ${criteriaForm.threshold || 'X'} ft.`
    case 'RIVER_FLOOD_CATEGORY':
      return `Alert when the gauge reaches ${formatFloodCategoryLabel(criteriaForm.riverFloodCategoryThreshold)} or higher.`
    default:
      return 'Alert when forecast conditions match this threshold.'
  }
}

function buildPreviewAlertSentence(criteriaForm: typeof initialCriteriaForm) {
  const thresholdHelp = buildThresholdHelp(criteriaForm)
  return thresholdHelp.replace(/\.$/, '')
}

function buildQuickStartHint(preset: QuickStartPreset) {
  const situation = getSituationConfig(preset.situationId)
  const sensitivity = getSensitivityForSituation(preset.situationId, preset.sensitivityId)
  if (sensitivity?.floodCategory) {
    return `Flood stage ${formatFloodCategoryLabel(sensitivity.floodCategory)}`
  }
  return `${situation?.helper ?? 'Watch'} ${sensitivity?.threshold ?? ''}`.trim()
}

function buildPreviewChips(currentWeather: WeatherCondition | null, unit: 'F' | 'C') {
  if (!currentWeather) {
    return []
  }

  return [
    currentWeather.temperature != null ? `🌡 ${formatTemperature(currentWeather.temperature, unit)}` : null,
    currentWeather.humidity != null ? `💧 ${currentWeather.humidity}%` : null,
    currentWeather.precipitationProbability != null ? `🌧 ${formatNumber(currentWeather.precipitationProbability)}%` : null,
    currentWeather.windSpeed != null ? `🌬 ${formatWind(currentWeather.windSpeed)}` : null,
  ].filter((value): value is string => Boolean(value))
}

function describeMonitoringMode(monitorCurrent: boolean, monitorForecast: boolean) {
  if (monitorCurrent && monitorForecast) {
    return 'Watching current conditions and forecast'
  }
  if (monitorForecast) {
    return 'Watching forecast conditions'
  }
  if (monitorCurrent) {
    return 'Watching current conditions'
  }
  return 'Monitoring mode not selected yet'
}

function buildCurrentSnapshotCopy(currentWeather: WeatherCondition | null) {
  if (!currentWeather?.location) {
    return ''
  }
  return `Current snapshot for ${formatFriendlyLocation(currentWeather.location)}: ${
    currentWeather.headline?.trim() || 'latest NOAA observation'
  }.`
}

function validateThreshold(ruleType: RuleType, threshold: number, temperatureUnit: 'F' | 'C') {
  switch (ruleType) {
    case 'TEMP_BELOW':
    case 'TEMP_ABOVE':
      return temperatureUnit === 'F'
        ? threshold < -80 || threshold > 160
          ? 'Use a temperature between -80°F and 160°F.'
          : undefined
        : threshold < -60 || threshold > 70
          ? 'Use a temperature between -60°C and 70°C.'
          : undefined
    case 'RAIN':
    case 'HUMIDITY_ABOVE':
    case 'HUMIDITY_BELOW':
    case 'SKY_COVER_ABOVE':
    case 'SKY_COVER_BELOW':
      return threshold < 0 || threshold > 100 ? 'Use a value between 0 and 100.' : undefined
    case 'WIND':
    case 'WIND_GUST':
      return threshold <= 0 || threshold > 300 ? 'Use a wind speed between 1 and 300 km/h.' : undefined
    case 'DEW_POINT_ABOVE':
    case 'DEW_POINT_BELOW':
      return temperatureUnit === 'F'
        ? threshold < -80 || threshold > 100
          ? 'Use a dew point between -80°F and 100°F.'
          : undefined
        : threshold < -60 || threshold > 40
          ? 'Use a dew point between -60°C and 40°C.'
          : undefined
    case 'RIVER_STAGE_ABOVE':
    case 'RIVER_STAGE_BELOW':
      return threshold <= 0 || threshold > 100 ? 'Use a river stage between 1 and 100 ft.' : undefined
    default:
      return undefined
  }
}

function mergeRiverGaugeConditions(current: WeatherCondition | null, forecast: WeatherCondition | null) {
  if (!current && !forecast) {
    return null
  }

  const base = current ?? forecast
  if (!base) {
    return null
  }

  return {
    ...base,
    riverObservedStage: current?.riverObservedStage ?? base.riverObservedStage,
    riverObservedCategory: current?.riverObservedCategory ?? base.riverObservedCategory,
    riverForecastStage: forecast?.riverForecastStage ?? base.riverForecastStage,
    riverForecastCategory: forecast?.riverForecastCategory ?? base.riverForecastCategory,
    riverDistanceKm: current?.riverDistanceKm ?? forecast?.riverDistanceKm ?? base.riverDistanceKm,
  }
}

function formatStage(value?: number, unit = 'ft') {
  if (value == null || Number.isNaN(value)) {
    return '--'
  }
  return `${formatNumber(value)} ${unit}`
}

function formatRiverCategoryLabel(value?: string) {
  switch ((value ?? '').toUpperCase()) {
    case 'ACTION':
      return 'Action stage'
    case 'MINOR':
      return 'Minor flood'
    case 'MODERATE':
      return 'Moderate flood'
    case 'MAJOR':
      return 'Major flood'
    default:
      return value ?? 'Unknown'
  }
}

function formatFloodCategoryLabel(value: FloodCategory) {
  switch (value) {
    case 'ACTION':
      return 'action stage'
    case 'MINOR':
      return 'minor flood'
    case 'MODERATE':
      return 'moderate flood'
    case 'MAJOR':
      return 'major flood'
  }
}

function formatThresholdForPreview(criteriaForm: typeof initialCriteriaForm) {
  if (criteriaForm.ruleType === 'RIVER_FLOOD_CATEGORY') {
    return formatFloodCategoryLabel(criteriaForm.riverFloodCategoryThreshold)
  }
  if (
    criteriaForm.ruleType === 'TEMP_ABOVE' ||
    criteriaForm.ruleType === 'TEMP_BELOW' ||
    criteriaForm.ruleType === 'DEW_POINT_ABOVE' ||
    criteriaForm.ruleType === 'DEW_POINT_BELOW'
  ) {
    return `${criteriaForm.threshold}°${criteriaForm.temperatureUnit}`
  }
  if (
    criteriaForm.ruleType === 'RAIN' ||
    criteriaForm.ruleType.startsWith('HUMIDITY') ||
    criteriaForm.ruleType.startsWith('SKY_COVER')
  ) {
    return `${criteriaForm.threshold}%`
  }
  if (criteriaForm.ruleType.startsWith('RIVER_STAGE')) {
    return `${criteriaForm.threshold} ft`
  }
  return `${criteriaForm.threshold} km/h`
}

function isRiverLikeSituation(situationId: SimpleSituationId) {
  return situationId === 'RIVER_RISING' || situationId === 'FLOOD_RISK'
}

function renderPresetIcon(icon: RuleBuilderIcon) {
  switch (icon) {
    case 'jacket':
      return '🧥'
    case 'heat':
      return '☀️'
    case 'rain':
      return '🌧️'
    case 'wind':
      return '🌬️'
    case 'humidity':
      return '💧'
    case 'dew':
      return '🌙'
    case 'river':
      return '🌊'
    case 'alert':
      return '⚠️'
    case 'flood':
      return '🚨'
    case 'sky':
      return '☁️'
    case 'custom':
    default:
      return '✨'
  }
}
