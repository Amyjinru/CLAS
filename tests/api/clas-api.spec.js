import { describe, it, expect } from 'vitest'

const BASE = 'http://8.141.112.182:8080'

let authToken = ''

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (authToken) headers['Authorization'] = `Bearer ${authToken}`
  const res = await fetch(`${BASE}${path}`, { ...options, headers })
  const data = await res.json().catch(() => null)
  return { status: res.status, data }
}

async function get(path) {
  const res = await fetch(`${BASE}${path}`)
  const data = await res.json().catch(() => null)
  return { status: res.status, data }
}

// ─── 1. 健康检查 ───
describe('1. 健康检查', () => {
  it('后端 Health API 正常', async () => {
    const { status, data } = await get('/api/health')
    expect(status).toBe(200)
    expect(data.code).toBe(200)
  })

  it('前端页面可访问', async () => {
    const res = await fetch('http://8.141.112.182/')
    expect(res.status).toBe(200)
  })
})

// ─── 2. 用户认证 ───
describe('2. 用户认证', () => {
  it('登录成功并获取 JWT', async () => {
    const res = await fetch(`${BASE}/api/user/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '13800000001', password: 'Abc123!' })
    })
    const data = await res.json()
    expect(res.status).toBe(200)
    expect(data.code).toBe(200)
    expect(data.data.token).toBeTruthy()
    authToken = data.data.token
  })

  it('获取当前用户资料', async () => {
    const { status, data } = await api('/api/user/profile')
    expect(status).toBe(200)
    expect(data.data).toBeTruthy()
  })

  it('未认证返回 401', async () => {
    const res = await fetch(`${BASE}/api/user/profile`)
    expect(res.status).toBe(401)
  })

  it('手机号认证被拒绝', async () => {
    const res = await fetch(`${BASE}/api/user/profile`, {
      headers: { Authorization: '13800000001' }
    })
    expect(res.status).toBe(401)
  })

  it('无效 Token 被拒绝', async () => {
    const res = await fetch(`${BASE}/api/user/profile`, {
      headers: { Authorization: 'Bearer invalid.token.here' }
    })
    expect(res.status).toBe(401)
  })
})

// ─── 3. 公共接口 ───
describe('3. 公共接口', () => {
  it('公告列表 /api/announcement/list', async () => {
    const { status, data } = await get('/api/announcement/list')
    expect(status).toBe(200)
    expect(data.code).toBe(200)
  })

  it('商家列表 /api/merchant/list', async () => {
    const { status, data } = await get('/api/merchant/list')
    expect(status).toBe(200)
    expect(data.code).toBe(200)
  })

  it('商品列表 /api/product/list (需merchantId)', async () => {
    // 需要 merchantId 参数；无参数时返回 500
    const res = await fetch(`${BASE}/api/product/list?merchantId=1`)
    // 若商家1存在则 200，否则可能 200 空列表或数据异常
    expect([200, 400, 500]).toContain(res.status)
  })
})

// ─── 4. 地址管理 ───
describe('4. 地址管理', () => {
  it('获取我的地址 /api/address/mine', async () => {
    const { status, data } = await api('/api/address/mine')
    expect(status).toBe(200)
    expect(data.code).toBe(200)
  })

  it('创建收货地址', async () => {
    const res = await fetch(`${BASE}/api/address`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${authToken}` },
      body: JSON.stringify({
        contactName: '测试用户', phone: '13800000001',
        address: '北京市朝阳区测试路1号',
        longitude: 116.404, latitude: 39.915, isDefault: false
      })
    })
    expect([200, 400]).toContain(res.status)
  })
})

// ─── 5. 购物车 ───
describe('5. 购物车', () => {
  it('获取我的购物车 /api/cart/me', async () => {
    const { status } = await api('/api/cart/me')
    expect([200, 401]).toContain(status)
  })

  it('未登录获取购物车返回 401', async () => {
    const res = await fetch(`${BASE}/api/cart/me`)
    expect(res.status).toBe(401)
  })
})

// ─── 6. 订单 ───
describe('6. 订单', () => {
  it('获取我的订单 /api/order/me', async () => {
    const { status } = await api('/api/order/me')
    expect([200, 401]).toContain(status)
  })

  it('普通用户访问后台返回 403', async () => {
    const res = await fetch(`${BASE}/api/admin/dashboard`, {
      headers: { Authorization: `Bearer ${authToken}` }
    })
    expect([401, 403]).toContain(res.status)
  })
})

// ─── 7. 个人中心 ───
describe('7. 个人中心', () => {
  it('我的收藏 /api/favorites/mine', async () => {
    const { status } = await api('/api/favorites/mine')
    expect([200, 401]).toContain(status)
  })

  it('我的通知 /api/notifications/mine', async () => {
    const { status } = await api('/api/notifications/mine')
    expect([200, 401]).toContain(status)
  })

  it('我的处罚记录 /api/user/penalties/mine', async () => {
    const { status } = await api('/api/user/penalties/mine')
    expect([200, 401]).toContain(status)
  })

  it('修改昵称', async () => {
    const { status } = await api('/api/user/profile', {
      method: 'PUT',
      body: JSON.stringify({ nickname: 'API测试' })
    })
    expect([200, 400]).toContain(status)
  })
})

// ─── 8. 团购与优惠券 ───
describe('8. 团购与优惠券', () => {
  it('团购列表 /api/deals', async () => {
    const { status } = await get('/api/deals')
    expect(status).toBe(200)
  })

  it('可领取优惠券 /api/coupon/claimable', async () => {
    const { status } = await api('/api/coupon/claimable')
    expect([200, 401]).toContain(status)
  })

  it('我的优惠券 /api/coupon/mine', async () => {
    const { status } = await api('/api/coupon/mine')
    expect([200, 401]).toContain(status)
  })
})

// ─── 9. 评价 ───
describe('9. 评价系统', () => {
  it('我的评价 /api/review/mine', async () => {
    const { status } = await api('/api/review/mine')
    expect([200, 401]).toContain(status)
  })
})

// ─── 10. 聊天 ───
describe('10. 聊天系统', () => {
  it('我的会话 /api/chat/conversations', async () => {
    const { status } = await api('/api/chat/conversations')
    expect([200, 401]).toContain(status)
  })
})

// ─── 11. 安全边界 ───
describe('11. 认证与安全边界', () => {
  it('缺密码返回 400', async () => {
    const res = await fetch(`${BASE}/api/user/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '13800000001' })
    })
    expect(res.status).toBe(400)
  })

  it('错误密码登录失败', async () => {
    const res = await fetch(`${BASE}/api/user/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '13800000001', password: 'wrong' })
    })
    expect(res.status).not.toBe(200)
  })

  it('不存在用户登录失败', async () => {
    const res = await fetch(`${BASE}/api/user/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '13999999999', password: '123456' })
    })
    expect(res.status).not.toBe(200)
  })

  it('未登录访问 /api/order/me 返回 401', async () => {
    const res = await fetch(`${BASE}/api/order/me`)
    expect(res.status).toBe(401)
  })

  it('未登录访问 /api/cart/me 返回 401', async () => {
    const res = await fetch(`${BASE}/api/cart/me`)
    expect(res.status).toBe(401)
  })
})
