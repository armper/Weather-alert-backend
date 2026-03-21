import { Suspense, lazy, useEffect, useRef, useState, type ReactNode } from 'react'
import { CalendarDays, Check, MapPinned, Plane } from 'lucide-react'
import { renderAppIcon } from '../lib/appIcons'
import { usePlaceSearch } from '../hooks/usePlaceSearch'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useActionState, useDataState } from '../state/useAppState'
import type { TravelPlan } from '../types'

const LocationPickerMap = lazy(() =>
  import('../components/maps/LocationPickerMap').then((module) => ({ default: module.LocationPickerMap })),
)
const StaticLocationMap = lazy(() =>
  import('../components/maps/StaticLocationMap').then((module) => ({ default: module.StaticLocationMap })),
)

/* ─── helpers ─── */

type ModalMode = 'closed' | 'create' | 'detail' | 'edit' | 'deleting'
type ModalStatus = 'idle' | 'saving' | 'success' | 'error'

function todayKey() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function tripStatus(plan: TravelPlan): 'active' | 'upcoming' | 'past' {
  const today = todayKey()
  if ((plan.endDate ?? '') < today) return 'past'
  if ((plan.startDate ?? '') > today) return 'upcoming'
  return 'active'
}

function statusIcon(s: 'active' | 'upcoming' | 'past'): ReactNode {
  if (s === 'active') return renderAppIcon(Plane)
  if (s === 'upcoming') return renderAppIcon(CalendarDays)
  return renderAppIcon(Check)
}

function formatRange(start?: string, end?: string) {
  if (!start || !end) return ''
  const fmt = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' })
  const s = new Date(start + 'T00:00')
  const e = new Date(end + 'T00:00')
  return `${fmt.format(s)} – ${fmt.format(e)}`
}

function daysLabel(plan: TravelPlan) {
  const status = tripStatus(plan)
  if (status === 'past') return 'Completed'
  if (status === 'active') {
    const end = new Date(plan.endDate + 'T00:00')
    const diff = Math.ceil((end.getTime() - Date.now()) / 86_400_000)
    return diff <= 0 ? 'Last day' : `${diff}d left`
  }
  const start = new Date(plan.startDate + 'T00:00')
  const diff = Math.ceil((start.getTime() - Date.now()) / 86_400_000)
  return diff <= 1 ? 'Tomorrow' : `In ${diff} days`
}

/* ─── component ─── */

export function TravelPlansPage() {
  const { travelPlans } = useDataState()
  const { handleCreateTravelPlan, handleUpdateTravelPlan, handleDeleteTravelPlan } = useActionState()

  const [mode, setMode] = useState<ModalMode>('closed')
  const [modalStatus, setModalStatus] = useState<ModalStatus>('idle')
  const [selected, setSelected] = useState<TravelPlan | null>(null)

  /* form fields */
  const [name, setName] = useState('')
  const [destination, setDestination] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [lat, setLat] = useState(Number(DEFAULT_LAT))
  const [lon, setLon] = useState(Number(DEFAULT_LON))
  const [notes, setNotes] = useState('')

  const backdropRef = useRef<HTMLDivElement>(null)
  const {
    results: geoResults,
    clearResults: clearGeoResults,
    skipNextSearchFor: skipDestinationSearch,
  } = usePlaceSearch(destination)

  /* sort: active first, then upcoming, then past */
  const sorted = [...travelPlans].sort((a, b) => {
    const order = { active: 0, upcoming: 1, past: 2 }
    const diff = order[tripStatus(a)] - order[tripStatus(b)]
    if (diff !== 0) return diff
    return (a.startDate ?? '').localeCompare(b.startDate ?? '')
  })

  /* ── open / close helpers ── */

  function openCreate() {
    setSelected(null)
    setName('')
    setDestination('')
    setStartDate('')
    setEndDate('')
    setLat(Number(DEFAULT_LAT))
    setLon(Number(DEFAULT_LON))
    setNotes('')
    clearGeoResults()
    setModalStatus('idle')
    setMode('create')
  }

  function openDetail(plan: TravelPlan) {
    setSelected(plan)
    populateForm(plan)
    setModalStatus('idle')
    setMode('detail')
  }

  function startEdit() {
    setMode('edit')
  }

  function populateForm(plan: TravelPlan) {
    setName(plan.name ?? '')
    setDestination(plan.destination ?? '')
    setStartDate(plan.startDate ?? '')
    setEndDate(plan.endDate ?? '')
    setLat(plan.latitude ?? Number(DEFAULT_LAT))
    setLon(plan.longitude ?? Number(DEFAULT_LON))
    setNotes(plan.notes ?? '')
  }

  function closeModal() {
    if (modalStatus === 'saving') return
    setMode('closed')
    setSelected(null)
    setModalStatus('idle')
  }

  /* ── actions ── */

  async function save() {
    if (!name.trim() || !destination.trim() || !startDate || !endDate) return
    setModalStatus('saving')
    const payload = {
      name: name.trim(),
      destination: destination.trim(),
      startDate,
      endDate,
      latitude: lat,
      longitude: lon,
      notes: notes.trim() || undefined,
      alertsEnabled: true,
      alertCoverageMode: 'ALL_ALERTS' as const,
      selectedAlertTopics: [] as never[],
      linkedCriteriaIds: [] as string[],
    }
    const ok = mode === 'edit' && selected
      ? await handleUpdateTravelPlan(selected.id, payload)
      : await handleCreateTravelPlan(payload)

    if (ok) {
      setModalStatus('success')
      setTimeout(closeModal, 800)
    } else {
      setModalStatus('error')
    }
  }

  async function confirmDelete() {
    if (!selected) return
    setMode('deleting')
    await handleDeleteTravelPlan(selected.id)
    closeModal()
  }

  /* esc key */
  useEffect(() => {
    if (mode === 'closed') return
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape' || modalStatus === 'saving') return
      setMode('closed')
      setSelected(null)
      setModalStatus('idle')
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [mode, modalStatus])

  /* ── form validity ── */
  const canSave = name.trim().length > 0 && destination.trim().length > 0 && startDate && endDate && endDate >= startDate

  /* ── render ── */

  const empty = travelPlans.length === 0

  return (
    <section className="page-stack travel-page-fresh">
      <div className="travel-page-content">
        {empty ? (
          /* ── empty state ── */
          <div className="travel-empty">
            <span className="travel-empty-icon">{renderAppIcon(Plane)}</span>
            <p className="travel-empty-label">No trips yet</p>
            <button className="travel-add-btn" type="button" onClick={openCreate}>
              + Plan a trip
            </button>
          </div>
        ) : (
          /* ── tile grid ── */
          <>
            <div className="travel-tile-grid">
              {sorted.map((plan) => {
                const status = tripStatus(plan)
                return (
                  <button
                    key={plan.id}
                    className={`travel-tile${status === 'active' ? ' is-active' : ''}${status === 'past' ? ' is-past' : ''}`}
                    type="button"
                    onClick={() => openDetail(plan)}
                  >
                    <div className="travel-tile-map-placeholder" aria-hidden>
                      <span>{renderAppIcon(MapPinned, 'travel-tile-map-icon', 2.15)}</span>
                    </div>
                    <div className="travel-tile-body">
                      <p className="travel-tile-destination">{plan.destination ?? 'Destination'}</p>
                      <p className="travel-tile-name">{plan.name}</p>
                      <p className="travel-tile-dates">{formatRange(plan.startDate, plan.endDate)}</p>
                      <span className="travel-tile-badge">
                        <span className="travel-tile-badge-icon" aria-hidden>{statusIcon(status)}</span>
                        <span>{daysLabel(plan)}</span>
                      </span>
                    </div>
                  </button>
                )
              })}
            </div>
            <button className="travel-add-btn travel-add-btn-below" type="button" onClick={openCreate}>
              + Create Travel Plan
            </button>
          </>
        )}
      </div>

      {/* ── modal ── */}
      {mode !== 'closed' ? (
        <div
          className="travel-modal-backdrop"
          ref={backdropRef}
          onClick={(e) => { if (e.target === backdropRef.current) closeModal() }}
        >
          <div className={`travel-modal travel-modal--${mode}${modalStatus === 'success' ? ' is-success' : ''}${modalStatus === 'error' ? ' is-error' : ''}`}>
            {/* header */}
            <div className="travel-modal-header">
              <span className="travel-modal-icon">{mode === 'create' ? renderAppIcon(Plane) : renderAppIcon(MapPinned)}</span>
              <h2 className="travel-modal-title">
                {mode === 'create' ? 'Plan a trip' : mode === 'edit' ? 'Edit trip' : (selected?.name ?? 'Trip')}
              </h2>
              <button className="travel-modal-close" type="button" onClick={closeModal}>✕</button>
            </div>

            {/* detail view */}
            {mode === 'detail' && selected ? (
              <div className="travel-detail">
                {selected.latitude != null && selected.longitude != null ? (
                  <div className="travel-detail-map">
                    <Suspense fallback={null}>
                      <StaticLocationMap
                        latitude={selected.latitude}
                        longitude={selected.longitude}
                        radiusKm={15}
                        ariaLabel={`Map of ${selected.destination}`}
                      />
                    </Suspense>
                  </div>
                ) : null}
                <p className="travel-detail-destination">{selected.destination}</p>
                <p className="travel-detail-dates">{formatRange(selected.startDate, selected.endDate)}</p>
                <span className="travel-detail-badge">
                  <span className="travel-tile-badge-icon" aria-hidden>{statusIcon(tripStatus(selected))}</span>
                  <span>{daysLabel(selected)}</span>
                </span>
                {selected.notes ? <p className="travel-detail-notes">{selected.notes}</p> : null}

                <div className="travel-detail-actions">
                  <button className="travel-btn travel-btn-primary" type="button" onClick={startEdit}>Edit</button>
                  <button className="travel-btn travel-btn-danger" type="button" onClick={confirmDelete}>Delete</button>
                </div>
              </div>
            ) : null}

            {/* create / edit form */}
            {(mode === 'create' || mode === 'edit') ? (
              <div className="travel-form">
                <div className="travel-name-wrapper">
                  <input
                    className="travel-input"
                    placeholder="Trip name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    maxLength={120}
                    autoFocus
                  />
                  {destination.trim() && destination.trim() !== name.trim() ? (
                    <button
                      className="travel-name-autofill"
                      type="button"
                      onClick={() => setName(destination.trim())}
                      title={`Use "${destination.trim()}"`}
                    >
                      <span className="travel-name-autofill-icon">↗</span>
                      {destination.trim()}
                    </button>
                  ) : null}
                </div>
                <div className="travel-destination-wrapper">
                  <input
                    className="travel-input"
                    placeholder="Search destination"
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                    maxLength={180}
                  />
                  {geoResults.length > 0 ? (
                    <ul className="travel-geo-results">
                      {geoResults.map((place) => (
                        <li key={place.id}>
                          <button
                            className="travel-geo-result"
                            type="button"
                            onClick={() => {
                              setDestination(place.name)
                              setLat(place.latitude)
                              setLon(place.longitude)
                              skipDestinationSearch(place.name)
                              clearGeoResults()
                            }}
                          >
                            <strong>{place.name}</strong>
                            <span>{place.displayName}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </div>
                <div className="travel-date-row">
                  <input
                    className="travel-input"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    aria-label="Start date"
                  />
                  <span className="travel-date-sep">→</span>
                  <input
                    className="travel-input"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    aria-label="End date"
                  />
                </div>
                <div className="travel-form-map">
                  <Suspense fallback={<div className="travel-form-map-loading" />}>
                    <LocationPickerMap
                      location={destination}
                      latitude={lat}
                      longitude={lon}
                      onSelect={(s) => {
                        setDestination(s.location)
                        setLat(s.latitude)
                        setLon(s.longitude)
                      }}
                      showSearchControls={false}
                    />
                  </Suspense>
                </div>
                <textarea
                  className="travel-input travel-textarea"
                  placeholder="Notes (optional)"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  maxLength={500}
                  rows={2}
                />
                <button
                  className="travel-btn travel-btn-primary travel-btn-full"
                  type="button"
                  disabled={!canSave || modalStatus === 'saving'}
                  onClick={save}
                >
                  {modalStatus === 'saving' ? 'Saving…' : mode === 'edit' ? 'Save changes' : 'Create trip'}
                </button>
              </div>
            ) : null}

            {mode === 'deleting' ? (
              <p className="travel-modal-desc">Deleting…</p>
            ) : null}
          </div>
        </div>
      ) : null}
    </section>
  )
}
