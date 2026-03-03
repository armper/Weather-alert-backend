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
  } = useAppState()

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

          <label>
            Threshold
            <input
              type="number"
              required
              value={criteriaForm.threshold}
              onChange={(event) => setCriteriaForm((state) => ({ ...state, threshold: event.target.value }))}
            />
          </label>

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
          <label>
            Forecast window (hours)
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
            Rearm window (minutes)
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

          <div className="toggle-row full-row">
            <label className="checkbox-pill">
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
              Current conditions
            </label>
            <label className="checkbox-pill">
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
              Forecast conditions
            </label>
            <label className="checkbox-pill">
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
              Notify once per event
            </label>
          </div>

          <button type="submit" className="primary full-row" disabled={!canSubmitCriteria || savingCriteria}>
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
              />
            ))}
          </div>
        )}
      </article>
    </section>
  )
}
