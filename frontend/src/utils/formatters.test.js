import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatCompactDateTime,
  formatDistance,
  formatFen,
  formatMoney,
  parseBusinessMinutes
} from './formatters.js'

test('formats monetary amounts from fen without losing the currency precision', () => {
  assert.equal(formatFen(12345), '123.45')
  assert.equal(formatFen(12345, { symbol: true }), '¥123.45')
  assert.equal(formatFen(125, { decimals: 0 }), '1')
  assert.equal(formatFen(null), '0.00')
  assert.equal(formatMoney(0), '0.00')
  assert.equal(formatMoney(undefined), '-')
})

test('formats compact date and delivery distance display values', () => {
  assert.equal(formatCompactDateTime('2026-08-31T09:30:00'), '2026-08-31 09:30:00')
  assert.equal(formatCompactDateTime(null), '')
  assert.equal(formatDistance(999), '999m')
  assert.equal(formatDistance(1000), '1.0km')
  assert.equal(formatDistance(null), '-')
})

test('parses normal and cross-midnight business-hour ranges and rejects malformed ranges', () => {
  assert.deepEqual(parseBusinessMinutes('09:30-21:05'), [570, 1265])
  assert.deepEqual(parseBusinessMinutes(' 22:00 - 02:30 '), [1320, 150])
  assert.deepEqual(parseBusinessMinutes('09:00-09:00'), [null, null])
  assert.deepEqual(parseBusinessMinutes('all day'), [null, null])
  assert.deepEqual(parseBusinessMinutes('09:00-'), [null, null])
  assert.deepEqual(parseBusinessMinutes('25:00-26:00'), [null, null])
  assert.deepEqual(parseBusinessMinutes('09:60-10:00'), [null, null])
})
