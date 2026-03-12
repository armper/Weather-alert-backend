import { divIcon } from 'leaflet'
import { Circle, MapContainer, Marker, TileLayer } from 'react-leaflet'

interface StaticLocationMapProps {
  latitude: number
  longitude: number
  radiusKm?: number
  ariaLabel?: string
  className?: string
  ruleCount?: number
}

export function StaticLocationMap({
  latitude,
  longitude,
  radiusKm = 8,
  ariaLabel = 'Location map',
  className,
  ruleCount = 1,
}: StaticLocationMapProps) {
  const center: [number, number] = [latitude, longitude]
  const markerIcon = divIcon({
    className: 'static-map-marker-wrapper',
    html: `
      <div class="static-map-marker">
        <span class="static-map-marker-dot"></span>
        ${ruleCount > 1 ? `<span class="static-map-marker-count">${ruleCount}</span>` : ''}
      </div>
    `,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })

  return (
    <div className={['static-map-shell', className ?? ''].filter(Boolean).join(' ')} aria-label={ariaLabel}>
      <MapContainer
        center={center}
        zoom={10}
        attributionControl={false}
        zoomControl={false}
        dragging={false}
        scrollWheelZoom={false}
        touchZoom={false}
        doubleClickZoom={false}
        keyboard={false}
        className="static-map"
      >
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <Circle center={center} radius={radiusKm * 1000} pathOptions={{ color: '#1d6a90', weight: 1, fillOpacity: 0.1 }} />
        <Marker position={center} icon={markerIcon} />
      </MapContainer>
    </div>
  )
}
