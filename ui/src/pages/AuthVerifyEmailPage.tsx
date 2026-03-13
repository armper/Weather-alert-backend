import { useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'
import { NoticeBanner } from '../components/common/NoticeBanner'
import { useAppState } from '../state/useAppState'

export function AuthVerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const {
    notice,
    loadingAuth,
    verifyState,
    setVerifyState,
    latestVerification,
    handleVerifyEmail,
    handleResendVerification,
  } = useAppState()
  const queryUserId = searchParams.get('userId')?.trim() ?? ''
  const queryVerificationId = searchParams.get('verificationId')?.trim() ?? ''

  useEffect(() => {
    const fallbackVerificationId = latestVerification?.id?.trim() ?? ''
    if (!queryUserId && !queryVerificationId && !fallbackVerificationId) {
      return
    }

    setVerifyState((state) => ({
      ...state,
      userId: queryUserId || state.userId,
      verificationId: queryVerificationId || state.verificationId || fallbackVerificationId,
    }))
  }, [latestVerification?.id, queryUserId, queryVerificationId, setVerifyState])

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-single-layout">
        <section className="panel stack">
          <div className="auth-header">
            <BrandLockup />
            <h1>Verify email</h1>
            <p className="muted">Enter the verification code from your email to finish setting up your account.</p>
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
              Verification code
              <input
                type="text"
                required
                autoComplete="one-time-code"
                inputMode="text"
                placeholder="ABCD-1234"
                value={verifyState.token}
                onChange={(event) => setVerifyState((state) => ({ ...state, token: event.target.value }))}
              />
            </label>
            <input type="hidden" value={verifyState.verificationId} readOnly />
            <div className="button-row">
              <button type="submit" className="primary" disabled={loadingAuth}>
                Confirm email
              </button>
              <button type="button" className="ghost" onClick={handleResendVerification} disabled={loadingAuth}>
                Resend code
              </button>
            </div>
          </form>

          {latestVerification ? (
            <p className="hint">
              We sent a code to <strong>{latestVerification.destination}</strong>.
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
