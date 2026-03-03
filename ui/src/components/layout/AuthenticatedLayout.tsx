import { NavLink, Outlet } from 'react-router-dom'
import { useAppState } from '../../state/useAppState'
import { BackgroundArtwork } from '../common/BackgroundArtwork'
import { NoticeBanner } from '../common/NoticeBanner'

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'app-nav-link active' : 'app-nav-link'
}

export function AuthenticatedLayout() {
  const { me, isAdmin, loadingData, refresh, logout, notice } = useAppState()

  return (
    <div className="app-shell">
      <BackgroundArtwork />
      <header className="topbar">
        <div>
          <p className="eyebrow">Weather Alerts</p>
          <h1>Alert Center</h1>
        </div>
        <div className="topbar-actions">
          <div className="user-chip">
            <span className="chip-role">{me?.role ?? 'ROLE_USER'}</span>
            <span className="chip-id">{me?.id}</span>
          </div>
          <button className="ghost" onClick={() => void refresh()} disabled={loadingData}>
            {loadingData ? 'Refreshing...' : 'Refresh'}
          </button>
          <button className="ghost danger" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>

      <nav className="app-nav panel">
        <NavLink to="/app/overview" className={navClass}>
          Overview
        </NavLink>
        <NavLink to="/app/rules" className={navClass}>
          Alert Rules
        </NavLink>
        <NavLink to="/app/events" className={navClass}>
          Triggered Alerts
        </NavLink>
        <NavLink to="/app/account" className={navClass}>
          Account
        </NavLink>
        {isAdmin ? (
          <NavLink to="/app/admin" className={navClass}>
            Admin Users
          </NavLink>
        ) : null}
      </nav>

      <main className="page-shell">
        <Outlet />
      </main>

      {notice ? <NoticeBanner notice={notice} /> : null}
    </div>
  )
}
