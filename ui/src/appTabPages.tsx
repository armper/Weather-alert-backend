import { lazy } from 'react'

type PageModule<T> = Promise<{ default: T }>

function loadOverviewPage(): PageModule<typeof import('./pages/OverviewPage').OverviewPage> {
  return import('./pages/OverviewPage').then((module) => ({ default: module.OverviewPage }))
}

function loadRulesPage(): PageModule<typeof import('./pages/RulesPage').RulesPage> {
  return import('./pages/RulesPage').then((module) => ({ default: module.RulesPage }))
}

function loadTravelPlansPage(): PageModule<typeof import('./pages/TravelPlansPage').TravelPlansPage> {
  return import('./pages/TravelPlansPage').then((module) => ({ default: module.TravelPlansPage }))
}

function loadAccountPage(): PageModule<typeof import('./pages/AccountPage').AccountPage> {
  return import('./pages/AccountPage').then((module) => ({ default: module.AccountPage }))
}

function loadSubscriptionPage(): PageModule<typeof import('./pages/SubscriptionPage').SubscriptionPage> {
  return import('./pages/SubscriptionPage').then((module) => ({ default: module.SubscriptionPage }))
}

function loadAdminPage(): PageModule<typeof import('./pages/AdminPage').AdminPage> {
  return import('./pages/AdminPage').then((module) => ({ default: module.AdminPage }))
}

export const OverviewPageRoute = lazy(loadOverviewPage)
export const RulesPageRoute = lazy(loadRulesPage)
export const TravelPlansPageRoute = lazy(loadTravelPlansPage)
export const AccountPageRoute = lazy(loadAccountPage)
export const SubscriptionPageRoute = lazy(loadSubscriptionPage)
export const AdminPageRoute = lazy(loadAdminPage)

export function AppTabPreview({ path }: { path: string | null }) {
  switch (path) {
    case '/app/overview':
      return <OverviewPageRoute />
    case '/app/rules':
      return <RulesPageRoute />
    case '/app/travel':
      return <TravelPlansPageRoute />
    case '/app/account':
      return <AccountPageRoute />
    case '/app/subscription':
      return <SubscriptionPageRoute />
    default:
      return null
  }
}

export const APP_TAB_PAGE_LOADERS = {
  overview: loadOverviewPage,
  rules: loadRulesPage,
  travel: loadTravelPlansPage,
  account: loadAccountPage,
  subscription: loadSubscriptionPage,
  admin: loadAdminPage,
} as const
