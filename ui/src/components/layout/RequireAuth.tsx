import type { ReactElement } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useSessionState } from '../../state/useAppState'

export function RequireAuth({ children }: { children: ReactElement }) {
  const { token } = useSessionState()
  const location = useLocation()

  if (!token) {
    return <Navigate to="/auth" replace state={{ from: location }} />
  }

  return children
}
