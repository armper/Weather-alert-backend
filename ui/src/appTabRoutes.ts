import { APP_TAB_PAGE_LOADERS } from './appTabPages'

type PageModule = Awaited<ReturnType<(typeof APP_TAB_PAGE_LOADERS)[keyof typeof APP_TAB_PAGE_LOADERS]>>

export const APP_PRIMARY_NAV_ITEMS = [
  {
    key: 'overview',
    label: 'Overview',
    to: '/app/overview',
    preload: APP_TAB_PAGE_LOADERS.overview,
  },
  {
    key: 'rules',
    label: 'New Alert',
    to: '/app/rules',
    preload: APP_TAB_PAGE_LOADERS.rules,
  },
  {
    key: 'travel',
    label: 'Travel Plans',
    to: '/app/travel',
    preload: APP_TAB_PAGE_LOADERS.travel,
  },
  {
    key: 'account',
    label: 'Settings',
    to: '/app/account',
    preload: APP_TAB_PAGE_LOADERS.account,
  },
  {
    key: 'subscription',
    label: 'Subscription',
    to: '/app/subscription',
    preload: APP_TAB_PAGE_LOADERS.subscription,
  },
] as const

export const APP_ADMIN_NAV_ITEM = {
  key: 'admin',
  label: 'Admin',
  to: '/app/admin',
  preload: APP_TAB_PAGE_LOADERS.admin,
} as const

export type AppPrimaryNavItem = (typeof APP_PRIMARY_NAV_ITEMS)[number]

const PRELOAD_BY_PATH = new Map<string, () => Promise<PageModule>>(
  APP_PRIMARY_NAV_ITEMS.map((item) => [item.to, item.preload]),
)

export function preloadAppTabPreview(path: string) {
  return PRELOAD_BY_PATH.get(path)?.()
}
