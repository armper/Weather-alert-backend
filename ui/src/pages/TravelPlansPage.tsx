import { useMemo, useState, type FormEvent } from 'react'
import { StaticLocationMap } from '../components/maps/StaticLocationMap'
import { LocationPickerMap } from '../components/maps/LocationPickerMap'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { searchPlaces } from '../services/geocoding'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useAppState } from '../state/useAppState'
import type { TravelPlan } from '../types'

type TravelFilter = 'all' | 'active' | 'upcoming' | 'past'

interface TravelPlanDraft {
  name: string
  destination: string
  startDate: string
  endDate: string
  latitude?: number
  longitude?: number
  notes: string
  alertsEnabled: boolean
}

const FILTER_OPTIONS: Array<{ id: TravelFilter; label: string }> = [
  { id: 'all', label: 'All' },
  { id: 'active', label: 'Active' },
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'past', label: 'Past' },
]

function createEmptyDraft(): TravelPlanDraft {
  return {
    name: '',
    destination: '',
    startDate: '',
    endDate: '',
    latitude: undefined,
    longitude: undefined,
    notes: '',
    alertsEnabled: true,
  }
}

function todayKey() {
  const today = new Date()
  const year = today.getFullYear()
  const month = `${today.getMonth() + 1}`.padStart(2, '0')
  const day = `${today.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getTripStatus(plan: TravelPlan, today = todayKey()): Exclude<TravelFilter, 'all'> {
  if (plan.endDate < today) {
    return 'past'
  }
  if (plan.startDate > today) {
    return 'upcoming'
  }
  return 'active'
}

function formatDateRange(startDate: string, endDate: string) {
  const start = new Date(`${startDate}T12:00:00`)
  const end = new Date(`${endDate}T12:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return `${startDate} - ${endDate}`
  }
  const formatter = new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
  if (startDate === endDate) {
    return formatter.format(start)
  }
  return `${formatter.format(start)} - ${formatter.format(end)}`
}

function formatCountdown(plan: TravelPlan, today = todayKey()) {
  const start = new Date(`${plan.startDate}T12:00:00`)
  const end = new Date(`${plan.endDate}T12:00:00`)
  const now = new Date(`${today}T12:00:00`)
  const daysUntilStart = Math.round((start.getTime() - now.getTime()) / 86400000)
  const daysUntilEnd = Math.round((end.getTime() - now.getTime()) / 86400000)

  if (daysUntilEnd < 0) {
    return 'Completed'
  }
  if (daysUntilStart > 1) {
    return `Starts in ${daysUntilStart} days`
  }
  if (daysUntilStart === 1) {
    return 'Starts tomorrow'
  }
  if (daysUntilStart === 0 && daysUntilEnd === 0) {
    return 'Happening today'
  }
  if (daysUntilStart <= 0) {
    return `In progress, ${daysUntilEnd + 1} day${daysUntilEnd === 0 ? '' : 's'} left`
  }
  return 'Scheduled'
}

export function TravelPlansPage() {
  const {
    criteria,
    savingTravelPlan,
    travelPlans,
    handleCreateTravelPlan,
    handleDeleteTravelPlan,
    handleUpdateTravelPlan,
  } = useAppState()

  const [activeFilter, setActiveFilter] = useState<TravelFilter>('all')
  const [draft, setDraft] = useState<TravelPlanDraft>(createEmptyDraft)
  const [editingPlan, setEditingPlan] = useState<TravelPlan | null>(null)
  const [showDialog, setShowDialog] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [searchingDestination, setSearchingDestination] = useState(false)
  const [deletePendingId, setDeletePendingId] = useState<string | null>(null)

  const today = todayKey()
  const baseLatitude = criteria[0]?.latitude ?? Number(DEFAULT_LAT)
  const baseLongitude = criteria[0]?.longitude ?? Number(DEFAULT_LON)

  const summary = useMemo(() => {
    const active = travelPlans.filter((plan) => getTripStatus(plan, today) === 'active').length
    const upcoming = travelPlans.filter((plan) => getTripStatus(plan, today) === 'upcoming').length
    const alertsOn = travelPlans.filter((plan) => plan.alertsEnabled !== false).length
    return { active, upcoming, alertsOn }
  }, [today, travelPlans])

  const featuredTrip = useMemo(() => {
    const sorted = [...travelPlans].sort((left, right) => left.startDate.localeCompare(right.startDate))
    return (
      sorted.find((plan) => getTripStatus(plan, today) === 'active') ??
      sorted.find((plan) => getTripStatus(plan, today) === 'upcoming') ??
      sorted[0] ??
      null
    )
  }, [today, travelPlans])

  const filteredTrips = useMemo(() => {
    if (activeFilter === 'all') {
      return travelPlans
    }
    return travelPlans.filter((plan) => getTripStatus(plan, today) === activeFilter)
  }, [activeFilter, today, travelPlans])

  function openCreateDialog() {
    setEditingPlan(null)
    setDraft(createEmptyDraft())
    setFormError(null)
    setShowDialog(true)
  }

  function openEditDialog(plan: TravelPlan) {
    setEditingPlan(plan)
    setDraft({
      name: plan.name,
      destination: plan.destination,
      startDate: plan.startDate,
      endDate: plan.endDate,
      latitude: plan.latitude,
      longitude: plan.longitude,
      notes: plan.notes ?? '',
      alertsEnabled: plan.alertsEnabled !== false,
    })
    setFormError(null)
    setShowDialog(true)
  }

  function closeDialog() {
    setShowDialog(false)
    setEditingPlan(null)
    setFormError(null)
  }

  function updateDraft<K extends keyof TravelPlanDraft>(key: K, value: TravelPlanDraft[K]) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  async function handleLocateDestination() {
    if (!draft.destination.trim()) {
      setFormError('Enter a destination before searching the map.')
      return
    }
    setSearchingDestination(true)
    setFormError(null)
    try {
      const results = await searchPlaces(draft.destination)
      const first = results[0]
      if (!first) {
        setFormError('No matching destination found. Try a more specific city or place name.')
        return
      }
      setDraft((current) => ({
        ...current,
        destination: first.name,
        latitude: first.latitude,
        longitude: first.longitude,
      }))
    } catch {
      setFormError('Unable to search destinations right now. Try again in a moment.')
    } finally {
      setSearchingDestination(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)

    if (!draft.name.trim() || !draft.destination.trim() || !draft.startDate || !draft.endDate) {
      setFormError('Trip name, destination, and dates are required.')
      return
    }
    if (draft.endDate < draft.startDate) {
      setFormError('End date must be on or after the start date.')
      return
    }

    const payload = {
      name: draft.name.trim(),
      destination: draft.destination.trim(),
      startDate: draft.startDate,
      endDate: draft.endDate,
      latitude: draft.latitude,
      longitude: draft.longitude,
      notes: draft.notes.trim() || undefined,
      alertsEnabled: draft.alertsEnabled,
    }

    const success = editingPlan
      ? await handleUpdateTravelPlan(editingPlan.id, payload)
      : await handleCreateTravelPlan(payload)

    if (success) {
      closeDialog()
    }
  }

  async function handleDelete(plan: TravelPlan) {
    setDeletePendingId(plan.id)
    try {
      await handleDeleteTravelPlan(plan.id)
    } finally {
      setDeletePendingId(null)
    }
  }

  return (
    <section className="page-stack">
      <article className="panel travel-page">
        <div className="panel-title-row travel-page-header">
          <div>
            <p className="eyebrow">Your Trips</p>
            <h2>Travel Plans</h2>
            <p className="muted travel-page-copy">
              Save travel dates so SkyPanda can watch destination weather before you leave and while you are there.
            </p>
          </div>
          <AriaButton className="primary button-inline" onPress={openCreateDialog}>
            Add trip
          </AriaButton>
        </div>

        <div className="travel-summary-grid">
          <article className="travel-summary-card">
            <span className="travel-summary-label">Active now</span>
            <strong>{summary.active}</strong>
            <span className="muted small">Trips already underway</span>
          </article>
          <article className="travel-summary-card">
            <span className="travel-summary-label">Upcoming</span>
            <strong>{summary.upcoming}</strong>
            <span className="muted small">Trips still ahead on the calendar</span>
          </article>
          <article className="travel-summary-card">
            <span className="travel-summary-label">Alerts on</span>
            <strong>{summary.alertsOn}</strong>
            <span className="muted small">Plans covered by destination weather alerts</span>
          </article>
        </div>

        {featuredTrip ? (
          <section className="travel-feature-panel">
            <div className="travel-feature-copy">
              <p className="eyebrow">Trip Spotlight</p>
              <h3>{featuredTrip.name}</h3>
              <p className="travel-feature-destination">{featuredTrip.destination}</p>
              <p className="muted">{formatDateRange(featuredTrip.startDate, featuredTrip.endDate)}</p>
              <div className="travel-feature-badges">
                <span className={`badge is-${getTripStatus(featuredTrip, today)}`}>{formatCountdown(featuredTrip, today)}</span>
                <span className={`badge ${featuredTrip.alertsEnabled === false ? 'is-muted' : ''}`}>
                  {featuredTrip.alertsEnabled === false ? 'Alerts paused' : 'Alerts active'}
                </span>
              </div>
              {featuredTrip.notes ? <p className="travel-feature-notes">{featuredTrip.notes}</p> : null}
            </div>
            {featuredTrip.latitude !== undefined && featuredTrip.longitude !== undefined ? (
              <StaticLocationMap
                latitude={featuredTrip.latitude}
                longitude={featuredTrip.longitude}
                radiusKm={6}
                className="travel-feature-map"
                ariaLabel={`${featuredTrip.destination} map`}
              />
            ) : (
              <div className="travel-feature-empty-map">
                <strong>Add a map pin for sharper weather matching</strong>
                <span className="muted small">Search the destination or click the map while editing this trip.</span>
              </div>
            )}
          </section>
        ) : null}

        <div className="travel-filter-row" role="tablist" aria-label="Travel plan filters">
          {FILTER_OPTIONS.map((option) => {
            const count =
              option.id === 'all'
                ? travelPlans.length
                : travelPlans.filter((plan) => getTripStatus(plan, today) === option.id).length
            return (
              <button
                key={option.id}
                type="button"
                className={`travel-filter-chip${activeFilter === option.id ? ' active' : ''}`}
                onClick={() => setActiveFilter(option.id)}
              >
                <span>{option.label}</span>
                <span className="travel-filter-count">{count}</span>
              </button>
            )
          })}
        </div>

        {filteredTrips.length === 0 ? (
          <div className="empty-state-panel travel-empty-panel">
            <h3>No trips here yet</h3>
            <p className="muted">
              {activeFilter === 'all'
                ? 'Add your first travel plan to track destination weather with clearer timing and better location context.'
                : `No ${activeFilter} trips right now. Switch filters or add another trip.`}
            </p>
            {activeFilter === 'all' ? (
              <div className="button-row empty-state-actions">
                <AriaButton className="primary button-inline" onPress={openCreateDialog}>
                  Add trip
                </AriaButton>
              </div>
            ) : null}
          </div>
        ) : (
          <div className="travel-card-grid">
            {filteredTrips.map((plan) => {
              const status = getTripStatus(plan, today)
              return (
                <article key={plan.id} className={`travel-card is-${status}`}>
                  <div className="travel-card-header">
                    <div>
                      <h3>{plan.name}</h3>
                      <p className="travel-card-destination">{plan.destination}</p>
                    </div>
                    <span className={`badge is-${status}`}>{status}</span>
                  </div>

                  <div className="travel-card-meta">
                    <span>{formatDateRange(plan.startDate, plan.endDate)}</span>
                    <span>{formatCountdown(plan, today)}</span>
                  </div>

                  {plan.latitude !== undefined && plan.longitude !== undefined ? (
                    <StaticLocationMap
                      latitude={plan.latitude}
                      longitude={plan.longitude}
                      radiusKm={5}
                      className="travel-card-map"
                      ariaLabel={`${plan.destination} preview`}
                    />
                  ) : null}

                  {plan.notes ? <p className="travel-card-notes">{plan.notes}</p> : null}

                  <div className="travel-card-footer">
                    <span className={`badge ${plan.alertsEnabled === false ? 'is-muted' : ''}`}>
                      {plan.alertsEnabled === false ? 'Alerts off' : 'Alerts on'}
                    </span>
                    <div className="travel-card-actions">
                      <AriaButton className="ghost button-inline small" onPress={() => openEditDialog(plan)}>
                        Edit
                      </AriaButton>
                      <AriaButton
                        className="ghost danger button-inline small"
                        onPress={() => void handleDelete(plan)}
                        isDisabled={deletePendingId === plan.id}
                      >
                        {deletePendingId === plan.id ? 'Deleting...' : 'Delete'}
                      </AriaButton>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </article>

      {showDialog ? (
        <div className="account-delete-dialog-backdrop" role="presentation" onClick={closeDialog}>
          <div
            className="account-delete-dialog travel-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="travel-dialog-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="account-delete-dialog-header">
              <div>
                <p className="eyebrow">{editingPlan ? 'Edit trip' : 'New trip'}</p>
                <h3 id="travel-dialog-title">{editingPlan ? editingPlan.name : 'Plan a trip'}</h3>
              </div>
              <AriaButton className="ghost button-inline" onPress={closeDialog}>
                Cancel
              </AriaButton>
            </div>

            <form className="travel-form" onSubmit={(event) => void handleSubmit(event)}>
              <div className="travel-form-grid">
                <AriaTextField
                  label="Trip name"
                  value={draft.name}
                  onChange={(value) => updateDraft('name', value)}
                  placeholder="e.g. Family beach weekend"
                  inputClassName="aria-input"
                  required
                />
                <AriaTextField
                  label="Destination"
                  value={draft.destination}
                  onChange={(value) => updateDraft('destination', value)}
                  placeholder="City, park, or region"
                  inputClassName="aria-input"
                  required
                  description="Search it below to pin the destination and improve weather matching."
                />
              </div>

              <div className="travel-form-grid">
                <AriaTextField
                  label="Start date"
                  value={draft.startDate}
                  onChange={(value) => updateDraft('startDate', value)}
                  type="date"
                  inputClassName="aria-input"
                  required
                />
                <AriaTextField
                  label="End date"
                  value={draft.endDate}
                  onChange={(value) => updateDraft('endDate', value)}
                  type="date"
                  inputClassName="aria-input"
                  min={draft.startDate || undefined}
                  required
                />
              </div>

              <div className="travel-map-toolbar">
                <div>
                  <strong>Destination map</strong>
                  <p className="muted small">Search by destination or click the map to pin the trip more precisely.</p>
                </div>
                <AriaButton
                  className="ghost button-inline"
                  onPress={() => void handleLocateDestination()}
                  isDisabled={searchingDestination}
                >
                  {searchingDestination ? 'Searching...' : 'Find on map'}
                </AriaButton>
              </div>

              <LocationPickerMap
                location={draft.destination}
                latitude={draft.latitude ?? baseLatitude}
                longitude={draft.longitude ?? baseLongitude}
                onSelect={(selection) => {
                  setDraft((current) => ({
                    ...current,
                    destination: selection.location,
                    latitude: selection.latitude,
                    longitude: selection.longitude,
                  }))
                }}
                showSearchControls={false}
              />

              <div className="travel-form-grid">
                <AriaTextField
                  label="Notes"
                  value={draft.notes}
                  onChange={(value) => updateDraft('notes', value)}
                  placeholder="Packing reminder, schedule risk, or trip context"
                  inputClassName="aria-input"
                />
                <div className="travel-form-switch">
                  <AriaSwitch
                    label="Enable travel alerts"
                    isSelected={draft.alertsEnabled}
                    onChange={(value) => updateDraft('alertsEnabled', value)}
                  />
                  <p className="muted small">Keep destination weather alerts active for this trip.</p>
                </div>
              </div>

              {formError ? <p className="field-error">{formError}</p> : null}

              <div className="account-delete-dialog-actions">
                <AriaButton className="ghost button-inline" onPress={closeDialog} type="button">
                  Keep editing later
                </AriaButton>
                <AriaButton className="primary button-inline" type="submit" isDisabled={savingTravelPlan}>
                  {savingTravelPlan ? 'Saving...' : editingPlan ? 'Save changes' : 'Add trip'}
                </AriaButton>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  )
}
