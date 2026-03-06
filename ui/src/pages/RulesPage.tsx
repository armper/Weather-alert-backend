import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Disclosure, DisclosurePanel } from 'react-aria-components'
import { useLocation } from 'react-router-dom'
import { defaultThreshold } from '../lib/criteria'
import { LocationPickerMap } from '../components/maps/LocationPickerMap'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { useAppState } from '../state/useAppState'
import { DEFAULT_LAT, DEFAULT_LON, type RuleType } from '../state/types'

interface RuleFormErrors {
  name?: string
  location?: string
  threshold?: string
  forecastWindowHours?: string
  rearmWindowMinutes?: string
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
] as const

const TEMP_UNIT_OPTIONS = [
  { id: 'F', label: 'Fahrenheit' },
  { id: 'C', label: 'Celsius' },
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
]

export function RulesPage() {
  const { criteriaForm, setCriteriaForm, canSubmitCriteria, savingCriteria, handleCreateCriteria } = useAppState()
  const location = useLocation()

  const [formErrors, setFormErrors] = useState<RuleFormErrors>({})
  const [advancedExpanded, setAdvancedExpanded] = useState(false)
  const [locationMode, setLocationMode] = useState<LocationMode>('CITY')
  const [useCustomCoordinates, setUseCustomCoordinates] = useState(false)
  const [flashForm, setFlashForm] = useState(false)

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'
  const isDewPointRule = criteriaForm.ruleType === 'DEW_POINT_ABOVE' || criteriaForm.ruleType === 'DEW_POINT_BELOW'
  const isTemperatureScaleRule = isTemperatureRule || isDewPointRule
  const isGridRule = GRID_RULE_TYPES.includes(criteriaForm.ruleType)
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
      default:
        return 'Alert when forecast conditions match this threshold.'
    }
  }, [criteriaForm.ruleType, criteriaForm.threshold, criteriaForm.temperatureUnit])

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

  function validateRuleForm(): RuleFormErrors {
    const errors: RuleFormErrors = {}

    if (!criteriaForm.name.trim()) {
      errors.name = 'Alert name is required.'
    }
    if (!criteriaForm.location.trim()) {
      errors.location = 'Location is required.'
    }

    const threshold = Number(criteriaForm.threshold)
    if (Number.isNaN(threshold)) {
      errors.threshold = 'Threshold must be a number.'
    } else {
      const thresholdError = validateThreshold(criteriaForm.ruleType, threshold, criteriaForm.temperatureUnit)
      if (thresholdError) {
        errors.threshold = thresholdError
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
    setCriteriaForm((state) => ({
      ...state,
      ruleType: next,
      threshold: defaultThreshold(next),
      monitorForecast: GRID_RULE_TYPES.includes(next) ? true : state.monitorForecast,
    }))
  }

  function applyPreset(preset: RulePreset) {
    setCriteriaForm((state) => ({
      ...state,
      name: preset.title,
      ruleType: preset.ruleType,
      threshold: preset.threshold,
      temperatureUnit: preset.temperatureUnit ?? state.temperatureUnit,
      monitorCurrent: preset.monitorCurrent ?? state.monitorCurrent,
      monitorForecast: preset.monitorForecast ?? state.monitorForecast,
      oncePerEvent: preset.oncePerEvent ?? state.oncePerEvent,
      forecastWindowHours: preset.forecastWindowHours ?? state.forecastWindowHours,
      rearmWindowMinutes: preset.rearmWindowMinutes ?? state.rearmWindowMinutes,
    }))
    setFormErrors({})
    setAdvancedExpanded(GRID_RULE_TYPES.includes(preset.ruleType))
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

          <div className="threshold-row full-row">
            <AriaTextField
              label="Threshold"
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
