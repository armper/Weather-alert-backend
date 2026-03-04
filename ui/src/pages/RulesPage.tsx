import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Disclosure, DisclosurePanel } from 'react-aria-components'
import { useLocation } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { defaultThreshold } from '../lib/criteria'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'
import { LocationPickerMap } from '../components/maps/LocationPickerMap'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { DEFAULT_LAT, DEFAULT_LON, type RuleType } from '../state/types'

interface EasyPreset {
  id: string
  icon: string
  title: string
  description: string
  ruleType: RuleType
  threshold: string
  temperatureUnit?: 'F' | 'C'
  monitorCurrent: boolean
  monitorForecast: boolean
  oncePerEvent: boolean
  forecastWindowHours: string
  rearmWindowMinutes: string
}

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

const RULE_TYPE_OPTIONS = [
  { id: 'TEMP_BELOW', label: 'Temperature below' },
  { id: 'TEMP_ABOVE', label: 'Temperature above' },
  { id: 'WIND', label: 'Wind speed above' },
  { id: 'RAIN', label: 'Rain probability at/above' },
]

const TEMP_UNIT_OPTIONS = [
  { id: 'F', label: 'Fahrenheit' },
  { id: 'C', label: 'Celsius' },
]

const LOCATION_MODE_OPTIONS = [
  { id: 'CITY', label: 'City or place' },
  { id: 'MANUAL', label: 'Manual coordinates' },
]

const EASY_PRESETS: EasyPreset[] = [
  {
    id: 'too-cold',
    icon: '🥶',
    title: 'Too Cold',
    description: 'Below 45°F',
    ruleType: 'TEMP_BELOW',
    threshold: '45',
    temperatureUnit: 'F',
    monitorCurrent: true,
    monitorForecast: true,
    oncePerEvent: true,
    forecastWindowHours: '48',
    rearmWindowMinutes: '240',
  },
  {
    id: 'too-hot',
    icon: '🔥',
    title: 'Too Hot',
    description: 'Above 90°F',
    ruleType: 'TEMP_ABOVE',
    threshold: '90',
    temperatureUnit: 'F',
    monitorCurrent: true,
    monitorForecast: true,
    oncePerEvent: true,
    forecastWindowHours: '48',
    rearmWindowMinutes: '240',
  },
  {
    id: 'rain-coming',
    icon: '🌧',
    title: 'Rain Coming',
    description: 'Rain ≥ 50%',
    ruleType: 'RAIN',
    threshold: '50',
    monitorCurrent: false,
    monitorForecast: true,
    oncePerEvent: true,
    forecastWindowHours: '48',
    rearmWindowMinutes: '180',
  },
  {
    id: 'windy',
    icon: '💨',
    title: 'Windy',
    description: 'Wind ≥ 25 km/h',
    ruleType: 'WIND',
    threshold: '25',
    monitorCurrent: true,
    monitorForecast: true,
    oncePerEvent: true,
    forecastWindowHours: '48',
    rearmWindowMinutes: '180',
  },
  {
    id: 'storm-risk',
    icon: '⛈',
    title: 'Storm Risk',
    description: 'Rain ≥ 70%',
    ruleType: 'RAIN',
    threshold: '70',
    monitorCurrent: true,
    monitorForecast: true,
    oncePerEvent: true,
    forecastWindowHours: '48',
    rearmWindowMinutes: '120',
  },
  {
    id: 'frost-risk',
    icon: '❄',
    title: 'Frost Risk',
    description: 'Below 36°F (24h)',
    ruleType: 'TEMP_BELOW',
    threshold: '36',
    temperatureUnit: 'F',
    monitorCurrent: false,
    monitorForecast: true,
    oncePerEvent: true,
    forecastWindowHours: '24',
    rearmWindowMinutes: '360',
  },
]

export function RulesPage() {
  const {
    criteria,
    criteriaForm,
    setCriteriaForm,
    canSubmitCriteria,
    savingCriteria,
    busyCriteriaId,
    handleCreateCriteria,
    handleDeleteCriteria,
    handleToggleCriteriaEnabled,
  } = useAppState()
  const location = useLocation()

  const [activePresetId, setActivePresetId] = useState<string | null>(null)
  const [flashPresetFields, setFlashPresetFields] = useState(false)
  const [formErrors, setFormErrors] = useState<RuleFormErrors>({})
  const [advancedExpanded, setAdvancedExpanded] = useState(false)
  const [locationMode, setLocationMode] = useState<LocationMode>('CITY')
  const [useCustomCoordinates, setUseCustomCoordinates] = useState(false)

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'
  const shouldShowCoordinateToggle = locationMode === 'MANUAL'
  const shouldUseCustomCoordinates = shouldShowCoordinateToggle && useCustomCoordinates
  const mapLatitude = Number(criteriaForm.latitude)
  const mapLongitude = Number(criteriaForm.longitude)
  const resolvedLatitude = Number.isNaN(mapLatitude) ? Number(DEFAULT_LAT) : mapLatitude
  const resolvedLongitude = Number.isNaN(mapLongitude) ? Number(DEFAULT_LON) : mapLongitude

  const thresholdHelp = useMemo(() => {
    if (criteriaForm.ruleType === 'TEMP_BELOW') {
      return `Alert when temperature drops below ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
    }
    if (criteriaForm.ruleType === 'TEMP_ABOVE') {
      return `Alert when temperature rises above ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
    }
    if (criteriaForm.ruleType === 'WIND') {
      return `Alert when wind speed goes above ${criteriaForm.threshold || 'X'} km/h.`
    }
    return `Alert when rain chance reaches ${criteriaForm.threshold || 'X'}% or higher.`
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

  function applyPreset(preset: EasyPreset) {
    setCriteriaForm((state) => ({
      ...state,
      name: preset.title,
      ruleType: preset.ruleType,
      threshold: preset.threshold,
      temperatureUnit: preset.temperatureUnit ?? state.temperatureUnit,
      monitorCurrent: preset.monitorCurrent,
      monitorForecast: preset.monitorForecast,
      oncePerEvent: preset.oncePerEvent,
      forecastWindowHours: preset.forecastWindowHours,
      rearmWindowMinutes: preset.rearmWindowMinutes,
    }))
    setActivePresetId(preset.id)
    setFlashPresetFields(true)
    window.setTimeout(() => setFlashPresetFields(false), 900)
  }

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
    } else if (criteriaForm.ruleType === 'TEMP_ABOVE' || criteriaForm.ruleType === 'TEMP_BELOW') {
      if (criteriaForm.temperatureUnit === 'F') {
        if (threshold < -50 || threshold > 140) {
          errors.threshold = 'For Fahrenheit, use a value between -50 and 140.'
        }
      } else if (threshold < -45 || threshold > 60) {
        errors.threshold = 'For Celsius, use a value between -45 and 60.'
      }
    } else if (criteriaForm.ruleType === 'RAIN') {
      if (threshold < 0 || threshold > 100) {
        errors.threshold = 'Rain chance must be between 0 and 100.'
      }
    } else if (threshold < 0) {
      errors.threshold = 'Wind threshold must be zero or greater.'
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

  return (
    <section className="page-stack">
      <article id="easy-alerts" tabIndex={-1} className="panel">
        <div className="panel-title-row">
          <h2>Easy Alerts</h2>
          <span className="muted">Quick presets for common alerts</span>
        </div>
        <div className="easy-alert-grid">
          {EASY_PRESETS.map((preset) => (
            <AriaButton
              key={preset.id}
              className={`easy-alert-card ${activePresetId === preset.id ? 'is-active' : ''}`}
              onPress={() => applyPreset(preset)}
            >
              <p className="easy-alert-title">
                <span className="easy-alert-icon">{preset.icon}</span> {preset.title}
              </p>
              <p className="easy-alert-desc">{preset.description}</p>
            </AriaButton>
          ))}
        </div>
      </article>

      <article id="create-custom-alert" tabIndex={-1} className="panel custom-alert-section">
        <div className="panel-title-row">
          <h2>Create Custom Alert</h2>
        </div>

        <form className="grid-form create-grid" onSubmit={handleCreateAlertSubmit} noValidate>
          <AriaTextField
            label="Alert name"
            className={flashPresetFields ? 'field-flash' : ''}
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
            className={flashPresetFields ? 'field-flash' : ''}
            buttonClassName="aria-select-trigger"
            popoverClassName="aria-select-popover"
            listBoxClassName="aria-select-listbox"
            selectedKey={criteriaForm.ruleType}
            options={RULE_TYPE_OPTIONS}
            onSelectionChange={(value) => {
              const next = value as RuleType
              setCriteriaForm((state) => ({
                ...state,
                ruleType: next,
                threshold: defaultThreshold(next),
              }))
            }}
          />

          <div className={`threshold-row full-row ${flashPresetFields ? 'field-flash' : ''}`}>
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

            {isTemperatureRule ? (
              <AriaSelect
                label="Temperature unit"
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

          <div className={`toggle-row full-row toggle-row-wide ${flashPresetFields ? 'field-flash' : ''}`}>
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
                <div className={`advanced-delivery-grid ${flashPresetFields ? 'field-flash' : ''}`}>
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
      </article>

      <article className="panel">
        <div className="panel-title-row">
          <h2>Active Alert Rules</h2>
          <span className="badge">{criteria.length} active</span>
        </div>
        {criteria.length === 0 ? (
          <p className="muted">No alert rules yet. Create your first one above.</p>
        ) : (
          <div className="criteria-grid">
            {criteria.map((item) => (
              <CriteriaCard
                key={item.id}
                criteria={item}
                busy={busyCriteriaId === item.id}
                onDelete={(criteriaId) => void handleDeleteCriteria(criteriaId)}
                onToggleEnabled={(criteriaId, enabled) => void handleToggleCriteriaEnabled(criteriaId, enabled)}
              />
            ))}
          </div>
        )}
      </article>
    </section>
  )
}
