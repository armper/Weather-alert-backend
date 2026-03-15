import { Suspense, lazy, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest, toErrorMessage } from '../api'
import { RecentActivityFeed } from '../components/features/dashboard/RecentActivityFeed'
import { WeatherTimeline } from '../components/features/dashboard/WeatherTimeline'
import { TrendSparkline } from '../components/features/dashboard/TrendSparkline'
import { DailyForecastStrip } from '../components/features/dashboard/DailyForecastStrip'
import { HourlyForecastStrip } from '../components/features/dashboard/HourlyForecastStrip'
import { NwsProductsPanel } from '../components/features/dashboard/NwsProductsPanel'
import {
  OverviewLocationSwitcher,
  type OverviewLocationOption,
} from '../components/features/dashboard/OverviewLocationSwitcher'
import { LoadingPlaceholder } from '../components/common/LoadingPlaceholder'
import { reverseGeocode } from '../services/geocoding'
import {
  degreesToCompass,
  formatFriendlyLocation,
  formatPercent,
  formatPercentOrNA,
  formatRelativeTimeCompact,
  formatTemperature,
  formatWind,
} from '../lib/formatting'
import { buildAlertConsoleSummary } from '../lib/alertConsole'
import { buildOverviewDashboardSummary } from '../lib/overviewDashboard'
import { useLiveNow } from '../lib/useLiveNow'
import { buildRuleDashboardSummary } from '../lib/ruleDashboard'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useActionState, useDataState, useSessionState } from '../state/useAppState'
import type { NwsProduct, WeatherCondition } from '../types'

const MonitoringRulesMap = lazy(() =>
  import('../components/maps/MonitoringRulesMap').then((module) => ({ default: module.MonitoringRulesMap })),
)
const StaticLocationMap = lazy(() =>
  import('../components/maps/StaticLocationMap').then((module) => ({ default: module.StaticLocationMap })),
)

interface LocationWeatherBundle {
  currentWeather: WeatherCondition | null
  observationHistory: WeatherCondition[]
  dailyForecast: WeatherCondition[]
  hourlyForecast: WeatherCondition[]
  nwsProducts: NwsProduct[]
}

const OVERVIEW_RECENT_LOCATIONS_KEY = 'skypanda.overview.recent-locations'

function coordinateKey(latitude: number, longitude: number): string {
  return `${latitude.toFixed(4)},${longitude.toFixed(4)}`
}

function createOverviewLocation(
  name: string,
  detail: string,
  latitude: number,
  longitude: number,
  kind: OverviewLocationOption['kind'],
): OverviewLocationOption {
  return {
    id: `${kind}:${coordinateKey(latitude, longitude)}`,
    name,
    detail,
    latitude,
    longitude,
    kind,
  }
}

function readRecentOverviewLocations(): OverviewLocationOption[] {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const raw = window.localStorage.getItem(OVERVIEW_RECENT_LOCATIONS_KEY)
    if (!raw) {
      return []
    }

    const parsed = JSON.parse(raw) as Array<Partial<OverviewLocationOption>>
    return parsed
      .filter(
        (item): item is OverviewLocationOption =>
          typeof item?.id === 'string' &&
          typeof item?.name === 'string' &&
          typeof item?.detail === 'string' &&
          typeof item?.latitude === 'number' &&
          typeof item?.longitude === 'number' &&
          (item?.kind === 'search' || item?.kind === 'device'),
      )
      .slice(0, 6)
  } catch {
    return []
  }
}

function rememberOverviewLocation(
  current: OverviewLocationOption[],
  nextLocation: OverviewLocationOption,
): OverviewLocationOption[] {
  const nextCoordinateKey = coordinateKey(nextLocation.latitude, nextLocation.longitude)
  return [nextLocation, ...current.filter((item) => coordinateKey(item.latitude, item.longitude) !== nextCoordinateKey)].slice(0, 6)
}

function renderMapFallback(title: string, copy: string) {
  return (
    <div className="weather-map-preview static-map-shell">
      <LoadingPlaceholder title={title} copy={copy} compact />
    </div>
  )
}

export function OverviewPage() {
  const { token, initialDataLoading } = useSessionState()
  const { loadNwsProduct } = useActionState()
  const { currentWeather, criteria, alerts, observationHistory, dailyForecast, hourlyForecast, nwsProducts } = useDataState()
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

  const monitoringLatitude = criteria[0]?.latitude ?? Number(DEFAULT_LAT)
  const monitoringLongitude = criteria[0]?.longitude ?? Number(DEFAULT_LON)
  const monitoringRadiusKm = criteria[0]?.radiusKm ?? 8
  const monitoringLocation = useMemo(
    () =>
      createOverviewLocation(
        criteria[0]?.location?.trim() || currentWeather?.location?.trim() || 'Monitored area',
        'Your saved alert watch area',
        monitoringLatitude,
        monitoringLongitude,
        'monitoring',
      ),
    [criteria, currentWeather?.location, monitoringLatitude, monitoringLongitude],
  )

  const [selectedViewLocation, setSelectedViewLocation] = useState<OverviewLocationOption | null>(null)
  const [recentViewLocations, setRecentViewLocations] = useState<OverviewLocationOption[]>(() => readRecentOverviewLocations())
  const [alternateWeatherBundle, setAlternateWeatherBundle] = useState<LocationWeatherBundle | null>(null)
  const [alternateWeatherLoading, setAlternateWeatherLoading] = useState(false)
  const [alternateWeatherError, setAlternateWeatherError] = useState<string | null>(null)
  const [resolvingCurrentLocation, setResolvingCurrentLocation] = useState(false)
  const weatherBundleCacheRef = useRef<Map<string, LocationWeatherBundle>>(new Map())

  const effectiveSelectedViewLocation =
    selectedViewLocation &&
    coordinateKey(selectedViewLocation.latitude, selectedViewLocation.longitude) !==
      coordinateKey(monitoringLocation.latitude, monitoringLocation.longitude)
      ? selectedViewLocation
      : null
  const activeLocation = effectiveSelectedViewLocation ?? monitoringLocation
  const isViewingAlternate = effectiveSelectedViewLocation != null

  useEffect(() => {
    if (typeof window === 'undefined') {
      return
    }
    window.localStorage.setItem(OVERVIEW_RECENT_LOCATIONS_KEY, JSON.stringify(recentViewLocations))
  }, [recentViewLocations])

  useEffect(() => {
    if (!token || !effectiveSelectedViewLocation || !isViewingAlternate) {
      return
    }

    const cacheKey = coordinateKey(effectiveSelectedViewLocation.latitude, effectiveSelectedViewLocation.longitude)
    const cachedBundle = weatherBundleCacheRef.current.get(cacheKey)
    if (cachedBundle) {
      return
    }

    let cancelled = false

    const loadAlternateWeather = async () => {
      try {
        const [nextCurrentWeather, nextObservationHistory, nextDailyForecast, nextHourlyForecast, nextNwsProducts] =
          await Promise.all([
            apiRequest<WeatherCondition>(
              `/api/weather/conditions/current?latitude=${encodeURIComponent(String(effectiveSelectedViewLocation.latitude))}&longitude=${encodeURIComponent(String(effectiveSelectedViewLocation.longitude))}`,
              { token },
            ).catch(() => null),
            apiRequest<WeatherCondition[]>(
              `/api/weather/conditions/history?latitude=${encodeURIComponent(String(effectiveSelectedViewLocation.latitude))}&longitude=${encodeURIComponent(String(effectiveSelectedViewLocation.longitude))}&hours=6`,
              { token },
            ).catch(() => [] as WeatherCondition[]),
            apiRequest<WeatherCondition[]>(
              `/api/weather/conditions/daily?latitude=${encodeURIComponent(String(effectiveSelectedViewLocation.latitude))}&longitude=${encodeURIComponent(String(effectiveSelectedViewLocation.longitude))}`,
              { token },
            ).catch(() => [] as WeatherCondition[]),
            apiRequest<WeatherCondition[]>(
              `/api/weather/conditions/forecast?latitude=${encodeURIComponent(String(effectiveSelectedViewLocation.latitude))}&longitude=${encodeURIComponent(String(effectiveSelectedViewLocation.longitude))}&hours=24`,
              { token },
            ).catch(() => [] as WeatherCondition[]),
            apiRequest<NwsProduct[]>(
              `/api/weather/products?type=AFD&latitude=${encodeURIComponent(String(effectiveSelectedViewLocation.latitude))}&longitude=${encodeURIComponent(String(effectiveSelectedViewLocation.longitude))}`,
              { token },
            ).catch(() => [] as NwsProduct[]),
          ])

        if (cancelled) {
          return
        }

        const nextBundle = {
          currentWeather: nextCurrentWeather,
          observationHistory: nextObservationHistory,
          dailyForecast: nextDailyForecast,
          hourlyForecast: nextHourlyForecast,
          nwsProducts: nextNwsProducts,
        }

        weatherBundleCacheRef.current.set(cacheKey, nextBundle)
        setAlternateWeatherBundle(nextBundle)
      } catch (error) {
        if (cancelled) {
          return
        }
        setAlternateWeatherError(toErrorMessage(error))
      } finally {
        if (!cancelled) {
          setAlternateWeatherLoading(false)
        }
      }
    }

    void loadAlternateWeather()

    return () => {
      cancelled = true
    }
  }, [effectiveSelectedViewLocation, isViewingAlternate, token])

  const selectOverviewLocation = useCallback(
    (location: OverviewLocationOption) => {
      if (coordinateKey(location.latitude, location.longitude) === coordinateKey(monitoringLocation.latitude, monitoringLocation.longitude)) {
        setSelectedViewLocation(null)
        setAlternateWeatherBundle(null)
        setAlternateWeatherLoading(false)
        setAlternateWeatherError(null)
        return
      }

      const cacheKey = coordinateKey(location.latitude, location.longitude)
      const cachedBundle = weatherBundleCacheRef.current.get(cacheKey) ?? null
      setSelectedViewLocation(location)
      setAlternateWeatherBundle(cachedBundle)
      setAlternateWeatherLoading(cachedBundle == null)
      setRecentViewLocations((current) => rememberOverviewLocation(current, location))
      setAlternateWeatherError(null)
    },
    [monitoringLocation.latitude, monitoringLocation.longitude],
  )

  const handleResetToMonitoring = useCallback(() => {
    setSelectedViewLocation(null)
    setAlternateWeatherBundle(null)
    setAlternateWeatherLoading(false)
    setAlternateWeatherError(null)
  }, [])

  const handleUseCurrentLocation = useCallback(async () => {
    if (!('geolocation' in navigator)) {
      setAlternateWeatherError('This browser cannot share your current location.')
      return
    }

    setResolvingCurrentLocation(true)
    setAlternateWeatherError(null)

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const latitude = position.coords.latitude
        const longitude = position.coords.longitude
        const reversePlace = await reverseGeocode(latitude, longitude).catch(() => null)
        const location = createOverviewLocation(
          reversePlace?.name ?? `Current location (${latitude.toFixed(3)}, ${longitude.toFixed(3)})`,
          reversePlace?.displayName ?? 'Using your device location',
          latitude,
          longitude,
          'device',
        )
        selectOverviewLocation(location)
        setResolvingCurrentLocation(false)
      },
      (error) => {
        const nextMessage =
          error.code === error.PERMISSION_DENIED
            ? 'Location access was blocked. Allow browser location access to use this shortcut.'
            : 'Unable to determine your current location right now.'
        setAlternateWeatherError(nextMessage)
        setResolvingCurrentLocation(false)
      },
      {
        enableHighAccuracy: false,
        timeout: 10_000,
        maximumAge: 300_000,
      },
    )
  }, [selectOverviewLocation])

  const loadDisplayedNwsProduct = useCallback(
    async (productId: string) => {
      if (!isViewingAlternate || !token || !effectiveSelectedViewLocation) {
        return loadNwsProduct(productId)
      }

      const cacheKey = coordinateKey(effectiveSelectedViewLocation.latitude, effectiveSelectedViewLocation.longitude)
      const existing = alternateWeatherBundle?.nwsProducts.find((item) => item.id === productId)
      if (existing?.productText?.trim()) {
        return existing
      }

      const product = await apiRequest<NwsProduct>(`/api/weather/products/${encodeURIComponent(productId)}`, {
        token,
      })

      setAlternateWeatherBundle((current) => {
        if (!current) {
          return current
        }
        const nextBundle = {
          ...current,
          nwsProducts: current.nwsProducts.map((item) => (item.id === productId ? { ...item, ...product } : item)),
        }
        weatherBundleCacheRef.current.set(cacheKey, nextBundle)
        return nextBundle
      })

      return product
    },
    [alternateWeatherBundle, effectiveSelectedViewLocation, isViewingAlternate, loadNwsProduct, token],
  )

  const displayCurrentWeather = isViewingAlternate ? alternateWeatherBundle?.currentWeather ?? null : currentWeather
  const displayObservationHistory = isViewingAlternate ? alternateWeatherBundle?.observationHistory ?? [] : observationHistory
  const displayDailyForecast = isViewingAlternate ? alternateWeatherBundle?.dailyForecast ?? [] : dailyForecast
  const displayHourlyForecast = isViewingAlternate ? alternateWeatherBundle?.hourlyForecast ?? [] : hourlyForecast
  const displayNwsProducts = isViewingAlternate ? alternateWeatherBundle?.nwsProducts ?? [] : nwsProducts
  const displayWeatherLoading = isViewingAlternate && alternateWeatherLoading && !alternateWeatherBundle

  const displayLocationStatusMessage = useMemo(() => {
    if (alternateWeatherError) {
      return alternateWeatherError
    }
    if (resolvingCurrentLocation) {
      return 'Finding your device location…'
    }
    if (isViewingAlternate && alternateWeatherLoading) {
      return `Refreshing weather for ${formatFriendlyLocation(activeLocation.name)}…`
    }
    if (isViewingAlternate) {
      return `Showing weather for ${formatFriendlyLocation(activeLocation.name)} while alerts and recent activity stay tied to ${formatFriendlyLocation(monitoringLocation.name)}.`
    }
    return null
  }, [activeLocation.name, alternateWeatherError, alternateWeatherLoading, isViewingAlternate, monitoringLocation.name, resolvingCurrentLocation])

  const displayLatitude = activeLocation.latitude
  const displayLongitude = activeLocation.longitude
  const resolvedConditionLocation = formatFriendlyLocation(displayCurrentWeather?.location ?? activeLocation.name)
  const currentHeadline = displayCurrentWeather?.headline?.trim() || 'Current NOAA observation'

  const tempHistory = displayObservationHistory.map((item) => item.temperature)
  const windHistory = displayObservationHistory.map((item) => item.windSpeed)
  const humidityHistory = displayObservationHistory.map((item) => item.humidity)
  const tempValues = tempHistory.filter((value): value is number => value != null)
  const rapidTempChange =
    tempValues.length >= 2 && Math.abs(tempValues[tempValues.length - 1] - tempValues[0]) > 5

  const showHeatWarning = displayCurrentWeather?.heatIndex != null && displayCurrentWeather.heatIndex > 37.8
  const showWindChillWarning = displayCurrentWeather?.windChill != null && displayCurrentWeather.windChill < -12.2
  const displayFreshnessLabel =
    displayCurrentWeather?.timestamp != null
      ? `Updated ${formatRelativeTimeCompact(displayCurrentWeather.timestamp, now)}`
      : isViewingAlternate
        ? `Viewing ${formatFriendlyLocation(activeLocation.name)}`
        : dashboard.freshnessLabel

  if (initialDataLoading) {
    return (
      <section className="page-stack">
        <div className="page-grid overview-grid">
          <article className="panel">
            <LoadingPlaceholder
              title="Loading current conditions"
              copy="Fetching your watch area, live weather snapshot, and alert summaries."
              lineCount={3}
            />
          </article>
          <article className="panel overview-activity-panel">
            <LoadingPlaceholder
              title="Loading recent activity"
              copy="SkyPanda is gathering your latest alerts and timeline events."
              lineCount={4}
            />
          </article>
        </div>
      </section>
    )
  }

  return (
    <section className="page-stack">
      <OverviewLocationSwitcher
        monitoringLocation={monitoringLocation}
        activeLocation={activeLocation}
        recentLocations={recentViewLocations}
        onSelectLocation={selectOverviewLocation}
        onUseCurrentLocation={handleUseCurrentLocation}
        onResetToMonitoring={handleResetToMonitoring}
        loadingLocationData={alternateWeatherLoading}
        resolvingCurrentLocation={resolvingCurrentLocation}
        statusMessage={displayLocationStatusMessage}
      />

      <div className="page-grid overview-grid">
        <article className="panel">
          <div className="panel-title-row overview-title-row">
            <div>
              <p className="eyebrow">{isViewingAlternate ? 'Explore View' : 'Watch Area'}</p>
              <h2>Current Conditions</h2>
              <div className="overview-monitoring-indicator" aria-live="polite">
                <span className="overview-monitoring-dot" aria-hidden />
                <span>{dashboard.monitoringLabel}</span>
              </div>
            </div>
            <span className="badge overview-status-badge">
              {summary.allClear ? 'All clear' : isViewingAlternate ? `Monitoring ${summary.activeCount} alerts` : `${summary.activeCount} active alerts`}
            </span>
          </div>
          {displayWeatherLoading ? (
            <LoadingPlaceholder
              title={`Loading ${formatFriendlyLocation(activeLocation.name)}`}
              copy="Pulling the latest conditions, forecast timeline, and local NWS discussion."
              lineCount={3}
              compact
            />
          ) : displayCurrentWeather ? (
            <div className="weather-stack overview-weather-stack">
              <div className="overview-monitoring-strip">
                <span>{displayFreshnessLabel}</span>
                {summary.allClear && summary.calmStreakLabel ? <span>{`Calm streak ${summary.calmStreakLabel}`}</span> : null}
                {!summary.allClear ? <span>{`${summary.activeCount} active alert${summary.activeCount === 1 ? '' : 's'}`}</span> : null}
              </div>
              {isViewingAlternate ? (
                <div className="overview-viewing-banner">
                  <span className="overview-viewing-chip">{`Viewing ${formatFriendlyLocation(activeLocation.name)}`}</span>
                  <span>{`Alerts still monitor ${formatFriendlyLocation(monitoringLocation.name)}`}</span>
                </div>
              ) : null}
              {!isViewingAlternate ? (
                <Link to="/app/rules#location-picker" className="weather-map-link" aria-label="Open location picker map">
                  <Suspense fallback={renderMapFallback('Loading map', 'Rendering watch area preview.')}>
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
                        latitude={displayLatitude}
                        longitude={displayLongitude}
                        radiusKm={monitoringRadiusKm}
                        className="weather-map-preview"
                        ruleCount={dashboard.mapRuleCount}
                      />
                    )}
                  </Suspense>
                </Link>
              ) : (
                <div className="weather-map-link is-static" aria-label={`Map preview for ${activeLocation.name}`}>
                  <Suspense fallback={renderMapFallback('Loading map', `Rendering ${formatFriendlyLocation(activeLocation.name)}.`)}>
                    <StaticLocationMap
                      latitude={displayLatitude}
                      longitude={displayLongitude}
                      radiusKm={monitoringRadiusKm}
                      className="weather-map-preview"
                      ruleCount={1}
                    />
                  </Suspense>
                </div>
              )}
              <p className="weather-temp weather-temp-hero">{formatTemperature(displayCurrentWeather.temperature, 'F')}</p>
              {displayCurrentWeather.apparentTemperature != null ? (
                <p className="weather-feels-like">Feels like {formatTemperature(displayCurrentWeather.apparentTemperature, 'F')}</p>
              ) : null}
              {showHeatWarning ? (
                <div className="weather-warning-strip weather-warning-heat" role="alert">
                  🔥 Heat index {formatTemperature(displayCurrentWeather.heatIndex, 'F')} - take precautions
                </div>
              ) : null}
              {showWindChillWarning ? (
                <div className="weather-warning-strip weather-warning-cold" role="alert">
                  ❄️ Wind chill {formatTemperature(displayCurrentWeather.windChill, 'F')} - bundle up
                </div>
              ) : null}
              <p className="overview-headline">{currentHeadline}</p>
              <p className="weather-location">{resolvedConditionLocation}</p>
              <div className="weather-meta overview-metric-row">
                <span className="metric-pill overview-metric-pill">{`Wind ${formatWind(displayCurrentWeather.windSpeed)}`}</span>
                <span className="metric-pill overview-metric-pill">
                  {`Rain ${formatPercentOrNA(displayCurrentWeather.precipitationProbability)}`}
                </span>
                {displayCurrentWeather.humidity != null ? (
                  <span className="metric-pill overview-metric-pill">{`Humidity ${formatPercent(displayCurrentWeather.humidity)}`}</span>
                ) : null}
                {displayCurrentWeather.visibility != null ? (
                  <span className="metric-pill overview-metric-pill">{`👁 Vis ${displayCurrentWeather.visibility.toFixed(1)} km`}</span>
                ) : null}
                {displayCurrentWeather.windDirection != null ? (
                  <span className="metric-pill overview-metric-pill">{`🧭 ${degreesToCompass(displayCurrentWeather.windDirection)}`}</span>
                ) : null}
              </div>
              {displayObservationHistory.length > 1 ? (
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

              <section className="overview-watch-section">
                <div className="panel-title-row overview-watch-header">
                  <h3>{isViewingAlternate ? 'Monitoring rules' : 'Watching for'}</h3>
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

      {!displayWeatherLoading && displayHourlyForecast.length > 0 ? <HourlyForecastStrip items={displayHourlyForecast} /> : null}

      {!displayWeatherLoading && displayNwsProducts.length > 0 ? (
        <NwsProductsPanel products={displayNwsProducts} onLoadProduct={loadDisplayedNwsProduct} />
      ) : null}

      {!displayWeatherLoading && displayDailyForecast.length > 0 ? <DailyForecastStrip items={displayDailyForecast} /> : null}

      <WeatherTimeline items={dashboard.timeline} />
    </section>
  )
}
