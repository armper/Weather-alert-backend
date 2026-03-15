import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAuthState, useNoticeState } from '../state/useAppState'

export function AuthLoginPage() {
  const { notice } = useNoticeState()
  const {
    loadingAuth,
    confirmingMagicLink,
    loginState,
    setLoginState,
    magicLinkState,
    setMagicLinkState,
    magicLinkMeta,
    handleLogin,
    handleMagicLinkRequest,
    handleMagicLinkConfirm,
  } = useAuthState()
  const [showPassword, setShowPassword] = useState(false)

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <BrandLockup />
            <h1>Sign in</h1>
            <p className="muted">Create and manage custom weather alerts with a calm, simple workflow.</p>
          </div>

          <section className="auth-option-card stack">
            <div className="stack-sm">
              <h2>Password</h2>
              <p className="muted">Use your username or email and password.</p>
            </div>

            <form onSubmit={handleLogin} className="grid-form">
              <label>
                Username or email
                <input
                  type="text"
                  required
                  value={loginState.username}
                  onChange={(event) => setLoginState((state) => ({ ...state, username: event.target.value }))}
                />
              </label>
              <label>
                Password
                <div className="input-with-action">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={loginState.password}
                    onChange={(event) => setLoginState((state) => ({ ...state, password: event.target.value }))}
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
              <button type="submit" className="primary" disabled={loadingAuth || confirmingMagicLink}>
                {loadingAuth ? 'Signing in...' : 'Sign in with password'}
              </button>
            </form>
          </section>

          <div className="auth-divider" aria-hidden="true">
            <span>or</span>
          </div>

          <section className="auth-option-card stack">
            <div className="stack-sm">
              <h2>Email link</h2>
              <p className="muted">We’ll send a one-time sign-in link to your inbox. No password required.</p>
            </div>

            <form onSubmit={handleMagicLinkRequest} className="grid-form">
              <label>
                Email or username
                <input
                  type="text"
                  required
                  value={magicLinkState.usernameOrEmail}
                  onChange={(event) =>
                    setMagicLinkState((state) => ({ ...state, usernameOrEmail: event.target.value }))
                  }
                />
              </label>
              <button type="submit" className="secondary" disabled={loadingAuth || confirmingMagicLink}>
                {loadingAuth ? 'Sending link...' : 'Email me a sign-in link'}
              </button>
            </form>

            {confirmingMagicLink ? (
              <div className="auth-magic-link-status">
                <strong>Signing you in…</strong>
                <span>Checking your one-time SkyPanda link.</span>
              </div>
            ) : null}

            {magicLinkMeta?.recoveryId ? (
              <form onSubmit={handleMagicLinkConfirm} className="grid-form auth-magic-link-fallback">
                <div className="stack-sm">
                  <strong>Already requested a link?</strong>
                  <p className="muted">If you opened the email on this same device, you can enter the backup code here.</p>
                </div>
                <label>
                  Backup code
                  <input
                    type="text"
                    inputMode="text"
                    autoCapitalize="characters"
                    placeholder="A2B3C4D5"
                    required
                    value={magicLinkState.code}
                    onChange={(event) =>
                      setMagicLinkState((state) => ({ ...state, code: event.target.value.toUpperCase() }))
                    }
                  />
                </label>
                <button type="submit" className="secondary" disabled={confirmingMagicLink}>
                  {confirmingMagicLink ? 'Verifying link...' : 'Sign in with code'}
                </button>
              </form>
            ) : null}
          </section>

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/forgot-password">
              Reset password
            </Link>
            <Link className="auth-link" to="/auth/register">
              Create account
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
