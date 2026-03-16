import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { LoadingPlaceholder } from '../components/common/LoadingPlaceholder'
import { formatStatusLabel } from '../lib/formatting'
import { AriaButton } from '../components/ui/AriaButton'
import { AriaSelect } from '../components/ui/AriaSelect'
import { AriaSwitch } from '../components/ui/AriaSwitch'
import { AriaTextField } from '../components/ui/AriaTextField'
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
  { id: 'FIRST_SUCCESS', label: 'First successful channel' },
  { id: 'FAIL_FAST', label: 'Fail fast' },
]

export function AccountPage() {
  const { me, initialDataLoading } = useSessionState()
  const { notificationPreference } = useDataState()
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

  const THEME_OPTIONS: Array<{ id: ThemePreference; label: string; detail: string }> = [
    { id: 'system', label: 'System', detail: 'Follow your device preference automatically.' },
    { id: 'light', label: 'Light', detail: 'Use the bright daytime palette.' },
    { id: 'dark', label: 'Dark', detail: 'Use the twilight palette all the time.' },
  ]

  return (
    <section className="page-stack">
      <article className="panel">
        <div className="panel-title-row">
          <h2>Settings</h2>
          <span className="badge">{initialDataLoading ? 'Loading\u2026' : formatStatusLabel(me?.approvalStatus)}</span>
        </div>

        {initialDataLoading ? (
          <LoadingPlaceholder
            title="Loading account details"
            copy="Fetching your profile and delivery preferences."
            lineCount={3}
          />
        ) : null}

        <div className="account-stack">
          {!initialDataLoading ? (
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
          ) : null}

          {!initialDataLoading ? (
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
          ) : null}

          {!initialDataLoading ? (
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
          ) : null}

          {!initialDataLoading ? (
            <section className="section-block">
              <h3>Delivery preferences</h3>
              <form onSubmit={onSavePreferences} className="grid-form">
                <div className="toggle-row">
                  <AriaSwitch compact label="Email" isSelected={preferenceForm.enabledChannels.includes('EMAIL')} onChange={() => toggleChannel('EMAIL')} />
                  <AriaSwitch compact label="SMS" isSelected={preferenceForm.enabledChannels.includes('SMS')} onChange={() => toggleChannel('SMS')} />
                  <AriaSwitch compact label="Push" isSelected={preferenceForm.enabledChannels.includes('PUSH')} onChange={() => toggleChannel('PUSH')} />
                </div>
                <AriaSelect
                  label="Preferred channel"
                  buttonClassName="aria-select-trigger"
                  popoverClassName="aria-select-popover"
                  listBoxClassName="aria-select-listbox"
                  selectedKey={preferenceForm.preferredChannel}
                  options={enabledChannelOptions}
                  onSelectionChange={(value) => setPreferenceDraft({ ...preferenceForm, preferredChannel: value as 'EMAIL' | 'SMS' | 'PUSH' })}
                />
                <AriaSelect
                  label="Delivery fallback strategy"
                  buttonClassName="aria-select-trigger"
                  popoverClassName="aria-select-popover"
                  listBoxClassName="aria-select-listbox"
                  selectedKey={preferenceForm.fallbackStrategy}
                  options={FALLBACK_OPTIONS}
                  onSelectionChange={(value) => setPreferenceDraft({ ...preferenceForm, fallbackStrategy: value as 'FIRST_SUCCESS' | 'FAIL_FAST' })}
                />
                <AriaButton type="submit" className="ghost button-inline" isDisabled={savingProfile}>
                  Save preferences
                </AriaButton>
                {prefsSaved ? <p className="inline-success">Preferences updated.</p> : null}
              </form>
            </section>
          ) : null}

          {!initialDataLoading ? (
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
          ) : null}
        </div>
      </article>

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
