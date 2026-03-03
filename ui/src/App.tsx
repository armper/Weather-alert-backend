import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import './App.css'
import { AuthenticatedLayout } from './components/layout/AuthenticatedLayout'
import { RequireAuth } from './components/layout/RequireAuth'
import { AdminPage } from './pages/AdminPage'
import { AccountPage } from './pages/AccountPage'
import { AuthForgotPasswordPage } from './pages/AuthForgotPasswordPage'
import { AuthForgotUsernamePage } from './pages/AuthForgotUsernamePage'
import { AuthLoginPage } from './pages/AuthLoginPage'
import { AuthRegisterPage } from './pages/AuthRegisterPage'
import { AuthVerifyEmailPage } from './pages/AuthVerifyEmailPage'
import { EventsPage } from './pages/EventsPage'
import { OverviewPage } from './pages/OverviewPage'
import { RulesPage } from './pages/RulesPage'
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
  return <Navigate to={`/auth/login${location.search}`} replace />
}

function AuthRoute() {
  const { token } = useAppState()
  if (token) {
    return <Navigate to="/app/overview" replace />
  }
  return <Outlet />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/auth" element={<AuthRoute />}>
        <Route index element={<Navigate to="login" replace />} />
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
