import type { CSSProperties, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import backgroundImage from '../../assets/background.webp'

interface PublicPosterLayoutProps {
  eyebrow?: string
  title: string
  summary?: string
  children: ReactNode
  notice?: ReactNode
  width?: 'default' | 'wide'
}

export function PublicPosterLayout({
  eyebrow,
  title,
  summary,
  children,
  notice,
  width = 'default',
}: PublicPosterLayoutProps) {
  return (
    <main className="public-poster-stage">
      <section
        className="public-poster-shell"
        style={{ '--auth-home-background': `url(${backgroundImage})` } as CSSProperties}
      >
        <div className={`public-poster-scroll${width === 'wide' ? ' is-wide' : ''}`}>
          <article className="public-poster-card">
            <header className="public-poster-header">
              {eyebrow ? <p className="public-poster-eyebrow">{eyebrow}</p> : null}
              <h1>{title}</h1>
              {summary ? <p className="public-poster-summary">{summary}</p> : null}
            </header>

            {notice}

            <div className="public-poster-content">{children}</div>
          </article>
        </div>

        <nav className="auth-home-footer-links public-poster-footer" aria-label="Public information">
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
