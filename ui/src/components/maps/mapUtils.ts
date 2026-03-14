import type { Feature } from 'geojson'
import type { StyleSpecification } from 'maplibre-gl'

export const osmStyle: StyleSpecification = {
  version: 8,
  sources: {
    osm: {
      type: 'raster',
      // OSM tile servers: {z}/{x}/{y} tile coordinates
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    },
  },
  layers: [{ id: 'osm', type: 'raster', source: 'osm' }],
}

/** Shared accent color used for location circles and markers */
export const MAP_ACCENT_COLOR = '#1d6a90'

/** Animation duration in ms for viewport fly-to transitions */
export const VIEWPORT_ANIMATION_DURATION_MS = 450

export function circleGeoJSON(latitude: number, longitude: number, radiusKm: number, steps = 64): Feature {
  const coords: [number, number][] = []
  // ~111.32 km per degree of longitude (at equator, scales with cos(lat))
  const distanceX = radiusKm / (111.32 * Math.cos((latitude * Math.PI) / 180))
  // ~110.574 km per degree of latitude (approximately constant)
  const distanceY = radiusKm / 110.574

  for (let i = 0; i < steps; i++) {
    const theta = (i / steps) * 2 * Math.PI
    coords.push([longitude + distanceX * Math.cos(theta), latitude + distanceY * Math.sin(theta)])
  }
  coords.push(coords[0])

  return {
    type: 'Feature',
    geometry: { type: 'Polygon', coordinates: [coords] },
    properties: {},
  }
}
