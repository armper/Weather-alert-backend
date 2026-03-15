import { useState } from 'react'
import { Link } from 'react-router-dom'
import { NoticeBanner } from '../components/common/NoticeBanner'
import backgroundLoginImage from '../assets/background-login.png'
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
  const trimmedIdentifier = loginState.username.trim()
  const hasPassword = loginState.password.trim().length > 0
  const loginReady = trimmedIdentifier.length > 0 && hasPassword
  const magicLinkReady = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedIdentifier)
  const recoveryCodeReady = magicLinkState.code.trim().length > 0

  const handleUsernameChange = (value: string) => {
    setLoginState((state) => ({ ...state, username: value }))
    setMagicLinkState((state) => ({ ...state, usernameOrEmail: value }))
  }

  return (
    <main className="auth-login-stage">
      <div className="auth-login-background" aria-hidden="true">
        <img className="auth-login-background-image" src={backgroundLoginImage} alt="" />
      </div>

      <section className="auth-login-shell">
        {notice ? (
          <div className="auth-login-notice">
            <NoticeBanner notice={notice} />
          </div>
        ) : null}

        <form onSubmit={handleLogin} className="auth-login-form">
          <input
            className="auth-login-input"
            type="text"
            required
            autoComplete="username"
            aria-label="Username or email"
            placeholder="Email address"
            value={loginState.username}
            onChange={(event) => handleUsernameChange(event.target.value)}
          />

          <div className="auth-login-password-shell">
            <input
              className="auth-login-input auth-login-input-password"
              type={showPassword ? 'text' : 'password'}
              required
              autoComplete="current-password"
              aria-label="Password"
              placeholder="Password"
              value={loginState.password}
              onChange={(event) => setLoginState((state) => ({ ...state, password: event.target.value }))}
            />
            <button type="button" className="auth-login-toggle" onClick={() => setShowPassword((value) => !value)}>
              {showPassword ? 'Hide' : 'Show'}
            </button>
          </div>

          <div className="auth-login-meta-row">
            <Link className="auth-login-text-link" to="/auth/forgot-password">
              Forgot password?
            </Link>
          </div>

          <button
            type="submit"
            className="action-bubble action-bubble-wide action-bubble-accent auth-login-primary"
            disabled={loadingAuth || confirmingMagicLink || !loginReady}
          >
            {loadingAuth ? 'Signing in...' : 'Log In'}
          </button>
        </form>

        <div className="auth-login-divider" aria-hidden="true">
          <span>or</span>
        </div>

        <form onSubmit={handleMagicLinkRequest} className="auth-login-secondary-form">
          <button
            type="submit"
            className="action-bubble action-bubble-wide action-bubble-soft auth-login-secondary"
            disabled={loadingAuth || confirmingMagicLink || !magicLinkReady}
          >
            {loadingAuth ? 'Sending link...' : 'Email me a sign-in link'}
          </button>
        </form>

        {confirmingMagicLink ? (
          <div className="auth-login-status">
            <strong>Signing you in...</strong>
            <span>Checking your one-time SkyPanda link.</span>
          </div>
        ) : null}

        {magicLinkMeta?.recoveryId ? (
          <form onSubmit={handleMagicLinkConfirm} className="auth-login-recovery">
            <input
              className="auth-login-input"
              type="text"
              inputMode="text"
              autoCapitalize="characters"
              aria-label="Backup code"
              placeholder="Backup code"
              required
              value={magicLinkState.code}
              onChange={(event) => setMagicLinkState((state) => ({ ...state, code: event.target.value.toUpperCase() }))}
            />
            <button
              type="submit"
              className="action-bubble action-bubble-wide action-bubble-soft auth-login-secondary"
              disabled={confirmingMagicLink || !recoveryCodeReady}
            >
              {confirmingMagicLink ? 'Verifying link...' : 'Sign in with code'}
            </button>
          </form>
        ) : null}

      </section>

      <nav className="auth-home-footer-links auth-login-footer" aria-label="Public information">
        <Link className="auth-home-footer-link" to="/about">
          About us
        </Link>
        <Link className="auth-home-footer-link" to="/privacy-policy">
          Privacy policy
        </Link>
        <Link className="auth-home-footer-link" to="/sms-consent">
          SMS policy
        </Link>
      </nav>
    </main>
  )
}
