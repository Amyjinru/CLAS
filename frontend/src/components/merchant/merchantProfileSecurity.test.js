import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildMerchantProfilePayload,
  isBankAccountReadyForSave,
  isVerificationReady,
  shouldResetVerification
} from './merchantProfileSecurity.js'

const baseForm = {
  merchantName: '  新店铺  ',
  phone: ' 13800138001 ',
  bankAccount: ' 6222020202020202020 ',
  address: ' 上海市黄浦区 ',
  longitude: 121.47,
  latitude: 31.23,
  deliveryRadiusM: 3500,
  businessHours: ' 09:00-21:00 ',
  phoneCode: '123456',
  bankCode: '654321'
}

test('merchant profile payload includes verification codes only for changed sensitive fields', () => {
  assert.deepEqual(buildMerchantProfilePayload(baseForm, { phoneChanged: false, bankChanged: false }), {
    merchantName: '新店铺',
    phone: '13800138001',
    bankAccount: '6222020202020202020',
    address: '上海市黄浦区',
    longitude: 121.47,
    latitude: 31.23,
    deliveryRadiusM: 3500,
    businessHours: '09:00-21:00'
  })

  assert.equal(buildMerchantProfilePayload(baseForm, { phoneChanged: true, bankChanged: false }).phoneCode, '123456')
  assert.equal(buildMerchantProfilePayload(baseForm, { phoneChanged: false, bankChanged: true }).bankCode, '654321')
})

test('verification is ready only when code was sent for the submitted value', () => {
  assert.equal(isVerificationReady({
    changed: false,
    code: '',
    sent: false,
    sentValue: '',
    currentValue: '13800138001'
  }), true)

  assert.equal(isVerificationReady({
    changed: true,
    code: '123456',
    sent: true,
    sentValue: '13800138001',
    currentValue: ' 13800138001 '
  }), true)

  assert.equal(isVerificationReady({
    changed: true,
    code: '123456',
    sent: true,
    sentValue: '13800138001',
    currentValue: '13900139001'
  }), false)
})

test('verification state resets after submitted field differs from the sent value', () => {
  assert.equal(shouldResetVerification(' 13800138001 ', '13800138001'), false)
  assert.equal(shouldResetVerification('13900139001', '13800138001'), true)
})

test('bank account is required only when the bank field changes', () => {
  assert.equal(isBankAccountReadyForSave('', false), true)
  assert.equal(isBankAccountReadyForSave('', true), false)
  assert.equal(isBankAccountReadyForSave('6222020202020202020', true), true)
})
