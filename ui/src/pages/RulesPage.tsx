import { useMemo, useState, type FormEvent } from 'react'
import { useAppState } from '../state/useAppState'
import type { RuleType } from '../state/types'
import { defaultThreshold } from '../lib/criteria'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'

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

  const [activePresetId, setActivePresetId] = useState<string | null>(null)
  const [flashPresetFields, setFlashPresetFields] = useState(false)
  const [formErrors, setFormErrors] = useState<RuleFormErrors>({})
  const [showAdvanced, setShowAdvanced] = useState(false)

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'

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
    setShowAdvanced(true)
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

    const latitude = Number(criteriaForm.latitude)
    if (Number.isNaN(latitude) || latitude < -90 || latitude > 90) {
      errors.latitude = 'Latitude must be between -90 and 90.'
    }

    const longitude = Number(criteriaForm.longitude)
    if (Number.isNaN(longitude) || longitude < -180 || longitude > 180) {
      errors.longitude = 'Longitude must be between -180 and 180.'
    }

    return errors
  }

  function handleCreateAlertSubmit(event: FormEvent<HTMLFormElement>) {
    const errors = validateRuleForm()
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) {
      event.preventDefault()
      return
    }
    void handleCreateCriteria(event)
  }

  return (
    <section className="page-stack">
      <article className="panel">
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

      <article className="panel custom-alert-section">
        <div className="panel-title-row">
          <h2>Create Custom Alert</h2>
          <span className="muted">Define focused thresholds for one location</span>
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

          <AriaTextField
            label="Location"
            inputClassName="aria-input"
            value={criteriaForm.location}
            required
            errorMessage={formErrors.location}
            onChange={(value) => setCriteriaForm((state) => ({ ...state, location: value }))}
          />

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

          <div className="advanced-card full-row">
            <AriaButton className="ghost button-inline" onPress={() => setShowAdvanced((state) => !state)}>
              {showAdvanced ? 'Hide advanced settings' : 'Show advanced settings'}
            </AriaButton>
            {showAdvanced ? (
              <div className="advanced-grid">
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
            ) : null}
          </div>

          <AriaTextField
            label="Check forecast within (hours)"
            className={flashPresetFields ? 'field-flash' : ''}
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

          <AriaTextField
            label="Minimum time between alerts (minutes)"
            className={flashPresetFields ? 'field-flash' : ''}
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

          <AriaButton type="submit" className="primary button-inline" isDisabled={!canSubmitCriteria || savingCriteria}>
            {savingCriteria ? 'Saving alert...' : 'Create Alert'}
          </AriaButton>
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

