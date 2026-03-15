import { useState } from 'react'
import { Link } from 'react-router-dom'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { PublicPosterLayout } from '../components/layout/PublicPosterLayout'
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
    <PublicPosterLayout
      eyebrow="Account"
      title="Sign in"
      summary="Create and manage custom weather alerts with a calm, simple workflow."
      notice={notice ? <NoticeBanner notice={notice} /> : null}
    >
      <section className="auth-option-card stack public-poster-section-card">
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
              <button type="button" className="input-inline-action" onClick={() => setShowPassword((value) => !value)}>
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </label>
          <button
            type="submit"
            className="action-bubble action-bubble-wide action-bubble-accent"
            disabled={loadingAuth || confirmingMagicLink}
          >
            {loadingAuth ? 'Signing in...' : 'Sign in with password'}
          </button>
        </form>
      </section>

      <div className="auth-divider public-poster-divider" aria-hidden="true">
        <span>or</span>
      </div>

      <section className="auth-option-card stack public-poster-section-card">
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
              onChange={(event) => setMagicLinkState((state) => ({ ...state, usernameOrEmail: event.target.value }))}
            />
          </label>
          <button
            type="submit"
            className="action-bubble action-bubble-wide action-bubble-soft"
            disabled={loadingAuth || confirmingMagicLink}
          >
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
                onChange={(event) => setMagicLinkState((state) => ({ ...state, code: event.target.value.toUpperCase() }))}
              />
            </label>
            <button type="submit" className="action-bubble action-bubble-wide action-bubble-soft" disabled={confirmingMagicLink}>
              {confirmingMagicLink ? 'Verifying link...' : 'Sign in with code'}
            </button>
          </form>
        ) : null}
      </section>

      <div className="auth-link-row public-poster-link-row">
        <Link className="auth-link" to="/auth/forgot-password">
          Reset password
        </Link>
        <Link className="auth-link" to="/auth/register">
          Create account
        </Link>
      </div>
    </PublicPosterLayout>
  )
}
