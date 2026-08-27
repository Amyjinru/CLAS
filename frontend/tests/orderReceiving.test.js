import test from 'node:test'
import assert from 'node:assert/strict'
import { isReceivingOrder } from '../src/utils/orderReceiving.js'

test('includes paid and accepted orders before delivery', () => {
  assert.equal(isReceivingOrder({ status: 'PAID', deliveryStatus: 'WAITING', refundStatus: 'NONE' }), true)
  assert.equal(isReceivingOrder({ status: 'ACCEPTED', deliveryStatus: 'PREPARING', refundStatus: 'NONE' }), true)
  assert.equal(isReceivingOrder({ status: 'ACCEPTED', deliveryStatus: 'DELIVERING', refundStatus: 'NONE' }), true)
})

test('excludes delivered and refund-flow orders', () => {
  assert.equal(isReceivingOrder({ status: 'ACCEPTED', deliveryStatus: 'DELIVERED', refundStatus: 'NONE' }), false)
  assert.equal(isReceivingOrder({ status: 'PAID', deliveryStatus: 'WAITING', refundStatus: 'PENDING' }), false)
  assert.equal(isReceivingOrder({ status: 'REFUND_PENDING', deliveryStatus: 'WAITING', refundStatus: 'PENDING' }), false)
})
