import assert from 'node:assert/strict'
import test from 'node:test'
import { bookingStatusMap, orderStatusMap, statusMeta } from './status.js'

test('maps known order and booking statuses to their intended presentation metadata', () => {
  assert.deepEqual(statusMeta('PAID'), { text: '待商家接单', type: 'primary' })
  assert.deepEqual(statusMeta('CONFIRMED', bookingStatusMap), { text: '已确认', type: 'success' })
  assert.equal(orderStatusMap.REFUNDED.type, 'danger')
})

test('keeps unknown and missing statuses safe for rendering', () => {
  assert.deepEqual(statusMeta('CUSTOM_STATUS'), { text: 'CUSTOM_STATUS', type: 'info' })
  assert.deepEqual(statusMeta(''), { text: '-', type: 'info' })
  assert.deepEqual(statusMeta(null), { text: '-', type: 'info' })
})
