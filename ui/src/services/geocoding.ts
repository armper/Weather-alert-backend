export interface GeocodePlace {
  id: string
  name: string
  displayName: string
  latitude: number
  longitude: number
}

interface NominatimPlace {
  place_id: number
  display_name: string
  name?: string
  lat: string
  lon: string
  address?: {
    city?: string
    town?: string
    village?: string
    municipality?: string
    county?: string
    state?: string
    country?: string
  }
}

function toPlace(entry: NominatimPlace): GeocodePlace | null {
  const latitude = Number(entry.lat)
  const longitude = Number(entry.lon)
  if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
    return null
  }

  const cityName =
    entry.address?.city ??
    entry.address?.town ??
    entry.address?.village ??
    entry.address?.municipality ??
    entry.address?.county ??
    entry.name ??
    entry.display_name.split(',')[0]?.trim() ??
    'Selected location'

  return {
    id: String(entry.place_id),
    name: cityName,
    displayName: entry.display_name,
    latitude,
    longitude,
  }
}

export async function searchPlaces(query: string): Promise<GeocodePlace[]> {
  const trimmed = query.trim()
  if (!trimmed) {
    return []
  }

  const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit=5&q=${encodeURIComponent(trimmed)}`
  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Location search failed')
  }

  const data = (await response.json()) as NominatimPlace[]
  return data
    .map((entry) => toPlace(entry))
    .filter((entry): entry is GeocodePlace => entry !== null)
}

export async function reverseGeocode(latitude: number, longitude: number): Promise<GeocodePlace | null> {
  if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
    return null
  }

  const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&addressdetails=1&lat=${encodeURIComponent(
    String(latitude),
  )}&lon=${encodeURIComponent(String(longitude))}`

  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    return null
  }

  const data = (await response.json()) as NominatimPlace
  return toPlace(data)
}
