import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthForgotUsernamePage() {
  const {
    notice,
    loadingAuth,
    forgotUsernameState,
    setForgotUsernameState,
    usernameRecoveryMeta,
    usernameRetryAfterSeconds,
    handleForgotUsernameRequest,
    handleForgotUsernameConfirm,
  } = useAppState()

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <p className="eyebrow">Weather Alert Console</p>
            <h1>Forgot username</h1>
          </div>

          <form onSubmit={handleForgotUsernameRequest} className="grid-form recovery-card">
            <label>
              Account email
              <input
                type="email"
                required
                value={forgotUsernameState.email}
                onChange={(event) => setForgotUsernameState((state) => ({ ...state, email: event.target.value }))}
              />
            </label>
            <button type="submit" className="primary" disabled={loadingAuth || usernameRetryAfterSeconds > 0}>
              {usernameRetryAfterSeconds > 0 ? `Retry in ${usernameRetryAfterSeconds}s` : 'Send code'}
            </button>
          </form>

          <form onSubmit={handleForgotUsernameConfirm} className="grid-form recovery-card">
            <label>
              Recovery ID
              <input
                type="text"
                required
                value={forgotUsernameState.recoveryId}
                onChange={(event) =>
                  setForgotUsernameState((state) => ({
                    ...state,
                    recoveryId: event.target.value,
                  }))
                }
              />
            </label>
            <label>
              Code
              <input
                type="text"
                required
                value={forgotUsernameState.code}
                onChange={(event) => setForgotUsernameState((state) => ({ ...state, code: event.target.value }))}
              />
            </label>
            <button type="submit" className="ghost" disabled={loadingAuth}>
              Reveal username
            </button>
          </form>

          {usernameRecoveryMeta?.recoveryId ? (
            <p className="hint">
              Recovery ID: <code>{usernameRecoveryMeta.recoveryId}</code>
              {usernameRecoveryMeta.recoveryCode ? (
                <>
                  {' '}
                  | dev code: <code>{usernameRecoveryMeta.recoveryCode}</code>
                </>
              ) : null}
            </p>
          ) : null}

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/login">
              Back to sign in
            </Link>
            <Link className="auth-link" to="/auth/forgot-password">
              Forgot password?
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
