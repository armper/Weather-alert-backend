import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'

const VALUE_POINTS = [
  'Watch one location free and upgrade only when needed.',
  'Hyper-local alerts for heat, rain, wind, air quality, and river rise.',
  'Email and SMS delivery for the alerts you care about.',
]

const PLAN_GLANCE = [
  { name: 'Free', detail: '1 active alert, email and SMS delivery, sponsored email footer' },
  { name: 'Plus', detail: '10 active alerts, ad-free email and SMS delivery', badge: 'Most Popular' },
  { name: 'Pro', detail: '50 active alerts, best fit for multi-location monitoring and broader coverage' },
]

export function AuthLandingPage() {
  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-layout auth-home-layout">
        <section className="panel hero-panel auth-home-hero">
          <BrandLockup />
          <div className="auth-home-copy">
            <h1>Weather alerts that actually matter to you.</h1>
            <p className="muted">
              SkyPanda turns noisy weather data into clear alerts you can act on. Watch the places that matter to you,
              tune each alert to a specific location, and only upgrade when your monitoring gets serious.
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

          <div className="auth-home-cta-stack">
            <Link className="primary auth-home-primary-link" to="/auth/register">
              Create a free account
            </Link>
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

          <div className="auth-link-row">
            <Link className="auth-link" to="/auth/verify-email">
              Verify email
            </Link>
            <Link className="auth-link" to="/auth/forgot-password">
              Reset password
            </Link>
          </div>
        </section>
      </main>
    </div>
  )
}
