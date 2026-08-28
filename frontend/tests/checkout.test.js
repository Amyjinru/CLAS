import test from 'node:test'
import assert from 'node:assert/strict'
import { isCheckoutReady, preferredProductIds } from '../src/utils/checkout.js'

test('preselects valid products from the merchant that opened checkout', () => {
  const items = [
    { productId: 1, merchantId: 10, valid: true },
    { productId: 2, merchantId: 10, valid: false },
    { productId: 3, merchantId: 20, valid: true }
  ]
  assert.deepEqual(preferredProductIds(items, '10'), [1])
  assert.deepEqual(preferredProductIds(items, undefined), [])
})

test('checkout readiness accepts complete manually entered delivery information', () => {
  const groups = [{ merchantId: 10 }]
  assert.equal(isCheckoutReady({
    groups,
    submitting: false,
    loadingIds: new Set(),
    previews: { 10: { canCheckout: true } },
    errors: {},
    deliveryAddress: '软件园 A 座 302',
    contactName: '张三',
    contactPhone: '13800000001',
    deliveryLongitude: 116.397,
    deliveryLatitude: 39.909
  }), true)
})

test('checkout remains blocked until address, contact and phone are complete', () => {
  const base = {
    groups: [{ merchantId: 10 }],
    submitting: false,
    loadingIds: new Set(),
    previews: { 10: { canCheckout: true } },
    errors: {},
    deliveryAddress: '软件园 A 座 302',
    contactName: '张三',
    contactPhone: '13800000001',
    deliveryLongitude: 116.397,
    deliveryLatitude: 39.909
  }
  assert.equal(isCheckoutReady({ ...base, deliveryAddress: '' }), false)
  assert.equal(isCheckoutReady({ ...base, contactName: '' }), false)
  assert.equal(isCheckoutReady({ ...base, contactPhone: '' }), false)
  assert.equal(isCheckoutReady({ ...base, deliveryLongitude: null }), false)
  assert.equal(isCheckoutReady({ ...base, deliveryLatitude: undefined }), false)
})

test('checkout remains blocked while preview is invalid or loading', () => {
  const groups = [{ merchantId: 10 }]
  const delivery = {
    deliveryAddress: '地址', contactName: '联系人', contactPhone: '13800000001',
    deliveryLongitude: 116.397, deliveryLatitude: 39.909
  }
  assert.equal(isCheckoutReady({ groups, submitting: false, loadingIds: new Set([10]), previews: {}, errors: {}, ...delivery }), false)
  assert.equal(isCheckoutReady({ groups, submitting: false, loadingIds: new Set(), previews: { 10: { canCheckout: false } }, errors: {}, ...delivery }), false)
})
