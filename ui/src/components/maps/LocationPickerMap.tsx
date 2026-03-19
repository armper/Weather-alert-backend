import { useCallback, useEffect, useMemo, useState } from 'react'
import type { MapMouseEvent } from 'react-map-gl/maplibre'
import { Layer, Map, Marker, Source, useMap } from 'react-map-gl/maplibre'
import type { GeocodePlace } from '../../services/geocoding'
import { reverseGeocode, searchPlaces } from '../../services/geocoding'
import { AriaButton } from '../ui/AriaButton'
import { MAP_ACCENT_COLOR, circleGeoJSON, osmStyle } from './mapUtils'

interface LocationPickerMapProps {
  location: string
  latitude: number
  longitude: number
  onSelect: (selection: { location: string; latitude: number; longitude: number }) => void
  showSearchControls?: boolean
}

function RecenterMap({ longitude, latitude }: { longitude: number; latitude: number }) {
  const { current: map } = useMap()

  useEffect(() => {
    map?.setCenter([longitude, latitude])
  }, [longitude, latitude, map])

  return null
}

export function LocationPickerMap({
  location,
  latitude,
  longitude,
  onSelect,
  showSearchControls = true,
}: LocationPickerMapProps) {
  const [query, setQuery] = useState(location)
  const [results, setResults] = useState<GeocodePlace[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)

  const circleData = useMemo(() => circleGeoJSON(latitude, longitude, 5), [latitude, longitude])

  useEffect(() => {
    setQuery(location)
  }, [location])

  async function runSearch() {
    if (!query.trim()) {
      setResults([])
      return
    }

    setSearching(true)
    setSearchError(null)

    try {
      const places = await searchPlaces(query)
      setResults(places)
      if (places.length > 0) {
        const first = places[0]
        onSelect({
          location: first.name,
          latitude: first.latitude,
          longitude: first.longitude,
        })
      }
    } catch {
      setSearchError('Unable to search right now. Try again in a moment.')
    } finally {
      setSearching(false)
    }
  }

  function applyResult(place: GeocodePlace) {
    onSelect({
      location: place.name,
      latitude: place.latitude,
      longitude: place.longitude,
    })
    setResults([])
  }

  const handleMapClick = useCallback(
    (event: MapMouseEvent) => {
      const { lat, lng } = event.lngLat
      void reverseGeocode(lat, lng)
        .then((place) => {
          onSelect({
            location: place?.name ?? 'Selected area',
            latitude: place?.latitude ?? lat,
            longitude: place?.longitude ?? lng,
          })
        })
        .catch(() => {
          onSelect({ location: 'Selected area', latitude: lat, longitude: lng })
        })
    },
    [onSelect],
  )

  return (
    <div className="location-picker-stack">
      {showSearchControls ? (
        <div className="location-search-row">
          <label className="location-search-field" htmlFor="location-search">
            <span>Location</span>
            <input
              id="location-search"
              className="aria-input"
              value={query}
              placeholder="Search city or place"
              onChange={(event) => setQuery(event.currentTarget.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  void runSearch()
                }
              }}
            />
          </label>
          <AriaButton className="ghost location-search-button" onPress={() => void runSearch()} isDisabled={searching}>
            {searching ? 'Searching...' : 'Search'}
          </AriaButton>
        </div>
      ) : null}

      {showSearchControls && searchError ? <p className="field-error">{searchError}</p> : null}

      {showSearchControls && results.length > 0 ? (
        <div className="location-search-results" role="listbox" aria-label="Location search results">
          {results.map((item) => (
            <AriaButton key={item.id} className="location-search-result" onPress={() => applyResult(item)}>
              <span className="location-result-name">{item.name}</span>
              <span className="location-result-detail">{item.displayName}</span>
            </AriaButton>
          ))}
        </div>
      ) : null}

      <div className="location-map-shell">
        <Map
          initialViewState={{ longitude, latitude, zoom: 10 }}
          style={{ width: '100%', height: '100%' }}
          mapStyle={osmStyle}
          onClick={handleMapClick}
          attributionControl={false}
        >
          <RecenterMap longitude={longitude} latitude={latitude} />
          <Source id="pick-circle" type="geojson" data={circleData}>
            <Layer id="pick-circle-fill" type="fill" paint={{ 'fill-color': MAP_ACCENT_COLOR, 'fill-opacity': 0.1 }} />
            <Layer id="pick-circle-line" type="line" paint={{ 'line-color': MAP_ACCENT_COLOR, 'line-width': 1 }} />
          </Source>
          <Marker longitude={longitude} latitude={latitude} anchor="center">
            <div
              style={{
                width: 14,
                height: 14,
                borderRadius: '50%',
                border: '2px solid #ffffff',
                background: MAP_ACCENT_COLOR,
              }}
            />
          </Marker>
        </Map>
      </div>

      <p className="muted small location-selected-label">Selected location: {location}</p>
    </div>
  )
}
