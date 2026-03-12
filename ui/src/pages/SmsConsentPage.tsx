import { Link } from 'react-router-dom'
import { PublicInfoLayout } from '../components/layout/PublicInfoLayout'

const CONSENT_STEPS = [
  'Create a SkyPanda account with a valid email address.',
  'Add a phone number to your account profile.',
  'Enable SMS in delivery preferences and choose SMS as an available alert channel.',
  'Save your preferences and keep at least one monitoring rule active for the locations you want watched.',
]

const MESSAGE_TYPES = [
  'Weather alert notifications tied to the rules you created in SkyPanda.',
  'Important account or delivery notices related to your alert setup.',
  'Verification or security messages when you request them.',
]

export function SmsConsentPage() {
  return (
    <PublicInfoLayout
      eyebrow="Compliance"
      title="SkyPanda SMS consent and program terms"
      summary="This public page explains how SkyPanda collects consent for SMS weather alerts and what subscribers can expect when they opt in."
    >
      <section className="public-info-section">
        <h2>How consent is collected</h2>
        <p>
          SkyPanda only sends SMS messages after the account owner actively enables SMS delivery inside the product. A user
          must complete all of these steps before receiving text alerts:
        </p>
        <ol className="public-info-list public-info-list-ordered">
          {CONSENT_STEPS.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ol>
        <p>
          SMS consent is not a condition of purchase. Users can keep using SkyPanda with email-only delivery if they do not
          want text messages.
        </p>
      </section>

      <section className="public-info-section">
        <h2>Consent language shown to users</h2>
        <div className="public-info-callout">
          <strong>Example disclosure</strong>
          <p>
            By enabling SMS delivery, you agree to receive SkyPanda weather alert text messages for the monitoring rules on
            your account. Message frequency varies based on weather conditions and your saved alerts. Message and data rates
            may apply. Reply <strong>STOP</strong> to opt out and <strong>HELP</strong> for help.
          </p>
        </div>
      </section>

      <section className="public-info-section">
        <h2>What messages are sent</h2>
        <ul className="public-info-list">
          {MESSAGE_TYPES.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>

      <section className="public-info-section">
        <h2>Message frequency</h2>
        <p>
          Message frequency varies. It depends on the alert rules a user creates, the number of monitored locations, and
          actual weather activity affecting those locations.
        </p>
      </section>

      <section className="public-info-section">
        <h2>Opt-out and help</h2>
        <p>
          Users can reply <strong>STOP</strong> to end SMS messages at any time. Users can reply <strong>HELP</strong> for
          assistance, or contact SkyPanda support through the product account area.
        </p>
      </section>

      <section className="public-info-section">
        <h2>Privacy</h2>
        <p>
          SkyPanda uses phone numbers and alert preferences only to deliver and manage requested messages. For more detail,
          review the{' '}
          <Link className="auth-link" to="/privacy-policy">
            SkyPanda Privacy Policy
          </Link>
          .
        </p>
      </section>
    </PublicInfoLayout>
  )
}
