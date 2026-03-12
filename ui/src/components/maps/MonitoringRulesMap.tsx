import { divIcon } from 'leaflet'
import { Fragment, useEffect, useMemo, useRef } from 'react'
import { Circle, MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { DEFAULT_LAT, DEFAULT_LON } from '../../state/types'
import type { RuleLocationGroup } from '../../lib/ruleDashboard'

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
  const map = useMap()
  const didFitRef = useRef(false)
  const previousSelectionRef = useRef<string | null>(null)
  const normalizedSelectedGroupId = selectedGroupId ?? null

  useEffect(() => {
    if (groups.length === 0) {
      return
    }

    const selectedGroup = normalizedSelectedGroupId
      ? groups.find((item) => item.id === normalizedSelectedGroupId)
      : undefined
    if (selectedGroup) {
      const targetZoom = compact ? 9 : 10
      const nextZoom = Math.max(map.getZoom(), targetZoom)
      if (previousSelectionRef.current !== normalizedSelectedGroupId) {
        map.flyTo([selectedGroup.latitude, selectedGroup.longitude], nextZoom, { duration: 0.45 })
        previousSelectionRef.current = normalizedSelectedGroupId
      }
      return
    }

    if (!didFitRef.current) {
      if (groups.length === 1) {
        map.setView([groups[0].latitude, groups[0].longitude], compact ? 8 : 9)
      } else {
        const bounds = groups.map((item) => [item.latitude, item.longitude] as [number, number])
        map.fitBounds(bounds, { padding: compact ? [24, 24] : [40, 40] })
      }
      didFitRef.current = true
    }
  }, [compact, groups, map, normalizedSelectedGroupId])

  return null
}

function buildMarkerIcon(group: RuleLocationGroup, selected: boolean) {
  const countBadge =
    group.ruleCount > 1 ? `<span class="monitoring-map-marker-count">${group.ruleCount}</span>` : ''

  return divIcon({
    className: 'monitoring-map-marker-wrapper',
    html: `
      <div class="monitoring-map-marker is-${group.statusTone}${selected ? ' is-selected' : ''}">
        <span class="monitoring-map-marker-icon" aria-hidden="true">${group.icon}</span>
        ${countBadge}
      </div>
    `,
    iconSize: [selected ? 48 : 42, selected ? 48 : 42],
    iconAnchor: [21, 21],
    popupAnchor: [0, -18],
  })
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
  const center = useMemo<[number, number]>(() => {
    const first = groups[0]
    if (first) {
      return [first.latitude, first.longitude]
    }
    return [Number(DEFAULT_LAT), Number(DEFAULT_LON)]
  }, [groups])

  return (
    <div
      className={['monitoring-map-shell', compact ? 'is-compact' : '', className ?? ''].filter(Boolean).join(' ')}
      aria-label={ariaLabel}
    >
      <MapContainer
        center={center}
        zoom={compact ? 8 : 9}
        className="monitoring-map"
        scrollWheelZoom={interactive}
        dragging={interactive}
        touchZoom={interactive}
        doubleClickZoom={interactive}
        zoomControl={interactive}
        attributionControl={false}
      >
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <ViewportController groups={groups} selectedGroupId={selectedGroupId} compact={compact} />
        {groups.map((group) => {
          const selected = group.id === selectedGroupId
          return (
            <Fragment key={group.id}>
              {selected ? (
                <Circle
                  center={[group.latitude, group.longitude]}
                  radius={group.radiusKm * 1000}
                  pathOptions={{
                    color: 'rgba(0, 119, 182, 0.55)',
                    weight: 1.2,
                    fillColor: 'rgba(0, 180, 216, 0.12)',
                    fillOpacity: 0.28,
                  }}
                />
              ) : null}
              <Marker
                key={group.id}
                position={[group.latitude, group.longitude]}
                icon={buildMarkerIcon(group, selected)}
                eventHandlers={
                  interactive
                    ? {
                        click: () => onMarkerSelect?.(group.id),
                        mouseover: () => onMarkerHover?.(group.id),
                        mouseout: () => onMarkerHover?.(null),
                      }
                    : undefined
                }
              >
                {interactive ? (
                  <Popup className="monitoring-map-popup" autoPan closeButton={false}>
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
              </Marker>
            </Fragment>
          )
        })}
      </MapContainer>
    </div>
  )
}
