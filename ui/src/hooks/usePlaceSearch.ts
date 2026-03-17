import { useCallback, useEffect, useRef, useState } from 'react'
import { searchPlaces, type GeocodePlace } from '../services/geocoding'

interface UsePlaceSearchOptions {
  debounceMs?: number
  enabled?: boolean
  minQueryLength?: number
}

interface UsePlaceSearchResult {
  results: GeocodePlace[]
  searching: boolean
  searchError: string | null
  clearResults: () => void
  skipNextSearchFor: (query: string) => void
}

export function usePlaceSearch(
  query: string,
  {
    debounceMs = 400,
    enabled = true,
    minQueryLength = 3,
  }: UsePlaceSearchOptions = {},
): UsePlaceSearchResult {
  const [results, setResults] = useState<GeocodePlace[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const skipQueryRef = useRef<string | null>(null)
  const clearResults = useCallback(() => {
    setResults([])
    setSearching(false)
    setSearchError(null)
  }, [])
  const skipNextSearchFor = useCallback((queryValue: string) => {
    skipQueryRef.current = queryValue.trim()
  }, [])

  useEffect(() => {
    if (!enabled) {
      clearResults()
      return
    }

    const trimmed = query.trim()
    if (!trimmed || trimmed.length < minQueryLength) {
      clearResults()
      return
    }

    if (skipQueryRef.current === trimmed) {
      skipQueryRef.current = null
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(async () => {
      setSearching(true)
      setSearchError(null)

      try {
        const places = await searchPlaces(trimmed)
        if (cancelled) {
          return
        }

        setResults(places)
        if (places.length === 0) {
          setSearchError('No matching place found yet.')
        }
      } catch {
        if (cancelled) {
          return
        }

        setResults([])
        setSearchError('Location search is unavailable right now.')
      } finally {
        if (!cancelled) {
          setSearching(false)
        }
      }
    }, debounceMs)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [clearResults, debounceMs, enabled, minQueryLength, query])

  return {
    results,
    searching,
    searchError,
    clearResults,
    skipNextSearchFor,
  }
}
