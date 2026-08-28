let amapPromise = null
const loadedPlugins = new Set()
const defaultPlugins = [
  'AMap.PlaceSearch',
  'AMap.Geocoder',
  'AMap.Geolocation'
]

export function hasAmapKey() {
  return Boolean(import.meta.env.VITE_AMAP_KEY)
}

export function loadAmap() {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (!hasAmapKey()) {
    return Promise.reject(new Error('AMAP_KEY_MISSING'))
  }
  if (amapPromise) {
    return amapPromise
  }

  const securityCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE
  if (securityCode) {
    window._AMapSecurityConfig = {
      securityJsCode: securityCode
    }
  }

  amapPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${import.meta.env.VITE_AMAP_KEY}&plugin=${defaultPlugins.join(',')}`
    script.async = true
    script.onload = () => {
      defaultPlugins.forEach((plugin) => loadedPlugins.add(plugin))
      resolve(window.AMap)
    }
    script.onerror = () => reject(new Error('AMAP_LOAD_FAILED'))
    document.head.appendChild(script)
  })

  return amapPromise
}

export async function ensureAmapPlugins(plugins = []) {
  const AMap = await loadAmap()
  const missing = plugins.filter((plugin) => !loadedPlugins.has(plugin))
  if (!missing.length) {
    return AMap
  }
  await new Promise((resolve) => {
    AMap.plugin(missing, () => {
      missing.forEach((plugin) => loadedPlugins.add(plugin))
      resolve()
    })
  })
  return AMap
}
