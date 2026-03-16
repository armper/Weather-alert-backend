import { type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useSessionState } from '../../state/useAppState'

interface AppShellProps {
  children: ReactNode
}

const PRIMARY_NAV_ITEMS = [
  { key: 'overview', label: 'Overview', to: '/app/overview' },
  { key: 'rules', label: 'New Alert', to: '/app/rules' },
  { key: 'travel', label: 'Travel Plans', to: '/app/travel' },
  { key: 'account', label: 'Settings', to: '/app/account' },
  { key: 'subscription', label: 'Subscription', to: '/app/subscription' },
] as const

function NavIcon({ itemKey, className = 'shell-mobile-nav-icon' }: Readonly<{ itemKey: string; className?: string }>) {
  switch (itemKey) {
    case 'overview':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M3.75 10.25 10 4.75l6.25 5.5v5a1 1 0 0 1-1 1h-2.75v-4h-5v4H4.75a1 1 0 0 1-1-1v-5Z" />
        </svg>
      )
    case 'rules':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M10 4.5v11M4.5 10h11" />
        </svg>
      )
    case 'travel':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M3.75 9.25h12.5" />
          <path d="M8 9.25 4.75 5.5" />
          <path d="M8 9.25 4.75 13" />
          <path d="M10.75 9.25 15.25 5.75" />
          <path d="M10.75 9.25 15.25 12.75" />
        </svg>
      )
    case 'account':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <circle cx="10" cy="10" r="6.5" fill="none" />
          <path d="M10 6.5a2.25 2.25 0 1 1 0 4.5 2.25 2.25 0 0 1 0-4.5Z" />
          <path d="M12.6 7.4l1.15-1.15M7.4 12.6l-1.15 1.15M13.5 10h1.6M4.9 10H6.5M12.6 12.6l1.15 1.15M7.4 7.4 6.25 6.25" />
        </svg>
      )
    case 'subscription':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <rect x="3.5" y="5" width="13" height="10" rx="1.5" fill="none" />
          <path d="M3.5 8.5h13" />
          <path d="M6.5 12h3" />
        </svg>
      )
    case 'admin':
      return (
        <svg viewBox="0 0 20 20" className={className} aria-hidden="true" focusable="false">
          <path d="M10 2.75 15.75 5v4.25c0 3.7-2.16 6.53-5.75 8-3.59-1.47-5.75-4.3-5.75-8V5L10 2.75Z" />
          <path d="M10 7.25v4.5M7.75 9.5h4.5" />
        </svg>
      )
    default:
      return null
  }
}

export function AppShell({ children }: AppShellProps) {
  const { isAdmin } = useSessionState()
  const location = useLocation()
  const overviewRoutePrefixes = [
    '/app/overview',
    '/app/rules',
  ]
  const bottomNavRoutePrefixes = [
    ...overviewRoutePrefixes,
    '/app/travel',
    '/app/account',
    '/app/subscription',
  ]
  const hasBottomNav = bottomNavRoutePrefixes.some((prefix) => location.pathname.startsWith(prefix))
  const isImmersiveRoute = overviewRoutePrefixes.some((prefix) => location.pathname.startsWith(prefix))
  const desktopNavItems = isAdmin
    ? [...PRIMARY_NAV_ITEMS, { key: 'admin', label: 'Admin', to: '/app/admin' as const }]
    : PRIMARY_NAV_ITEMS

  return (
    <div className={`app-shell${hasBottomNav ? ' has-bottom-nav' : ''}${isImmersiveRoute ? ' is-overview-route' : ''}`}>
      <div className="shell-body">
        <nav className="shell-sidebar panel" aria-label="Primary navigation">
          {desktopNavItems.map((item) => (
            <NavLink key={item.key} to={item.to} className={({ isActive }) => `shell-nav-link${isActive ? ' active' : ''}`}>
              <NavIcon itemKey={item.key} className="shell-nav-icon" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <main className="shell-content">{children}</main>
      </div>

      <nav className="shell-mobile-nav" aria-label="Mobile primary navigation">
        {PRIMARY_NAV_ITEMS.map((item) => (
          <NavLink
            key={item.key}
            to={item.to}
            className={({ isActive }) => `shell-mobile-nav-link${isActive ? ' active' : ''}`}
            aria-label={item.label}
            aria-current={location.pathname === item.to ? 'page' : undefined}
          >
            <NavIcon itemKey={item.key} />
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
