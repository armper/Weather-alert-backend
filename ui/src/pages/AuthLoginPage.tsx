import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthLoginPage() {
  const { notice, loadingAuth, loginState, setLoginState, handleLogin } = useAppState()
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
            <button type="submit" className="primary" disabled={loadingAuth}>
              {loadingAuth ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

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
