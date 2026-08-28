import test from 'node:test'
import assert from 'node:assert/strict'

import { passwordChecks, passwordRuleMessage, passwordStrength } from './passwordRules.js'

function passwordOk(password) {
  return passwordChecks(password).every((item) => item.ok)
}

test('password rules match backend validator complexity', () => {
  assert.equal(passwordOk('Abc123!'), true)
  assert.equal(passwordOk('Abc12!'), true)
  assert.equal(passwordOk('Abc1!'), false)
  assert.equal(passwordOk('abc123!'), false)
  assert.equal(passwordOk('ABC123!'), false)
  assert.equal(passwordOk('Abcdef!'), false)
  assert.equal(passwordOk('Abc1234'), false)
  assert.equal(passwordOk('Abc 123!'), false)
})

test('password rule message describes the six character frontend rule', () => {
  assert.equal(passwordRuleMessage, '至少6位，包含大小写字母、数字和特殊符号，不能包含空白字符')
})

test('password strength uses six characters as the length threshold', () => {
  assert.equal(passwordStrength('Abc123!'), 4)
  assert.equal(passwordStrength('Abc12!'), 4)
  assert.equal(passwordStrength('Abc1!'), 3)
})
