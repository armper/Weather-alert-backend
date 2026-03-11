import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'

const THEME_STORAGE_KEY = 'weather-alert-theme'

export type Theme = 'light' | 'dark'
export type ThemePreference = Theme | 'system'

export type ThemeContextValue = {
  theme: Theme
  themePreference: ThemePreference
  setThemePreference: (themePreference: ThemePreference) => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

function resolveTheme(themePreference: ThemePreference): Theme {
  if (themePreference !== 'system') {
    return themePreference
  }

  if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }

  return 'light'
}

export function ThemeProvider({ children }: PropsWithChildren) {
  const [themePreference, setThemePreference] = useState<ThemePreference>(() => {
    if (typeof window === 'undefined') {
      return 'system'
    }

    const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
    if (storedTheme === 'light' || storedTheme === 'dark') {
      return storedTheme
    }

    return 'system'
  })
  const [theme, setTheme] = useState<Theme>(() => resolveTheme(themePreference))

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
    setTheme(resolveTheme(themePreference))

    if (themePreference !== 'system') {
      return undefined
    }

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    function handleChange(event: MediaQueryListEvent) {
      setTheme(event.matches ? 'dark' : 'light')
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
    [theme, themePreference],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useThemePreference() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useThemePreference must be used within ThemeContext')
  }
  return context
}
