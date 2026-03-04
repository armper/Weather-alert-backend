import { useMemo, useState, type FormEvent } from 'react'
import { useAppState } from '../state/useAppState'
import { formatStatusLabel } from '../lib/formatting'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'

const CHANNEL_OPTIONS = [
  { id: 'EMAIL', label: 'Email' },
  { id: 'SMS', label: 'SMS' },
  { id: 'PUSH', label: 'Push' },
]

const FALLBACK_OPTIONS = [
  { id: 'FIRST_SUCCESS', label: 'First successful channel' },
  { id: 'FAIL_FAST', label: 'Fail fast' },
]

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

