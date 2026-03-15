import { useEffect, useMemo, useRef, useState } from 'react'
import { searchPlaces } from '../../../services/geocoding'
import { formatFriendlyLocation } from '../../../lib/formatting'
import { AriaButton } from '../../ui/AriaButton'

export interface OverviewLocationOption {
  id: string
  name: string
  detail: string
  latitude: number
  longitude: number
  kind: 'monitoring' | 'search' | 'device'
}

interface OverviewLocationSwitcherProps {
  monitoringLocation: OverviewLocationOption
  activeLocation: OverviewLocationOption
  recentLocations: OverviewLocationOption[]
  onSelectLocation: (location: OverviewLocationOption) => void
  onUseCurrentLocation: () => Promise<void> | void
  onResetToMonitoring: () => void
  loadingLocationData: boolean
  resolvingCurrentLocation: boolean
  statusMessage?: string | null
}

export function OverviewLocationSwitcher({
  monitoringLocation,
  activeLocation,
  recentLocations,
  onSelectLocation,
  onUseCurrentLocation,
  onResetToMonitoring,
  resolvingCurrentLocation,
  statusMessage,
}: OverviewLocationSwitcherProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<OverviewLocationOption[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const searchInputRef = useRef<HTMLInputElement | null>(null)

  const isViewingAlternate = activeLocation.id !== monitoringLocation.id
  const recentLocationsToShow = useMemo(
    () => recentLocations.filter((item) => item.id !== activeLocation.id).slice(0, 4),
    [activeLocation.id, recentLocations],
  )

  useEffect(() => {
    if (!isOpen) {
      return
    }

    const timeoutId = window.setTimeout(() => {
      searchInputRef.current?.focus()
    }, 30)

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    window.addEventListener('keydown', handleEscape)

    return () => {
      window.clearTimeout(timeoutId)
      window.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen])

  async function handleSearch() {
    if (!query.trim()) {
      setResults([])
      return
    }

    setSearching(true)
    setSearchError(null)

    try {
      const places = await searchPlaces(query)
      const mapped = places.map((place) => ({
        id: `search:${place.latitude.toFixed(4)},${place.longitude.toFixed(4)}`,
        name: place.name,
        detail: place.displayName,
        latitude: place.latitude,
        longitude: place.longitude,
        kind: 'search' as const,
      }))
      setResults(mapped)
      if (mapped.length === 0) {
        setSearchError('No matching location found. Try a city, ZIP code, or landmark.')
      }
    } catch {
      setSearchError('Location search is unavailable right now. Try again in a moment.')
    } finally {
      setSearching(false)
    }
  }

  function applySelection(location: OverviewLocationOption) {
    onSelectLocation(location)
    setQuery('')
    setResults([])
    setSearchError(null)
    setIsOpen(false)
  }

  return (
    <>
      <AriaButton className="overview-location-inline-trigger" onPress={() => setIsOpen(true)}>
        Change location
      </AriaButton>

      {isOpen ? (
        <div className="overview-location-dialog-backdrop" role="presentation" onClick={() => setIsOpen(false)}>
          <div
            className="overview-location-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="overview-location-dialog-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="overview-location-dialog-header">
              <div>
                <p className="eyebrow">Explore Forecasts</p>
                <h2 id="overview-location-dialog-title">Check another location</h2>
                <p className="overview-location-dialog-copy">
                  Browse live weather and forecast discussions somewhere else without changing the places SkyPanda is actively watching.
                </p>
              </div>
              <AriaButton className="ghost button-inline overview-location-close" onPress={() => setIsOpen(false)}>
                Done
              </AriaButton>
            </div>

            <div className="overview-location-search-row">
              <label className="overview-location-search-field" htmlFor="overview-location-search">
                <span>Search another place</span>
                <input
                  ref={searchInputRef}
                  id="overview-location-search"
                  className="aria-input"
                  value={query}
                  placeholder="City, ZIP, park, airport..."
                  onChange={(event) => setQuery(event.currentTarget.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      void handleSearch()
                    }
                  }}
                />
              </label>
              <div className="overview-location-search-actions">
                <AriaButton className="ghost button-inline overview-location-action" onPress={() => void handleSearch()} isDisabled={searching}>
                  {searching ? 'Searching...' : 'Search'}
                </AriaButton>
                <AriaButton
                  className="ghost button-inline overview-location-action"
                  onPress={async () => {
                    await onUseCurrentLocation()
                    setIsOpen(false)
                  }}
                  isDisabled={resolvingCurrentLocation}
                >
                  {resolvingCurrentLocation ? 'Locating...' : 'Use my location'}
                </AriaButton>
                {isViewingAlternate ? (
                  <AriaButton
                    className="button-inline overview-location-reset"
                    onPress={() => {
                      onResetToMonitoring()
                      setIsOpen(false)
                    }}
                  >
                    Back to monitored area
                  </AriaButton>
                ) : null}
              </div>
            </div>

            {searchError ? <p className="field-error">{searchError}</p> : null}
            {statusMessage ? <p className="muted small overview-location-status">{statusMessage}</p> : null}

            {results.length > 0 ? (
              <div className="overview-location-search-results" role="listbox" aria-label="Overview location results">
                {results.map((item) => (
                  <AriaButton key={item.id} className="overview-location-search-result" onPress={() => applySelection(item)}>
                    <span className="overview-location-result-name">{formatFriendlyLocation(item.name)}</span>
                    <span className="overview-location-result-detail">{item.detail}</span>
                  </AriaButton>
                ))}
              </div>
            ) : null}

            <div className="overview-location-meta-row">
              <div className="overview-location-meta-card">
                <span className="overview-location-meta-label">Now viewing</span>
                <strong>{formatFriendlyLocation(activeLocation.name)}</strong>
                <span>{activeLocation.detail}</span>
              </div>
              <div className="overview-location-meta-card">
                <span className="overview-location-meta-label">Alerts stay tied to</span>
                <strong>{formatFriendlyLocation(monitoringLocation.name)}</strong>
                <span>{monitoringLocation.detail}</span>
              </div>
            </div>

            {recentLocationsToShow.length > 0 ? (
              <div className="overview-location-recent-row">
                <span className="overview-location-recent-label">Recent places</span>
                <div className="overview-location-recent-chips">
                  {recentLocationsToShow.map((item) => (
                    <AriaButton key={item.id} className="overview-location-chip" onPress={() => applySelection(item)}>
                      {formatFriendlyLocation(item.name)}
                    </AriaButton>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </>
  )
}
