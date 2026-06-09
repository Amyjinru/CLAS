function valueText(value) {
  if (Array.isArray(value)) return value[0] || ''
  if (value && typeof value === 'object') return value.name || value.street || value.number || ''
  return value || ''
}

function detailFromComponent(component) {
  const streetNumber = component?.streetNumber
  const streetNumberText = streetNumber && typeof streetNumber === 'object'
    ? [valueText(streetNumber.street), valueText(streetNumber.number)].filter(Boolean).join('')
    : valueText(streetNumber)
  return [
    valueText(component?.township),
    valueText(component?.street),
    streetNumberText,
    valueText(component?.neighborhood),
    valueText(component?.building)
  ].filter(Boolean).join('')
}

function hasStructuredAddress(location, fallback) {
  return Boolean(
    location.province
    && location.city
    && location.district
    && location.street
    && location.street !== fallback
  )
}

export function autoLocationFromAmapResult(result, fallback = '当前位置') {
  const component = result?.addressComponent || {}
  const province = valueText(component.province)
  const city = valueText(component.city) || province
  const district = valueText(component.district)
  const formattedAddress = result?.formattedAddress || fallback
  const prefix = `${province}${city}${district}`
  let street = ''

  if (formattedAddress.startsWith(prefix)) {
    street = formattedAddress.substring(prefix.length)
  }
  if (!street) {
    street = detailFromComponent(component)
  }
  if (!street && formattedAddress !== prefix) {
    street = formattedAddress
  }

  const address = `${province}${city}${district}${street}` || formattedAddress

  return {
    province,
    city,
    district,
    street,
    address,
    longitude: result?.position?.lng ?? null,
    latitude: result?.position?.lat ?? null,
    source: 'auto'
  }
}

function reverseGeocode(AMapRef, longitude, latitude) {
  if (!AMapRef?.Geocoder || longitude === null || longitude === undefined || latitude === null || latitude === undefined) {
    return Promise.resolve(null)
  }
  return new Promise((resolve) => {
    const geocoder = new AMapRef.Geocoder({ city: '全国' })
    geocoder.getAddress([longitude, latitude], (status, result) => {
      if (status !== 'complete' || !result?.regeocode) {
        resolve(null)
        return
      }
      resolve({
        addressComponent: result.regeocode.addressComponent || {},
        formattedAddress: result.regeocode.formattedAddress || '',
        position: {
          lng: longitude,
          lat: latitude
        }
      })
    })
  })
}

export async function resolveAutoLocationFromAmap(AMapRef, result, fallback = '当前位置') {
  const direct = autoLocationFromAmapResult(result, fallback)
  if (hasStructuredAddress(direct, fallback)) {
    return direct
  }
  const reversed = await reverseGeocode(AMapRef, direct.longitude, direct.latitude)
  if (!reversed) {
    return direct
  }
  const resolved = autoLocationFromAmapResult(reversed, fallback)
  return hasStructuredAddress(resolved, fallback) ? resolved : direct
}
