import { Link } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { StaticLocationMap } from '../components/maps/StaticLocationMap'
import { formatFriendlyLocation, formatPercentOrNA, formatTemperature, formatWind } from '../lib/formatting'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'

export function OverviewPage() {
  const { currentWeather, criteria, alerts } = useAppState()
  const activeRules = criteria.filter((item) => item.enabled !== false).length
  const defaultAlertLocation = criteria[0]?.location?.trim() ?? ''
  const resolvedConditionLocation = formatFriendlyLocation(currentWeather?.location ?? defaultAlertLocation)
  const defaultLatitude = criteria[0]?.latitude ?? Number(DEFAULT_LAT)
  const defaultLongitude = criteria[0]?.longitude ?? Number(DEFAULT_LON)
  const defaultRadiusKm = criteria[0]?.radiusKm ?? 8

  return (
    <section className="page-stack">
      <div className="page-grid overview-grid">
        <article className="panel">
          <h2>Current Conditions</h2>
          {currentWeather ? (
            <div className="weather-stack">
              <p className="weather-location">{resolvedConditionLocation}</p>
              <Link to="/app/rules#location-picker" className="weather-map-link" aria-label="Open location picker map">
                <StaticLocationMap
                  latitude={defaultLatitude}
                  longitude={defaultLongitude}
                  radiusKm={defaultRadiusKm}
                  className="weather-map-preview"
                />
              </Link>
              <p className="weather-temp weather-temp-hero">{formatTemperature(currentWeather.temperature, 'F')}</p>
              <div className="weather-meta weather-secondary-line">
                <span>
                  {`Wind ${formatWind(currentWeather.windSpeed)} • Rain ${formatPercentOrNA(
                    currentWeather.precipitationProbability,
                  )}`}
                </span>
              </div>
              <p className="muted small">Conditions: {currentWeather.headline ?? 'Current NOAA observation'}</p>
            </div>
          ) : (
            <p className="muted">No current weather snapshot available yet.</p>
          )}

          {defaultAlertLocation ? (
            <p className="muted small overview-location-note">Alerts for: {formatFriendlyLocation(defaultAlertLocation)}</p>
          ) : (
              <p className="muted small overview-location-note">
              Alerts for: Selected area. <Link to="/app/rules#location-picker">Set location</Link>
            </p>
          )}
        </article>

        <article className="panel overview-hub">
          <div className="panel-title-row">
            <h2>Your alerts</h2>
            <Link to="/app/rules#create-custom-alert" className="primary overview-create-link">
              <span aria-hidden className="overview-create-icon">
                <svg viewBox="0 0 12 12" className="create-plus-svg" focusable="false">
                  <path d="M6 2.25v7.5M2.25 6h7.5" />
                </svg>
              </span>
              <span>Create alert</span>
            </Link>
          </div>
          {activeRules === 0 ? (
            <div className="overview-empty-state">
              <p className="muted">No alerts yet. Create your first alert in 10 seconds.</p>
              <div className="button-row overview-empty-actions">
                <Link to="/app/rules#create-custom-alert" className="primary overview-action-link">
                  Create alert
                </Link>
                <Link to="/app/alerts" className="ghost overview-action-link">
                  Manage alerts
                </Link>
              </div>
            </div>
          ) : (
            <div className="stats-grid overview-stats-grid">
              <Link to="/app/alerts" className="stat-card-link overview-nav-card">
                <p className="muted small">Alerts</p>
                <p className="weather-temp stat-number">{activeRules}</p>
                <p className="muted small stat-caption">active alerts</p>
              </Link>
              <div className="overview-stats-divider" aria-hidden />
              <Link to="/app/events" className="stat-card-link overview-nav-card">
                <p className="muted small">Triggered</p>
                <p className="weather-temp stat-number">{alerts.length}</p>
                <p className="muted small stat-caption">recent alerts</p>
              </Link>
            </div>
          )}
        </article>
      </div>
    </section>
  )
}
