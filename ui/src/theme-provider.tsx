import { useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { ThemeContext, type Theme, type ThemePreference } from './theme'

const THEME_STORAGE_KEY = 'weather-alert-theme'

function resolveSystemTheme(): Theme {
  if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }
  return 'light'
}

export function ThemeProvider({ children }: PropsWithChildren) {
  const [themePreference, setThemePreferenceState] = useState<ThemePreference>(() => {
    if (typeof window === 'undefined') {
      return 'system'
    }

    const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
    if (storedTheme === 'light' || storedTheme === 'dark') {
      return storedTheme
    }

    return 'system'
  })
  const [systemTheme, setSystemTheme] = useState<Theme>(() => resolveSystemTheme())

  const theme = themePreference === 'system' ? systemTheme : themePreference

  const setThemePreference = useCallback((nextPreference: ThemePreference) => {
    if (nextPreference === 'system') {
      setSystemTheme(resolveSystemTheme())
    }
    setThemePreferenceState(nextPreference)
  }, [])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
  }, [theme])

  useEffect(() => {
    if (themePreference === 'system') {
      window.localStorage.removeItem(THEME_STORAGE_KEY)
    } else {
      window.localStorage.setItem(THEME_STORAGE_KEY, themePreference)
    }
  }, [themePreference])

  useEffect(() => {
    if (themePreference !== 'system') {
      return undefined
    }

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    function handleChange(event: MediaQueryListEvent) {
      setSystemTheme(event.matches ? 'dark' : 'light')
    }

    mediaQuery.addEventListener('change', handleChange)
    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [themePreference])

  const value = useMemo(
    () => ({
      theme,
      themePreference,
      setThemePreference,
    }),
    [theme, themePreference, setThemePreference],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
