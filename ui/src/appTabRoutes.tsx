import { lazy, type ComponentType, type LazyExoticComponent } from 'react'

type PageModule = { default: ComponentType }

function loadOverviewPage(): Promise<PageModule> {
  return import('./pages/OverviewPage').then((module) => ({ default: module.OverviewPage }))
}

function loadRulesPage(): Promise<PageModule> {
  return import('./pages/RulesPage').then((module) => ({ default: module.RulesPage }))
}

function loadTravelPlansPage(): Promise<PageModule> {
  return import('./pages/TravelPlansPage').then((module) => ({ default: module.TravelPlansPage }))
}

function loadAccountPage(): Promise<PageModule> {
  return import('./pages/AccountPage').then((module) => ({ default: module.AccountPage }))
}

function loadSubscriptionPage(): Promise<PageModule> {
  return import('./pages/SubscriptionPage').then((module) => ({ default: module.SubscriptionPage }))
}

function loadAdminPage(): Promise<PageModule> {
  return import('./pages/AdminPage').then((module) => ({ default: module.AdminPage }))
}

export const OverviewPageRoute = lazy(loadOverviewPage)
export const RulesPageRoute = lazy(loadRulesPage)
export const TravelPlansPageRoute = lazy(loadTravelPlansPage)
export const AccountPageRoute = lazy(loadAccountPage)
export const SubscriptionPageRoute = lazy(loadSubscriptionPage)
export const AdminPageRoute = lazy(loadAdminPage)

export const APP_PRIMARY_NAV_ITEMS = [
  { key: 'overview', label: 'Overview', to: '/app/overview', previewComponent: OverviewPageRoute, preload: loadOverviewPage },
  { key: 'rules', label: 'New Alert', to: '/app/rules', previewComponent: RulesPageRoute, preload: loadRulesPage },
  { key: 'travel', label: 'Travel Plans', to: '/app/travel', previewComponent: TravelPlansPageRoute, preload: loadTravelPlansPage },
  { key: 'account', label: 'Settings', to: '/app/account', previewComponent: AccountPageRoute, preload: loadAccountPage },
  { key: 'subscription', label: 'Subscription', to: '/app/subscription', previewComponent: SubscriptionPageRoute, preload: loadSubscriptionPage },
] as const

export const APP_ADMIN_NAV_ITEM = {
  key: 'admin',
  label: 'Admin',
  to: '/app/admin',
  previewComponent: AdminPageRoute,
  preload: loadAdminPage,
} as const

export type AppPrimaryNavItem = (typeof APP_PRIMARY_NAV_ITEMS)[number]

const PREVIEW_COMPONENT_BY_PATH = new Map<string, LazyExoticComponent<ComponentType>>(
  APP_PRIMARY_NAV_ITEMS.map((item) => [item.to, item.previewComponent]),
)

const PRELOAD_BY_PATH = new Map<string, () => Promise<PageModule>>(
  APP_PRIMARY_NAV_ITEMS.map((item) => [item.to, item.preload]),
)

export function getAppTabPreviewComponent(path: string) {
  return PREVIEW_COMPONENT_BY_PATH.get(path) ?? null
}

export function preloadAppTabPreview(path: string) {
  return PRELOAD_BY_PATH.get(path)?.()
}
