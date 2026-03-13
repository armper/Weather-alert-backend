import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
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
    changingPlan,
    openingBillingPortal,
    deletingAccount,
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
    handleChangePlan,
    handleOpenBillingPortal,
    handleDeleteAccount,
    refresh,
  } = useAppState()
  const { theme, themePreference, setThemePreference } = useThemePreference()
  const location = useLocation()
  const navigate = useNavigate()

  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [profileSaved, setProfileSaved] = useState(false)
  const [passwordSaved, setPasswordSaved] = useState(false)
  const [prefsSaved, setPrefsSaved] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [deleteConfirmation, setDeleteConfirmation] = useState('')
  const [pendingPlanChange, setPendingPlanChange] = useState<BillingPlan | null>(null)
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
  const deletePhrase = me?.id ?? ''
  const canConfirmDelete = deletePhrase.length > 0 && deleteConfirmation.trim() === deletePhrase
  const pendingPlanDetails = pendingPlanChange == null
    ? null
    : PLAN_DETAILS.find((plan) => plan.id === pendingPlanChange) ?? null
  const pendingPlanLimit = pendingPlanChange == null
    ? maxActiveAlerts
    : pendingPlanChange === 'FREE'
      ? 1
      : pendingPlanChange === 'PLUS'
        ? 10
        : 50
  const excessRulesOnDowngrade = Math.max(enabledCriteriaCount - pendingPlanLimit, 0)
  const isDowngradePlanChange = pendingPlanChange != null
    && ((currentPlan === 'PRO' && (pendingPlanChange === 'PLUS' || pendingPlanChange === 'FREE'))
      || (currentPlan === 'PLUS' && pendingPlanChange === 'FREE'))
  const isUpgradePlanChange = pendingPlanChange != null
    && ((currentPlan === 'FREE' && pendingPlanChange !== 'FREE')
      || (currentPlan === 'PLUS' && pendingPlanChange === 'PRO'))

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

  async function onDeleteAccount() {
    const success = await handleDeleteAccount()
    if (success) {
      setShowDeleteDialog(false)
      setDeleteConfirmation('')
      navigate('/auth?accountDeleted=1', { replace: true })
    }
  }

  async function onConfirmPlanChange() {
    if (pendingPlanChange == null) {
      return
    }

    const success = billingStatus?.activeSubscription && currentPlan !== 'FREE'
      ? await handleChangePlan(pendingPlanChange)
      : await handleStartCheckout(pendingPlanChange)

    if (success) {
      setPendingPlanChange(null)
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
    if (changingPlan === planId) {
      return planId === 'FREE' ? 'Downgrading...' : 'Updating...'
    }
    if (billingStatus?.activeSubscription && currentPlan !== 'FREE') {
      if (planId === 'FREE') {
        return 'Downgrade to The Basics'
      }
      if (currentPlan === 'PLUS' && planId === 'PRO') {
        return 'Upgrade to The Globetrotter'
      }
      if (currentPlan === 'PRO' && planId === 'PLUS') {
        return 'Switch to The Family Plan'
      }
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

  function billingEmailModeCopy() {
    if (loadingBilling) {
      return 'Loading your plan details and current entitlements.'
    }
    if (currentPlan === 'FREE' || !billingStatus?.activeSubscription) {
      return 'Your email alerts include a sponsored footer on The Basics.'
    }
    if (billingStatus?.adSponsoredEmails) {
      return 'Your email alerts currently include a sponsored footer on this plan.'
    }
    return 'Your alert emails are ad-free on the current paid tier.'
  }

  function openPlanChangeDialog(planId: BillingPlan) {
    setPendingPlanChange(planId)
  }

  function billingDialogTitle() {
    if (pendingPlanDetails == null) {
      return ''
    }
    if (isUpgradePlanChange) {
      return `Move to ${pendingPlanDetails.name}?`
    }
    if (isDowngradePlanChange) {
      return `Change to ${pendingPlanDetails.name}?`
    }
    return `Switch plans to ${pendingPlanDetails.name}?`
  }

  function billingDialogCopy() {
    if (pendingPlanDetails == null) {
      return ''
    }
    if (isUpgradePlanChange) {
      if (billingStatus?.activeSubscription && currentPlan !== 'FREE') {
        return `You’ll unlock ${pendingPlanDetails.activeAlertsLabel.toLowerCase()} right away. Your current watches stay in place and SkyPanda will update billing immediately.`
      }
      return `You’ll unlock ${pendingPlanDetails.activeAlertsLabel.toLowerCase()} and ad-free alerts as soon as Stripe checkout finishes.`
    }
    if (isDowngradePlanChange) {
      return `This change happens immediately. SkyPanda will keep you within the new ${pendingPlanDetails.activeAlertsLabel.toLowerCase()} limit and disable any extra active rules instead of deleting them.`
    }
    return `SkyPanda will switch your subscription to ${pendingPlanDetails.name} and keep your monitoring rules in sync with the new plan.`
  }

  function billingDialogActionLabel() {
    if (pendingPlanDetails == null) {
      return 'Continue'
    }
    if (isUpgradePlanChange) {
      return `Yes, move to ${pendingPlanDetails.name}`
    }
    if (isDowngradePlanChange) {
      return `Yes, change to ${pendingPlanDetails.name}`
    }
    return `Switch to ${pendingPlanDetails.name}`
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
                <p className="account-billing-copy">{billingEmailModeCopy()}</p>
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
                const disableAction = isCurrentPlan
                  ? openingBillingPortal
                  : checkoutPlan !== null || changingPlan !== null || openingBillingPortal
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

                    {plan.id === 'FREE' && !billingStatus?.activeSubscription ? (
                      <div className="billing-plan-note">
                        <span className="small muted">
                          The Basics includes one live alert and full email or SMS delivery.
                        </span>
                      </div>
                    ) : (
                      <AriaButton
                        className={plan.id === 'PRO' ? 'primary button-inline' : 'ghost button-inline'}
                        isDisabled={disableAction}
                        onPress={() =>
                          void (isCurrentPlan ? handleOpenBillingPortal() : openPlanChangeDialog(plan.id))
                        }
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
                  Switch plans directly here. Use the billing portal on your current plan card for payment methods and invoices.
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

          <section className="section-block account-danger-block">
            <div className="account-danger-copy">
              <div>
                <p className="eyebrow">Danger zone</p>
                <h3>Delete account</h3>
              </div>
              <p className="small muted">
                Permanently delete your account, saved alert rules, alert history, notification preferences, billing
                references, and profile details including your email address and phone number.
              </p>
            </div>
            <AriaButton className="ghost danger button-inline" onPress={() => setShowDeleteDialog(true)}>
              Delete account permanently
            </AriaButton>
          </section>
        </div>
      </article>

      {pendingPlanDetails ? (
        <div className="account-delete-dialog-backdrop" role="presentation" onClick={() => setPendingPlanChange(null)}>
          <div
            className={`account-delete-dialog billing-change-dialog${isDowngradePlanChange ? ' is-caution' : ''}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="billing-change-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="account-delete-dialog-header">
              <div>
                <p className="eyebrow">{isUpgradePlanChange ? 'Upgrade plan' : isDowngradePlanChange ? 'Downgrade plan' : 'Change plan'}</p>
                <h3 id="billing-change-title">{billingDialogTitle()}</h3>
              </div>
              <AriaButton className="ghost button-inline" onPress={() => setPendingPlanChange(null)}>
                Cancel
              </AriaButton>
            </div>

            <div className="account-delete-dialog-body">
              <p>{billingDialogCopy()}</p>

              <div className="billing-change-summary">
                <span className="badge">{pendingPlanDetails.name}</span>
                <strong>{pendingPlanDetails.activeAlertsLabel}</strong>
                <span className="small muted">{pendingPlanDetails.emailMode}</span>
              </div>

              {isDowngradePlanChange ? (
                <div className="billing-change-warning">
                  <p>
                    You currently have {enabledCriteriaCount} active rules. The new plan allows {pendingPlanLimit}.
                    {excessRulesOnDowngrade > 0
                      ? ` ${excessRulesOnDowngrade} extra active ${excessRulesOnDowngrade === 1 ? 'rule will' : 'rules will'} be disabled automatically.`
                      : ' No active rules need to be disabled.'}
                  </p>
                  <p className="small muted">
                    Disabled rules stay saved in your account so you can re-enable them later if you upgrade again.
                  </p>
                </div>
              ) : null}

              {isUpgradePlanChange ? (
                <p className="small muted">
                  {billingStatus?.activeSubscription && currentPlan !== 'FREE'
                    ? 'This keeps monitoring running without interruption.'
                    : 'You’ll be redirected to Stripe to confirm billing details securely.'}
                </p>
              ) : null}
            </div>

            <div className="account-delete-dialog-actions">
              <AriaButton className="ghost button-inline" onPress={() => setPendingPlanChange(null)}>
                Keep current plan
              </AriaButton>
              <AriaButton
                className={isDowngradePlanChange ? 'ghost button-inline' : 'primary button-inline'}
                isDisabled={changingPlan === pendingPlanChange || checkoutPlan === pendingPlanChange}
                onPress={() => void onConfirmPlanChange()}
              >
                {changingPlan === pendingPlanChange || checkoutPlan === pendingPlanChange
                  ? 'Working...'
                  : billingDialogActionLabel()}
              </AriaButton>
            </div>
          </div>
        </div>
      ) : null}

      {showDeleteDialog ? (
        <div className="account-delete-dialog-backdrop" role="presentation" onClick={() => setShowDeleteDialog(false)}>
          <div
            className="account-delete-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="account-delete-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="account-delete-dialog-header">
              <div>
                <p className="eyebrow">Permanent deletion</p>
                <h3 id="account-delete-title">Delete your account forever?</h3>
              </div>
              <AriaButton className="ghost button-inline" onPress={() => setShowDeleteDialog(false)}>
                Cancel
              </AriaButton>
            </div>

            <div className="account-delete-dialog-body">
              <p>
                This permanently removes your account, phone number, email address, saved alert rules, notification
                history, and associated billing/customer data from SkyPanda.
              </p>
              <p className="small muted">
                If you have an active paid plan, SkyPanda will cancel it before deleting the account. Type{' '}
                <strong>{deletePhrase}</strong> to confirm.
              </p>

              <AriaTextField
                label="Type your username to confirm"
                inputClassName="aria-input"
                value={deleteConfirmation}
                onChange={setDeleteConfirmation}
                placeholder={deletePhrase}
              />
            </div>

            <div className="account-delete-dialog-actions">
              <AriaButton className="ghost button-inline" onPress={() => setShowDeleteDialog(false)}>
                Keep my account
              </AriaButton>
              <AriaButton
                className="ghost danger button-inline"
                isDisabled={!canConfirmDelete || deletingAccount}
                onPress={() => void onDeleteAccount()}
              >
                {deletingAccount ? 'Deleting account...' : 'Delete forever'}
              </AriaButton>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
