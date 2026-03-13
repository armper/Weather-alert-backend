import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthRegisterPage() {
  const { notice, loadingAuth, registerState, setRegisterState, verifyState, latestVerification, handleRegister } = useAppState()
  const [showPassword, setShowPassword] = useState(false)
  const [searchParams] = useSearchParams()
  const initialEmail = searchParams.get('email')?.trim() ?? ''

  useEffect(() => {
    if (!initialEmail || registerState.email) {
      return
    }
    setRegisterState((state) => (state.email ? state : { ...state, email: initialEmail }))
  }, [initialEmail, registerState.email, setRegisterState])

  const verifyEmailLink = useMemo(() => {
    const params = new URLSearchParams()
    const userId = verifyState.userId.trim()
    const verificationId = (verifyState.verificationId || latestVerification?.id || '').trim()

    if (userId) {
      params.set('userId', userId)
    }
    if (verificationId) {
      params.set('verificationId', verificationId)
    }

    const query = params.toString()
    return query ? `/auth/verify-email?${query}` : '/auth/verify-email'
  }, [latestVerification?.id, verifyState.userId, verifyState.verificationId])

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <BrandLockup />
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
              <div className="input-with-action">
                <input
                  type={showPassword ? 'text' : 'password'}
                  minLength={8}
                  required
                  value={registerState.password}
                  onChange={(event) => setRegisterState((state) => ({ ...state, password: event.target.value }))}
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
            <Link className="auth-link" to={verifyEmailLink}>
              Verify email
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
