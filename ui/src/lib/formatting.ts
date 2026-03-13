export function formatNumber(value?: number | null): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '-'
  }
  return Number(value).toFixed(1).replace(/\.0$/, '')
}

export function formatDate(value?: string): string {
  if (!value) {
    return 'Unknown time'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString()
}

export function formatTemperature(celsius?: number, target: 'F' | 'C' = 'F'): string {
  if (celsius === undefined || celsius === null || Number.isNaN(celsius)) {
    return '--'
  }

  if (target === 'C') {
    return `${formatNumber(celsius)}°C`
  }

  return `${formatNumber((celsius * 9) / 5 + 32)}°F`
}

export function formatWind(kmh?: number): string {
  if (kmh === undefined || kmh === null || Number.isNaN(kmh)) {
    return '--'
  }
  return `${formatNumber(kmh)} km/h`
}

export function formatMillimeters(mm?: number): string {
  if (mm === undefined || mm === null || Number.isNaN(mm)) {
    return '--'
  }
  return `${formatNumber(mm)} mm`
}

export function formatPercent(value?: number): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--'
  }
  return `${formatNumber(value)}%`
}

export function formatPercentOrNA(value?: number): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return 'N/A'
  }
  return `${formatNumber(value)}%`
}

export function formatMetricLabel(label: string, value: string): string {
  return `${label}: ${value}`
}

export function formatRelativeTime(value?: string): string {
  if (!value) {
    return 'Unknown time'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const seconds = Math.round((date.getTime() - Date.now()) / 1000)
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' })
  const absSeconds = Math.abs(seconds)

  if (absSeconds < 60) {
    return rtf.format(seconds, 'second')
  }
  const minutes = Math.round(seconds / 60)
  if (Math.abs(minutes) < 60) {
    return rtf.format(minutes, 'minute')
  }
  const hours = Math.round(minutes / 60)
  if (Math.abs(hours) < 24) {
    return rtf.format(hours, 'hour')
  }
  const days = Math.round(hours / 24)
  return rtf.format(days, 'day')
}

export function formatRelativeTimeCompact(value?: string, now = Date.now()): string {
  if (!value) {
    return 'pending'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const seconds = Math.round((now - date.getTime()) / 1000)
  const absSeconds = Math.abs(seconds)

  if (absSeconds < 15) {
    return 'just now'
  }
  if (absSeconds < 60) {
    return `${absSeconds}s ago`
  }

  const minutes = Math.round(absSeconds / 60)
  if (minutes < 60) {
    return `${minutes}m ago`
  }

  const hours = Math.round(minutes / 60)
  if (hours < 24) {
    return `${hours}h ago`
  }

  const days = Math.round(hours / 24)
  return `${days}d ago`
}

export function formatFriendlyLocation(location?: string): string {
  if (!location || location.trim() === '') {
    return 'Selected area'
  }

  const normalized = location.toLowerCase()
  if (normalized.includes('orlando')) {
    return 'Orlando, FL'
  }
  return location
}

export function formatStatusLabel(status?: string): string {
  if (!status) {
    return 'Unknown'
  }
  const lower = status.toLowerCase()
  return `${lower.charAt(0).toUpperCase()}${lower.slice(1)}`
}

const COMPASS_DIRECTIONS = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']

/**
 * Converts a wind direction in degrees (0–360) to a 16-point compass abbreviation.
 * 0° and 360° map to 'N'; each 22.5° sector maps to the next direction clockwise.
 */
export function degreesToCompass(degrees: number): string {
  const normalized = ((degrees % 360) + 360) % 360
  const index = Math.round(normalized / 22.5) % 16
  return COMPASS_DIRECTIONS[index]
}
