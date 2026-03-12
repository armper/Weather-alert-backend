import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BackgroundArtwork } from '../common/BackgroundArtwork'
import { BrandLockup } from '../common/BrandLockup'

interface PublicInfoLayoutProps {
  eyebrow: string
  title: string
  summary: string
  children: ReactNode
}

export function PublicInfoLayout({ eyebrow, title, summary, children }: PublicInfoLayoutProps) {
  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <main className="public-info-layout">
        <section className="panel public-info-panel">
          <div className="public-info-header">
            <BrandLockup compact />
            <p className="eyebrow">{eyebrow}</p>
            <h1>{title}</h1>
            <p className="muted public-info-summary">{summary}</p>
          </div>

          <div className="public-info-content">{children}</div>

          <div className="auth-link-row public-info-links">
            <Link className="auth-link" to="/">
              Back to home
            </Link>
            <Link className="auth-link" to="/privacy-policy">
              Privacy policy
            </Link>
            <Link className="auth-link" to="/sms-consent">
              SMS consent
            </Link>
          </div>
        </section>
      </main>
    </div>
  )
}
