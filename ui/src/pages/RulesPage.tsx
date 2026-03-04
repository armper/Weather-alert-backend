import { useAppState } from '../state/useAppState'
import type { RuleType } from '../state/types'
import { defaultThreshold } from '../lib/criteria'
import { CriteriaCard } from '../components/features/dashboard/CriteriaCard'

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

  const isTemperatureRule = criteriaForm.ruleType === 'TEMP_BELOW' || criteriaForm.ruleType === 'TEMP_ABOVE'
  const thresholdHelp =
    criteriaForm.ruleType === 'TEMP_BELOW'
      ? `Alert when temperature drops below ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
      : criteriaForm.ruleType === 'TEMP_ABOVE'
        ? `Alert when temperature rises above ${criteriaForm.threshold || 'X'}°${criteriaForm.temperatureUnit}.`
        : criteriaForm.ruleType === 'WIND'
          ? `Alert when wind speed goes above ${criteriaForm.threshold || 'X'} km/h.`
          : `Alert when rain chance reaches ${criteriaForm.threshold || 'X'}% or higher.`

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Create Alert Rule</h2>
          <span className="muted">Define focused thresholds for one location</span>
        </div>
        <form className="grid-form create-grid" onSubmit={handleCreateCriteria}>
          <label>
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

          <label>
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

          <div className="threshold-row full-row">
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

          <label>
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
          <label>
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

          <div className="toggle-row full-row toggle-row-wide">
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
            {savingCriteria ? 'Saving rule...' : 'Create alert rule'}
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
