import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const passwordEntryViews = [
  'LoginView.vue',
  'ForgotPasswordView.vue',
  'MerchantRegisterView.vue'
]

test('password entry views do not mention the old eight character rule', () => {
  for (const view of passwordEntryViews) {
    const source = readFileSync(new URL(`./${view}`, import.meta.url), 'utf8')
    assert.equal(source.includes('至少8位'), false, `${view} still says 至少8位`)
    assert.equal(source.includes('不少于8位'), false, `${view} still says 不少于8位`)
    assert.equal(source.includes('少于8位'), false, `${view} still says 少于8位`)
  }
})
