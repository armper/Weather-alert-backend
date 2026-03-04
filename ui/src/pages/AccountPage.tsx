import { useMemo, useState, type FormEvent } from 'react'
import { useAppState } from '../state/useAppState'
import { formatStatusLabel } from '../lib/formatting'

export function AccountPage() {
  const {
    me,
    profileForm,
    setProfileForm,
    passwordForm,
    setPasswordForm,
    notificationPreference,
    savingProfile,
    handleSaveProfile,
    handleChangePassword,
    handleSaveNotificationPreference,
  } = useAppState()
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

  const enabledChannelOptions = preferenceForm.enabledChannels

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

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Account</h2>
          <span className="badge">{formatStatusLabel(me?.approvalStatus)}</span>
        </div>

        <div className="account-stack">
          <section className="section-block">
            <h3>Profile</h3>
            <form onSubmit={onSaveProfile} className="grid-form">
              <label>
                Name
                <input
                  type="text"
                  value={profileForm.name}
                  onChange={(event) => setProfileForm((state) => ({ ...state, name: event.target.value }))}
                />
              </label>
              <label>
                Phone
                <input
                  type="text"
                  value={profileForm.phoneNumber}
                  onChange={(event) => setProfileForm((state) => ({ ...state, phoneNumber: event.target.value }))}
                />
              </label>
              <button type="submit" className="primary button-inline" disabled={savingProfile}>
                {savingProfile ? 'Updating...' : 'Update profile'}
              </button>
              {profileSaved ? <p className="inline-success">Profile updated.</p> : null}
            </form>
          </section>

          <section className="section-block">
            <h3>Password</h3>
            <form onSubmit={onChangePassword} className="grid-form">
              <label>
                Current password
                <div className="input-with-action">
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    minLength={8}
                    value={passwordForm.currentPassword}
                    onChange={(event) => setPasswordForm((state) => ({ ...state, currentPassword: event.target.value }))}
                  />
                  <button
                    type="button"
                    className="input-inline-action"
                    onClick={() => setShowCurrentPassword((state) => !state)}
                  >
                    {showCurrentPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </label>
              <label>
                New password
                <div className="input-with-action">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    minLength={8}
                    value={passwordForm.newPassword}
                    onChange={(event) => setPasswordForm((state) => ({ ...state, newPassword: event.target.value }))}
                  />
                  <button
                    type="button"
                    className="input-inline-action"
                    onClick={() => setShowNewPassword((state) => !state)}
                  >
                    {showNewPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
                <span className="muted small">Strength: {passwordStrength}</span>
              </label>
              <label>
                Confirm new password
                <div className="input-with-action">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    minLength={8}
                    value={passwordForm.confirmNewPassword}
                    onChange={(event) =>
                      setPasswordForm((state) => ({ ...state, confirmNewPassword: event.target.value }))
                    }
                  />
                  <button
                    type="button"
                    className="input-inline-action"
                    onClick={() => setShowConfirmPassword((state) => !state)}
                  >
                    {showConfirmPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </label>
              <button type="submit" className="ghost button-inline" disabled={savingProfile}>
                {savingProfile ? 'Updating...' : 'Change password'}
              </button>
              {passwordSaved ? <p className="inline-success">Password updated.</p> : null}
            </form>
          </section>

          <section className="section-block">
            <h3>Delivery preferences</h3>
            <form onSubmit={onSavePreferences} className="grid-form">
              <div className="toggle-row">
                <label className="switch-field compact">
                  <span>Email</span>
                  <span className="switch">
                    <input
                      type="checkbox"
                      checked={preferenceForm.enabledChannels.includes('EMAIL')}
                      onChange={() => toggleChannel('EMAIL')}
                    />
                    <span className="switch-slider" />
                  </span>
                </label>
                <label className="switch-field compact">
                  <span>SMS</span>
                  <span className="switch">
                    <input
                      type="checkbox"
                      checked={preferenceForm.enabledChannels.includes('SMS')}
                      onChange={() => toggleChannel('SMS')}
                    />
                    <span className="switch-slider" />
                  </span>
                </label>
                <label className="switch-field compact">
                  <span>Push</span>
                  <span className="switch">
                    <input
                      type="checkbox"
                      checked={preferenceForm.enabledChannels.includes('PUSH')}
                      onChange={() => toggleChannel('PUSH')}
                    />
                    <span className="switch-slider" />
                  </span>
                </label>
              </div>
              <label>
                Preferred channel
                <select
                  value={preferenceForm.preferredChannel}
                  onChange={(event) =>
                    setPreferenceDraft({
                      ...preferenceForm,
                      preferredChannel: event.target.value as 'EMAIL' | 'SMS' | 'PUSH',
                    })
                  }
                >
                  {enabledChannelOptions.map((channel) => (
                    <option key={channel} value={channel}>
                      {channel}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Delivery fallback strategy
                <select
                  value={preferenceForm.fallbackStrategy}
                  onChange={(event) =>
                    setPreferenceDraft({
                      ...preferenceForm,
                      fallbackStrategy: event.target.value as 'FIRST_SUCCESS' | 'FAIL_FAST',
                    })
                  }
                >
                  <option value="FIRST_SUCCESS">First successful channel</option>
                  <option value="FAIL_FAST">Fail fast</option>
                </select>
              </label>
              <button type="submit" className="ghost button-inline" disabled={savingProfile}>
                Save preferences
              </button>
              {prefsSaved ? <p className="inline-success">Preferences updated.</p> : null}
            </form>
          </section>
        </div>
      </article>
    </section>
  )
}
