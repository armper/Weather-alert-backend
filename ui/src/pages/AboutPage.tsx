import { PublicInfoLayout } from '../components/layout/PublicInfoLayout'

export function AboutPage() {
  return (
    <PublicInfoLayout
      eyebrow="About"
      title="What SkyPanda does"
      summary="SkyPanda keeps weather alerting simple. You choose a place, choose what matters, and get a clear alert when conditions change."
    >
      <section className="public-info-section">
        <h2>How it works</h2>
        <ul className="public-info-list">
          <li>Pick the place you want watched.</li>
          <li>Choose what you care about, like rain, heat, wind, air quality, or river rise.</li>
          <li>Get a clear alert without digging through a complicated weather app.</li>
        </ul>
      </section>

      <section className="public-info-section">
        <h2>Who it is for</h2>
        <p>
          SkyPanda is made for normal people who just want to know what matters for home, school, work, or travel without
          having to decode weather jargon.
        </p>
      </section>

      <section className="public-info-section">
        <h2>Plans</h2>
        <ul className="public-info-list">
          <li>Free: one watched place to get started.</li>
          <li>Family: more places for the routines you manage every day.</li>
          <li>Travel: broader coverage for people moving between many locations.</li>
        </ul>
      </section>
    </PublicInfoLayout>
  )
}
