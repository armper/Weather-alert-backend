import type { ReactNode } from 'react'
import { AppStateContextValue } from './AppStateContextValue'
import { useWeatherAppState } from './useWeatherAppState'

export function AppStateProvider({ children }: { children: ReactNode }) {
  const state = useWeatherAppState()
  return <AppStateContextValue.Provider value={state}>{children}</AppStateContextValue.Provider>
}
