import { useEffect, useMemo, useState } from 'react'
import { Circle, CircleMarker, MapContainer, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import type { GeocodePlace } from '../../services/geocoding'
import { reverseGeocode, searchPlaces } from '../../services/geocoding'
import { AriaButton } from '../ui/AriaButton'

interface LocationPickerMapProps {
  location: string
  latitude: number
  longitude: number
  onSelect: (selection: { location: string; latitude: number; longitude: number }) => void
}

function RecenterMap({ center }: { center: [number, number] }) {
  const map = useMap()

  useEffect(() => {
    map.setView(center)
  }, [center, map])

  return null
}

function MapClickCapture({ onPick }: { onPick: (latitude: number, longitude: number) => void }) {
  useMapEvents({
    click: (event) => {
      onPick(event.latlng.lat, event.latlng.lng)
    },
  })
  return null
}

export function LocationPickerMap({ location, latitude, longitude, onSelect }: LocationPickerMapProps) {
  const [query, setQuery] = useState(location)
  const [results, setResults] = useState<GeocodePlace[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const center = useMemo<[number, number]>(() => [latitude, longitude], [latitude, longitude])

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

  async function handleMapPick(nextLatitude: number, nextLongitude: number) {
    const fallbackName = `Selected point (${nextLatitude.toFixed(3)}, ${nextLongitude.toFixed(3)})`
    onSelect({
      location: fallbackName,
      latitude: nextLatitude,
      longitude: nextLongitude,
    })

    const place = await reverseGeocode(nextLatitude, nextLongitude)
    if (place) {
      onSelect({
        location: place.name,
        latitude: place.latitude,
        longitude: place.longitude,
      })
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

  return (
    <div className="location-picker-stack">
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

      {searchError ? <p className="field-error">{searchError}</p> : null}

      {results.length > 0 ? (
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
        <MapContainer center={center} zoom={10} className="location-map" scrollWheelZoom>
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <RecenterMap center={center} />
          <MapClickCapture onPick={(lat, lon) => void handleMapPick(lat, lon)} />
          <Circle center={center} radius={5000} pathOptions={{ color: '#1d6a90', weight: 1, fillOpacity: 0.1 }} />
          <CircleMarker
            center={center}
            radius={7}
            pathOptions={{ color: '#ffffff', weight: 2, fillColor: '#1d6a90', fillOpacity: 0.95 }}
          />
        </MapContainer>
      </div>

      <p className="muted small location-selected-label">Selected location: {location}</p>
    </div>
  )
}
