import { useAppState } from '../state/useAppState'

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
  } = useAppState()

  return (
    <section className="page-stack two-column-page">
      <article className="panel">
        <h2>Profile</h2>
        <form onSubmit={handleSaveProfile} className="grid-form">
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
          <button type="submit" className="primary" disabled={savingProfile}>
            {savingProfile ? 'Updating...' : 'Update profile'}
          </button>
        </form>

        <div className="prefs-summary">
          <p className="muted small">Username</p>
          <p>{me?.id ?? 'Unknown'}</p>
          <p className="muted small">Email</p>
          <p>{me?.email ?? 'Unknown'}</p>
          <p className="muted small">Approval status</p>
          <p>{me?.approvalStatus ?? 'Unknown'}</p>
        </div>
      </article>

      <article className="panel">
        <h2>Password and Notification</h2>
        <form onSubmit={handleChangePassword} className="grid-form">
          <label>
            Current password
            <input
              type="password"
              minLength={8}
              value={passwordForm.currentPassword}
              onChange={(event) => setPasswordForm((state) => ({ ...state, currentPassword: event.target.value }))}
            />
          </label>
          <label>
            New password
            <input
              type="password"
              minLength={8}
              value={passwordForm.newPassword}
              onChange={(event) => setPasswordForm((state) => ({ ...state, newPassword: event.target.value }))}
            />
          </label>
          <label>
            Confirm new password
            <input
              type="password"
              minLength={8}
              value={passwordForm.confirmNewPassword}
              onChange={(event) => setPasswordForm((state) => ({ ...state, confirmNewPassword: event.target.value }))}
            />
          </label>
          <button type="submit" className="ghost" disabled={savingProfile}>
            {savingProfile ? 'Updating...' : 'Change password'}
          </button>
        </form>

        {notificationPreference ? (
          <div className="prefs-summary">
            <p className="muted small">Delivery channels</p>
            <p>{notificationPreference.enabledChannels.join(', ')}</p>
            <p className="muted small">Preferred channel</p>
            <p>{notificationPreference.preferredChannel}</p>
            {me?.passwordResetRequired ? <p className="muted small">Password reset required: Yes</p> : null}
          </div>
        ) : (
          <p className="muted">No notification preference found.</p>
        )}
      </article>
    </section>
  )
}
