import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../components/common/BackgroundArtwork'
import { BrandLockup } from '../components/common/BrandLockup'

const VALUE_POINTS = [
  'Watch one place for free and upgrade only when you need more coverage.',
  'Build hyper-local alerts for heat, rain, wind, air quality, and river rise.',
  'Choose the channels that fit your routine: email today, SMS-ready next.',
]

const PLAN_GLANCE = [
  { name: 'Free', detail: '1 active alert, email delivery, sponsored footer' },
  { name: 'Plus', detail: '10 active alerts, ad-free email delivery' },
  { name: 'Pro', detail: '50 active alerts, best fit for multi-location monitoring' },
]

export function AuthLandingPage() {
  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="auth-layout auth-home-layout">
        <section className="panel hero-panel auth-home-hero">
          <BrandLockup />
          <div className="auth-home-copy">
            <h1>Storms, heat, river rise, and sudden changes should not catch you late.</h1>
            <p className="muted">
              SkyPanda turns noisy weather data into calm, human alerts you can act on. Build one watch in minutes,
              keep tabs on the places that matter, and only pay when your monitoring gets serious.
            </p>
          </div>

          <ul className="feature-list auth-home-value-list">
            {VALUE_POINTS.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>

          <div className="auth-home-proof">
            <div className="auth-home-proof-card">
              <strong>Start free</strong>
              <span>One live alert is enough to see how the product fits your routine.</span>
            </div>
            <div className="auth-home-proof-card">
              <strong>Built for local detail</strong>
              <span>Use the map, current conditions, and forecast windows to tune each watch.</span>
            </div>
          </div>
        </section>

        <section className="panel stack auth-home-signup">
          <div className="auth-header">
            <p className="eyebrow">Get Started</p>
            <h1>Create your first watch</h1>
            <p className="muted">Open a free account, set your first alert, and decide later if you need more coverage.</p>
          </div>

          <div className="auth-home-cta-stack">
            <Link className="primary auth-home-primary-link" to="/auth/register">
              Create a free account
            </Link>
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
                  <strong>{plan.name}</strong>
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
