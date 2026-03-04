import { Circle, MapContainer, Marker, TileLayer } from 'react-leaflet'
import { ensureLeafletIcons } from './leafletConfig'

ensureLeafletIcons()

interface StaticLocationMapProps {
  latitude: number
  longitude: number
  radiusKm?: number
  ariaLabel?: string
  className?: string
}

export function StaticLocationMap({
  latitude,
  longitude,
  radiusKm = 8,
  ariaLabel = 'Location map',
  className,
}: StaticLocationMapProps) {
  const center: [number, number] = [latitude, longitude]

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
        <Marker position={center} />
      </MapContainer>
    </div>
  )
}
