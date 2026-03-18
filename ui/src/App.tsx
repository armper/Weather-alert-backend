import { Suspense, lazy, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import {
  AccountPageRoute,
  AdminPageRoute,
  OverviewPageRoute,
  RulesPageRoute,
  SubscriptionPageRoute,
  TravelPlansPageRoute,
} from './appTabPages'
import { AuthenticatedLayout } from './components/layout/AuthenticatedLayout'
import { RequireAuth } from './components/layout/RequireAuth'
import { LoadingPlaceholder } from './components/common/LoadingPlaceholder'
import { AppStateProvider } from './state/AppStateContext'
import { useSessionState } from './state/useAppState'

const AboutPage = lazy(() => import('./pages/AboutPage').then((module) => ({ default: module.AboutPage })))
const AuthForgotPasswordPage = lazy(() =>
  import('./pages/AuthForgotPasswordPage').then((module) => ({ default: module.AuthForgotPasswordPage })),
)
const AuthForgotUsernamePage = lazy(() =>
  import('./pages/AuthForgotUsernamePage').then((module) => ({ default: module.AuthForgotUsernamePage })),
)
const AuthLandingPage = lazy(() => import('./pages/AuthLandingPage').then((module) => ({ default: module.AuthLandingPage })))
const AuthLoginPage = lazy(() => import('./pages/AuthLoginPage').then((module) => ({ default: module.AuthLoginPage })))
const AuthRegisterPage = lazy(() => import('./pages/AuthRegisterPage').then((module) => ({ default: module.AuthRegisterPage })))
const AuthVerifyEmailPage = lazy(() =>
  import('./pages/AuthVerifyEmailPage').then((module) => ({ default: module.AuthVerifyEmailPage })),
)
const PrivacyPolicyPage = lazy(() =>
  import('./pages/PrivacyPolicyPage').then((module) => ({ default: module.PrivacyPolicyPage })),
)
const SmsConsentPage = lazy(() => import('./pages/SmsConsentPage').then((module) => ({ default: module.SmsConsentPage })))

function PageSuspense({ children }: { children: ReactNode }) {
  return (
    <Suspense
      fallback={
        <section className="page-stack">
          <article className="panel">
            <LoadingPlaceholder
              title="Loading page"
              copy="Preparing the next view."
              lineCount={3}
            />
          </article>
        </section>
      }
    >
      {children}
    </Suspense>
  )
}

function RootRedirect() {
  const { token } = useSessionState()
  const location = useLocation()
  if (token) {
    return <Navigate to="/app/overview" replace />
  }
  const params = new URLSearchParams(location.search)
  const mode = params.get('recoveryMode')
  const authMode = params.get('authMode')
  if (mode === 'password') {
    return <Navigate to={`/auth/forgot-password${location.search}`} replace />
  }
  if (mode === 'username') {
    return <Navigate to={`/auth/forgot-username${location.search}`} replace />
  }
  if (authMode === 'magic-link') {
    return <Navigate to={`/auth/login${location.search}`} replace />
  }
  return <AuthLandingPage />
}

function AuthRoute() {
  const { token } = useSessionState()
  if (token) {
    return <Navigate to="/app/overview" replace />
  }
  return <Outlet />
}

function BillingRedirect({ status }: { status: 'success' | 'cancel' }) {
  const location = useLocation()
  const params = new URLSearchParams(location.search)
  params.set('billing', status)
  const suffix = params.toString()
  return <Navigate to={`/app/subscription${suffix ? `?${suffix}` : ''}`} replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<PageSuspense><RootRedirect /></PageSuspense>} />
      <Route path="/about" element={<PageSuspense><AboutPage /></PageSuspense>} />
      <Route path="/billing/success" element={<PageSuspense><BillingRedirect status="success" /></PageSuspense>} />
      <Route path="/billing/cancel" element={<PageSuspense><BillingRedirect status="cancel" /></PageSuspense>} />
      <Route path="/privacy-policy" element={<PageSuspense><PrivacyPolicyPage /></PageSuspense>} />
      <Route path="/sms-consent" element={<PageSuspense><SmsConsentPage /></PageSuspense>} />
      <Route path="/auth" element={<AuthRoute />}>
        <Route index element={<PageSuspense><AuthLandingPage /></PageSuspense>} />
        <Route path="login" element={<PageSuspense><AuthLoginPage /></PageSuspense>} />
        <Route path="register" element={<PageSuspense><AuthRegisterPage /></PageSuspense>} />
        <Route path="verify-email" element={<PageSuspense><AuthVerifyEmailPage /></PageSuspense>} />
        <Route path="forgot-password" element={<PageSuspense><AuthForgotPasswordPage /></PageSuspense>} />
        <Route path="forgot-username" element={<PageSuspense><AuthForgotUsernamePage /></PageSuspense>} />
      </Route>

      <Route
        path="/app"
        element={
          <RequireAuth>
            <AuthenticatedLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="overview" replace />} />
        <Route path="overview" element={<PageSuspense><OverviewPageRoute /></PageSuspense>} />
        <Route path="rules" element={<PageSuspense><RulesPageRoute /></PageSuspense>} />
        <Route path="travel" element={<PageSuspense><TravelPlansPageRoute /></PageSuspense>} />
        <Route path="subscription" element={<PageSuspense><SubscriptionPageRoute /></PageSuspense>} />
        <Route path="account" element={<PageSuspense><AccountPageRoute /></PageSuspense>} />
        <Route path="admin" element={<PageSuspense><AdminPageRoute /></PageSuspense>} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AppStateProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AppStateProvider>
  )
}
