import { useState } from 'react'
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
    passwordRetryAfterSeconds,
    handleForgotPasswordRequest,
    handleForgotPasswordConfirm,
  } = useAppState()
  const [showPassword, setShowPassword] = useState(false)
  const [manualCodeEntry, setManualCodeEntry] = useState(false)
  const hasRecoveryLink = forgotPasswordState.recoveryId.trim() !== '' && forgotPasswordState.code.trim() !== ''
  const hideRecoveryInternals = hasRecoveryLink && !manualCodeEntry

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <p className="eyebrow">Weather Alerts</p>
            <h1>Reset password</h1>
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
            {hideRecoveryInternals ? (
              <>
                <p className="recovery-inline-note">
                  You opened a secure reset link from your email. Choose a new password to finish.
                </p>
                <button
                  type="button"
                  className="link-button"
                  onClick={() => setManualCodeEntry(true)}
                >
                  Enter code manually instead
                </button>
              </>
            ) : (
              <>
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
              </>
            )}
            <label>
              New password
              <div className="input-with-action">
                <input
                  type={showPassword ? 'text' : 'password'}
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
                <button
                  type="button"
                  className="input-inline-action"
                  onClick={() => setShowPassword((value) => !value)}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </label>
            <button type="submit" className="ghost" disabled={loadingAuth}>
              Update password
            </button>
          </form>

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/login">
              Back to sign in
            </Link>
            <Link className="auth-link" to="/auth/forgot-username">
              Recover username
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
