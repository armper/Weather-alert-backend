import { useContext } from 'react'
import { AppStateContextValue } from './AppStateContextValue'

export function useAppState() {
  const context = useContext(AppStateContextValue)
  if (!context) {
    throw new Error('useAppState must be used within AppStateProvider')
  }
  return context
}
