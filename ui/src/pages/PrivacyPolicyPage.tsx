import { PublicInfoLayout } from '../components/layout/PublicInfoLayout'

export function PrivacyPolicyPage() {
  return (
    <PublicInfoLayout
      eyebrow="Policy"
      title="SkyPanda Privacy Policy"
      summary="This page explains what information SkyPanda collects, how it is used to deliver weather monitoring and alerts, and how users can manage their data."
    >
      <section className="public-info-section">
        <h2>Information we collect</h2>
        <ul className="public-info-list">
          <li>Account details such as username, email address, password hash, and optional phone number.</li>
          <li>Monitoring preferences, alert rules, saved locations, and delivery-channel choices.</li>
          <li>Operational message history such as alerts sent, acknowledgements, and delivery outcomes.</li>
          <li>Technical and security metadata needed to protect accounts and operate the service.</li>
        </ul>
      </section>

      <section className="public-info-section">
        <h2>How we use information</h2>
        <ul className="public-info-list">
          <li>Provide weather monitoring, alerting, and account access.</li>
          <li>Deliver requested email and SMS notifications.</li>
          <li>Improve reliability, security, and abuse prevention.</li>
          <li>Maintain billing, subscription, and support workflows.</li>
        </ul>
      </section>

      <section className="public-info-section">
        <h2>SMS and communications data</h2>
        <p>
          Phone numbers and SMS consent records are used only for the alerting and verification workflows the user enabled.
          SkyPanda does not sell SMS opt-in data or consent records. Text messaging originators and delivery providers may
          process message metadata solely to deliver requested messages.
        </p>
      </section>

      <section className="public-info-section">
        <h2>Sharing</h2>
        <p>
          SkyPanda shares data only with service providers needed to run the product, such as cloud hosting, email, SMS,
          mapping, authentication, and billing providers. Those providers only receive the information required for their
          function.
        </p>
      </section>

      <section className="public-info-section">
        <h2>Retention</h2>
        <p>
          SkyPanda keeps account and alert data for as long as needed to operate the account, meet security and billing
          obligations, and support user-requested monitoring. Some operational weather and alert records may be removed on a
          rolling retention schedule.
        </p>
      </section>

      <section className="public-info-section">
        <h2>User choices</h2>
        <ul className="public-info-list">
          <li>Users can edit alert rules, delivery settings, and profile details from their account.</li>
          <li>Users can disable SMS delivery or reply STOP to opt out of text messages.</li>
          <li>Users can use email-only delivery if they do not want SMS.</li>
        </ul>
      </section>

      <section className="public-info-section">
        <h2>Contact</h2>
        <p>
          Questions about this policy or messaging practices should be directed through the SkyPanda account and support
          channels.
        </p>
      </section>
    </PublicInfoLayout>
  )
}
