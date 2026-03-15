import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { NoticeBanner } from '../components/common/NoticeBanner'
import backgroundLoginImage from '../assets/background-login.png'
import { useAuthState, useNoticeState } from '../state/useAppState'

export function AuthRegisterPage() {
  const { notice } = useNoticeState()
  const { loadingAuth, registerState, setRegisterState, handleRegister } = useAuthState()
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [searchParams] = useSearchParams()
  const initialEmail = searchParams.get('email')?.trim() ?? ''
  const trimmedEmail = registerState.email.trim()
  const trimmedPhone = registerState.phoneNumber.trim()
  const phoneDigits = trimmedPhone.replace(/\D/g, '')
  const usernameReady = registerState.username.trim().length >= 3
  const passwordLength = registerState.password.length
  const passwordReady = passwordLength >= 8
  const showPasswordHint = passwordLength > 0 && !passwordReady
  const emailReady = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)
  const showEmailHint = trimmedEmail.length > 0 && !emailReady
  const phoneReady =
    trimmedPhone.length === 0 || (/^[+()\-\s.\d]+$/.test(trimmedPhone) && phoneDigits.length >= 10 && phoneDigits.length <= 15)
  const showPhoneHint = trimmedPhone.length > 0 && !phoneReady
  const registerReady = usernameReady && passwordReady && emailReady && phoneReady

  useEffect(() => {
    if (!initialEmail || registerState.email) {
      return
    }
    setRegisterState((state) => (state.email ? state : { ...state, email: initialEmail }))
  }, [initialEmail, registerState.email, setRegisterState])

  async function handleRegisterSubmit(event: FormEvent<HTMLFormElement>) {
    const success = await handleRegister(event)

    if (success) {
      navigate('/auth/login')
    }
  }

  return (
    <main className="auth-login-stage">
      <div className="auth-login-background" aria-hidden="true">
        <img className="auth-login-background-image" src={backgroundLoginImage} alt="" />
      </div>

      <section className="auth-login-shell auth-register-shell">
        {notice ? (
          <div className="auth-login-notice">
            <NoticeBanner notice={notice} />
          </div>
        ) : null}

        <form onSubmit={handleRegisterSubmit} className="auth-register-form">
          <input
            className="auth-login-input"
            type="text"
            minLength={3}
            required
            autoComplete="username"
            aria-label="Username"
            placeholder="Username"
            value={registerState.username}
            onChange={(event) => setRegisterState((state) => ({ ...state, username: event.target.value }))}
          />

          <input
            className="auth-login-input"
            type="email"
            required
            autoComplete="email"
            aria-label="Email address"
            aria-invalid={showEmailHint}
            placeholder="Email address"
            value={registerState.email}
            onChange={(event) => setRegisterState((state) => ({ ...state, email: event.target.value }))}
          />
          {showEmailHint ? (
            <p className="auth-register-helper" aria-live="polite">
              Enter a valid email address
            </p>
          ) : null}

          <div className="auth-login-password-shell">
            <input
              className="auth-login-input auth-login-input-password"
              type={showPassword ? 'text' : 'password'}
              minLength={8}
              required
              autoComplete="new-password"
              aria-label="Password"
              placeholder="Password"
              value={registerState.password}
              onChange={(event) => setRegisterState((state) => ({ ...state, password: event.target.value }))}
            />
            <button type="button" className="auth-login-toggle" onClick={() => setShowPassword((value) => !value)}>
              {showPassword ? 'Hide' : 'Show'}
            </button>
          </div>
          {showPasswordHint ? (
            <p className="auth-register-helper" aria-live="polite">
              Use at least 8 characters
            </p>
          ) : null}

          <input
            className="auth-login-input"
            type="text"
            autoComplete="name"
            aria-label="Name"
            placeholder="Name"
            value={registerState.name}
            onChange={(event) => setRegisterState((state) => ({ ...state, name: event.target.value }))}
          />

          <input
            className="auth-login-input"
            type="tel"
            autoComplete="tel"
            aria-label="Phone number"
            aria-invalid={showPhoneHint}
            placeholder="Phone number"
            value={registerState.phoneNumber}
            onChange={(event) => setRegisterState((state) => ({ ...state, phoneNumber: event.target.value }))}
          />
          {showPhoneHint ? (
            <p className="auth-register-helper" aria-live="polite">
              Enter a valid phone number or leave it blank
            </p>
          ) : null}

          <button
            type="submit"
            className="action-bubble action-bubble-wide action-bubble-accent auth-login-primary"
            disabled={loadingAuth || !registerReady}
          >
            {loadingAuth ? 'Creating account...' : 'Create account'}
          </button>
        </form>
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
