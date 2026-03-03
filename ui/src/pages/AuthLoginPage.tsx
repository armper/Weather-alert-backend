import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthLoginPage() {
  const { notice, loadingAuth, loginState, setLoginState, handleLogin } = useAppState()

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <p className="eyebrow">Weather Alert Console</p>
            <h1>Sign in</h1>
            <p className="muted">Monitor alerts without the extra account-management clutter.</p>
          </div>

          <form onSubmit={handleLogin} className="grid-form">
            <label>
              Username
              <input
                type="text"
                required
                value={loginState.username}
                onChange={(event) => setLoginState((state) => ({ ...state, username: event.target.value }))}
              />
            </label>
            <label>
              Password
              <input
                type="password"
                required
                value={loginState.password}
                onChange={(event) => setLoginState((state) => ({ ...state, password: event.target.value }))}
              />
            </label>
            <button type="submit" className="primary" disabled={loadingAuth}>
              {loadingAuth ? 'Signing in...' : 'Enter Dashboard'}
            </button>
          </form>

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/forgot-password">
              Forgot password?
            </Link>
            <Link className="auth-link" to="/auth/forgot-username">
              Forgot username?
            </Link>
          </div>

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/register">
              Create account
            </Link>
            <Link className="auth-link" to="/auth/verify-email">
              Verify email
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
