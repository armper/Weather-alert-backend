import { useState } from 'react'
import { useAppState } from '../state/useAppState'
import type { RuleType } from '../state/types'
import { defaultThreshold } from '../lib/criteria'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'

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

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'
  const thresholdHelp =
    criteriaForm.ruleType === 'TEMP_BELOW'
      ? `Alert when temperature drops below ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
      : criteriaForm.ruleType === 'TEMP_ABOVE'
        ? `Alert when temperature rises above ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
        : criteriaForm.ruleType === 'WIND'
          ? `Alert when wind speed goes above ${criteriaForm.threshold || 'X'} km/h.`
          : `Alert when rain chance reaches ${criteriaForm.threshold || 'X'}% or higher.`

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

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Easy Alerts</h2>
          <span className="muted">Quick presets for common alerts</span>
        </div>
        <div className="easy-alert-grid">
          {EASY_PRESETS.map((preset) => (
            <button
              key={preset.id}
              type="button"
              className={`easy-alert-card ${activePresetId === preset.id ? 'is-active' : ''}`}
              onClick={() => applyPreset(preset)}
            >
              <p className="easy-alert-title">
                <span className="easy-alert-icon">{preset.icon}</span> {preset.title}
              </p>
              <p className="easy-alert-desc">{preset.description}</p>
            </button>
          ))}
        </div>
      </article>

      <article className="panel custom-alert-section">
        <div className="panel-title-row">
          <h2>Create Custom Alert</h2>
          <span className="muted">Define focused thresholds for one location</span>
        </div>
        <form className="grid-form create-grid" onSubmit={handleCreateCriteria}>
          <label className={flashPresetFields ? 'field-flash' : ''}>
            Alert name
            <input
              type="text"
              required
              maxLength={120}
              value={criteriaForm.name}
              onChange={(event) => setCriteriaForm((state) => ({ ...state, name: event.target.value }))}
            />
          </label>
          <label>
            Location
            <input
              type="text"
              required
              value={criteriaForm.location}
              onChange={(event) => setCriteriaForm((state) => ({ ...state, location: event.target.value }))}
            />
          </label>

          <label className={flashPresetFields ? 'field-flash' : ''}>
            Rule type
            <select
              value={criteriaForm.ruleType}
              onChange={(event) => {
                const next = event.target.value as RuleType
                setCriteriaForm((state) => ({
                  ...state,
                  ruleType: next,
                  threshold: defaultThreshold(next),
                }))
              }}
            >
              <option value="TEMP_BELOW">Temperature below</option>
              <option value="TEMP_ABOVE">Temperature above</option>
              <option value="WIND">Wind speed above</option>
              <option value="RAIN">Rain probability at/above</option>
            </select>
          </label>

          <div className={`threshold-row full-row ${flashPresetFields ? 'field-flash' : ''}`}>
            <label>
              Threshold
              <input
                type="number"
                required
                value={criteriaForm.threshold}
                onChange={(event) => setCriteriaForm((state) => ({ ...state, threshold: event.target.value }))}
              />
              <span className="muted small">{thresholdHelp}</span>
            </label>
            {isTemperatureRule ? (
              <label>
                Temperature unit
                <select
                  value={criteriaForm.temperatureUnit}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      temperatureUnit: event.target.value as 'F' | 'C',
                    }))
                  }
                >
                  <option value="F">Fahrenheit</option>
                  <option value="C">Celsius</option>
                </select>
              </label>
            ) : null}
          </div>

          <details className="advanced-card full-row">
            <summary>Advanced settings</summary>
            <div className="advanced-grid">
              <label>
                Latitude
                <input
                  type="number"
                  step="0.0001"
                  required
                  value={criteriaForm.latitude}
                  onChange={(event) => setCriteriaForm((state) => ({ ...state, latitude: event.target.value }))}
                />
              </label>
              <label>
                Longitude
                <input
                  type="number"
                  step="0.0001"
                  required
                  value={criteriaForm.longitude}
                  onChange={(event) => setCriteriaForm((state) => ({ ...state, longitude: event.target.value }))}
                />
              </label>
            </div>
          </details>

          <label className={flashPresetFields ? 'field-flash' : ''}>
            Check forecast within (hours)
            <input
              type="number"
              min={1}
              max={168}
              value={criteriaForm.forecastWindowHours}
              onChange={(event) =>
                setCriteriaForm((state) => ({
                  ...state,
                  forecastWindowHours: event.target.value,
                }))
              }
            />
          </label>
          <label className={flashPresetFields ? 'field-flash' : ''}>
            Minimum time between alerts (minutes)
            <input
              type="number"
              min={0}
              value={criteriaForm.rearmWindowMinutes}
              onChange={(event) =>
                setCriteriaForm((state) => ({
                  ...state,
                  rearmWindowMinutes: event.target.value,
                }))
              }
            />
          </label>

          <div className={`toggle-row full-row toggle-row-wide ${flashPresetFields ? 'field-flash' : ''}`}>
            <label className="switch-field">
              <span>Current</span>
              <span className="switch">
                <input
                  type="checkbox"
                  checked={criteriaForm.monitorCurrent}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      monitorCurrent: event.target.checked,
                    }))
                  }
                />
                <span className="switch-slider" />
              </span>
            </label>
            <label className="switch-field">
              <span>Forecast</span>
              <span className="switch">
                <input
                  type="checkbox"
                  checked={criteriaForm.monitorForecast}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      monitorForecast: event.target.checked,
                    }))
                  }
                />
                <span className="switch-slider" />
              </span>
            </label>
            <label className="switch-field">
              <span>Notify once</span>
              <span className="switch">
                <input
                  type="checkbox"
                  checked={criteriaForm.oncePerEvent}
                  onChange={(event) =>
                    setCriteriaForm((state) => ({
                      ...state,
                      oncePerEvent: event.target.checked,
                    }))
                  }
                />
                <span className="switch-slider" />
              </span>
            </label>
          </div>

          <button type="submit" className="primary button-inline" disabled={!canSubmitCriteria || savingCriteria}>
            {savingCriteria ? 'Saving alert...' : 'Create Alert'}
          </button>
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
