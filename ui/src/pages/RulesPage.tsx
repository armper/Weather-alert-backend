import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Disclosure, DisclosurePanel } from 'react-aria-components'
import { useLocation } from 'react-router-dom'
import { apiRequest, toErrorMessage } from '../api'
import { formatNumber } from '../lib/formatting'
import { RIVER_RULE_TYPES, RIVER_STAGE_RULE_TYPES, defaultThreshold } from '../lib/criteria'
import { LocationPickerMap } from '../components/maps/LocationPickerMap'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { useAppState } from '../state/useAppState'
import { DEFAULT_LAT, DEFAULT_LON, type RuleType } from '../state/types'
import type { FloodCategory, WeatherCondition } from '../types'

interface RuleFormErrors {
  name?: string
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

interface RulePreset {
  id: string
  title: string
  description: string
  icon: string
  ruleType: RuleType
  threshold: string
  temperatureUnit?: 'F' | 'C'
  forecastWindowHours?: string
  rearmWindowMinutes?: string
  riverFloodCategoryThreshold?: FloodCategory
  monitorCurrent?: boolean
  monitorForecast?: boolean
  oncePerEvent?: boolean
}

const RULE_TYPE_OPTIONS = [
  { id: 'TEMP_BELOW', label: 'Temperature below' },
  { id: 'TEMP_ABOVE', label: 'Temperature above' },
  { id: 'WIND', label: 'Wind speed above' },
  { id: 'RAIN', label: 'Rain probability at/above' },
  { id: 'HUMIDITY_ABOVE', label: 'Humidity at/above' },
  { id: 'HUMIDITY_BELOW', label: 'Humidity below' },
  { id: 'DEW_POINT_ABOVE', label: 'Dew point at/above' },
  { id: 'DEW_POINT_BELOW', label: 'Dew point below' },
  { id: 'WIND_GUST', label: 'Wind gust above' },
  { id: 'SKY_COVER_ABOVE', label: 'Sky cover at/above' },
  { id: 'SKY_COVER_BELOW', label: 'Sky cover below' },
  { id: 'RIVER_STAGE_ABOVE', label: 'River stage above' },
  { id: 'RIVER_STAGE_BELOW', label: 'River stage below' },
  { id: 'RIVER_FLOOD_CATEGORY', label: 'River flood category at/above' },
] as const

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

const GRID_RULE_TYPES: RuleType[] = [
  'HUMIDITY_ABOVE',
  'HUMIDITY_BELOW',
  'DEW_POINT_ABOVE',
  'DEW_POINT_BELOW',
  'WIND_GUST',
  'SKY_COVER_ABOVE',
  'SKY_COVER_BELOW',
]

const RULE_PRESETS: RulePreset[] = [
  {
    id: 'bring-jacket',
    title: 'Bring a Jacket',
    description: 'Warn when temperatures dip into light-jacket territory.',
    icon: 'Coat',
    ruleType: 'TEMP_BELOW',
    threshold: '60',
    temperatureUnit: 'F',
    monitorCurrent: true,
    monitorForecast: true,
  },
  {
    id: 'heat-watch',
    title: 'Heat Watch',
    description: 'Catch hotter afternoons before they spike.',
    icon: 'Heat',
    ruleType: 'TEMP_ABOVE',
    threshold: '92',
    temperatureUnit: 'F',
    monitorCurrent: true,
    monitorForecast: true,
  },
  {
    id: 'storm-window',
    title: 'Storm Window',
    description: 'Track higher rain chances over the next day.',
    icon: 'Rain',
    ruleType: 'RAIN',
    threshold: '65',
    forecastWindowHours: '24',
    monitorCurrent: false,
    monitorForecast: true,
  },
  {
    id: 'wind-advisory',
    title: 'Wind Advisory',
    description: 'Flag stronger sustained winds for plans outdoors.',
    icon: 'Wind',
    ruleType: 'WIND',
    threshold: '30',
    monitorCurrent: true,
    monitorForecast: true,
  },
  {
    id: 'sticky-air',
    title: 'Sticky Air',
    description: 'Use NOAA grid humidity to catch swampy conditions.',
    icon: 'Humidity',
    ruleType: 'HUMIDITY_ABOVE',
    threshold: '85',
    forecastWindowHours: '18',
    monitorCurrent: false,
    monitorForecast: true,
  },
  {
    id: 'tropical-night',
    title: 'Tropical Night',
    description: 'Watch for muggy dew points that keep evenings uncomfortable.',
    icon: 'Dew',
    ruleType: 'DEW_POINT_ABOVE',
    threshold: '70',
    temperatureUnit: 'F',
    forecastWindowHours: '18',
    monitorCurrent: false,
    monitorForecast: true,
  },
  {
    id: 'gust-watch',
    title: 'Gust Watch',
    description: 'Use forecast grid gusts to catch squalls before they arrive.',
    icon: 'Gust',
    ruleType: 'WIND_GUST',
    threshold: '40',
    forecastWindowHours: '12',
    monitorCurrent: false,
    monitorForecast: true,
  },
  {
    id: 'blue-sky',
    title: 'Blue Sky Break',
    description: 'Alert when sky cover opens up for a clear-weather window.',
    icon: 'Sky',
    ruleType: 'SKY_COVER_BELOW',
    threshold: '20',
    forecastWindowHours: '12',
    monitorCurrent: false,
    monitorForecast: true,
  },
  {
    id: 'river-rising',
    title: 'River Rising',
    description: 'A starting stage threshold you can tune after picking a gauge.',
    icon: 'River',
    ruleType: 'RIVER_STAGE_ABOVE',
    threshold: '8',
    monitorCurrent: true,
    monitorForecast: true,
    forecastWindowHours: '24',
  },
  {
    id: 'action-stage-watch',
    title: 'Action Stage Watch',
    description: 'Warn when a river is forecast to reach official action stage.',
    icon: 'Gauge',
    ruleType: 'RIVER_FLOOD_CATEGORY',
    threshold: '',
    riverFloodCategoryThreshold: 'ACTION',
    monitorCurrent: true,
    monitorForecast: true,
    forecastWindowHours: '24',
  },
  {
    id: 'minor-flood-risk',
    title: 'Minor Flood Risk',
    description: 'Track gauges that are entering official minor flood territory.',
    icon: 'Flood',
    ruleType: 'RIVER_FLOOD_CATEGORY',
    threshold: '',
    riverFloodCategoryThreshold: 'MINOR',
    monitorCurrent: true,
    monitorForecast: true,
    forecastWindowHours: '36',
  },
]

export function RulesPage() {
  const { token, setNotice, criteriaForm, setCriteriaForm, canSubmitCriteria, savingCriteria, handleCreateCriteria } =
    useAppState()
  const location = useLocation()

  const [formErrors, setFormErrors] = useState<RuleFormErrors>({})
  const [advancedExpanded, setAdvancedExpanded] = useState(false)
  const [locationMode, setLocationMode] = useState<LocationMode>('CITY')
  const [useCustomCoordinates, setUseCustomCoordinates] = useState(false)
  const [flashForm, setFlashForm] = useState(false)
  const [resolvingRiverGauge, setResolvingRiverGauge] = useState(false)
  const [resolvedRiverGauge, setResolvedRiverGauge] = useState<WeatherCondition | null>(null)

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'
  const isDewPointRule = criteriaForm.ruleType === 'DEW_POINT_ABOVE' || criteriaForm.ruleType === 'DEW_POINT_BELOW'
  const isTemperatureScaleRule = isTemperatureRule || isDewPointRule
  const isGridRule = GRID_RULE_TYPES.includes(criteriaForm.ruleType)
  const isRiverRule = RIVER_RULE_TYPES.includes(criteriaForm.ruleType)
  const isRiverStageRule = RIVER_STAGE_RULE_TYPES.includes(criteriaForm.ruleType)
  const isRiverCategoryRule = criteriaForm.ruleType === 'RIVER_FLOOD_CATEGORY'
  const shouldShowCoordinateToggle = locationMode === 'MANUAL'
  const shouldUseCustomCoordinates = shouldShowCoordinateToggle && useCustomCoordinates
  const mapLatitude = Number(criteriaForm.latitude)
  const mapLongitude = Number(criteriaForm.longitude)
  const resolvedLatitude = Number.isNaN(mapLatitude) ? Number(DEFAULT_LAT) : mapLatitude
  const resolvedLongitude = Number.isNaN(mapLongitude) ? Number(DEFAULT_LON) : mapLongitude

  const thresholdHelp = useMemo(() => {
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
        return `Alert when river stage rises above ${criteriaForm.threshold || 'X'} ft at gauge ${criteriaForm.riverGaugeId || 'XXXX'}.`
      case 'RIVER_STAGE_BELOW':
        return `Alert when river stage drops below ${criteriaForm.threshold || 'X'} ft at gauge ${criteriaForm.riverGaugeId || 'XXXX'}.`
      case 'RIVER_FLOOD_CATEGORY':
        return `Alert when the gauge reaches ${formatFloodCategoryLabel(criteriaForm.riverFloodCategoryThreshold)} or higher.`
      default:
        return 'Alert when forecast conditions match this threshold.'
    }
  }, [
    criteriaForm.ruleType,
    criteriaForm.threshold,
    criteriaForm.temperatureUnit,
    criteriaForm.riverGaugeId,
    criteriaForm.riverFloodCategoryThreshold,
  ])

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

  function validateRuleForm(): RuleFormErrors {
    const errors: RuleFormErrors = {}

    if (!criteriaForm.name.trim()) {
      errors.name = 'Alert name is required.'
    }
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
      errors.riverGaugeId = 'Gauge ID is required for river alerts.'
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
        throw new Error('No nearby river gauge was found for this location.')
      }

      setCriteriaForm((state) => ({
        ...state,
        riverGaugeId: merged.riverGaugeId ?? state.riverGaugeId,
      }))
      setResolvedRiverGauge(merged)
      setNotice({
        kind: 'success',
        text: `Nearest gauge set to ${merged.riverGaugeId}${merged.location ? ` (${merged.location})` : ''}.`,
      })
    } catch (error) {
      setNotice({ kind: 'error', text: toErrorMessage(error) })
    } finally {
      setResolvingRiverGauge(false)
    }
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

  function handleRuleTypeChange(value: string) {
    const next = value as RuleType
    const nextIsGridRule = GRID_RULE_TYPES.includes(next)
    const nextIsRiverRule = RIVER_RULE_TYPES.includes(next)
    setCriteriaForm((state) => ({
      ...state,
      ruleType: next,
      threshold: defaultThreshold(next),
      riverFloodCategoryThreshold: next === 'RIVER_FLOOD_CATEGORY' ? state.riverFloodCategoryThreshold || 'ACTION' : state.riverFloodCategoryThreshold,
      monitorCurrent: nextIsRiverRule ? true : state.monitorCurrent,
      monitorForecast: nextIsGridRule || nextIsRiverRule ? true : state.monitorForecast,
    }))
    setFormErrors((state) => ({
      ...state,
      threshold: undefined,
      riverGaugeId: undefined,
      gaugeSearchRadiusKm: undefined,
    }))
  }

  function applyPreset(preset: RulePreset) {
    setCriteriaForm((state) => ({
      ...state,
      name: preset.title,
      ruleType: preset.ruleType,
      threshold: preset.threshold,
      temperatureUnit: preset.temperatureUnit ?? state.temperatureUnit,
      riverFloodCategoryThreshold: preset.riverFloodCategoryThreshold ?? state.riverFloodCategoryThreshold,
      monitorCurrent: preset.monitorCurrent ?? state.monitorCurrent,
      monitorForecast: preset.monitorForecast ?? state.monitorForecast,
      oncePerEvent: preset.oncePerEvent ?? state.oncePerEvent,
      forecastWindowHours: preset.forecastWindowHours ?? state.forecastWindowHours,
      rearmWindowMinutes: preset.rearmWindowMinutes ?? state.rearmWindowMinutes,
    }))
    setFormErrors({})
    setAdvancedExpanded(GRID_RULE_TYPES.includes(preset.ruleType) || RIVER_RULE_TYPES.includes(preset.ruleType))
    setFlashForm(true)

    window.requestAnimationFrame(() => {
      const target = document.getElementById('create-custom-alert')
      target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
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
            <p className="muted small">Load a common alert preset, then adjust it if needed.</p>
          </div>
          <span className="badge">{RULE_PRESETS.length} presets</span>
        </div>

        <fieldset className="rules-fieldset-reset" disabled={savingCriteria}>
          <div className="easy-alert-grid">
          {RULE_PRESETS.map((preset) => (
            <AriaButton
              key={preset.id}
              type="button"
              className={`easy-alert-card${criteriaForm.name === preset.title ? ' is-active' : ''}`}
              isDisabled={savingCriteria}
              onPress={() => applyPreset(preset)}
            >
              <p className="easy-alert-title">
                <span aria-hidden className="easy-alert-icon">
                  {preset.icon}
                </span>{' '}
                {preset.title}
              </p>
              <p className="easy-alert-desc">{preset.description}</p>
            </AriaButton>
          ))}
          </div>
        </fieldset>
      </article>

      <article
        id="create-custom-alert"
        tabIndex={-1}
        className={`panel custom-alert-section${flashForm ? ' field-flash' : ''}`}
      >
        <div className="panel-title-row">
          <h2>Create Alert</h2>
        </div>

        <fieldset className="rules-fieldset-reset" disabled={savingCriteria}>
          <form className="grid-form create-grid" onSubmit={handleCreateAlertSubmit} noValidate>
          <AriaTextField
            label="Alert name"
            inputClassName="aria-input"
            value={criteriaForm.name}
            required
            errorMessage={formErrors.name}
            onChange={(value) => setCriteriaForm((state) => ({ ...state, name: value }))}
          />

          <div id="location-picker" tabIndex={-1} className="full-row location-picker-panel">
            <LocationPickerMap
              location={criteriaForm.location}
              latitude={resolvedLatitude}
              longitude={resolvedLongitude}
              onSelect={({ location: selectedLocation, latitude, longitude }) =>
                setCriteriaForm((state) => ({
                  ...state,
                  location: selectedLocation,
                  latitude: String(latitude),
                  longitude: String(longitude),
                }))
              }
            />
            {formErrors.location ? <p className="field-error">{formErrors.location}</p> : null}
          </div>

          <AriaSelect
            label="Rule type"
            buttonClassName="aria-select-trigger"
            popoverClassName="aria-select-popover"
            listBoxClassName="aria-select-listbox"
            selectedKey={criteriaForm.ruleType}
            options={RULE_TYPE_OPTIONS.map((option) => ({ ...option }))}
            onSelectionChange={handleRuleTypeChange}
          />

          {isGridRule ? (
            <p className="muted small full-row rule-mode-note">
              Advanced grid rules come from NOAA forecast grid data. Keep Forecast enabled to use the best signal.
            </p>
          ) : null}

          {isRiverRule ? (
            <section className="full-row river-rule-panel">
              <div className="river-rule-header">
                <div>
                  <h3>River gauge</h3>
                  <p className="muted small river-rule-copy">
                    Pick a known NWPS gauge or resolve the nearest one from the map point above.
                  </p>
                </div>
                <AriaButton
                  type="button"
                  className="button-inline river-helper-button"
                  isDisabled={savingCriteria || resolvingRiverGauge}
                  onPress={() => void handleResolveNearestGauge()}
                >
                  {resolvingRiverGauge ? 'Finding gauge...' : 'Use nearest gauge'}
                </AriaButton>
              </div>

              <div className="river-gauge-grid">
                <AriaTextField
                  label="Gauge ID"
                  inputClassName="aria-input"
                  value={criteriaForm.riverGaugeId}
                  required
                  errorMessage={formErrors.riverGaugeId}
                  onChange={(value) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      riverGaugeId: value.toUpperCase(),
                    }))
                  }
                />

                <AriaTextField
                  label="Nearest-gauge radius (km)"
                  inputClassName="aria-input"
                  type="number"
                  min="1"
                  max="500"
                  value={criteriaForm.gaugeSearchRadiusKm}
                  errorMessage={formErrors.gaugeSearchRadiusKm}
                  onChange={(value) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      gaugeSearchRadiusKm: value,
                    }))
                  }
                />
              </div>

              {resolvedRiverGauge?.riverGaugeId ? (
                <div className="river-gauge-card">
                  <div className="river-gauge-card-header">
                    <p className="river-gauge-title">
                      {resolvedRiverGauge.location || 'Resolved gauge'} <span>{resolvedRiverGauge.riverGaugeId}</span>
                    </p>
                    {resolvedRiverGauge.riverDistanceKm != null ? (
                      <span className="badge">{formatNumber(resolvedRiverGauge.riverDistanceKm)} km away</span>
                    ) : null}
                  </div>

                  <div className="river-gauge-metrics">
                    {resolvedRiverGauge.riverObservedStage != null ? (
                      <span className="metric-pill">
                        Observed: {formatStage(resolvedRiverGauge.riverObservedStage, resolvedRiverGauge.riverStageUnit)}
                      </span>
                    ) : null}
                    {resolvedRiverGauge.riverForecastStage != null ? (
                      <span className="metric-pill">
                        Forecast: {formatStage(resolvedRiverGauge.riverForecastStage, resolvedRiverGauge.riverStageUnit)}
                      </span>
                    ) : null}
                    {resolvedRiverGauge.riverActionStage != null ? (
                      <span className="metric-pill">
                        Action: {formatStage(resolvedRiverGauge.riverActionStage, resolvedRiverGauge.riverStageUnit)}
                      </span>
                    ) : null}
                    {resolvedRiverGauge.riverFloodStage != null ? (
                      <span className="metric-pill">
                        Flood: {formatStage(resolvedRiverGauge.riverFloodStage, resolvedRiverGauge.riverStageUnit)}
                      </span>
                    ) : null}
                    {resolvedRiverGauge.riverObservedCategory ? (
                      <span className="metric-pill">
                        Current: {formatRiverCategoryLabel(resolvedRiverGauge.riverObservedCategory)}
                      </span>
                    ) : null}
                    {resolvedRiverGauge.riverForecastCategory ? (
                      <span className="metric-pill">
                        Forecast category: {formatRiverCategoryLabel(resolvedRiverGauge.riverForecastCategory)}
                      </span>
                    ) : null}
                  </div>
                </div>
              ) : null}
            </section>
          ) : null}

          {isRiverCategoryRule ? (
            <div className="threshold-row full-row river-threshold-row">
              <AriaSelect
                label="Flood category"
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

              <div className="river-threshold-help">
                <p className="muted small">{thresholdHelp}</p>
              </div>
            </div>
          ) : (
            <div className="threshold-row full-row">
              <AriaTextField
                label={isRiverStageRule ? 'Stage threshold' : 'Threshold'}
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
                  label={isDewPointRule ? 'Dew point unit' : 'Temperature unit'}
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
          )}

          <div className="toggle-row full-row toggle-row-wide">
            <AriaSwitch
              label="Current"
              isSelected={criteriaForm.monitorCurrent}
              onChange={(value) =>
                setCriteriaForm((state) => ({
                  ...state,
                  monitorCurrent: value,
                }))
              }
            />
            <AriaSwitch
              label="Forecast"
              isSelected={criteriaForm.monitorForecast}
              onChange={(value) =>
                setCriteriaForm((state) => ({
                  ...state,
                  monitorForecast: value,
                }))
              }
            />
            <AriaSwitch
              label="Notify once"
              isSelected={criteriaForm.oncePerEvent}
              onChange={(value) =>
                setCriteriaForm((state) => ({
                  ...state,
                  oncePerEvent: value,
                }))
              }
            />
          </div>

          <AriaButton
            type="submit"
            className="primary button-inline full-row"
            isDisabled={!canSubmitCriteria || savingCriteria}
          >
            {savingCriteria ? 'Saving alert...' : 'Create Alert'}
          </AriaButton>

          <Disclosure
            className="advanced-disclosure full-row"
            isExpanded={advancedExpanded}
            onExpandedChange={setAdvancedExpanded}
          >
            <AriaButton type="button" slot="trigger" className="advanced-trigger" aria-label="Advanced settings">
              <span>Advanced</span>
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
                    onChange={(value) =>
                      setCriteriaForm((state) => ({
                        ...state,
                        rearmWindowMinutes: value,
                      }))
                    }
                  />

                  <AriaTextField
                    label="Look ahead (forecast hours)"
                    inputClassName="aria-input"
                    type="number"
                    value={criteriaForm.forecastWindowHours}
                    errorMessage={formErrors.forecastWindowHours}
                    onChange={(value) =>
                      setCriteriaForm((state) => ({
                        ...state,
                        forecastWindowHours: value,
                      }))
                    }
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
                      label="Use custom coordinates"
                      isSelected={useCustomCoordinates}
                      onChange={handleCustomCoordinatesToggle}
                    />

                    {shouldUseCustomCoordinates ? (
                      <>
                        <p className="muted small advanced-helper">
                          Only needed if you want alerts for a specific coordinate.
                        </p>
                        <div className="advanced-coordinate-grid">
                          <AriaTextField
                            label="Latitude"
                            inputClassName="aria-input"
                            type="number"
                            step="0.0001"
                            value={criteriaForm.latitude}
                            errorMessage={formErrors.latitude}
                            onChange={(value) => setCriteriaForm((state) => ({ ...state, latitude: value }))}
                          />
                          <AriaTextField
                            label="Longitude"
                            inputClassName="aria-input"
                            type="number"
                            step="0.0001"
                            value={criteriaForm.longitude}
                            errorMessage={formErrors.longitude}
                            onChange={(value) => setCriteriaForm((state) => ({ ...state, longitude: value }))}
                          />
                        </div>
                      </>
                    ) : null}
                  </>
                ) : null}
              </section>
            </DisclosurePanel>
          </Disclosure>
          </form>
        </fieldset>
      </article>
    </section>
  )
}

function validateThreshold(ruleType: RuleType, threshold: number, unit: 'F' | 'C'): string | undefined {
  if (ruleType === 'TEMP_ABOVE' || ruleType === 'TEMP_BELOW' || ruleType === 'DEW_POINT_ABOVE' || ruleType === 'DEW_POINT_BELOW') {
    if (unit === 'F') {
      if (threshold < -50 || threshold > 140) {
        return 'For Fahrenheit, use a value between -50 and 140.'
      }
    } else if (threshold < -45 || threshold > 60) {
      return 'For Celsius, use a value between -45 and 60.'
    }
    return undefined
  }

  if (ruleType === 'RAIN' || ruleType === 'HUMIDITY_ABOVE' || ruleType === 'HUMIDITY_BELOW' || ruleType === 'SKY_COVER_ABOVE' || ruleType === 'SKY_COVER_BELOW') {
    if (threshold < 0 || threshold > 100) {
      return 'Use a value between 0 and 100.'
    }
    return undefined
  }

  if (threshold < 0) {
    return 'Threshold must be zero or greater.'
  }

  return undefined
}

function mergeRiverGaugeConditions(
  current: WeatherCondition | null,
  forecast: WeatherCondition | null,
): WeatherCondition | null {
  if (!current && !forecast) {
    return null
  }

  return {
    ...(current ?? {}),
    ...(forecast ?? {}),
    id: current?.id ?? forecast?.id ?? 'resolved-river-gauge',
    riverGaugeId: current?.riverGaugeId ?? forecast?.riverGaugeId,
    location: current?.location ?? forecast?.location,
    riverObservedStage: current?.riverObservedStage ?? forecast?.riverObservedStage,
    riverObservedCategory: current?.riverObservedCategory ?? forecast?.riverObservedCategory,
    riverForecastStage: forecast?.riverForecastStage ?? current?.riverForecastStage,
    riverForecastCategory: forecast?.riverForecastCategory ?? current?.riverForecastCategory,
    riverFloodStage: current?.riverFloodStage ?? forecast?.riverFloodStage,
    riverActionStage: current?.riverActionStage ?? forecast?.riverActionStage,
    riverStageUnit: current?.riverStageUnit ?? forecast?.riverStageUnit,
    riverDistanceKm: current?.riverDistanceKm ?? forecast?.riverDistanceKm,
    timestamp: current?.timestamp ?? forecast?.timestamp,
  }
}

function formatFloodCategoryLabel(category: FloodCategory): string {
  switch (category) {
    case 'ACTION':
      return 'action stage'
    case 'MINOR':
      return 'minor flooding'
    case 'MODERATE':
      return 'moderate flooding'
    case 'MAJOR':
      return 'major flooding'
  }
}

function formatRiverCategoryLabel(category?: string | null): string {
  if (!category) {
    return 'Unknown'
  }

  const normalized = category.replace(/_/g, ' ').trim()
  if (normalized.length === 0) {
    return 'Unknown'
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1)
}

function formatStage(stage?: number | null, unit?: string | null): string {
  const stageLabel = formatNumber(stage)
  if (stageLabel === '-') {
    return '--'
  }
  return `${stageLabel} ${unit ?? 'ft'}`
}
