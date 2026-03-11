import { useMemo, type ReactNode } from 'react'
import { Menu, MenuItem, MenuTrigger, Popover } from 'react-aria-components'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { formatFriendlyLocation } from '../../lib/formatting'
import { useAppState } from '../../state/useAppState'
import { AriaButton } from '../ui/AriaButton'

interface AppShellProps {
  children: ReactNode
}

const PRIMARY_NAV_ITEMS = [
  { key: 'overview', label: 'Overview', to: '/app/overview' },
  { key: 'rules', label: 'New Alert', to: '/app/rules' },
  { key: 'alerts', label: 'My Alerts', to: '/app/alerts' },
  { key: 'events', label: 'Triggered Alerts', to: '/app/events' },
]

function NavIcon({ itemKey }: { itemKey: string }) {
  switch (itemKey) {
    case 'overview':
      return (
        <svg viewBox="0 0 20 20" className="shell-mobile-nav-icon" aria-hidden="true" focusable="false">
          <path d="M3.75 10.25 10 4.75l6.25 5.5v5a1 1 0 0 1-1 1h-2.75v-4h-5v4H4.75a1 1 0 0 1-1-1v-5Z" />
        </svg>
      )
    case 'rules':
      return (
        <svg viewBox="0 0 20 20" className="shell-mobile-nav-icon" aria-hidden="true" focusable="false">
          <path d="M10 4.5v11M4.5 10h11" />
        </svg>
      )
    case 'alerts':
      return (
        <svg viewBox="0 0 20 20" className="shell-mobile-nav-icon" aria-hidden="true" focusable="false">
          <path d="M10 3.75a4 4 0 0 0-4 4v2.4c0 .6-.2 1.18-.58 1.63L4.5 12.9v1.35h11v-1.35l-.92-1.12A2.5 2.5 0 0 1 14 10.15v-2.4a4 4 0 0 0-4-4Z" />
          <path d="M8.25 15.25a1.75 1.75 0 0 0 3.5 0" />
        </svg>
      )
    case 'events':
      return (
        <svg viewBox="0 0 20 20" className="shell-mobile-nav-icon" aria-hidden="true" focusable="false">
          <path d="M4.5 11.75h2.25l1.5-4 3.25 7 1.75-4h2.25" />
          <path d="M4.5 5.75h11" />
        </svg>
      )
    default:
      return null
  }
}

export function AppShell({ children }: AppShellProps) {
  const { me, criteria, logout } = useAppState()
  const location = useLocation()
  const navigate = useNavigate()

  const locationLabel = useMemo(() => {
    const primaryLocation = criteria[0]?.location?.trim()
    return formatFriendlyLocation(primaryLocation)
  }, [criteria])

  const userLabel = me?.name?.trim() || me?.id || 'Account'
  const avatarInitial = userLabel.charAt(0).toUpperCase()

  function handleUserMenuAction(key: React.Key) {
    if (key === 'account') {
      navigate('/app/account')
      return
    }
    if (key === 'signout') {
      logout()
    }
  }

  return (
    <div className="app-shell">
      <header className="shell-header panel">
        <div className="shell-header-left">
          <div>
            <p className="eyebrow">Weather Alerts</p>
            <h1>Alert Center</h1>
          </div>
        </div>

        <div className="shell-header-right">
          <Link to="/app/rules#location-picker" className="shell-location-chip" aria-label={`Current location ${locationLabel}`}>
            <span aria-hidden>📍</span>
            <span>{locationLabel}</span>
          </Link>

          <Link to="/app/rules#create-custom-alert" className="primary overview-create-link shell-create-link">
            <span aria-hidden className="overview-create-icon">
              <svg viewBox="0 0 12 12" className="create-plus-svg" focusable="false">
                <path d="M6 2.25v7.5M2.25 6h7.5" />
              </svg>
            </span>
            <span>New alert</span>
          </Link>

          <MenuTrigger>
            <AriaButton className="ghost shell-user-trigger" aria-label="Open account menu">
              <span className="shell-avatar" aria-hidden>
                {avatarInitial}
              </span>
              <span className="shell-user-mobile-label">Account</span>
              <span className="shell-user-id">{me?.id ?? 'Account'}</span>
              <span aria-hidden>▾</span>
            </AriaButton>

            <Popover placement="bottom end" className="shell-user-popover">
              <Menu className="shell-user-menu" onAction={handleUserMenuAction} aria-label="User menu">
                <MenuItem id="account" className="shell-user-menu-item">
                  Account
                </MenuItem>
                <MenuItem id="signout" className="shell-user-menu-item">
                  Sign out
                </MenuItem>
              </Menu>
            </Popover>
          </MenuTrigger>
        </div>
      </header>

      <div className="shell-body">
        <nav className="shell-sidebar panel" aria-label="Primary navigation">
          {PRIMARY_NAV_ITEMS.map((item) => (
            <NavLink key={item.key} to={item.to} className={({ isActive }) => `shell-nav-link${isActive ? ' active' : ''}`}>
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
            aria-current={location.pathname === item.to ? 'page' : undefined}
          >
            <NavIcon itemKey={item.key} />
            <span className="shell-mobile-nav-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
