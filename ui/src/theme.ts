import { createContext, useContext } from 'react'

export type Theme = 'light' | 'dark'
export type ThemePreference = Theme | 'system'

export type ThemeContextValue = {
  theme: Theme
  themePreference: ThemePreference
  setThemePreference: (themePreference: ThemePreference) => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useThemePreference() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useThemePreference must be used within ThemeContext')
  }
  return context
}
