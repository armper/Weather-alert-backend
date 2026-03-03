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
    return `${formatNumber(celsius)} C`
  }

  return `${formatNumber((celsius * 9) / 5 + 32)} F`
}

export function formatWind(kmh?: number): string {
  if (kmh === undefined || kmh === null || Number.isNaN(kmh)) {
    return '--'
  }
  return `${formatNumber(kmh)} km/h`
}

export function formatPercent(value?: number): string {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--'
  }
  return `${formatNumber(value)}%`
}
