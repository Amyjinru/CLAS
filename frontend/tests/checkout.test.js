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

test('checkout readiness does not require a saved address', () => {
  const groups = [{ merchantId: 10 }]
  assert.equal(isCheckoutReady({
    groups,
    submitting: false,
    loadingIds: new Set(),
    previews: { 10: { canCheckout: true } },
    errors: {}
  }), true)
})

test('checkout remains blocked while preview is invalid or loading', () => {
  const groups = [{ merchantId: 10 }]
  assert.equal(isCheckoutReady({ groups, submitting: false, loadingIds: new Set([10]), previews: {}, errors: {} }), false)
  assert.equal(isCheckoutReady({ groups, submitting: false, loadingIds: new Set(), previews: { 10: { canCheckout: false } }, errors: {} }), false)
})
