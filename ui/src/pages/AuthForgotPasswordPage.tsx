import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAuthState, useNoticeState } from '../state/useAppState'

export function AuthForgotPasswordPage() {
  const { notice } = useNoticeState()
  const {
    loadingAuth,
    forgotPasswordState,
    setForgotPasswordState,
    passwordRetryAfterSeconds,
    handleForgotPasswordRequest,
    handleForgotPasswordConfirm,
  } = useAuthState()
  const [showPassword, setShowPassword] = useState(false)
  const [manualCodeEntry, setManualCodeEntry] = useState(false)
  const hasRecoveryLink = forgotPasswordState.recoveryId.trim() !== '' && forgotPasswordState.code.trim() !== ''
  const hideRecoveryInternals = hasRecoveryLink && !manualCodeEntry
  const showPasswordResetForm = hasRecoveryLink || manualCodeEntry

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <BrandLockup />
            <h1>Reset password</h1>
            <p className="muted">
              We’ll email you a secure reset link. Open it to choose a new password without dealing with recovery codes.
            </p>
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
              {passwordRetryAfterSeconds > 0 ? `Retry in ${passwordRetryAfterSeconds}s` : 'Email me a reset link'}
            </button>
          </form>

          {showPasswordResetForm ? (
            <form onSubmit={handleForgotPasswordConfirm} className="grid-form recovery-card">
              {hideRecoveryInternals ? (
                <>
                  <p className="recovery-inline-note">
                    You opened a secure reset link from your email. Choose a new password to finish.
                  </p>
                  <button type="button" className="link-button" onClick={() => setManualCodeEntry(true)}>
                    Having trouble with the link? Enter code manually
                  </button>
                </>
              ) : (
                <>
                  <p className="recovery-inline-note">
                    Enter the backup details from your reset email only if the link did not open correctly.
                  </p>
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
                    Reset code
                    <input
                      type="text"
                      required
                      value={forgotPasswordState.code}
                      onChange={(event) =>
                        setForgotPasswordState((state) => ({ ...state, code: event.target.value.toUpperCase() }))
                      }
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
          ) : (
            <section className="recovery-card recovery-awaiting-link">
              <strong>Next step</strong>
              <p className="muted">
                Check your inbox for the SkyPanda reset email. The link will take you straight to the new-password step.
              </p>
            </section>
          )}

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
