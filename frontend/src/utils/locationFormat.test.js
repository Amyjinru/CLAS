import assert from 'node:assert/strict'
import test from 'node:test'
import { autoLocationFromAmapResult, resolveAutoLocationFromAmap } from './locationFormat.js'

test('derives a structured address directly from an AMap geolocation result', () => {
  const location = autoLocationFromAmapResult({
    formattedAddress: '北京市北京市海淀区中关村大街1号',
    addressComponent: {
      province: '北京市', city: '北京市', district: '海淀区', street: '中关村大街'
    },
    position: { lng: 116.31, lat: 39.98 }
  })

  assert.deepEqual(location, {
    province: '北京市', city: '北京市', district: '海淀区', street: '中关村大街1号',
    address: '北京市北京市海淀区中关村大街1号', longitude: 116.31, latitude: 39.98, source: 'auto'
  })
})

test('uses address components when formatted address is unavailable', () => {
  const location = autoLocationFromAmapResult({
    addressComponent: {
      province: '浙江省', city: '杭州市', district: '西湖区', township: '西湖街道',
      streetNumber: { street: '文三路', number: '90号' }
    },
    position: { lng: 120.1, lat: 30.2 }
  })

  assert.equal(location.street, '西湖街道文三路90号')
  assert.equal(location.address, '浙江省杭州市西湖区西湖街道文三路90号')
})

test('reverse geocodes incomplete GPS results and preserves direct results if reverse lookup fails', async () => {
  class Geocoder {
    getAddress([lng, lat], callback) {
      assert.deepEqual([lng, lat], [120.1, 30.2])
      callback('complete', {
        regeocode: {
          formattedAddress: '浙江省杭州市西湖区文三路90号',
          addressComponent: { province: '浙江省', city: '杭州市', district: '西湖区', street: '文三路' }
        }
      })
    }
  }

  const resolved = await resolveAutoLocationFromAmap({ Geocoder }, {
    formattedAddress: '当前位置', position: { lng: 120.1, lat: 30.2 }
  })
  assert.equal(resolved.address, '浙江省杭州市西湖区文三路90号')
  assert.equal(resolved.street, '文三路90号')

  const original = { formattedAddress: '当前位置', position: { lng: 120.1, lat: 30.2 } }
  const fallback = await resolveAutoLocationFromAmap({
    Geocoder: class { getAddress(_, callback) { callback('error', null) } }
  }, original)
  assert.equal(fallback.address, '当前位置')
  assert.equal(fallback.longitude, 120.1)
})
