import { createContext } from 'react'
import type { WeatherAppState } from './useWeatherAppState'

export const AppStateContextValue = createContext<WeatherAppState | null>(null)
