import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'

const VALUE_POINTS = [
  'Watch one location free and upgrade only when needed.',
  'Hyper-local alerts for heat, rain, wind, air quality, and river rise.',
  'Email and SMS delivery for the alerts you care about.',
]

const PLAN_GLANCE = [
  {
    name: 'The Basics',
    detail: '1 active alert. Perfect for your home or daily commute. Includes a small sponsored link.',
  },
  {
    name: 'The Family Plan',
    detail: 'Up to 10 alerts. Keep an eye on home, work, the kids’ school, and weekend sports. Ad-free.',
    badge: 'Most Popular',
  },
  {
    name: 'The Globetrotter',
    detail: 'Up to 50 alerts. Ideal for frequent travelers, RVers, or keeping tabs on extended family across the country.',
  },
]

export function AuthLandingPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const accountDeleted = new URLSearchParams(location.search).get('accountDeleted') === '1'

  function handleGetStarted(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedEmail = email.trim()
    const query = trimmedEmail ? `?email=${encodeURIComponent(trimmedEmail)}` : ''
    navigate(`/auth/register${query}`)
  }

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-layout auth-home-layout">
        <section className="panel hero-panel auth-home-hero">
          <BrandLockup />
          <div className="auth-home-copy">
            <h1>Weather alerts that actually matter to you.</h1>
            <p className="muted">
              SkyPanda turns noisy weather data into clear alerts. Monitor the exact locations you care about, and
              only upgrade when your needs grow.
            </p>
          </div>

          <ul className="feature-list auth-home-value-list">
            {VALUE_POINTS.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>

          <article className="auth-home-example-card">
            <span className="badge">Example alert</span>
            <strong>Heat Alert</strong>
            <p>Orlando, FL</p>
            <p>Temp above 92°F tomorrow afternoon</p>
          </article>

          <div className="auth-home-proof">
            <div className="auth-home-proof-card">
              <strong>Start free</strong>
              <span>One live alert is enough to see how the product fits your routine.</span>
            </div>
            <div className="auth-home-proof-card">
              <strong>Built for local detail</strong>
              <span>Use the map, current conditions, and forecast windows to tune each alert.</span>
            </div>
          </div>
        </section>

        <section className="panel stack auth-home-signup">
          <div className="auth-header">
            <p className="eyebrow">Get Started</p>
            <h1>Create your first alert</h1>
            <p className="muted">Open a free account, set your first alert, and decide later if you need more coverage.</p>
          </div>

          {accountDeleted ? (
            <div className="notice notice-success">
              Your account and saved personal data were deleted. You can create a new account at any time.
            </div>
          ) : null}

          <div className="auth-home-cta-stack">
            <form className="auth-home-signup-form" onSubmit={handleGetStarted}>
              <label className="auth-home-email-field">
                Email address
                <input
                  type="email"
                  name="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                  required
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </label>
              <p className="small muted auth-home-form-note">Start with your email. Finish the rest in the next step.</p>
              <button className="primary auth-home-primary-link" type="submit">
                Create a free account
              </button>
            </form>
            <p className="small muted">No credit card required.</p>
            <p className="small muted">
              Already using SkyPanda?{' '}
              <Link className="auth-link" to="/auth/login">
                Sign in
              </Link>
            </p>
          </div>

          <section className="auth-home-plan-glance">
            <div className="panel-title-row">
              <h2>Simple plans</h2>
              <span className="badge">Free first</span>
            </div>
            <div className="auth-home-plan-list">
              {PLAN_GLANCE.map((plan) => (
                <article key={plan.name} className="auth-home-plan-card">
                  <div className="auth-home-plan-card-header">
                    <strong>{plan.name}</strong>
                    {'badge' in plan && plan.badge ? <span className="badge">{plan.badge}</span> : null}
                  </div>
                  <p>{plan.detail}</p>
                </article>
              ))}
            </div>
          </section>

          <div className="auth-link-row auth-home-legal-links">
            <Link className="auth-link" to="/sms-consent">
              SMS consent
            </Link>
            <Link className="auth-link" to="/privacy-policy">
              Privacy policy
            </Link>
          </div>
        </section>
      </main>
    </div>
  )
}
