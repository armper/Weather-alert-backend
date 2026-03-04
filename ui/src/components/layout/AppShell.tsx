import { useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  Dialog,
  Menu,
  MenuItem,
  MenuTrigger,
  Modal,
  ModalOverlay,
  Popover,
} from 'react-aria-components'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { formatFriendlyLocation } from '../../lib/formatting'
import { useAppState } from '../../state/useAppState'
import { AriaButton } from '../ui/AriaButton'

interface AppShellProps {
  children: ReactNode
}

const PRIMARY_NAV_ITEMS = [
  { key: 'overview', label: 'Overview', to: '/app/overview' },
  { key: 'rules', label: 'Create', to: '/app/rules' },
  { key: 'alerts', label: 'Alerts', to: '/app/alerts' },
  { key: 'events', label: 'Activity', to: '/app/events' },
]

export function AppShell({ children }: AppShellProps) {
  const { me, criteria, logout } = useAppState()
  const location = useLocation()
  const navigate = useNavigate()
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)

  useEffect(() => {
    setIsDrawerOpen(false)
  }, [location.pathname, location.hash])

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
          <AriaButton
            className="ghost icon-button shell-menu-toggle"
            aria-label="Open navigation menu"
            onPress={() => setIsDrawerOpen(true)}
          >
            ☰
          </AriaButton>

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
            <span>Create alert</span>
          </Link>

          <MenuTrigger>
            <AriaButton className="ghost shell-user-trigger" aria-label="Open account menu">
              <span className="shell-avatar" aria-hidden>
                {avatarInitial}
              </span>
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

      <ModalOverlay className="shell-drawer-overlay" isOpen={isDrawerOpen} onOpenChange={setIsDrawerOpen}>
        <Modal className="shell-drawer-modal">
          <Dialog className="shell-drawer-dialog">
            <div className="shell-drawer-top">
              <p className="eyebrow">Navigation</p>
              <AriaButton className="ghost icon-button" slot="close" aria-label="Close navigation menu">
                ✕
              </AriaButton>
            </div>

            <nav className="shell-drawer-nav" aria-label="Mobile primary navigation">
              {PRIMARY_NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.key}
                  to={item.to}
                  onClick={() => setIsDrawerOpen(false)}
                  className={({ isActive }) => `shell-nav-link shell-drawer-link${isActive ? ' active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </Dialog>
        </Modal>
      </ModalOverlay>
    </div>
  )
}
