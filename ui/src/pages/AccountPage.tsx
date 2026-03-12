import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAppState } from '../state/useAppState'
import { formatStatusLabel } from '../lib/formatting'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
import { useThemePreference, type ThemePreference } from '../theme'
import type { BillingPlan } from '../types'

const CHANNEL_OPTIONS = [
  { id: 'EMAIL', label: 'Email' },
  { id: 'SMS', label: 'SMS' },
  { id: 'PUSH', label: 'Push' },
]

const FALLBACK_OPTIONS = [
  { id: 'FIRST_SUCCESS', label: 'First successful channel' },
  { id: 'FAIL_FAST', label: 'Fail fast' },
]

const PLAN_DETAILS: Array<{
  id: BillingPlan
  name: string
  monthlyPrice: string
  activeAlertsLabel: string
  emailMode: string
  highlight: string
}> = [
  {
    id: 'FREE',
    name: 'The Basics',
    monthlyPrice: '$0',
    activeAlertsLabel: '1 active alert',
    emailMode: 'Includes a small sponsored link',
    highlight: 'Perfect for your home or daily commute.',
  },
  {
    id: 'PLUS',
    name: 'The Family Plan',
    monthlyPrice: '$9',
    activeAlertsLabel: '10 active alerts',
    emailMode: 'Ad-free',
    highlight: 'Keep an eye on home, work, the kids’ school, and weekend sports.',
  },
  {
    id: 'PRO',
    name: 'The Globetrotter',
    monthlyPrice: '$19',
    activeAlertsLabel: '50 active alerts',
    emailMode: 'Ad-free',
    highlight: 'Ideal for frequent travelers, RVers, or keeping tabs on extended family across the country.',
  },
]

export function AccountPage() {
  const {
    me,
    criteria,
    billingStatus,
    loadingBilling,
    checkoutPlan,
    openingBillingPortal,
    profileForm,
    setProfileForm,
    passwordForm,
    setPasswordForm,
    notificationPreference,
    savingProfile,
    handleSaveProfile,
    handleChangePassword,
    handleSaveNotificationPreference,
    handleStartCheckout,
    handleOpenBillingPortal,
    refresh,
  } = useAppState()
  const { theme, themePreference, setThemePreference } = useThemePreference()
  const location = useLocation()

  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [profileSaved, setProfileSaved] = useState(false)
  const [passwordSaved, setPasswordSaved] = useState(false)
  const [prefsSaved, setPrefsSaved] = useState(false)
  const [preferenceDraft, setPreferenceDraft] = useState<{
    enabledChannels: Array<'EMAIL' | 'SMS' | 'PUSH'>
    preferredChannel: 'EMAIL' | 'SMS' | 'PUSH'
    fallbackStrategy: 'FIRST_SUCCESS' | 'FAIL_FAST'
  } | null>(null)

  const preferenceForm = preferenceDraft ?? {
    enabledChannels: notificationPreference?.enabledChannels ?? ['EMAIL'],
    preferredChannel: notificationPreference?.preferredChannel ?? 'EMAIL',
    fallbackStrategy: notificationPreference?.fallbackStrategy ?? 'FIRST_SUCCESS',
  }

  const enabledChannelOptions = CHANNEL_OPTIONS.filter((option) => preferenceForm.enabledChannels.includes(option.id as 'EMAIL' | 'SMS' | 'PUSH'))

  const passwordStrength = useMemo(() => {
    const value = passwordForm.newPassword ?? ''
    if (!value) {
      return 'No password entered'
    }
    let score = 0
    if (value.length >= 8) score += 1
    if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score += 1
    if (/\d/.test(value)) score += 1
    if (/[^A-Za-z0-9]/.test(value)) score += 1
    if (score <= 1) return 'Weak'
    if (score <= 3) return 'Medium'
    return 'Strong'
  }, [passwordForm.newPassword])

  const billingFlow = useMemo(() => new URLSearchParams(location.search).get('billing'), [location.search])
  const currentPlan = billingStatus?.plan ?? 'FREE'
  const enabledCriteriaCount = criteria.filter((item) => item.enabled !== false).length
  const maxActiveAlerts = billingStatus?.maxActiveAlerts ?? 1
  const remainingAlerts = Math.max(maxActiveAlerts - enabledCriteriaCount, 0)
  const billingStatusLabel = billingStatus?.activeSubscription
    ? formatStatusLabel(billingStatus.stripeSubscriptionStatus)
    : currentPlan === 'FREE'
      ? 'No paid subscription'
      : 'Plan pending'

  useEffect(() => {
    if (billingFlow === 'success' && me) {
      void refresh()
    }
  }, [billingFlow, me, refresh])

  async function onSaveProfile(event: FormEvent<HTMLFormElement>) {
    const success = await handleSaveProfile(event)
    setProfileSaved(success)
  }

  async function onChangePassword(event: FormEvent<HTMLFormElement>) {
    const success = await handleChangePassword(event)
    setPasswordSaved(success)
  }

  async function onSavePreferences(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const success = await handleSaveNotificationPreference(preferenceForm)
    setPrefsSaved(success)
    if (success) {
      setPreferenceDraft(null)
    }
  }

  function toggleChannel(channel: 'EMAIL' | 'SMS' | 'PUSH') {
    const exists = preferenceForm.enabledChannels.includes(channel)
    const nextChannels = exists
      ? preferenceForm.enabledChannels.filter((item) => item !== channel)
      : [...preferenceForm.enabledChannels, channel]
    if (nextChannels.length === 0) {
      return
    }

    const fallbackPreferred = nextChannels.includes(preferenceForm.preferredChannel)
      ? preferenceForm.preferredChannel
      : nextChannels[0] ?? 'EMAIL'

    setPreferenceDraft({
      ...preferenceForm,
      enabledChannels: nextChannels,
      preferredChannel: fallbackPreferred,
    })
  }

  function planActionLabel(planId: BillingPlan) {
    if (planId === currentPlan) {
      return billingStatus?.activeSubscription ? 'Manage billing' : 'Included'
    }
    if (billingStatus?.activeSubscription) {
      if (planId === 'FREE') {
        return 'Downgrade in billing portal'
      }
      return `Change in billing portal`
    }
    if (checkoutPlan === planId) {
      return 'Redirecting...'
    }
    return `Choose ${PLAN_DETAILS.find((plan) => plan.id === planId)?.name ?? planId}`
  }

  function billingFeedbackText() {
    if (billingFlow === 'success') {
      return 'Stripe checkout completed. Billing status will refresh as soon as the webhook sync finishes.'
    }
    if (billingFlow === 'cancel') {
      return 'Stripe checkout was canceled. Your current plan has not changed.'
    }
    return null
  }

  const THEME_OPTIONS: Array<{ id: ThemePreference; label: string; detail: string }> = [
    { id: 'system', label: 'System', detail: 'Follow your device preference automatically.' },
    { id: 'light', label: 'Light', detail: 'Use the bright daytime palette.' },
    { id: 'dark', label: 'Dark', detail: 'Use the twilight palette all the time.' },
  ]

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Account</h2>
          <span className="badge">{formatStatusLabel(me?.approvalStatus)}</span>
        </div>

        <div className="account-stack">
          <section className="section-block account-billing-block">
            <div className="account-billing-hero">
              <div>
                <p className="eyebrow">Billing</p>
                <h3>Current plan: {PLAN_DETAILS.find((plan) => plan.id === currentPlan)?.name ?? currentPlan}</h3>
                <p className="account-billing-copy">
                  {loadingBilling
                    ? 'Loading your plan details and current entitlements.'
                    : billingStatus?.adSponsoredEmails
                    ? 'Your email alerts include a sponsored footer on the free tier.'
                    : 'Your alert emails are ad-free on the current paid tier.'}
                </p>
              </div>

              <div className="account-plan-summary">
                <span className={`badge ${billingStatus?.activeSubscription ? '' : 'is-muted'}`}>{billingStatusLabel}</span>
                <strong>
                  {enabledCriteriaCount}/{maxActiveAlerts} active alerts in use
                </strong>
                <span className="muted small">{remainingAlerts} slots remaining before you hit the plan limit.</span>
              </div>
            </div>

            {billingFeedbackText() ? (
              <div className={`billing-feedback ${billingFlow === 'cancel' ? 'is-warning' : 'is-success'}`}>
                <p>{billingFeedbackText()}</p>
              </div>
            ) : null}

            <div className="billing-plan-grid">
              {PLAN_DETAILS.map((plan) => {
                const isCurrentPlan = plan.id === currentPlan
                const usePortalAction = Boolean(billingStatus?.activeSubscription)
                const disableAction = usePortalAction ? openingBillingPortal : isCurrentPlan || checkoutPlan !== null
                return (
                  <article
                    key={plan.id}
                    className={`billing-plan-card${isCurrentPlan ? ' is-current' : ''}${plan.id === 'PRO' ? ' is-emphasized' : ''}`}
                  >
                    <div className="billing-plan-header">
                      <div>
                        <h4>{plan.name}</h4>
                        <p className="billing-plan-price">{plan.monthlyPrice}<span>/month</span></p>
                      </div>
                      <span className={`badge ${isCurrentPlan ? '' : 'is-muted'}`}>
                        {isCurrentPlan ? 'Current' : plan.id}
                      </span>
                    </div>

                    <div className="billing-plan-body">
                      <p>{plan.highlight}</p>
                      <ul className="billing-plan-list">
                        <li>{plan.activeAlertsLabel}</li>
                        <li>{plan.emailMode}</li>
                        <li>Billing shows up in your account before checkout redirect.</li>
                      </ul>
                    </div>

                    {plan.id === 'FREE' && !usePortalAction ? (
                      <div className="billing-plan-note">
                        <span className="small muted">
                          The Basics includes one live alert and full email or SMS delivery.
                        </span>
                      </div>
                    ) : (
                      <AriaButton
                        className={plan.id === 'PRO' ? 'primary button-inline' : 'ghost button-inline'}
                        isDisabled={disableAction}
                        onPress={() => void (usePortalAction ? handleOpenBillingPortal() : handleStartCheckout(plan.id))}
                      >
                        {planActionLabel(plan.id)}
                      </AriaButton>
                    )}
                  </article>
                )
              })}
            </div>

            <div className="billing-footnote">
              {billingStatus?.activeSubscription ? (
                <p className="small muted">
                  Manage upgrades, downgrades, and cancellation in the Stripe billing portal.
                </p>
              ) : (
                <p className="small muted">
                  Need more than one alert right away? Start with The Family Plan, or jump to The Globetrotter if
                  you’re monitoring many places.
                </p>
              )}
              {currentPlan === 'FREE' ? (
                <p className="small muted">
                  You are at {enabledCriteriaCount}/{maxActiveAlerts} active alerts. Create or edit rules on{' '}
                  <Link to="/app/rules" className="auth-link">
                    New Alert
                  </Link>
                  .
                </p>
              ) : null}
            </div>
          </section>

          <section className="section-block">
            <h3>Profile</h3>
            <form onSubmit={onSaveProfile} className="grid-form">
              <AriaTextField
                label="Name"
                inputClassName="aria-input"
                value={profileForm.name}
                onChange={(value) => setProfileForm((state) => ({ ...state, name: value }))}
              />
              <AriaTextField
                label="Phone"
                inputClassName="aria-input"
                value={profileForm.phoneNumber}
                onChange={(value) => setProfileForm((state) => ({ ...state, phoneNumber: value }))}
              />
              <AriaButton type="submit" className="primary button-inline" isDisabled={savingProfile}>
                {savingProfile ? 'Updating...' : 'Update profile'}
              </AriaButton>
              {profileSaved ? <p className="inline-success">Profile updated.</p> : null}
            </form>
          </section>

          <section className="section-block">
            <h3>Password</h3>
            <form onSubmit={onChangePassword} className="grid-form">
              <AriaTextField
                label="Current password"
                inputClassName="aria-input"
                inputWrapperClassName="input-with-action"
                type={showCurrentPassword ? 'text' : 'password'}
                minLength={8}
                value={passwordForm.currentPassword}
                onChange={(value) => setPasswordForm((state) => ({ ...state, currentPassword: value }))}
                endAction={
                  <AriaButton
                    className="input-inline-action"
                    onPress={() => setShowCurrentPassword((state) => !state)}
                  >
                    {showCurrentPassword ? 'Hide' : 'Show'}
                  </AriaButton>
                }
              />

              <AriaTextField
                label="New password"
                inputClassName="aria-input"
                inputWrapperClassName="input-with-action"
                type={showNewPassword ? 'text' : 'password'}
                minLength={8}
                value={passwordForm.newPassword}
                description={`Strength: ${passwordStrength}`}
                onChange={(value) => setPasswordForm((state) => ({ ...state, newPassword: value }))}
                endAction={
                  <AriaButton className="input-inline-action" onPress={() => setShowNewPassword((state) => !state)}>
                    {showNewPassword ? 'Hide' : 'Show'}
                  </AriaButton>
                }
              />

              <AriaTextField
                label="Confirm new password"
                inputClassName="aria-input"
                inputWrapperClassName="input-with-action"
                type={showConfirmPassword ? 'text' : 'password'}
                minLength={8}
                value={passwordForm.confirmNewPassword}
                onChange={(value) => setPasswordForm((state) => ({ ...state, confirmNewPassword: value }))}
                endAction={
                  <AriaButton
                    className="input-inline-action"
                    onPress={() => setShowConfirmPassword((state) => !state)}
                  >
                    {showConfirmPassword ? 'Hide' : 'Show'}
                  </AriaButton>
                }
              />

              <AriaButton type="submit" className="ghost button-inline" isDisabled={savingProfile}>
                {savingProfile ? 'Updating...' : 'Change password'}
              </AriaButton>
              {passwordSaved ? <p className="inline-success">Password updated.</p> : null}
            </form>
          </section>

          <section className="section-block">
            <h3>Appearance</h3>
            <div className="theme-settings-block">
              <p className="muted small">
                Current appearance: <strong>{theme === 'dark' ? 'Dark mode' : 'Light mode'}</strong>
              </p>
              <div className="theme-settings-grid">
                {THEME_OPTIONS.map((option) => {
                  const isActive = themePreference === option.id
                  return (
                    <button
                      key={option.id}
                      type="button"
                      className={`theme-settings-option${isActive ? ' is-active' : ''}`}
                      onClick={() => setThemePreference(option.id)}
                    >
                      <span className="theme-settings-option-title">{option.label}</span>
                      <span className="theme-settings-option-copy">{option.detail}</span>
                    </button>
                  )
                })}
              </div>
            </div>
          </section>

          <section className="section-block">
            <h3>Delivery preferences</h3>
            <form onSubmit={onSavePreferences} className="grid-form">
              <div className="toggle-row">
                <AriaSwitch
                  compact
                  label="Email"
                  isSelected={preferenceForm.enabledChannels.includes('EMAIL')}
                  onChange={() => toggleChannel('EMAIL')}
                />
                <AriaSwitch
                  compact
                  label="SMS"
                  isSelected={preferenceForm.enabledChannels.includes('SMS')}
                  onChange={() => toggleChannel('SMS')}
                />
                <AriaSwitch
                  compact
                  label="Push"
                  isSelected={preferenceForm.enabledChannels.includes('PUSH')}
                  onChange={() => toggleChannel('PUSH')}
                />
              </div>

              <AriaSelect
                label="Preferred channel"
                buttonClassName="aria-select-trigger"
                popoverClassName="aria-select-popover"
                listBoxClassName="aria-select-listbox"
                selectedKey={preferenceForm.preferredChannel}
                options={enabledChannelOptions}
                onSelectionChange={(value) =>
                  setPreferenceDraft({
                    ...preferenceForm,
                    preferredChannel: value as 'EMAIL' | 'SMS' | 'PUSH',
                  })
                }
              />

              <AriaSelect
                label="Delivery fallback strategy"
                buttonClassName="aria-select-trigger"
                popoverClassName="aria-select-popover"
                listBoxClassName="aria-select-listbox"
                selectedKey={preferenceForm.fallbackStrategy}
                options={FALLBACK_OPTIONS}
                onSelectionChange={(value) =>
                  setPreferenceDraft({
                    ...preferenceForm,
                    fallbackStrategy: value as 'FIRST_SUCCESS' | 'FAIL_FAST',
                  })
                }
              />

              <AriaButton type="submit" className="ghost button-inline" isDisabled={savingProfile}>
                Save preferences
              </AriaButton>
              {prefsSaved ? <p className="inline-success">Preferences updated.</p> : null}
            </form>
          </section>
        </div>
      </article>
    </section>
  )
}
