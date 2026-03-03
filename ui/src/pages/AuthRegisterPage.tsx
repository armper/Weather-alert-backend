import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthRegisterPage() {
  const { notice, loadingAuth, registerState, setRegisterState, handleRegister } = useAppState()

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <p className="eyebrow">Weather Alert Console</p>
            <h1>Create account</h1>
          </div>

          <form onSubmit={handleRegister} className="grid-form two-col">
            <label>
              Username
              <input
                type="text"
                minLength={3}
                required
                value={registerState.username}
                onChange={(event) => setRegisterState((state) => ({ ...state, username: event.target.value }))}
              />
            </label>
            <label>
              Password
              <input
                type="password"
                minLength={8}
                required
                value={registerState.password}
                onChange={(event) => setRegisterState((state) => ({ ...state, password: event.target.value }))}
              />
            </label>
            <label>
              Email
              <input
                type="email"
                required
                value={registerState.email}
                onChange={(event) => setRegisterState((state) => ({ ...state, email: event.target.value }))}
              />
            </label>
            <label>
              Phone
              <input
                type="text"
                placeholder="+14075551234"
                value={registerState.phoneNumber}
                onChange={(event) => setRegisterState((state) => ({ ...state, phoneNumber: event.target.value }))}
              />
            </label>
            <label className="full-row">
              Name
              <input
                type="text"
                value={registerState.name}
                onChange={(event) => setRegisterState((state) => ({ ...state, name: event.target.value }))}
              />
            </label>
            <button type="submit" className="primary full-row" disabled={loadingAuth}>
              {loadingAuth ? 'Creating account...' : 'Create account'}
            </button>
          </form>

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/login">
              Back to sign in
            </Link>
            <Link className="auth-link" to="/auth/verify-email">
              Already registered? Verify email
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
