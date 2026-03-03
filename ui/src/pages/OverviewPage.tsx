import { useAppState } from '../state/useAppState'
import { formatPercent, formatTemperature, formatWind } from '../lib/formatting'

export function OverviewPage() {
  const { currentWeather, criteria, alerts, me } = useAppState()

  return (
    <section className="page-grid">
      <article className="panel">
        <h2>Current Conditions</h2>
        {currentWeather ? (
          <div className="weather-stack">
            <p className="weather-location">{currentWeather.location ?? 'Selected area'}</p>
            <p className="weather-temp">{formatTemperature(currentWeather.temperature, 'F')}</p>
            <div className="weather-meta">
              <span>Wind: {formatWind(currentWeather.windSpeed)}</span>
              <span>Rain chance: {formatPercent(currentWeather.precipitationProbability)}</span>
            </div>
            <p className="muted small">{currentWeather.headline ?? 'Current NOAA observation'}</p>
          </div>
        ) : (
          <p className="muted">No current weather snapshot available yet.</p>
        )}
      </article>

      <article className="panel stats-grid">
        <div>
          <p className="muted small">Active rules</p>
          <p className="weather-temp">{criteria.length}</p>
        </div>
        <div>
          <p className="muted small">Triggered events</p>
          <p className="weather-temp">{alerts.length}</p>
        </div>
        <div>
          <p className="muted small">Account status</p>
          <p>{me?.approvalStatus ?? 'Unknown'}</p>
        </div>
        <div>
          <p className="muted small">Email verified</p>
          <p>{me?.emailVerified ? 'Yes' : 'No'}</p>
        </div>
      </article>
    </section>
  )
}
