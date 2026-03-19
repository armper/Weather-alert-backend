import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { House, MapPinned, Radar, type LucideIcon } from 'lucide-react'
import type { BillingPlan } from '../types'
import {
  useActionState,
  useAsyncState,
  useDataState,
  useSessionState,
} from '../state/useAppState'

const PLAN_DETAILS: Array<{
  id: BillingPlan
  name: string
  icon: LucideIcon
  monthlyPrice: string
  activeAlertsLabel: string
  travelPlansLabel: string
  emailMode: string
  highlight: string
}> = [
  {
    id: 'FREE',
    name: 'Home',
    icon: House,
    monthlyPrice: '$0',
    activeAlertsLabel: '1 active alert',
    travelPlansLabel: 'No travel plans',
    emailMode: 'Includes a small sponsored link',
    highlight: 'A clean starting point for home, commute, and everyday weather.',
  },
  {
    id: 'PLUS',
    name: 'Neighborhood',
    icon: MapPinned,
    monthlyPrice: '$6',
    activeAlertsLabel: '10 active alerts',
    travelPlansLabel: '3 travel plans',
    emailMode: 'Ad-free',
    highlight: 'More room for the places and plans you actually keep tabs on each week.',
  },
  {
    id: 'PRO',
    name: 'Everywhere',
    icon: Radar,
    monthlyPrice: '$9',
    activeAlertsLabel: '50 active alerts',
    travelPlansLabel: '15 travel plans',
    emailMode: 'Ad-free',
    highlight: 'Built for multi-location monitoring, frequent travel, and full family coverage.',
  },
]

export function SubscriptionPage() {
  const { me, initialDataLoading, refresh } = useSessionState()
  const { criteria, travelPlans, billingStatus } = useDataState()
  const { checkoutPlan, changingPlan, openingBillingPortal } = useAsyncState()
  const { handleStartCheckout, handleChangePlan, handleOpenBillingPortal } = useActionState()

  const location = useLocation()
  const [pendingPlan, setPendingPlan] = useState<BillingPlan | null>(null)

  /* derived billing state */
  const billingFlow = useMemo(() => new URLSearchParams(location.search).get('billing'), [location.search])
  const currentPlan = billingStatus?.plan ?? 'FREE'
  const enabledCriteriaCount = criteria.filter((c) => c.enabled !== false).length
  const maxActiveAlerts = billingStatus?.maxActiveAlerts ?? 1
  const maxTravelPlans = billingStatus?.maxTravelPlans ?? 0
  const currentTripCount = travelPlans.length
  const remainingAlerts = Math.max(maxActiveAlerts - enabledCriteriaCount, 0)
  const remainingTrips = Math.max(maxTravelPlans - currentTripCount, 0)

  const pendingDetails = pendingPlan == null ? null : PLAN_DETAILS.find((p) => p.id === pendingPlan) ?? null
  const pendingLimit = pendingPlan === 'FREE' ? 1 : pendingPlan === 'PLUS' ? 10 : pendingPlan === 'PRO' ? 50 : maxActiveAlerts
  const pendingTravelLimit = pendingPlan === 'FREE' ? 0 : pendingPlan === 'PLUS' ? 3 : pendingPlan === 'PRO' ? 15 : maxTravelPlans
  const excessRules = Math.max(enabledCriteriaCount - pendingLimit, 0)
  const excessTrips = Math.max(currentTripCount - pendingTravelLimit, 0)
  const isDowngrade = pendingPlan != null && (
    (currentPlan === 'PRO' && pendingPlan !== 'PRO') ||
    (currentPlan === 'PLUS' && pendingPlan === 'FREE')
  )
  const isUpgrade = pendingPlan != null && (
    (currentPlan === 'FREE' && pendingPlan !== 'FREE') ||
    (currentPlan === 'PLUS' && pendingPlan === 'PRO')
  )

  useEffect(() => {
    if (billingFlow === 'success' && me) void refresh()
  }, [billingFlow, me, refresh])

  async function confirmChange() {
    if (pendingPlan == null) return
    const ok = billingStatus?.activeSubscription && currentPlan !== 'FREE'
      ? await handleChangePlan(pendingPlan)
      : await handleStartCheckout(pendingPlan)
    if (ok) setPendingPlan(null)
  }

  function actionLabel(planId: BillingPlan) {
    if (planId === currentPlan) return billingStatus?.activeSubscription ? 'Manage billing' : 'Current plan'
    if (changingPlan === planId || checkoutPlan === planId) return 'Working…'
    if (billingStatus?.activeSubscription && currentPlan !== 'FREE') {
      if (planId === 'FREE') return 'Downgrade'
      if (currentPlan === 'PLUS' && planId === 'PRO') return 'Upgrade'
      return 'Switch'
    }
    return 'Choose plan'
  }

  const isBusy = checkoutPlan !== null || changingPlan !== null || openingBillingPortal

  return (
    <section className="page-stack sub-page">
      <div className="sub-page-content">
        {/* ── hero ── */}
        <div className="sub-hero">
          <h1 className="sub-hero-title">Your plan</h1>
          {!initialDataLoading ? (
            <div className="sub-hero-stats">
              <span className="sub-stat">
                <strong>{enabledCriteriaCount}</strong>/{maxActiveAlerts} alerts
                <span className="sub-stat-hint">{remainingAlerts} left</span>
              </span>
              <span className="sub-stat-divider" />
              <span className="sub-stat">
                <strong>{currentTripCount}</strong>/{maxTravelPlans} trips
                <span className="sub-stat-hint">
                  {maxTravelPlans === 0 ? 'Starts on Neighborhood' : `${remainingTrips} left`}
                </span>
              </span>
            </div>
          ) : null}
        </div>

        {/* ── billing feedback ── */}
        {billingFlow === 'success' ? (
          <p className="sub-feedback is-success">Checkout completed — billing status will refresh momentarily.</p>
        ) : billingFlow === 'cancel' ? (
          <p className="sub-feedback is-warning">Checkout canceled. Your plan has not changed.</p>
        ) : null}

        {/* ── plan tiles ── */}
        {!initialDataLoading ? (
          <div className="sub-plan-grid">
            {PLAN_DETAILS.map((plan) => {
              const isCurrent = plan.id === currentPlan
              const isCurrentFree = isCurrent && !billingStatus?.activeSubscription
              return (
                <button
                  key={plan.id}
                  className={`sub-plan-tile${isCurrent ? ' is-current' : ''}${plan.id === 'PRO' ? ' is-pro' : ''}`}
                  type="button"
                  disabled={isCurrentFree || isBusy}
                  onClick={() => isCurrent ? void handleOpenBillingPortal() : setPendingPlan(plan.id)}
                >
                  <div className="sub-plan-top">
                    <span className="sub-plan-icon" aria-hidden><plan.icon size="1em" strokeWidth={2.1} /></span>
                    <span className="sub-plan-price">{plan.monthlyPrice}<small>/mo</small></span>
                  </div>
                  <h3 className="sub-plan-name">{plan.name}</h3>
                  <p className="sub-plan-highlight">{plan.highlight}</p>
                  <ul className="sub-plan-features">
                    <li>{plan.activeAlertsLabel}</li>
                    <li>{plan.travelPlansLabel}</li>
                    <li>{plan.emailMode}</li>
                  </ul>
                  <span className={`sub-plan-action${isCurrent ? ' is-current' : ''}`}>
                    {actionLabel(plan.id)}
                  </span>
                </button>
              )
            })}
          </div>
        ) : null}

        {/* ── footnote ── */}
        {!initialDataLoading ? (
          <p className="sub-footnote">
            {currentPlan === 'FREE'
              ? <>You're on Home — <Link to="/app/rules" className="sub-link">create alerts</Link> and upgrade when you need more.</>
              : 'Switch plans above or use "Manage billing" for payment methods and invoices.'}
          </p>
        ) : null}
      </div>

      {/* ── confirm dialog ── */}
      {pendingDetails ? (
        <div className="sub-dialog-backdrop" onClick={() => setPendingPlan(null)}>
          <div
            className={`sub-dialog${isDowngrade ? ' is-downgrade' : ''}`}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <div className="sub-dialog-header">
              <h3>{isUpgrade ? 'Upgrade' : isDowngrade ? 'Downgrade' : 'Switch'} to {pendingDetails.name}?</h3>
              <button className="sub-dialog-close" type="button" onClick={() => setPendingPlan(null)}>✕</button>
            </div>

            <div className="sub-dialog-body">
              <div className="sub-dialog-plan-card">
                <span className="sub-plan-icon" aria-hidden><pendingDetails.icon size="1em" strokeWidth={2.1} /></span>
                <div>
                  <strong>{pendingDetails.name}</strong>
                  <span className="sub-dialog-price">{pendingDetails.monthlyPrice}/mo</span>
                </div>
              </div>

              {isUpgrade ? (
                <p>
                  You'll unlock {pendingDetails.activeAlertsLabel.toLowerCase()} and {pendingDetails.travelPlansLabel.toLowerCase()} right away.
                  {!billingStatus?.activeSubscription || currentPlan === 'FREE'
                    ? " You\u2019ll be redirected to Stripe to confirm."
                    : ' Billing updates immediately.'}
                </p>
              ) : isDowngrade ? (
                <>
                  <p>This change happens immediately.</p>
                  {excessRules > 0 ? (
                    <p className="sub-dialog-warn">{excessRules} extra rule{excessRules > 1 ? 's' : ''} will be disabled automatically. They stay saved for re-enabling later.</p>
                  ) : null}
                  {excessTrips > 0 ? (
                    <p className="sub-dialog-warn">You have {excessTrips} trip{excessTrips > 1 ? 's' : ''} over the new limit — you can keep them but can't add more until below the cap.</p>
                  ) : null}
                </>
              ) : (
                <p>SkyPanda will switch your subscription to {pendingDetails.name}.</p>
              )}
            </div>

            <div className="sub-dialog-actions">
              <button className="sub-btn sub-btn-ghost" type="button" onClick={() => setPendingPlan(null)}>Cancel</button>
              <button
                className={`sub-btn ${isDowngrade ? 'sub-btn-warn' : 'sub-btn-primary'}`}
                type="button"
                disabled={changingPlan === pendingPlan || checkoutPlan === pendingPlan}
                onClick={() => void confirmChange()}
              >
                {changingPlan === pendingPlan || checkoutPlan === pendingPlan
                  ? 'Working…'
                  : isUpgrade ? `Upgrade to ${pendingDetails.name}` : `Confirm`}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
