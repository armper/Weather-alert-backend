import { Fragment, useEffect, useMemo, useRef, useState } from 'react'
import type { MapRef } from 'react-map-gl/maplibre'
import { Layer, Map, Marker, Popup, Source, useMap } from 'react-map-gl/maplibre'
import { DEFAULT_LAT, DEFAULT_LON } from '../../state/types'
import type { RuleLocationGroup } from '../../lib/ruleDashboard'
import { VIEWPORT_ANIMATION_DURATION_MS, circleGeoJSON, osmStyle } from './mapUtils'

interface MonitoringRulesMapProps {
  groups: RuleLocationGroup[]
  selectedGroupId?: string | null
  onMarkerSelect?: (groupId: string) => void
  onMarkerHover?: (groupId: string | null) => void
  ariaLabel?: string
  className?: string
  compact?: boolean
  interactive?: boolean
}

interface ViewportControllerProps {
  groups: RuleLocationGroup[]
  selectedGroupId?: string | null
  compact: boolean
}

function ViewportController({ groups, selectedGroupId, compact }: ViewportControllerProps) {
  const { current: map } = useMap()
  const didFitRef = useRef(false)
  const previousSelectionRef = useRef<string | null>(null)
  const normalizedSelectedGroupId = selectedGroupId ?? null

  useEffect(() => {
    if (!map || groups.length === 0) {
      return
    }

    const selectedGroup = normalizedSelectedGroupId
      ? groups.find((item) => item.id === normalizedSelectedGroupId)
      : undefined

    if (selectedGroup) {
      const targetZoom = compact ? 9 : 10
      const nextZoom = Math.max(map.getZoom(), targetZoom)
      if (previousSelectionRef.current !== normalizedSelectedGroupId) {
        map.flyTo({ center: [selectedGroup.longitude, selectedGroup.latitude], zoom: nextZoom, duration: VIEWPORT_ANIMATION_DURATION_MS })
        previousSelectionRef.current = normalizedSelectedGroupId
      }
      return
    }

    if (!didFitRef.current) {
      if (groups.length === 1) {
        map.setCenter([groups[0].longitude, groups[0].latitude])
        map.setZoom(compact ? 8 : 9)
      } else {
        const lngs = groups.map((g) => g.longitude)
        const lats = groups.map((g) => g.latitude)
        const padding = compact ? 24 : 40
        map.fitBounds(
          [
            [Math.min(...lngs), Math.min(...lats)],
            [Math.max(...lngs), Math.max(...lats)],
          ],
          { padding },
        )
      }
      didFitRef.current = true
    }
  }, [compact, groups, map, normalizedSelectedGroupId])

  return null
}

export function MonitoringRulesMap({
  groups,
  selectedGroupId,
  onMarkerSelect,
  onMarkerHover,
  ariaLabel = 'Monitoring map',
  className,
  compact = false,
  interactive = true,
}: MonitoringRulesMapProps) {
  const mapRef = useRef<MapRef>(null)
  const [openPopupId, setOpenPopupId] = useState<string | null>(null)

  const initialCenter = useMemo(() => {
    const first = groups[0]
    if (first) {
      return { longitude: first.longitude, latitude: first.latitude }
    }
    return { longitude: Number(DEFAULT_LON), latitude: Number(DEFAULT_LAT) }
  }, [groups])

  return (
    <div
      className={['monitoring-map-shell', compact ? 'is-compact' : '', className ?? ''].filter(Boolean).join(' ')}
      aria-label={ariaLabel}
    >
      <div className="monitoring-map">
        <Map
          ref={mapRef}
          initialViewState={{ ...initialCenter, zoom: compact ? 8 : 9 }}
          style={{ width: '100%', height: '100%' }}
          mapStyle={osmStyle}
          scrollZoom={interactive}
          dragPan={interactive}
          touchZoomRotate={interactive}
          doubleClickZoom={interactive}
          attributionControl={false}
        >
        <ViewportController groups={groups} selectedGroupId={selectedGroupId} compact={compact} />

        {groups.map((group) => {
          const selected = group.id === selectedGroupId
          const circleData = circleGeoJSON(group.latitude, group.longitude, group.radiusKm)

          return (
            <Fragment key={group.id}>
              {selected ? (
                <Source id={`circle-${group.id}`} type="geojson" data={circleData}>
                  <Layer
                    id={`circle-fill-${group.id}`}
                    type="fill"
                    paint={{ 'fill-color': 'rgba(0, 180, 216, 0.12)', 'fill-opacity': 0.28 }}
                  />
                  <Layer
                    id={`circle-line-${group.id}`}
                    type="line"
                    paint={{ 'line-color': 'rgba(0, 119, 182, 0.55)', 'line-width': 1.2 }}
                  />
                </Source>
              ) : null}

              <Marker
                longitude={group.longitude}
                latitude={group.latitude}
                anchor="center"
                onClick={
                  interactive
                    ? (e) => {
                        e.originalEvent.stopPropagation()
                        onMarkerSelect?.(group.id)
                        setOpenPopupId(openPopupId === group.id ? null : group.id)
                      }
                    : undefined
                }
              >
                <div
                  className={`monitoring-map-marker is-${group.statusTone}${selected ? ' is-selected' : ''}`}
                  style={{ width: selected ? 48 : 42, height: selected ? 48 : 42 }}
                  onMouseEnter={interactive ? () => onMarkerHover?.(group.id) : undefined}
                  onMouseLeave={interactive ? () => onMarkerHover?.(null) : undefined}
                >
                  <span className="monitoring-map-marker-icon" aria-hidden="true">{group.icon}</span>
                  {group.ruleCount > 1 ? (
                    <span className="monitoring-map-marker-count">{group.ruleCount}</span>
                  ) : null}
                </div>
              </Marker>

              {interactive && openPopupId === group.id ? (
                <Popup
                  longitude={group.longitude}
                  latitude={group.latitude}
                  anchor="bottom"
                  offset={[0, -24]}
                  closeButton={false}
                  className="monitoring-map-popup"
                  onClose={() => setOpenPopupId(null)}
                >
                  <div className="monitoring-map-popup-content">
                    <div className="monitoring-map-popup-header">
                      <strong>{group.locationLabel}</strong>
                      <span className={`badge ${group.statusTone === 'critical' ? 'is-live' : ''}`}>{group.statusLabel}</span>
                    </div>
                    <div className="monitoring-map-popup-list">
                      {group.rules.map((rule) => (
                        <div key={rule.criteriaId} className="monitoring-map-popup-rule">
                          <span className="monitoring-map-popup-rule-icon" aria-hidden="true">
                            {rule.icon}
                          </span>
                          <div>
                            <strong>{rule.ruleName}</strong>
                            <span>{rule.triggerCondition}</span>
                          </div>
                          <span className={`criteria-signal-chip is-${rule.monitoringTone}`}>{rule.monitoringState}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </Popup>
              ) : null}
            </Fragment>
          )
        })}
        </Map>
      </div>
    </div>
  )
}
