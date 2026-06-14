const LOCATION_KEY = 'clas_current_location'
const LOCATION_CHANGED_EVENT = 'clas_current_location_changed'

function hasCoordinate(location) {
  return Boolean(location?.longitude && location?.latitude)
}

function normalizeLocation(location) {
  if (!hasCoordinate(location)) {
    return null
  }
  return {
    province: location.province || '',
    city: location.city || '',
    district: location.district || '',
    street: location.street || '',
    address: location.address || location.street || '',
    longitude: Number(location.longitude),
    latitude: Number(location.latitude),
    source: location.source || 'manual'
  }
}

export function getCurrentLocation() {
  try {
    return normalizeLocation(JSON.parse(localStorage.getItem(LOCATION_KEY) || 'null'))
  } catch {
    return null
  }
}

export function setCurrentLocation(location) {
  const next = normalizeLocation(location)
  if (!next) {
    return null
  }
  localStorage.setItem(LOCATION_KEY, JSON.stringify(next))
  window.dispatchEvent(new CustomEvent(LOCATION_CHANGED_EVENT, { detail: next }))
  return next
}

export function clearCurrentLocation() {
  localStorage.removeItem(LOCATION_KEY)
  window.dispatchEvent(new CustomEvent(LOCATION_CHANGED_EVENT, { detail: null }))
}

export function locationFromAddress(address) {
  return normalizeLocation({
    province: '',
    city: '',
    district: '',
    street: address?.address || '',
    address: address?.address || '',
    longitude: address?.longitude,
    latitude: address?.latitude,
    source: 'manual'
  })
}

export function subscribeCurrentLocation(callback) {
  const onLocationChanged = (event) => {
    callback(event.detail || getCurrentLocation())
  }
  const onStorage = (event) => {
    if (event.key === LOCATION_KEY) {
      callback(getCurrentLocation())
    }
  }
  window.addEventListener(LOCATION_CHANGED_EVENT, onLocationChanged)
  window.addEventListener('storage', onStorage)
  return () => {
    window.removeEventListener(LOCATION_CHANGED_EVENT, onLocationChanged)
    window.removeEventListener('storage', onStorage)
  }
}
