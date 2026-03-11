import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthVerifyEmailPage() {
  const {
    notice,
    loadingAuth,
    verifyState,
    setVerifyState,
    latestVerification,
    handleVerifyEmail,
    handleResendVerification,
  } = useAppState()

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <BrandLockup />
            <h1>Verify email</h1>
          </div>

          <form onSubmit={handleVerifyEmail} className="grid-form">
            <label>
              Username
              <input
                type="text"
                required
                value={verifyState.userId}
                onChange={(event) => setVerifyState((state) => ({ ...state, userId: event.target.value }))}
              />
            </label>
            <label>
              Verification ID
              <input
                type="text"
                required
                value={verifyState.verificationId}
                onChange={(event) => setVerifyState((state) => ({ ...state, verificationId: event.target.value }))}
              />
            </label>
            <label>
              Token
              <input
                type="text"
                required
                value={verifyState.token}
                onChange={(event) => setVerifyState((state) => ({ ...state, token: event.target.value }))}
              />
            </label>
            <div className="button-row">
              <button type="submit" className="primary" disabled={loadingAuth}>
                Confirm email
              </button>
              <button type="button" className="ghost" onClick={handleResendVerification} disabled={loadingAuth}>
                Resend token
              </button>
            </div>
          </form>

          {latestVerification ? (
            <p className="hint">
              Latest verification: <strong>{latestVerification.id}</strong>
              {latestVerification.verificationToken ? (
                <>
                  {' '}
                  | dev token: <code>{latestVerification.verificationToken}</code>
                </>
              ) : null}
            </p>
          ) : null}

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/login">
              Back to sign in
            </Link>
            <Link className="auth-link" to="/auth/register">
              Need an account?
            </Link>
          </div>
        </section>

        {notice ? <NoticeBanner notice={notice} /> : null}
      </main>
    </div>
  )
}
