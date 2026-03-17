import { lazy, Suspense, useEffect, useId, useRef, useState } from 'react'
import { usePlaceSearch } from '../../../hooks/usePlaceSearch'
import { formatFriendlyLocation } from '../../../lib/formatting'
import { reverseGeocode } from '../../../services/geocoding'
import { AriaButton } from '../../ui/AriaButton'

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
  const [resolvingCurrentLocation, setResolvingCurrentLocation] = useState(false)
  const [saving, setSaving] = useState(false)
  const [showTooltip, setShowTooltip] = useState(false)
  const [query, setQuery] = useState(activeLocation.name)
  const [draftLocation, setDraftLocation] = useState(activeLocation)
  const [hasTypedQuery, setHasTypedQuery] = useState(false)
  const [locationError, setLocationError] = useState<string | null>(null)
  const {
    results,
    searching,
    searchError,
    clearResults,
    skipNextSearchFor,
  } = usePlaceSearch(query, { debounceMs: 320, enabled: isOpen && hasTypedQuery })

  const isViewingCustomLocation =
    Math.abs(activeLocation.latitude - monitoringLocation.latitude) > 0.0001 ||
    Math.abs(activeLocation.longitude - monitoringLocation.longitude) > 0.0001 ||
    formatFriendlyLocation(activeLocation.name) !== formatFriendlyLocation(monitoringLocation.name)

  useEffect(() => {
    if (!isOpen) {
      return
    }

    setQuery(activeLocation.name)
    clearResults()
    setDraftLocation(activeLocation)
    setHasTypedQuery(false)
    setLocationError(null)

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
    setResolvingCurrentLocation(false)
    setLocationError(null)
  }

  function applyDraftLocation(location: OverviewLocationSelection) {
    setDraftLocation(location)
    setQuery(location.name)
    skipNextSearchFor(location.name)
    clearResults()
    setHasTypedQuery(false)
    setLocationError(null)
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

  async function handleUseCurrentLocation() {
    if (!navigator.geolocation) {
      setLocationError('Current location is not available on this device.')
      return
    }

    setResolvingCurrentLocation(true)
    clearResults()
    setLocationError(null)

    try {
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 10_000,
          maximumAge: 60_000,
        })
      })

      const latitude = position.coords.latitude
      const longitude = position.coords.longitude
      const place = await reverseGeocode(latitude, longitude)

      applyDraftLocation({
        name: place?.name ?? 'Current location',
        detail: place?.displayName,
        latitude,
        longitude,
      })
    } catch {
      setLocationError('SkyPanda could not read your current location.')
    } finally {
      setResolvingCurrentLocation(false)
    }
  }

  async function handleSave() {
    setSaving(true)

    try {
      await onSaveLocation(draftLocation)
      closeModal()
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
              <div>
                <h2 id={`${titleId}-dialog`}>Change location</h2>
                <p className="overview-location-dialog-copy">
                  Search or tap the map to update the weather shown here.
                </p>
              </div>
              <AriaButton className="ghost button-inline overview-location-close" onPress={closeModal}>
                Close
              </AriaButton>
            </div>

            <div className="overview-location-picker-grid">
              <div className="overview-location-search-column">
                <div className="overview-location-search-wrapper">
                  <label className="overview-location-search-field" htmlFor={`${titleId}-search`}>
                    <span>Find a place</span>
                    <input
                      ref={searchInputRef}
                      autoComplete="off"
                      className="travel-input overview-location-search-input"
                      id={`${titleId}-search`}
                      maxLength={180}
                      onChange={(event) => {
                        setHasTypedQuery(true)
                        setQuery(event.target.value)
                        setLocationError(null)
                      }}
                      placeholder="City, ZIP, landmark"
                      value={query}
                    />
                  </label>

                  {results.length > 0 ? (
                    <ul className="travel-geo-results overview-location-results">
                      {results.map((item) => (
                        <li key={item.id}>
                          <button
                            className="travel-geo-result"
                            onClick={() =>
                              applyDraftLocation({
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

                <div className="overview-location-search-actions">
                  <AriaButton
                    className="ghost button-inline overview-location-action"
                    isDisabled={resolvingCurrentLocation}
                    onPress={() => void handleUseCurrentLocation()}
                  >
                    {resolvingCurrentLocation ? 'Locating...' : 'Use current location'}
                  </AriaButton>
                  {isViewingCustomLocation ? (
                    <AriaButton
                      className="ghost button-inline overview-location-action"
                      isDisabled={saving}
                      onPress={() => applyDraftLocation(monitoringLocation)}
                    >
                      Use monitored location
                    </AriaButton>
                  ) : null}
                </div>

                {searching ? <p className="muted small overview-location-status">Finding places…</p> : null}
                {locationError ?? searchError ? <p className="field-error">{locationError ?? searchError}</p> : null}

                <div className="overview-location-selection-card">
                  <span className="overview-location-selection-label">Selected location</span>
                  <strong>{formatFriendlyLocation(draftLocation.name)}</strong>
                  <span>{draftLocation.detail ?? `${draftLocation.latitude.toFixed(3)}, ${draftLocation.longitude.toFixed(3)}`}</span>
                </div>
              </div>

              <div className="overview-location-map-panel">
                <Suspense fallback={<div className="overview-location-map-loading" />}>
                  <LocationPickerMap
                    latitude={draftLocation.latitude}
                    location={draftLocation.name}
                    longitude={draftLocation.longitude}
                    onSelect={({ location, latitude, longitude }) => {
                      applyDraftLocation({
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

            <div className="overview-location-dialog-footer">
              <AriaButton className="ghost button-inline overview-location-action" isDisabled={saving} onPress={closeModal}>
                Cancel
              </AriaButton>
              <AriaButton className="button-inline overview-location-save" isDisabled={saving} onPress={() => void handleSave()}>
                {saving ? 'Updating...' : 'Update location'}
              </AriaButton>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
