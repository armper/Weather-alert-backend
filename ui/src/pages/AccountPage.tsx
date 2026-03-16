import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import backgroundOverviewImage from '../assets/background-overview.png'
import backgroundRainImage from '../assets/background-rain.png'
import backgroundThunderstormImage from '../assets/background-thunderstorm.png'
import { useThemePreference, type ThemePreference } from '../theme'
import {
  useActionState,
  useAsyncState,
  useDataState,
  useFormState,
  useSessionState,
} from '../state/useAppState'

const CHANNEL_OPTIONS = [
  { id: 'EMAIL', label: 'Email' },
  { id: 'SMS', label: 'SMS' },
  { id: 'PUSH', label: 'Push' },
]

const FALLBACK_OPTIONS = [
  { id: 'FIRST_SUCCESS', label: 'First success' },
  { id: 'FAIL_FAST', label: 'Fail fast' },
]

export function AccountPage() {
  const { me, initialDataLoading } = useSessionState()
  const { notificationPreference, currentWeather } = useDataState()
  const { profileForm, setProfileForm, passwordForm, setPasswordForm } = useFormState()
  const { deletingAccount, savingProfile } = useAsyncState()
  const {
    handleSaveProfile,
    handleChangePassword,
    handleSaveNotificationPreference,
    handleDeleteAccount,
  } = useActionState()
  const { theme, themePreference, setThemePreference } = useThemePreference()
  const navigate = useNavigate()

  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [profileSaved, setProfileSaved] = useState(false)
  const [passwordSaved, setPasswordSaved] = useState(false)
  const [prefsSaved, setPrefsSaved] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [deleteConfirmation, setDeleteConfirmation] = useState('')
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

  const enabledChannelOptions = CHANNEL_OPTIONS.filter((option) =>
    preferenceForm.enabledChannels.includes(option.id as 'EMAIL' | 'SMS' | 'PUSH'),
  )

  const passwordStrength = useMemo(() => {
    const value = passwordForm.newPassword ?? ''
    if (!value) return 'No password entered'
    let score = 0
    if (value.length >= 8) score += 1
    if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score += 1
    if (/\d/.test(value)) score += 1
    if (/[^A-Za-z0-9]/.test(value)) score += 1
    if (score <= 1) return 'Weak'
    if (score <= 3) return 'Medium'
    return 'Strong'
  }, [passwordForm.newPassword])

  const deletePhrase = me?.id ?? ''
  const canConfirmDelete = deletePhrase.length > 0 && deleteConfirmation.trim() === deletePhrase

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
    if (success) setPreferenceDraft(null)
  }

  async function onDeleteAccount() {
    const success = await handleDeleteAccount()
    if (success) {
      setShowDeleteDialog(false)
      setDeleteConfirmation('')
      navigate('/auth?accountDeleted=1', { replace: true })
    }
  }

  function toggleChannel(channel: 'EMAIL' | 'SMS' | 'PUSH') {
    const exists = preferenceForm.enabledChannels.includes(channel)
    const nextChannels = exists
      ? preferenceForm.enabledChannels.filter((item) => item !== channel)
      : [...preferenceForm.enabledChannels, channel]
    if (nextChannels.length === 0) return
    const fallbackPreferred = nextChannels.includes(preferenceForm.preferredChannel)
      ? preferenceForm.preferredChannel
      : nextChannels[0] ?? 'EMAIL'
    setPreferenceDraft({ ...preferenceForm, enabledChannels: nextChannels, preferredChannel: fallbackPreferred })
  }

  const THEME_OPTIONS: Array<{ id: ThemePreference; label: string; emoji: string; detail: string }> = [
    { id: 'system', label: 'System', emoji: '🖥️', detail: 'Follow your device preference.' },
    { id: 'light', label: 'Light', emoji: '☀️', detail: 'Bright daytime palette.' },
    { id: 'dark', label: 'Dark', emoji: '🌙', detail: 'Twilight palette always.' },
  ]

  const headline = currentWeather?.description ?? ''
  const isThunderstorm = /thunder|storm|tstms/i.test(headline)
  const isRainy = /rain|drizzle|shower/i.test(headline)
  const bgImage = isThunderstorm
    ? backgroundThunderstormImage
    : isRainy
      ? backgroundRainImage
      : backgroundOverviewImage

  return (
    <section className="page-stack settings-page">
      <div className="overview-page-background" aria-hidden="true">
        <img className="overview-page-background-image" src={bgImage} alt="" />
      </div>

      <div className="settings-page-content">
        {/* ── hero ── */}
        <div className="settings-hero">
          <h1 className="settings-hero-title">Settings</h1>
          {!initialDataLoading && me ? (
            <p className="settings-hero-sub">{me.email}</p>
          ) : null}
        </div>

        {initialDataLoading ? (
          <div className="settings-card">
            <p className="settings-loading">Loading account details&hellip;</p>
          </div>
        ) : (
          <>
            {/* ── Profile ── */}
            <div className="settings-card">
              <h2 className="settings-card-header">Profile</h2>
              <form onSubmit={onSaveProfile} className="settings-form">
                <label className="settings-field">
                  <span className="settings-label">Name</span>
                  <input
                    className="settings-input"
                    type="text"
                    value={profileForm.name}
                    onChange={(e) => setProfileForm((s) => ({ ...s, name: e.target.value }))}
                  />
                </label>
                <label className="settings-field">
                  <span className="settings-label">Phone</span>
                  <input
                    className="settings-input"
                    type="tel"
                    value={profileForm.phoneNumber}
                    onChange={(e) => setProfileForm((s) => ({ ...s, phoneNumber: e.target.value }))}
                  />
                </label>
                <div className="settings-form-actions">
                  <button type="submit" className="settings-btn is-primary" disabled={savingProfile}>
                    {savingProfile ? 'Saving\u2026' : 'Update profile'}
                  </button>
                  {profileSaved ? <span className="settings-inline-ok">Saved</span> : null}
                </div>
              </form>
            </div>

            {/* ── Password ── */}
            <div className="settings-card">
              <h2 className="settings-card-header">Password</h2>
              <form onSubmit={onChangePassword} className="settings-form">
                <label className="settings-field">
                  <span className="settings-label">Current password</span>
                  <div className="settings-input-group">
                    <input
                      className="settings-input"
                      type={showCurrentPassword ? 'text' : 'password'}
                      minLength={8}
                      value={passwordForm.currentPassword}
                      onChange={(e) => setPasswordForm((s) => ({ ...s, currentPassword: e.target.value }))}
                    />
                    <button type="button" className="settings-input-toggle" onClick={() => setShowCurrentPassword((v) => !v)}>
                      {showCurrentPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </label>
                <label className="settings-field">
                  <span className="settings-label">New password</span>
                  <div className="settings-input-group">
                    <input
                      className="settings-input"
                      type={showNewPassword ? 'text' : 'password'}
                      minLength={8}
                      value={passwordForm.newPassword}
                      onChange={(e) => setPasswordForm((s) => ({ ...s, newPassword: e.target.value }))}
                    />
                    <button type="button" className="settings-input-toggle" onClick={() => setShowNewPassword((v) => !v)}>
                      {showNewPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                  <span className={`settings-strength is-${passwordStrength.toLowerCase()}`}>
                    Strength: {passwordStrength}
                  </span>
                </label>
                <label className="settings-field">
                  <span className="settings-label">Confirm new password</span>
                  <div className="settings-input-group">
                    <input
                      className="settings-input"
                      type={showConfirmPassword ? 'text' : 'password'}
                      minLength={8}
                      value={passwordForm.confirmNewPassword}
                      onChange={(e) => setPasswordForm((s) => ({ ...s, confirmNewPassword: e.target.value }))}
                    />
                    <button type="button" className="settings-input-toggle" onClick={() => setShowConfirmPassword((v) => !v)}>
                      {showConfirmPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </label>
                <div className="settings-form-actions">
                  <button type="submit" className="settings-btn" disabled={savingProfile}>
                    {savingProfile ? 'Saving\u2026' : 'Change password'}
                  </button>
                  {passwordSaved ? <span className="settings-inline-ok">Updated</span> : null}
                </div>
              </form>
            </div>

            {/* ── Appearance ── */}
            <div className="settings-card">
              <h2 className="settings-card-header">Appearance</h2>
              <p className="settings-hint">
                Currently using <strong>{theme === 'dark' ? 'dark' : 'light'}</strong> mode
              </p>
              <div className="settings-theme-grid">
                {THEME_OPTIONS.map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    className={`settings-theme-tile${themePreference === opt.id ? ' is-active' : ''}`}
                    onClick={() => setThemePreference(opt.id)}
                  >
                    <span className="settings-theme-emoji">{opt.emoji}</span>
                    <span className="settings-theme-name">{opt.label}</span>
                    <span className="settings-theme-detail">{opt.detail}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* ── Delivery preferences ── */}
            <div className="settings-card">
              <h2 className="settings-card-header">Delivery preferences</h2>
              <form onSubmit={onSavePreferences} className="settings-form">
                <div className="settings-channels">
                  {CHANNEL_OPTIONS.map((ch) => {
                    const enabled = preferenceForm.enabledChannels.includes(ch.id as 'EMAIL' | 'SMS' | 'PUSH')
                    return (
                      <button
                        key={ch.id}
                        type="button"
                        className={`settings-channel-chip${enabled ? ' is-on' : ''}`}
                        onClick={() => toggleChannel(ch.id as 'EMAIL' | 'SMS' | 'PUSH')}
                      >
                        <span className={`settings-channel-dot${enabled ? ' is-on' : ''}`} />
                        {ch.label}
                      </button>
                    )
                  })}
                </div>
                <label className="settings-field">
                  <span className="settings-label">Preferred channel</span>
                  <select
                    className="settings-select"
                    value={preferenceForm.preferredChannel}
                    onChange={(e) => setPreferenceDraft({ ...preferenceForm, preferredChannel: e.target.value as 'EMAIL' | 'SMS' | 'PUSH' })}
                  >
                    {enabledChannelOptions.map((o) => (
                      <option key={o.id} value={o.id}>{o.label}</option>
                    ))}
                  </select>
                </label>
                <label className="settings-field">
                  <span className="settings-label">Fallback strategy</span>
                  <select
                    className="settings-select"
                    value={preferenceForm.fallbackStrategy}
                    onChange={(e) => setPreferenceDraft({ ...preferenceForm, fallbackStrategy: e.target.value as 'FIRST_SUCCESS' | 'FAIL_FAST' })}
                  >
                    {FALLBACK_OPTIONS.map((o) => (
                      <option key={o.id} value={o.id}>{o.label}</option>
                    ))}
                  </select>
                </label>
                <div className="settings-form-actions">
                  <button type="submit" className="settings-btn" disabled={savingProfile}>
                    Save preferences
                  </button>
                  {prefsSaved ? <span className="settings-inline-ok">Saved</span> : null}
                </div>
              </form>
            </div>

            {/* ── Danger zone ── */}
            <div className="settings-card is-danger">
              <div className="settings-danger-top">
                <div>
                  <p className="settings-danger-eyebrow">Danger zone</p>
                  <h2 className="settings-card-header">Delete account</h2>
                </div>
                <button type="button" className="settings-btn is-danger" onClick={() => setShowDeleteDialog(true)}>
                  Delete account
                </button>
              </div>
              <p className="settings-danger-copy">
                Permanently removes your profile, alert rules, history, notification preferences, billing data,
                email&nbsp;address, and phone&nbsp;number.
              </p>
            </div>
          </>
        )}
      </div>

      {/* ── delete confirmation dialog ── */}
      {showDeleteDialog ? (
        <div className="settings-dialog-backdrop" role="presentation" onClick={() => setShowDeleteDialog(false)}>
          <div
            className="settings-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="settings-delete-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="settings-dialog-header">
              <div>
                <p className="settings-danger-eyebrow">Permanent deletion</p>
                <h3 id="settings-delete-title">Delete your account forever?</h3>
              </div>
              <button type="button" className="settings-dialog-close" onClick={() => setShowDeleteDialog(false)}>
                &times;
              </button>
            </div>
            <div className="settings-dialog-body">
              <p>
                This permanently removes your account, phone number, email, saved alert rules,
                notification history, and associated billing data from SkyPanda.
              </p>
              <p className="settings-dialog-hint">
                Type <strong>{deletePhrase}</strong> to confirm.
              </p>
              <input
                className="settings-input"
                placeholder={deletePhrase}
                value={deleteConfirmation}
                onChange={(e) => setDeleteConfirmation(e.target.value)}
              />
            </div>
            <div className="settings-dialog-actions">
              <button type="button" className="settings-btn" onClick={() => setShowDeleteDialog(false)}>
                Keep my account
              </button>
              <button
                type="button"
                className="settings-btn is-danger"
                disabled={!canConfirmDelete || deletingAccount}
                onClick={() => void onDeleteAccount()}
              >
                {deletingAccount ? 'Deleting\u2026' : 'Delete forever'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
