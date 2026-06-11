import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const source = readFileSync(new URL('./ProfileMessageBlock.vue', import.meta.url), 'utf8')

describe('ProfileMessageBlock notification click behavior', () => {
  it('marks unread notifications as read before navigating', () => {
    const handler = source.match(/function handleNotificationClick\(item\) \{[\s\S]*?\n\}/)?.[0] || ''

    assert.match(handler, /if \(!item\.readFlag\) \{\s*emit\('read', item\.id\)\s*\}/)
    assert.ok(
      handler.indexOf("emit('read', item.id)") < handler.indexOf('router.push(target)'),
      'read event should be emitted before route navigation'
    )
  })
})
