import { useMemo } from 'react'
import { Layer, Map, Marker, Source } from 'react-map-gl/maplibre'
import { MAP_ACCENT_COLOR, circleGeoJSON, osmStyle } from './mapUtils'

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
  const circleData = useMemo(() => circleGeoJSON(latitude, longitude, radiusKm), [latitude, longitude, radiusKm])

  return (
    <div className={['static-map-shell', className ?? ''].filter(Boolean).join(' ')} aria-label={ariaLabel}>
      <Map
        initialViewState={{ longitude, latitude, zoom: 10 }}
        style={{ width: '100%', height: '100%' }}
        mapStyle={osmStyle}
        attributionControl={false}
        interactive={false}
      >
        <Source id="radius-circle" type="geojson" data={circleData}>
          <Layer id="radius-fill" type="fill" paint={{ 'fill-color': MAP_ACCENT_COLOR, 'fill-opacity': 0.1 }} />
          <Layer id="radius-line" type="line" paint={{ 'line-color': MAP_ACCENT_COLOR, 'line-width': 1 }} />
        </Source>
        <Marker longitude={longitude} latitude={latitude} anchor="center">
          <div className="static-map-marker-wrapper">
            <div className="static-map-marker">
              <span className="static-map-marker-dot" />
              {ruleCount > 1 ? <span className="static-map-marker-count">{ruleCount}</span> : null}
            </div>
          </div>
        </Marker>
      </Map>
    </div>
  )
}
