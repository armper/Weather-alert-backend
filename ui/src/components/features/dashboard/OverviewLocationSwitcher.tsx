import { lazy, Suspense, useEffect, useId, useRef, useState } from 'react'
import { usePlaceSearch } from '../../../hooks/usePlaceSearch'
import { formatFriendlyLocation } from '../../../lib/formatting'

const LocationPickerMap = lazy(() =>
  import('../../maps/LocationPickerMap').then((module) => ({ default: module.LocationPickerMap })),
)

export interface OverviewLocationSelection {
  name: string
  latitude: number
  longitude: number
  detail?: string
}

interface OverviewLocationSwitcherProps {
  activeLocation: OverviewLocationSelection
  monitoringLocation: OverviewLocationSelection
  onSaveLocation: (location: OverviewLocationSelection) => Promise<void> | void
}

const LONG_PRESS_TOOLTIP_DELAY_MS = 420

export function OverviewLocationSwitcher({
  activeLocation,
  monitoringLocation,
  onSaveLocation,
}: OverviewLocationSwitcherProps) {
  const titleId = useId()
  const tooltipTimerRef = useRef<number | null>(null)
  const searchInputRef = useRef<HTMLInputElement | null>(null)

  const [isOpen, setIsOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [showTooltip, setShowTooltip] = useState(false)
  const [query, setQuery] = useState(activeLocation.name)
  const [selectedLocation, setSelectedLocation] = useState(activeLocation)
  const [hasTypedQuery, setHasTypedQuery] = useState(false)
  const {
    results,
    clearResults,
    skipNextSearchFor,
  } = usePlaceSearch(query, { debounceMs: 320, enabled: isOpen && hasTypedQuery })

  useEffect(() => {
    if (!isOpen) {
      return
    }

    setQuery(activeLocation.name)
    clearResults()
    setSelectedLocation(activeLocation)
    setHasTypedQuery(false)

    const timeoutId = window.setTimeout(() => {
      searchInputRef.current?.focus()
      searchInputRef.current?.select()
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
  }, [activeLocation, clearResults, isOpen])

  useEffect(() => {
    return () => {
      if (tooltipTimerRef.current != null) {
        window.clearTimeout(tooltipTimerRef.current)
      }
    }
  }, [])

  function openModal() {
    setShowTooltip(false)
    setIsOpen(true)
  }

  function closeModal() {
    setIsOpen(false)
    clearResults()
  }

  function syncSelectedLocationState(location: OverviewLocationSelection) {
    setSelectedLocation(location)
    setQuery(location.name)
    skipNextSearchFor(location.name)
    clearResults()
    setHasTypedQuery(false)
  }

  function beginTooltipLongPress() {
    if (tooltipTimerRef.current != null) {
      window.clearTimeout(tooltipTimerRef.current)
    }

    tooltipTimerRef.current = window.setTimeout(() => {
      setShowTooltip(true)
    }, LONG_PRESS_TOOLTIP_DELAY_MS)
  }

  function endTooltipLongPress() {
    if (tooltipTimerRef.current != null) {
      window.clearTimeout(tooltipTimerRef.current)
      tooltipTimerRef.current = null
    }
    setShowTooltip(false)
  }

  async function commitSelection(location: OverviewLocationSelection) {
    if (saving) {
      return
    }

    syncSelectedLocationState(location)
    setIsOpen(false)
    setSaving(true)

    try {
      await onSaveLocation(location)
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <div className="overview-location-trigger-shell">
        <button
          aria-label="Change location"
          aria-describedby={showTooltip ? titleId : undefined}
          aria-haspopup="dialog"
          className={`overview-location-icon-trigger${showTooltip ? ' is-tooltip-visible' : ''}`}
          data-tooltip="Change location"
          onClick={openModal}
          onTouchCancel={endTooltipLongPress}
          onTouchEnd={endTooltipLongPress}
          onTouchStart={beginTooltipLongPress}
          title="Change location"
          type="button"
        >
          <svg aria-hidden="true" className="overview-location-icon" viewBox="0 0 24 24">
            <path
              d="M12 20.25c-1.72-2.04-5.75-6.82-5.75-10.5a5.75 5.75 0 1 1 11.5 0c0 3.68-4.03 8.46-5.75 10.5Z"
              fill="none"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="1.6"
            />
            <circle cx="12" cy="9.75" r="2.1" fill="none" stroke="currentColor" strokeWidth="1.6" />
            <path
              d="M17.2 16.1h4.1m-2.05-2.05v4.1"
              fill="none"
              stroke="currentColor"
              strokeLinecap="round"
              strokeWidth="1.6"
            />
          </svg>
        </button>
        <span className="overview-location-trigger-tooltip" id={titleId} role="tooltip">
          Change location
        </span>
      </div>

      {isOpen ? (
        <div className="overview-location-dialog-backdrop" role="presentation" onClick={closeModal}>
          <div
            aria-labelledby={`${titleId}-dialog`}
            aria-modal="true"
            className="overview-location-dialog overview-location-dialog--picker"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
          >
            <div className="overview-location-dialog-header">
              <h2 id={`${titleId}-dialog`}>Choose location</h2>
              <button className="overview-location-close" aria-label="Close location picker" onClick={closeModal} type="button">
                ✕
              </button>
            </div>

            <div className="overview-location-picker-grid">
              <div className="overview-location-search-column">
                <div className="overview-location-selection-card">
                  <span className="overview-location-selection-label">Selected area</span>
                  <strong>{formatFriendlyLocation(selectedLocation.name || monitoringLocation.name)}</strong>
                </div>

                <div className="overview-location-search-wrapper">
                  <input
                    ref={searchInputRef}
                    aria-label="Find a place"
                    autoComplete="off"
                    className="travel-input overview-location-search-input"
                    id={`${titleId}-search`}
                    maxLength={180}
                    onChange={(event) => {
                      setHasTypedQuery(true)
                      setQuery(event.target.value)
                    }}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' && results[0]) {
                        event.preventDefault()
                        void commitSelection({
                          name: results[0].name,
                          detail: results[0].displayName,
                          latitude: results[0].latitude,
                          longitude: results[0].longitude,
                        })
                      }
                    }}
                    placeholder="Find a place"
                    value={query}
                  />

                  {results.length > 0 ? (
                    <ul className="travel-geo-results overview-location-results">
                      {results.map((item) => (
                        <li key={item.id}>
                          <button
                            className="travel-geo-result"
                            onClick={() =>
                              void commitSelection({
                                name: item.name,
                                detail: item.displayName,
                                latitude: item.latitude,
                                longitude: item.longitude,
                              })
                            }
                            type="button"
                          >
                            <strong>{formatFriendlyLocation(item.name)}</strong>
                            <span>{item.displayName}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              </div>

              <div className="overview-location-map-panel">
                <Suspense fallback={<div className="overview-location-map-loading" />}>
                  <LocationPickerMap
                    latitude={selectedLocation.latitude}
                    location={selectedLocation.name}
                    longitude={selectedLocation.longitude}
                    onSelect={({ location, latitude, longitude }) => {
                      void commitSelection({
                        name: location,
                        latitude,
                        longitude,
                      })
                    }}
                    showSearchControls={false}
                  />
                </Suspense>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
