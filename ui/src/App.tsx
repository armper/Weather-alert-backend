import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { AuthenticatedLayout } from './components/layout/AuthenticatedLayout'
import { RequireAuth } from './components/layout/RequireAuth'
import { AdminPage } from './pages/AdminPage'
import { AccountPage } from './pages/AccountPage'
import { AuthForgotPasswordPage } from './pages/AuthForgotPasswordPage'
import { AuthForgotUsernamePage } from './pages/AuthForgotUsernamePage'
import { AuthLandingPage } from './pages/AuthLandingPage'
import { AuthLoginPage } from './pages/AuthLoginPage'
import { PrivacyPolicyPage } from './pages/PrivacyPolicyPage'
import { AuthRegisterPage } from './pages/AuthRegisterPage'
import { AuthVerifyEmailPage } from './pages/AuthVerifyEmailPage'
import { EventsPage } from './pages/EventsPage'
import { ManageAlertsPage } from './pages/ManageAlertsPage'
import { OverviewPage } from './pages/OverviewPage'
import { RulesPage } from './pages/RulesPage'
import { SmsConsentPage } from './pages/SmsConsentPage'
import { AppStateProvider } from './state/AppStateContext'
import { useAppState } from './state/useAppState'

function RootRedirect() {
  const { token } = useAppState()
  const location = useLocation()
  if (token) {
    return <Navigate to="/app/overview" replace />
  }
  const params = new URLSearchParams(location.search)
  const mode = params.get('recoveryMode')
  if (mode === 'password') {
    return <Navigate to={`/auth/forgot-password${location.search}`} replace />
  }
  if (mode === 'username') {
    return <Navigate to={`/auth/forgot-username${location.search}`} replace />
  }
  return <AuthLandingPage />
}

function AuthRoute() {
  const { token } = useAppState()
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
  return <Navigate to={`/app/account${suffix ? `?${suffix}` : ''}`} replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/billing/success" element={<BillingRedirect status="success" />} />
      <Route path="/billing/cancel" element={<BillingRedirect status="cancel" />} />
      <Route path="/privacy-policy" element={<PrivacyPolicyPage />} />
      <Route path="/sms-consent" element={<SmsConsentPage />} />
      <Route path="/auth" element={<AuthRoute />}>
        <Route index element={<AuthLandingPage />} />
        <Route path="login" element={<AuthLoginPage />} />
        <Route path="register" element={<AuthRegisterPage />} />
        <Route path="verify-email" element={<AuthVerifyEmailPage />} />
        <Route path="forgot-password" element={<AuthForgotPasswordPage />} />
        <Route path="forgot-username" element={<AuthForgotUsernamePage />} />
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
        <Route path="overview" element={<OverviewPage />} />
        <Route path="rules" element={<RulesPage />} />
        <Route path="alerts" element={<ManageAlertsPage />} />
        <Route path="events" element={<EventsPage />} />
        <Route path="account" element={<AccountPage />} />
        <Route path="admin" element={<AdminPage />} />
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
