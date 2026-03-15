import { useEffect, useRef, type CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import backgroundImage from '../assets/background.png'

export function AuthLandingPage() {
  const stageRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    const node = stageRef.current
    if (!node) {
      return
    }

    let frame = 0

    const syncScrollOffset = () => {
      frame = 0
      const offset = Math.min(window.scrollY, 42) * 0.16
      node.style.setProperty('--auth-home-scroll-offset', `${offset}px`)
    }

    const handleScroll = () => {
      if (frame !== 0) {
        return
      }
      frame = window.requestAnimationFrame(syncScrollOffset)
    }

    syncScrollOffset()
    window.addEventListener('scroll', handleScroll, { passive: true })

    return () => {
      window.removeEventListener('scroll', handleScroll)
      if (frame !== 0) {
        window.cancelAnimationFrame(frame)
      }
    }
  }, [])

  return (
    <main className="auth-home-stage" ref={stageRef}>
      <section
        className="auth-home-poster"
        style={{ '--auth-home-background': `url(${backgroundImage})` } as CSSProperties}
      >
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
