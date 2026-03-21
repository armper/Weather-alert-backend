import { Link } from 'react-router-dom'
import backgroundImage from '../assets/background.webp'

export function AuthLandingPage() {
  return (
    <main className="auth-home-stage">
      <div className="auth-home-background" aria-hidden="true">
        <img
          className="auth-home-background-image"
          src={backgroundImage}
          alt=""
          decoding="async"
          fetchPriority="high"
        />
      </div>

      <section className="auth-home-poster">
        <div className="auth-home-hero">
          <div className="auth-home-title">
            <span>Welcome to</span>
            <strong>Sky Panda</strong>
          </div>
          <div className="auth-home-actions" role="group" aria-label="Authentication actions">
            <Link className="action-bubble action-bubble-accent auth-home-action-bubble" to="/auth/register">
              Sign up
            </Link>
            <Link className="action-bubble auth-home-action-bubble" to="/auth/login">
              Log in
            </Link>
          </div>
        </div>

        <nav className="auth-home-footer-links" aria-label="Public information">
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
      </section>
    </main>
  )
}
