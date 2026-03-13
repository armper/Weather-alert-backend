import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { RecentActivityFeed } from '../components/features/dashboard/RecentActivityFeed'
import { WeatherTimeline } from '../components/features/dashboard/WeatherTimeline'
import { TrendSparkline } from '../components/features/dashboard/TrendSparkline'
import { DailyForecastStrip } from '../components/features/dashboard/DailyForecastStrip'
import { HourlyForecastStrip } from '../components/features/dashboard/HourlyForecastStrip'
import { StaticLocationMap } from '../components/maps/StaticLocationMap'
import { MonitoringRulesMap } from '../components/maps/MonitoringRulesMap'
import {
  degreesToCompass,
  formatFriendlyLocation,
  formatPercentOrNA,
  formatTemperature,
  formatWind,
} from '../lib/formatting'
import { buildAlertConsoleSummary } from '../lib/alertConsole'
import { buildOverviewDashboardSummary } from '../lib/overviewDashboard'
import { useLiveNow } from '../lib/useLiveNow'
import { buildRuleDashboardSummary } from '../lib/ruleDashboard'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'

export function OverviewPage() {
  const { currentWeather, criteria, alerts, observationHistory, dailyForecast, hourlyForecast } = useAppState()
  const now = useLiveNow(20_000)
  const summary = useMemo(() => buildAlertConsoleSummary(criteria, alerts, currentWeather, now), [criteria, alerts, currentWeather, now])
  const dashboard = useMemo(
    () => buildOverviewDashboardSummary(criteria, alerts, currentWeather, now),
    [criteria, alerts, currentWeather, now],
  )
  const ruleSummary = useMemo(
    () => buildRuleDashboardSummary(criteria, alerts, currentWeather, now),
    [criteria, alerts, currentWeather, now],
  )
  const defaultAlertLocation = criteria[0]?.location?.trim() ?? ''
  const resolvedConditionLocation = formatFriendlyLocation(currentWeather?.location ?? defaultAlertLocation)
  const defaultLatitude = criteria[0]?.latitude ?? Number(DEFAULT_LAT)
  const defaultLongitude = criteria[0]?.longitude ?? Number(DEFAULT_LON)
  const defaultRadiusKm = criteria[0]?.radiusKm ?? 8
  const currentHeadline = currentWeather?.headline?.trim() || 'Current NOAA observation'

  const tempHistory = observationHistory.map((item) => item.temperature)
  const windHistory = observationHistory.map((item) => item.windSpeed)
  const humidityHistory = observationHistory.map((item) => item.humidity)
  const tempValues = tempHistory.filter((value): value is number => value != null)
  const rapidTempChange =
    tempValues.length >= 2 && Math.abs(tempValues[tempValues.length - 1] - tempValues[0]) > 5

  const showHeatWarning = currentWeather?.heatIndex != null && currentWeather.heatIndex > 37.8
  const showWindChillWarning = currentWeather?.windChill != null && currentWeather.windChill < -12.2

  return (
    <section className="page-stack">
      <div className="page-grid overview-grid">
        <article className="panel">
          <div className="panel-title-row overview-title-row">
            <div>
              <p className="eyebrow">Watch Area</p>
              <h2>Current Conditions</h2>
              <div className="overview-monitoring-indicator" aria-live="polite">
                <span className="overview-monitoring-dot" aria-hidden />
                <span>{dashboard.monitoringLabel}</span>
              </div>
            </div>
            <span className="badge overview-status-badge">
              {summary.allClear ? 'All clear' : `${summary.activeCount} active alerts`}
            </span>
          </div>
          {currentWeather ? (
            <div className="weather-stack overview-weather-stack">
              <div className="overview-monitoring-strip">
                <span>{dashboard.freshnessLabel}</span>
                {summary.allClear && summary.calmStreakLabel ? <span>{`Calm streak ${summary.calmStreakLabel}`}</span> : null}
                {!summary.allClear ? <span>{`${summary.activeCount} active alert${summary.activeCount === 1 ? '' : 's'}`}</span> : null}
              </div>
              <Link to="/app/rules#location-picker" className="weather-map-link" aria-label="Open location picker map">
                {ruleSummary.locationGroups.length > 0 ? (
                  <MonitoringRulesMap
                    groups={ruleSummary.locationGroups}
                    selectedGroupId={ruleSummary.locationGroups.find((item) => item.statusTone === 'critical')?.id ?? null}
                    compact
                    interactive={false}
                    className="weather-map-preview"
                    ariaLabel="Monitored locations overview"
                  />
                ) : (
                  <StaticLocationMap
                    latitude={defaultLatitude}
                    longitude={defaultLongitude}
                    radiusKm={defaultRadiusKm}
                    className="weather-map-preview"
                    ruleCount={dashboard.mapRuleCount}
                  />
                )}
              </Link>
              <p className="weather-temp weather-temp-hero">{formatTemperature(currentWeather.temperature, 'F')}</p>
              {currentWeather.apparentTemperature != null ? (
                <p className="weather-feels-like">Feels like {formatTemperature(currentWeather.apparentTemperature, 'F')}</p>
              ) : null}
              {showHeatWarning ? (
                <div className="weather-warning-strip weather-warning-heat" role="alert">
                  🔥 Heat index {formatTemperature(currentWeather.heatIndex, 'F')} — take precautions
                </div>
              ) : null}
              {showWindChillWarning ? (
                <div className="weather-warning-strip weather-warning-cold" role="alert">
                  ❄️ Wind chill {formatTemperature(currentWeather.windChill, 'F')} — bundle up
                </div>
              ) : null}
              <p className="overview-headline">{currentHeadline}</p>
              <p className="weather-location">{resolvedConditionLocation}</p>
              <div className="weather-meta overview-metric-row">
                <span className="metric-pill overview-metric-pill">{`Wind ${formatWind(currentWeather.windSpeed)}`}</span>
                <span className="metric-pill overview-metric-pill">
                  {`Rain ${formatPercentOrNA(currentWeather.precipitationProbability)}`}
                </span>
                {currentWeather.humidity != null ? (
                  <span className="metric-pill overview-metric-pill">{`Humidity ${currentWeather.humidity}%`}</span>
                ) : null}
                {currentWeather.visibility != null ? (
                  <span className="metric-pill overview-metric-pill">{`👁 Vis ${currentWeather.visibility.toFixed(1)} km`}</span>
                ) : null}
                {currentWeather.windDirection != null ? (
                  <span className="metric-pill overview-metric-pill">{`🧭 ${degreesToCompass(currentWeather.windDirection)}`}</span>
                ) : null}
              </div>
              {observationHistory.length > 1 ? (
                <div className="overview-trends">
                  {rapidTempChange ? (
                    <span className="overview-rapid-change-badge" title="Rapid temperature change in the past 6 hours">
                      ⚡ Rapid change
                    </span>
                  ) : null}
                  <TrendSparkline label="Temp" data={tempHistory} unit="°" />
                  <TrendSparkline label="Wind" data={windHistory} unit=" km/h" />
                  <TrendSparkline label="Humidity" data={humidityHistory} unit="%" />
                </div>
              ) : null}
              {hourlyForecast.length > 0 ? (
                <HourlyForecastStrip items={hourlyForecast} />
              ) : null}
              <section className="overview-watch-section">
                <div className="panel-title-row overview-watch-header">
                  <h3>Watching for</h3>
                  <Link to="/app/alerts" className="auth-link overview-watch-link">
                    Manage rules
                  </Link>
                </div>
                {dashboard.watchRules.length > 0 ? (
                  <div className="overview-watch-context">
                    {dashboard.watchRules.map((item) => (
                      <Link key={item.id} to={item.href} className="overview-watch-chip">
                        <span aria-hidden>{item.icon}</span>
                        <span>{item.label}</span>
                      </Link>
                    ))}
                  </div>
                ) : (
                  <p className="muted small overview-location-note">No alert rules are watching this area yet.</p>
                )}
              </section>
            </div>
          ) : (
            <p className="muted">No current weather snapshot available yet.</p>
          )}
        </article>

        <RecentActivityFeed items={dashboard.recentActivity} calmLabel={summary.allClear ? dashboard.calmLabel : undefined} now={now} />
      </div>

      <WeatherTimeline items={dashboard.timeline} />

      {dailyForecast.length > 0 ? <DailyForecastStrip items={dailyForecast} /> : null}
    </section>
  )
}

