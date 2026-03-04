import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Tab, TabList, TabPanel, Tabs } from 'react-aria-components'
import { useAppState } from '../../state/useAppState'
import { BackgroundArtwork } from '../common/BackgroundArtwork'
import { NoticeBanner } from '../common/NoticeBanner'
import { AriaButton } from '../ui/AriaButton'

const TAB_IDS = ['overview', 'rules', 'events', 'account'] as const

export function AuthenticatedLayout() {
  const { me, isAdmin, loadingData, refresh, logout, notice } = useAppState()
  const navigate = useNavigate()
  const location = useLocation()
  const pathnameTab = location.pathname.split('/')[2] ?? 'overview'
  const selectedTab = isAdmin
    ? (TAB_IDS.includes(pathnameTab as (typeof TAB_IDS)[number]) || pathnameTab === 'admin' ? pathnameTab : 'overview')
    : (TAB_IDS.includes(pathnameTab as (typeof TAB_IDS)[number]) ? pathnameTab : 'overview')

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
            <span className="chip-id">{me?.id}</span>
          </div>
          <AriaButton
            className="ghost icon-button"
            aria-label="Refresh data"
            onPress={() => void refresh()}
            isDisabled={loadingData}
          >
            {loadingData ? '…' : '↻'}
          </AriaButton>
          <AriaButton className="ghost subtle" onPress={logout}>
            Sign out
          </AriaButton>
        </div>
      </header>

      <Tabs
        selectedKey={selectedTab}
        className="app-tabs"
        onSelectionChange={(key) => navigate(`/app/${String(key)}`)}
      >
        <TabList aria-label="Main sections" className="app-nav panel">
          <Tab id="overview" className="app-nav-link">
            Overview
          </Tab>
          <Tab id="rules" className="app-nav-link">
            Alert Rules
          </Tab>
          <Tab id="events" className="app-nav-link">
            Triggered Alerts
          </Tab>
          <Tab id="account" className="app-nav-link">
            Account
          </Tab>
          {isAdmin ? (
            <Tab id="admin" className="app-nav-link">
              Admin Users
            </Tab>
          ) : null}
        </TabList>
        <TabPanel id={selectedTab} className="page-shell">
          <Outlet />
        </TabPanel>
      </Tabs>

      {notice ? <NoticeBanner notice={notice} /> : null}
    </div>
  )
}
