import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('./MerchantRegisterView.vue', import.meta.url), 'utf8')

test('merchant registration password copy uses the shared six character rule', () => {
  assert.equal(source.includes('至少8位'), false)
  assert.equal(source.includes('不少于8位'), false)
  assert.equal(source.includes('少于8位'), false)
  assert.equal(source.includes('至少6位'), true)
})
