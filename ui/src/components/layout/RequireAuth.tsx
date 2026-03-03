import type { ReactElement } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAppState } from '../../state/useAppState'

export function RequireAuth({ children }: { children: ReactElement }) {
  const { token } = useAppState()
  const location = useLocation()

  if (!token) {
    return <Navigate to="/auth" replace state={{ from: location }} />
  }

  return children
}
