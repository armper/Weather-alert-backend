import { useAppState } from '../state/useAppState'
import { Link } from 'react-router-dom'
import {
  formatFriendlyLocation,
  formatPercentOrNA,
  formatStatusLabel,
  formatTemperature,
  formatWind,
} from '../lib/formatting'

export function OverviewPage() {
  const { currentWeather, criteria, alerts, me } = useAppState()
  const activeRules = criteria.filter((item) => item.enabled !== false).length

  return (
    <section className="page-grid">
      <article className="panel">
        <h2>Current Conditions</h2>
        {currentWeather ? (
          <div className="weather-stack">
            <p className="weather-location">
              {formatFriendlyLocation(currentWeather.location ?? criteria[0]?.location)}
            </p>
            <p className="weather-temp weather-temp-hero">{formatTemperature(currentWeather.temperature, 'F')}</p>
            <div className="weather-meta weather-secondary-line">
              <span>Wind: {formatWind(currentWeather.windSpeed)}</span>
              <span>Rain chance: {formatPercentOrNA(currentWeather.precipitationProbability)}</span>
            </div>
            <p className="muted small">{currentWeather.headline ?? 'Current NOAA observation'}</p>
          </div>
        ) : (
          <p className="muted">No current weather snapshot available yet.</p>
        )}
      </article>

      <article className="panel stats-grid">
        <Link to="/app/rules" className="stat-card-link">
          <p className="muted small">Active rules</p>
          <p className="weather-temp">{activeRules}</p>
        </Link>
        <Link to="/app/events" className="stat-card-link">
          <p className="muted small">Triggered events</p>
          <p className="weather-temp">{alerts.length}</p>
        </Link>
        <div className="stat-card-link">
          <p className="muted small">Account status</p>
          <p>
            <span className="badge">{formatStatusLabel(me?.approvalStatus)}</span>
          </p>
        </div>
        <div className="stat-card-link">
          <p className="muted small">Email verified</p>
          <p>
            <span className="badge">{me?.emailVerified ? 'Verified' : 'Not verified'}</span>
          </p>
        </div>
      </article>
    </section>
  )
}
