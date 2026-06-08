const LOCATION_KEY = 'clas_current_location'

export function getCurrentLocation() {
  try {
    return JSON.parse(localStorage.getItem(LOCATION_KEY) || 'null')
  } catch {
    return null
  }
}

export function setCurrentLocation(location) {
  if (!location?.longitude || !location?.latitude) {
    return
  }
  localStorage.setItem(LOCATION_KEY, JSON.stringify({
    province: location.province || '',
    city: location.city || '',
    district: location.district || '',
    address: location.address || '',
    longitude: Number(location.longitude),
    latitude: Number(location.latitude),
    source: location.source || 'manual'
  }))
}

export function clearCurrentLocation() {
  localStorage.removeItem(LOCATION_KEY)
}
