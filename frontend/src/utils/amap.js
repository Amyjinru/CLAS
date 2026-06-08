let amapPromise = null

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
    const plugins = [
      'AMap.PlaceSearch',
      'AMap.Geocoder',
      'AMap.ToolBar',
      'AMap.Scale',
      'AMap.Geolocation',
      'AMap.DistrictSearch',
      'AMap.Driving'
    ]
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${import.meta.env.VITE_AMAP_KEY}&plugin=${plugins.join(',')}`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('AMAP_LOAD_FAILED'))
    document.head.appendChild(script)
  })

  return amapPromise
}
