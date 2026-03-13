import { useState, useMemo } from 'react'
import { AriaTextField } from '../components/ui/AriaTextField'
import { AriaButton } from '../components/ui/AriaButton'
import { useAppState } from '../state/useAppState'
import type { TravelPlan } from '../types'

type TabFilter = 'all' | 'upcoming' | 'active' | 'past'

function getTripStatus(plan: TravelPlan): 'active' | 'upcoming' | 'past' {
  const today = new Date().toISOString().slice(0, 10)
  if (plan.startDate <= today && plan.endDate >= today) return 'active'
  if (plan.startDate > today) return 'upcoming'
  return 'past'
}

function formatDateRange(startDate: string, endDate: string): string {
  const fmt = (d: string) => {
    const [year, month, day] = d.split('-')
    const date = new Date(Number(year), Number(month) - 1, Number(day))
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  }
  return `${fmt(startDate)} – ${fmt(endDate)}`
}

interface TravelFormState {
  name: string
  destination: string
  latitude: string
  longitude: string
  startDate: string
  endDate: string
  notes: string
  alertsEnabled: boolean
}

const emptyForm = (): TravelFormState => ({
  name: '',
  destination: '',
  latitude: '',
  longitude: '',
  startDate: '',
  endDate: '',
  notes: '',
  alertsEnabled: true,
})

function planToForm(plan: TravelPlan): TravelFormState {
  return {
    name: plan.name ?? '',
    destination: plan.destination ?? '',
    latitude: plan.latitude != null ? String(plan.latitude) : '',
    longitude: plan.longitude != null ? String(plan.longitude) : '',
    startDate: plan.startDate ?? '',
    endDate: plan.endDate ?? '',
    notes: plan.notes ?? '',
    alertsEnabled: plan.alertsEnabled !== false,
  }
}

export function TravelPage() {
  const { travelPlans, handleCreateTravelPlan, handleUpdateTravelPlan, handleDeleteTravelPlan, savingTravelPlan } =
    useAppState()

  const [activeTab, setActiveTab] = useState<TabFilter>('all')
  const [showForm, setShowForm] = useState(false)
  const [editingPlan, setEditingPlan] = useState<TravelPlan | null>(null)
  const [form, setForm] = useState<TravelFormState>(emptyForm())
  const [formError, setFormError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const filteredPlans = useMemo(() => {
    if (activeTab === 'all') return travelPlans
    return travelPlans.filter((p) => getTripStatus(p) === activeTab)
  }, [travelPlans, activeTab])

  const counts = useMemo(() => {
    return {
      all: travelPlans.length,
      upcoming: travelPlans.filter((p) => getTripStatus(p) === 'upcoming').length,
      active: travelPlans.filter((p) => getTripStatus(p) === 'active').length,
      past: travelPlans.filter((p) => getTripStatus(p) === 'past').length,
    }
  }, [travelPlans])

  function openCreateForm() {
    setEditingPlan(null)
    setForm(emptyForm())
    setFormError(null)
    setShowForm(true)
  }

  function openEditForm(plan: TravelPlan) {
    setEditingPlan(plan)
    setForm(planToForm(plan))
    setFormError(null)
    setShowForm(true)
  }

  function closeForm() {
    setShowForm(false)
    setEditingPlan(null)
    setFormError(null)
  }

  function setField<K extends keyof TravelFormState>(key: K, value: TravelFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setFormError(null)

    if (!form.name.trim()) {
      setFormError('Trip name is required.')
      return
    }
    if (!form.destination.trim()) {
      setFormError('Destination is required.')
      return
    }
    if (!form.startDate) {
      setFormError('Start date is required.')
      return
    }
    if (!form.endDate) {
      setFormError('End date is required.')
      return
    }
    if (form.endDate < form.startDate) {
      setFormError('End date must be on or after start date.')
      return
    }

    const payload = {
      name: form.name.trim(),
      destination: form.destination.trim(),
      latitude: form.latitude.trim() ? Number(form.latitude) : undefined,
      longitude: form.longitude.trim() ? Number(form.longitude) : undefined,
      startDate: form.startDate,
      endDate: form.endDate,
      notes: form.notes.trim() || undefined,
      alertsEnabled: form.alertsEnabled,
    }

    let success = false
    if (editingPlan) {
      success = await handleUpdateTravelPlan(editingPlan.id, payload)
    } else {
      success = await handleCreateTravelPlan(payload)
    }
    if (success) {
      closeForm()
    }
  }

  async function confirmDelete(planId: string) {
    setDeletingId(planId)
    await handleDeleteTravelPlan(planId)
    setDeletingId(null)
  }

  const TABS: Array<{ key: TabFilter; label: string }> = [
    { key: 'all', label: 'All' },
    { key: 'active', label: 'Active' },
    { key: 'upcoming', label: 'Upcoming' },
    { key: 'past', label: 'Past' },
  ]

  return (
    <section className="travel-page-stack page-stack">
      <article className="panel">
        <div className="travel-header-row panel-title-row">
          <div>
            <p className="eyebrow">Your trips</p>
            <h2>Travel Plans</h2>
            <p className="muted small">
              Add upcoming trips to receive weather alerts for each destination on the dates you'll be there.
            </p>
          </div>
          <AriaButton className="primary" onPress={openCreateForm}>
            <span aria-hidden>✈️</span> Add trip
          </AriaButton>
        </div>

        <div className="travel-section-tabs">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`travel-tab-btn${activeTab === tab.key ? ' is-active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
              <span className="badge" style={{ marginLeft: '0.4rem', fontSize: '0.68rem', padding: '0.1rem 0.4rem' }}>
                {counts[tab.key]}
              </span>
            </button>
          ))}
        </div>

        {filteredPlans.length === 0 ? (
          <div className="travel-empty-state">
            <div className="travel-empty-icon" aria-hidden>
              🌍
            </div>
            <h3>No trips here yet</h3>
            <p className="muted">
              {activeTab === 'all'
                ? 'Add your first travel plan and SkyPanda will monitor weather at your destination.'
                : `No ${activeTab} trips. Switch to "All" or add a new trip.`}
            </p>
            {activeTab === 'all' ? (
              <div className="button-row">
                <AriaButton className="primary" onPress={openCreateForm}>
                  Add trip
                </AriaButton>
              </div>
            ) : null}
          </div>
        ) : (
          <div className="travel-grid">
            {filteredPlans.map((plan) => {
              const status = getTripStatus(plan)
              return (
                <article key={plan.id} className={`travel-card is-${status}`}>
                  <div className="travel-card-header">
                    <div>
                      <h3 className="travel-card-title">{plan.name}</h3>
                      <p className="travel-card-destination">
                        <span aria-hidden>📍</span> {plan.destination}
                      </p>
                    </div>
                    <span className={`travel-status-badge is-${status}`}>
                      {status === 'active' ? '🟢 Now' : status === 'upcoming' ? '🔵 Upcoming' : '⚫ Past'}
                    </span>
                  </div>

                  <p className="travel-card-dates">
                    <span aria-hidden>📅</span> {formatDateRange(plan.startDate, plan.endDate)}
                  </p>

                  {plan.notes ? <p className="travel-card-notes">{plan.notes}</p> : null}

                  <div className="travel-card-footer">
                    <span className={`travel-alerts-badge${plan.alertsEnabled === false ? ' is-off' : ''}`}>
                      {plan.alertsEnabled !== false ? '🔔 Alerts on' : '🔕 Alerts off'}
                    </span>
                    <div className="travel-card-actions">
                      <AriaButton
                        className="ghost small"
                        aria-label={`Edit ${plan.name}`}
                        onPress={() => openEditForm(plan)}
                      >
                        Edit
                      </AriaButton>
                      <AriaButton
                        className="ghost small danger"
                        aria-label={`Delete ${plan.name}`}
                        isDisabled={deletingId === plan.id}
                        onPress={() => void confirmDelete(plan.id)}
                      >
                        {deletingId === plan.id ? '…' : 'Delete'}
                      </AriaButton>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </article>

      {showForm ? (
        <div
          className="travel-form-backdrop"
          role="dialog"
          aria-modal="true"
          aria-labelledby="travel-form-title"
          onClick={(e) => {
            if (e.target === e.currentTarget) closeForm()
          }}
        >
          <div className="travel-form-dialog">
            <div className="travel-form-header">
              <div>
                <p className="eyebrow">{editingPlan ? 'Edit trip' : 'New trip'}</p>
                <h3 id="travel-form-title">{editingPlan ? `Editing: ${editingPlan.name}` : 'Plan a trip'}</h3>
              </div>
              <AriaButton className="ghost" aria-label="Close" onPress={closeForm}>
                ✕
              </AriaButton>
            </div>

            <form className="travel-form-fields" onSubmit={(e) => void handleSubmit(e)}>
              <AriaTextField
                label="Trip name"
                value={form.name}
                onChange={(v) => setField('name', v)}
                placeholder="e.g. NYC Conference"
                inputClassName="aria-input"
                required
              />

              <AriaTextField
                label="Destination"
                value={form.destination}
                onChange={(v) => setField('destination', v)}
                placeholder="e.g. New York City"
                inputClassName="aria-input"
                required
                description="City name or location description used for weather lookups."
              />

              <div className="travel-form-row">
                <AriaTextField
                  label="Start date"
                  value={form.startDate}
                  onChange={(v) => setField('startDate', v)}
                  type="date"
                  inputClassName="aria-input"
                  required
                />
                <AriaTextField
                  label="End date"
                  value={form.endDate}
                  onChange={(v) => setField('endDate', v)}
                  type="date"
                  inputClassName="aria-input"
                  min={form.startDate || undefined}
                  required
                />
              </div>

              <div className="travel-form-coords">
                <AriaTextField
                  label="Latitude (optional)"
                  value={form.latitude}
                  onChange={(v) => setField('latitude', v)}
                  type="number"
                  step="any"
                  placeholder="40.7128"
                  inputClassName="aria-input"
                  description="Improves location accuracy."
                />
                <AriaTextField
                  label="Longitude (optional)"
                  value={form.longitude}
                  onChange={(v) => setField('longitude', v)}
                  type="number"
                  step="any"
                  placeholder="-74.0060"
                  inputClassName="aria-input"
                />
              </div>

              <AriaTextField
                label="Notes (optional)"
                value={form.notes}
                onChange={(v) => setField('notes', v)}
                placeholder="Any trip context…"
                inputClassName="aria-input"
              />

              <label className="travel-form-toggle">
                <input
                  type="checkbox"
                  checked={form.alertsEnabled}
                  onChange={(e) => setField('alertsEnabled', e.target.checked)}
                />
                Enable weather alerts for this trip
              </label>

              {formError ? (
                <p className="field-error" role="alert">
                  {formError}
                </p>
              ) : null}

              <div className="travel-form-actions">
                <AriaButton className="ghost" type="button" onPress={closeForm}>
                  Cancel
                </AriaButton>
                <AriaButton className="primary" type="submit" isDisabled={savingTravelPlan}>
                  {savingTravelPlan ? 'Saving…' : editingPlan ? 'Save changes' : 'Add trip'}
                </AriaButton>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  )
}
