import { Suspense, lazy, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { describeCriteria } from '../lib/criteria'
import { LoadingPlaceholder } from '../components/common/LoadingPlaceholder'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { searchPlaces } from '../services/geocoding'
import { DEFAULT_LAT, DEFAULT_LON } from '../state/types'
import { useActionState, useAsyncState, useDataState, useSessionState } from '../state/useAppState'
import type { AlertCriteria, TravelAlertCoverageMode, TravelAlertTopic, TravelPlan } from '../types'

const LocationPickerMap = lazy(() =>
  import('../components/maps/LocationPickerMap').then((module) => ({ default: module.LocationPickerMap })),
)
const StaticLocationMap = lazy(() =>
  import('../components/maps/StaticLocationMap').then((module) => ({ default: module.StaticLocationMap })),
)

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
  alertCoverageMode: TravelAlertCoverageMode
  selectedAlertTopics: TravelAlertTopic[]
  linkedCriteriaIds: string[]
}

interface TravelCoverageSummary {
  title: string
  detail: string
  chips: string[]
  emphasis: string
}

const FILTER_OPTIONS: Array<{ id: TravelFilter; label: string }> = [
  { id: 'all', label: 'All' },
  { id: 'active', label: 'Active' },
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'past', label: 'Past' },
]

const COVERAGE_MODE_OPTIONS: Array<{
  id: TravelAlertCoverageMode
  label: string
  summary: string
}> = [
  {
    id: 'ALL_ALERTS',
    label: 'All alerts',
    summary: 'Use every active saved rule you already run.',
  },
  {
    id: 'TOPICS',
    label: 'Trip topics',
    summary: 'Pick only the weather situations that matter on this trip.',
  },
  {
    id: 'LINKED_RULES',
    label: 'Specific saved rules',
    summary: 'Attach exact saved alerts and leave the rest out.',
  },
]

const TOPIC_OPTIONS: Array<{
  id: TravelAlertTopic
  label: string
  summary: string
}> = [
  { id: 'RAIN', label: 'Rain', summary: 'Wet travel days, showers, and storm timing.' },
  { id: 'WIND', label: 'Wind', summary: 'Windy drives, gusts, and outdoor disruptions.' },
  { id: 'HEAT', label: 'Heat', summary: 'Hot afternoons, heat stress, and sun exposure.' },
  { id: 'COLD', label: 'Cold', summary: 'Cold snaps, chilly nights, and freeze risk.' },
  { id: 'HUMIDITY', label: 'Humidity', summary: 'Muggy air or very dry conditions.' },
  { id: 'SKY', label: 'Sky', summary: 'Cloud cover swings for views, photos, or solar plans.' },
  { id: 'RIVER', label: 'River', summary: 'River rise, flood stage, and waterfront impacts.' },
]

const TOPIC_PRESETS: Array<{
  id: string
  label: string
  topics: TravelAlertTopic[]
}> = [
  { id: 'travel-basics', label: 'Travel basics', topics: ['RAIN', 'WIND'] },
  { id: 'outdoor-plans', label: 'Outdoor plans', topics: ['RAIN', 'WIND', 'HEAT'] },
  { id: 'waterfront', label: 'Waterfront', topics: ['RAIN', 'WIND', 'RIVER'] },
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
    alertCoverageMode: 'TOPICS',
    selectedAlertTopics: ['RAIN', 'WIND'],
    linkedCriteriaIds: [],
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

function hasCoordinates(
  plan: Pick<TravelPlan, 'latitude' | 'longitude'>,
): plan is Pick<TravelPlan, 'latitude' | 'longitude'> & { latitude: number; longitude: number } {
  return plan.latitude != null && plan.longitude != null
}

function getTopicLabel(topic: TravelAlertTopic) {
  return TOPIC_OPTIONS.find((item) => item.id === topic)?.label ?? topic
}

function normalizeCoverageMode(plan: Pick<TravelPlan, 'alertCoverageMode'>): TravelAlertCoverageMode {
  return plan.alertCoverageMode ?? 'ALL_ALERTS'
}

function normalizeSelectedTopics(plan: Pick<TravelPlan, 'selectedAlertTopics'>): TravelAlertTopic[] {
  return plan.selectedAlertTopics ?? []
}

function normalizeLinkedCriteriaIds(plan: Pick<TravelPlan, 'linkedCriteriaIds'>): string[] {
  return plan.linkedCriteriaIds ?? []
}

function deriveCriteriaTopic(criteria: AlertCriteria): TravelAlertTopic | null {
  if (criteria.temperatureThreshold != null) {
    return criteria.temperatureDirection === 'BELOW' ? 'COLD' : 'HEAT'
  }
  if (criteria.maxWindSpeed != null || criteria.windGustThreshold != null) {
    return 'WIND'
  }
  if (criteria.rainThreshold != null) {
    return 'RAIN'
  }
  if (criteria.humidityThreshold != null || criteria.dewPointThreshold != null) {
    return 'HUMIDITY'
  }
  if (criteria.skyCoverThreshold != null) {
    return 'SKY'
  }
  if (criteria.riverStageThreshold != null || criteria.riverFloodCategoryThreshold) {
    return 'RIVER'
  }
  return null
}

function buildRuleLabel(criteria: AlertCriteria) {
  const topic = deriveCriteriaTopic(criteria)
  return criteria.name?.trim() || (topic ? `${getTopicLabel(topic)} rule` : 'Saved alert')
}

function buildCoverageSummary(
  plan: TravelPlan,
  criteriaById: Map<string, AlertCriteria>,
  activeCriteria: AlertCriteria[],
): TravelCoverageSummary {
  if (plan.alertsEnabled === false) {
    return {
      title: 'Coverage paused',
      detail: 'This trip is saved, but SkyPanda will not watch weather for it right now.',
      chips: ['Paused'],
      emphasis: 'paused',
    }
  }

  const mode = normalizeCoverageMode(plan)
  if (mode === 'TOPICS') {
    const topics = normalizeSelectedTopics(plan)
    if (topics.length === 0) {
      return {
        title: 'Trip topics',
        detail: 'Choose the weather topics that matter on this trip.',
        chips: ['Needs setup'],
        emphasis: 'attention',
      }
    }
    return {
      title: 'Trip topics',
      detail: `Watching ${topics.map(getTopicLabel).join(', ')} for this trip.`,
      chips: topics.map(getTopicLabel),
      emphasis: 'focused',
    }
  }

  if (mode === 'LINKED_RULES') {
    const linkedRules = normalizeLinkedCriteriaIds(plan)
      .map((criteriaId) => criteriaById.get(criteriaId))
      .filter((criteria): criteria is AlertCriteria => Boolean(criteria))

    if (linkedRules.length === 0) {
      return {
        title: 'Specific saved rules',
        detail: 'This trip is set to exact saved alerts, but none are currently linked.',
        chips: ['Needs relink'],
        emphasis: 'attention',
      }
    }

    return {
      title: 'Specific saved rules',
      detail: `${linkedRules.length} saved rule${linkedRules.length === 1 ? '' : 's'} tied directly to this trip.`,
      chips: linkedRules.slice(0, 3).map((item) => buildRuleLabel(item)),
      emphasis: 'linked',
    }
  }

  if (activeCriteria.length === 0) {
    return {
      title: 'All alerts',
      detail: 'This trip will follow your active alerts once you create some saved rules.',
      chips: ['No saved rules yet'],
      emphasis: 'default',
    }
  }

  return {
    title: 'All alerts',
    detail: `${activeCriteria.length} active saved rule${activeCriteria.length === 1 ? '' : 's'} will follow this trip.`,
    chips: ['All active rules'],
    emphasis: 'default',
  }
}

function buildCoveragePreview(
  draft: TravelPlanDraft,
  criteriaById: Map<string, AlertCriteria>,
  activeCriteria: AlertCriteria[],
): string {
  if (!draft.alertsEnabled) {
    return 'Trip monitoring is off. SkyPanda will save the itinerary but ignore weather until you turn coverage back on.'
  }

  if (draft.alertCoverageMode === 'TOPICS') {
    if (draft.selectedAlertTopics.length === 0) {
      return 'Pick at least one weather topic so this trip knows what to watch.'
    }
    return `SkyPanda will watch ${draft.selectedAlertTopics.map(getTopicLabel).join(', ')} for this trip.`
  }

  if (draft.alertCoverageMode === 'LINKED_RULES') {
    const linkedLabels = draft.linkedCriteriaIds
      .map((criteriaId) => criteriaById.get(criteriaId))
      .filter((criteria): criteria is AlertCriteria => Boolean(criteria))
      .map((criteria) => buildRuleLabel(criteria))

    if (linkedLabels.length === 0) {
      return 'Link one or more saved rules to tell SkyPanda exactly which alerts should travel with this trip.'
    }
    return `This trip will use ${linkedLabels.join(', ')}.`
  }

  if (activeCriteria.length === 0) {
    return 'All-alert coverage is selected, but there are no active saved rules yet.'
  }

  return `This trip will inherit all ${activeCriteria.length} active saved rule${activeCriteria.length === 1 ? '' : 's'}.`
}

export function TravelPlansPage() {
  const { initialDataLoading } = useSessionState()
  const { savingTravelPlan } = useAsyncState()
  const { handleCreateTravelPlan, handleDeleteTravelPlan, handleUpdateTravelPlan } = useActionState()
  const {
    billingStatus,
    criteria,
    travelPlans,
  } = useDataState()

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

  const activeCriteria = useMemo(() => criteria.filter((item) => item.enabled !== false), [criteria])
  const maxTravelPlans = billingStatus?.maxTravelPlans ?? null
  const tripLimitReached = maxTravelPlans != null && maxTravelPlans > 0 && travelPlans.length >= maxTravelPlans
  const travelLockedOnPlan = maxTravelPlans === 0
  const canCreateTrips = maxTravelPlans == null || (!travelLockedOnPlan && !tripLimitReached)
  const criteriaById = useMemo(() => new Map(criteria.map((item) => [item.id, item])), [criteria])
  const linkedRuleOptions = useMemo(() => activeCriteria, [activeCriteria])
  const topicCounts = useMemo(() => {
    const counts = new Map<TravelAlertTopic, number>()
    activeCriteria.forEach((item) => {
      const topic = deriveCriteriaTopic(item)
      if (!topic) {
        return
      }
      counts.set(topic, (counts.get(topic) ?? 0) + 1)
    })
    return counts
  }, [activeCriteria])

  const summary = useMemo(() => {
    const active = travelPlans.filter((plan) => getTripStatus(plan, today) === 'active').length
    const upcoming = travelPlans.filter((plan) => getTripStatus(plan, today) === 'upcoming').length
    const focusedCoverage = travelPlans.filter((plan) => {
      if (plan.alertsEnabled === false) {
        return false
      }
      const mode = normalizeCoverageMode(plan)
      return mode === 'TOPICS' || mode === 'LINKED_RULES'
    }).length
    return { active, upcoming, focusedCoverage }
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
    if (!canCreateTrips) {
      return
    }
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
      latitude: plan.latitude ?? undefined,
      longitude: plan.longitude ?? undefined,
      notes: plan.notes ?? '',
      alertsEnabled: plan.alertsEnabled !== false,
      alertCoverageMode: normalizeCoverageMode(plan),
      selectedAlertTopics: normalizeSelectedTopics(plan),
      linkedCriteriaIds: normalizeLinkedCriteriaIds(plan),
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

  function toggleTopic(topic: TravelAlertTopic) {
    setDraft((current) => ({
      ...current,
      selectedAlertTopics: current.selectedAlertTopics.includes(topic)
        ? current.selectedAlertTopics.filter((item) => item !== topic)
        : [...current.selectedAlertTopics, topic],
    }))
  }

  function applyTopicPreset(topics: TravelAlertTopic[]) {
    setDraft((current) => ({
      ...current,
      alertCoverageMode: 'TOPICS',
      selectedAlertTopics: topics,
    }))
  }

  function toggleLinkedRule(criteriaId: string) {
    setDraft((current) => ({
      ...current,
      linkedCriteriaIds: current.linkedCriteriaIds.includes(criteriaId)
        ? current.linkedCriteriaIds.filter((item) => item !== criteriaId)
        : [...current.linkedCriteriaIds, criteriaId],
    }))
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
    if (draft.alertsEnabled && draft.alertCoverageMode === 'TOPICS' && draft.selectedAlertTopics.length === 0) {
      setFormError('Choose at least one trip topic, or switch to all alerts or specific saved rules.')
      return
    }
    if (draft.alertsEnabled && draft.alertCoverageMode === 'LINKED_RULES' && draft.linkedCriteriaIds.length === 0) {
      setFormError('Link at least one saved rule, or switch to trip topics or all alerts.')
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
      alertCoverageMode: draft.alertCoverageMode,
      selectedAlertTopics: draft.alertsEnabled && draft.alertCoverageMode === 'TOPICS' ? draft.selectedAlertTopics : [],
      linkedCriteriaIds: draft.alertsEnabled && draft.alertCoverageMode === 'LINKED_RULES' ? draft.linkedCriteriaIds : [],
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

  const coveragePreview = buildCoveragePreview(draft, criteriaById, activeCriteria)
  const featuredCoverage = featuredTrip ? buildCoverageSummary(featuredTrip, criteriaById, activeCriteria) : null

  return (
    <section className="page-stack">
      <article className="panel travel-page">
        <div className="panel-title-row travel-page-header">
          <div>
            <p className="eyebrow">Your Trips</p>
            <h2>Travel Plans</h2>
            <p className="muted travel-page-copy">
              Plan the itinerary, then decide whether this trip should inherit your full alert lineup, follow only a few
              weather topics like rain, or use exact saved rules.
            </p>
          </div>
          <AriaButton className="primary button-inline" onPress={openCreateDialog} isDisabled={initialDataLoading || !canCreateTrips}>
            Add trip
          </AriaButton>
        </div>

        {travelLockedOnPlan ? (
          <div className="travel-coverage-callout">
            <strong>Travel monitoring starts on The Family Plan</strong>
            <p className="muted small">
              The free tier is built for one home-base alert. Upgrade on{' '}
              <Link to="/app/account" className="auth-link">
                Account
              </Link>{' '}
              to unlock trip planning, destination weather tracking, and focused trip coverage.
            </p>
          </div>
        ) : null}

        {tripLimitReached ? (
          <div className="travel-coverage-callout">
            <strong>Trip limit reached</strong>
            <p className="muted small">
              You are using {travelPlans.length} of {maxTravelPlans} trip slot{maxTravelPlans === 1 ? '' : 's'}. Upgrade on{' '}
              <Link to="/app/account" className="auth-link">
                Account
              </Link>{' '}
              to add more trips.
            </p>
          </div>
        ) : null}

        {maxTravelPlans != null && maxTravelPlans > 0 ? (
          <div className="travel-coverage-preview">
            <strong>{initialDataLoading ? 'Loading trip usage…' : `${travelPlans.length}/${maxTravelPlans} trip slots used`}</strong>
            <p className="muted small">
              The Family Plan includes 3 trips. The Globetrotter includes 15 for heavier travel coverage.
            </p>
          </div>
        ) : null}

        {initialDataLoading ? (
          <LoadingPlaceholder
            title="Loading trips"
            copy="Fetching saved itineraries, travel coverage, and current plan limits."
            lineCount={4}
          />
        ) : null}

        <div className="travel-summary-grid">
          <article className="travel-summary-card">
            <span className="travel-summary-label">Active now</span>
            <strong>{initialDataLoading ? '...' : summary.active}</strong>
            <span className="muted small">Trips already underway</span>
          </article>
          <article className="travel-summary-card">
            <span className="travel-summary-label">Upcoming</span>
            <strong>{initialDataLoading ? '...' : summary.upcoming}</strong>
            <span className="muted small">Trips still ahead on the calendar</span>
          </article>
          <article className="travel-summary-card">
            <span className="travel-summary-label">Focused coverage</span>
            <strong>{initialDataLoading ? '...' : summary.focusedCoverage}</strong>
            <span className="muted small">Trips using custom topics or linked saved rules</span>
          </article>
        </div>

        {!initialDataLoading && featuredTrip ? (
          <section className="travel-feature-panel">
            <div className="travel-feature-copy">
              <p className="eyebrow">Trip Spotlight</p>
              <h3>{featuredTrip.name}</h3>
              <p className="travel-feature-destination">{featuredTrip.destination}</p>
              <p className="muted">{formatDateRange(featuredTrip.startDate, featuredTrip.endDate)}</p>
              <div className="travel-feature-badges">
                <span className={`badge is-${getTripStatus(featuredTrip, today)}`}>{formatCountdown(featuredTrip, today)}</span>
              </div>
              <div className={`travel-coverage-box is-${featuredCoverage?.emphasis ?? 'default'}`}>
                <strong>{featuredCoverage?.title}</strong>
                <p className="muted small">{featuredCoverage?.detail}</p>
                <div className="travel-coverage-chip-row">
                  {featuredCoverage?.chips.map((chip) => (
                    <span key={chip} className="travel-coverage-chip">
                      {chip}
                    </span>
                  ))}
                </div>
              </div>
              {featuredTrip.notes ? <p className="travel-feature-notes">{featuredTrip.notes}</p> : null}
            </div>
            {hasCoordinates(featuredTrip) ? (
              <Suspense
                fallback={
                  <div className="travel-feature-map static-map-shell">
                    <LoadingPlaceholder title="Loading map" copy="Preparing trip spotlight." compact />
                  </div>
                }
              >
                <StaticLocationMap
                  latitude={featuredTrip.latitude}
                  longitude={featuredTrip.longitude}
                  radiusKm={6}
                  className="travel-feature-map"
                  ariaLabel={`${featuredTrip.destination} map`}
                />
              </Suspense>
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
                <span className="travel-filter-count">{initialDataLoading ? '...' : count}</span>
              </button>
            )
          })}
        </div>

        {initialDataLoading ? (
          <LoadingPlaceholder
            title="Loading trip list"
            copy="Sorting your itineraries and coverage settings for this page."
            lineCount={5}
          />
        ) : filteredTrips.length === 0 ? (
          <div className="empty-state-panel travel-empty-panel">
            <h3>No trips here yet</h3>
            <p className="muted">
              {activeFilter === 'all'
                ? 'Add your first trip, then tell SkyPanda whether it should watch everything, just rain and wind, or a hand-picked set of saved alerts.'
                : `No ${activeFilter} trips right now. Switch filters or add another trip.`}
            </p>
            {activeFilter === 'all' ? (
              <div className="button-row empty-state-actions">
                <AriaButton className="primary button-inline" onPress={openCreateDialog} isDisabled={!canCreateTrips}>
                  Add trip
                </AriaButton>
              </div>
            ) : null}
          </div>
        ) : (
          <div className="travel-card-grid">
            {filteredTrips.map((plan) => {
              const status = getTripStatus(plan, today)
              const coverage = buildCoverageSummary(plan, criteriaById, activeCriteria)

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

                  {hasCoordinates(plan) ? (
                    <Suspense
                      fallback={
                        <div className="travel-card-map static-map-shell">
                          <LoadingPlaceholder title="Loading map" copy="Rendering trip preview." compact />
                        </div>
                      }
                    >
                      <StaticLocationMap
                        latitude={plan.latitude}
                        longitude={plan.longitude}
                        radiusKm={5}
                        className="travel-card-map"
                        ariaLabel={`${plan.destination} preview`}
                      />
                    </Suspense>
                  ) : null}

                  <div className={`travel-coverage-box is-${coverage.emphasis}`}>
                    <strong>{coverage.title}</strong>
                    <p className="muted small">{coverage.detail}</p>
                    <div className="travel-coverage-chip-row">
                      {coverage.chips.map((chip) => (
                        <span key={chip} className="travel-coverage-chip">
                          {chip}
                        </span>
                      ))}
                    </div>
                  </div>

                  {plan.notes ? <p className="travel-card-notes">{plan.notes}</p> : null}

                  <div className="travel-card-footer">
                    <span className={`badge ${plan.alertsEnabled === false ? 'is-muted' : ''}`}>
                      {plan.alertsEnabled === false ? 'Monitoring off' : 'Monitoring on'}
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
                  type="button"
                >
                  {searchingDestination ? 'Searching...' : 'Find on map'}
                </AriaButton>
              </div>

              <Suspense
                fallback={
                  <div className="location-map-shell">
                    <LoadingPlaceholder title="Loading map" copy="Preparing destination picker." compact />
                  </div>
                }
              >
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
              </Suspense>

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
                    label="Watch weather for this trip"
                    isSelected={draft.alertsEnabled}
                    onChange={(value) => updateDraft('alertsEnabled', value)}
                  />
                  <p className="muted small">Turn this off if you just want the itinerary saved without trip monitoring.</p>
                </div>
              </div>

              <section className="travel-coverage-panel">
                <div className="travel-coverage-panel-header">
                  <div>
                    <strong>Trip coverage</strong>
                    <p className="muted small">
                      Decide whether this trip should inherit all your alerts, only a few weather topics, or exact saved
                      rules.
                    </p>
                  </div>
                </div>

                {draft.alertsEnabled ? (
                  <>
                    <div className="travel-mode-grid" role="radiogroup" aria-label="Trip coverage mode">
                      {COVERAGE_MODE_OPTIONS.map((option) => (
                        <button
                          key={option.id}
                          type="button"
                          role="radio"
                          aria-checked={draft.alertCoverageMode === option.id}
                          className={`travel-mode-button${draft.alertCoverageMode === option.id ? ' is-active' : ''}`}
                          onClick={() => updateDraft('alertCoverageMode', option.id)}
                        >
                          <strong>{option.label}</strong>
                          <span>{option.summary}</span>
                        </button>
                      ))}
                    </div>

                    {draft.alertCoverageMode === 'ALL_ALERTS' ? (
                      <div className="travel-coverage-callout">
                        <strong>Use the full alert lineup</strong>
                        <p className="muted small">
                          Best when this trip should follow every active saved rule you already trust day to day.
                        </p>
                      </div>
                    ) : null}

                    {draft.alertCoverageMode === 'TOPICS' ? (
                      <div className="travel-coverage-stack">
                        <div className="travel-topic-preset-row">
                          {TOPIC_PRESETS.map((preset) => (
                            <button
                              key={preset.id}
                              type="button"
                              className="travel-preset-chip"
                              onClick={() => applyTopicPreset(preset.topics)}
                            >
                              {preset.label}
                            </button>
                          ))}
                        </div>

                        <div className="travel-topic-grid">
                          {TOPIC_OPTIONS.map((topic) => {
                            const isSelected = draft.selectedAlertTopics.includes(topic.id)
                            const matchingRules = topicCounts.get(topic.id) ?? 0

                            return (
                              <button
                                key={topic.id}
                                type="button"
                                aria-pressed={isSelected}
                                className={`travel-topic-button${isSelected ? ' is-selected' : ''}`}
                                onClick={() => toggleTopic(topic.id)}
                              >
                                <div className="travel-topic-button-head">
                                  <strong>{topic.label}</strong>
                                  <span className="travel-selection-indicator">{isSelected ? 'Selected' : 'Add'}</span>
                                </div>
                                <span>{topic.summary}</span>
                                <span className="travel-topic-helper">
                                  {matchingRules > 0
                                    ? `${matchingRules} saved rule${matchingRules === 1 ? '' : 's'} already match this topic`
                                    : 'No saved rules for this topic yet'}
                                </span>
                              </button>
                            )
                          })}
                        </div>
                      </div>
                    ) : null}

                    {draft.alertCoverageMode === 'LINKED_RULES' ? (
                      <div className="travel-coverage-stack">
                        {linkedRuleOptions.length > 0 ? (
                          <div className="travel-rule-list">
                            {linkedRuleOptions.map((item) => {
                              const topic = deriveCriteriaTopic(item)
                              const isSelected = draft.linkedCriteriaIds.includes(item.id)

                              return (
                                <button
                                  key={item.id}
                                  type="button"
                                  aria-pressed={isSelected}
                                  className={`travel-rule-option${isSelected ? ' is-selected' : ''}`}
                                  onClick={() => toggleLinkedRule(item.id)}
                                >
                                  <div className="travel-rule-copy">
                                    <strong>{buildRuleLabel(item)}</strong>
                                    <span className="muted small">{item.location || 'Saved alert'}</span>
                                    <span className="muted small">{describeCriteria(item)}</span>
                                  </div>
                                  <div className="travel-rule-side">
                                    {topic ? <span className="travel-coverage-chip">{getTopicLabel(topic)}</span> : null}
                                    <span className="travel-selection-indicator">{isSelected ? 'Selected' : 'Select'}</span>
                                  </div>
                                </button>
                              )
                            })}
                          </div>
                        ) : (
                          <div className="travel-coverage-callout">
                            <strong>No saved rules available yet</strong>
                            <p className="muted small">
                              Switch to trip topics now, or create a saved alert first on <a href="/app/rules">New Alert</a>.
                            </p>
                          </div>
                        )}
                      </div>
                    ) : null}
                  </>
                ) : (
                  <div className="travel-coverage-callout is-muted">
                    <strong>Trip monitoring is off</strong>
                    <p className="muted small">Turn on trip monitoring when you want this itinerary to drive weather coverage.</p>
                  </div>
                )}

                <div className="travel-coverage-preview">
                  <strong>Coverage preview</strong>
                  <p className="muted small">{coveragePreview}</p>
                </div>
              </section>

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
