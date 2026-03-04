import { Circle, CircleMarker, MapContainer, TileLayer } from 'react-leaflet'

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
        <CircleMarker
          center={center}
          radius={6}
          pathOptions={{ color: '#ffffff', weight: 2, fillColor: '#1d6a90', fillOpacity: 0.95 }}
        />
      </MapContainer>
    </div>
  )
}
