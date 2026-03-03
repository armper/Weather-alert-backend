import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthForgotPasswordPage() {
  const {
    notice,
    loadingAuth,
    forgotPasswordState,
    setForgotPasswordState,
    passwordRecoveryMeta,
    passwordRetryAfterSeconds,
    handleForgotPasswordRequest,
    handleForgotPasswordConfirm,
  } = useAppState()

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <p className="eyebrow">Weather Alert Console</p>
            <h1>Forgot password</h1>
          </div>

          <form onSubmit={handleForgotPasswordRequest} className="grid-form recovery-card">
            <label>
              Username or email
              <input
                type="text"
                required
                value={forgotPasswordState.usernameOrEmail}
                onChange={(event) =>
                  setForgotPasswordState((state) => ({
                    ...state,
                    usernameOrEmail: event.target.value,
                  }))
                }
              />
            </label>
            <button type="submit" className="primary" disabled={loadingAuth || passwordRetryAfterSeconds > 0}>
              {passwordRetryAfterSeconds > 0 ? `Retry in ${passwordRetryAfterSeconds}s` : 'Send reset code'}
            </button>
          </form>

          <form onSubmit={handleForgotPasswordConfirm} className="grid-form recovery-card">
            <label>
              Recovery ID
              <input
                type="text"
                required
                value={forgotPasswordState.recoveryId}
                onChange={(event) =>
                  setForgotPasswordState((state) => ({
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
                value={forgotPasswordState.code}
                onChange={(event) => setForgotPasswordState((state) => ({ ...state, code: event.target.value }))}
              />
            </label>
            <label>
              New password
              <input
                type="password"
                minLength={8}
                required
                value={forgotPasswordState.newPassword}
                onChange={(event) =>
                  setForgotPasswordState((state) => ({
                    ...state,
                    newPassword: event.target.value,
                  }))
                }
              />
            </label>
            <button type="submit" className="ghost" disabled={loadingAuth}>
              Update password
            </button>
          </form>

          {passwordRecoveryMeta?.recoveryId ? (
            <p className="hint">
              Password recovery ID: <code>{passwordRecoveryMeta.recoveryId}</code>
              {passwordRecoveryMeta.recoveryCode ? (
                <>
                  {' '}
                  | dev code: <code>{passwordRecoveryMeta.recoveryCode}</code>
                </>
              ) : null}
            </p>
          ) : null}

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/login">
              Back to sign in
            </Link>
            <Link className="auth-link" to="/auth/forgot-username">
              Forgot username?
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
