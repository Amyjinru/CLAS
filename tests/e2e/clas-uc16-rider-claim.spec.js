import { test, expect } from '@playwright/test'

const baseUrl = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const apiUrl = process.env.PLAYWRIGHT_API_BASE_URL || `${new URL(baseUrl).origin.replace(':5173', ':8080')}/api`
const riderA = process.env.UC16_RIDER_A_PHONE
const riderB = process.env.UC16_RIDER_B_PHONE
const riderPassword = process.env.UC16_RIDER_PASSWORD
const orderId = process.env.UC16_ORDER_ID

async function login(phone) {
  const response = await fetch(`${apiUrl}/user/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone, password: riderPassword, deviceId: `uc16-e2e-${phone}` })
  })
  const payload = await response.json()
  expect(payload.code).toBe(200)
  return { ...payload.data.user, roles: payload.data.roles, token: payload.data.token }
}

async function openWorkbench(page, session) {
  await page.addInitScript(value => localStorage.setItem('clas_user', JSON.stringify(value)), session)
  await page.goto(`${baseUrl}/rider`, { waitUntil: 'networkidle' })
  await expect(page.getByRole('heading', { name: /待命中|正在接单/ })).toBeVisible()
}

test.describe('UC16 骑手配送协同', () => {
  test.skip(!riderA || !riderB || !riderPassword || !orderId, '需要提供隔离的 UC16 骑手与订单测试数据')

  test('两名骑手在工作台竞争同一配送任务时仅一人领取成功', async ({ browser }) => {
    const [sessionA, sessionB] = await Promise.all([login(riderA), login(riderB)])
    const [contextA, contextB] = await Promise.all([
      browser.newContext({ geolocation: { longitude: 116.397428, latitude: 39.909230 }, permissions: ['geolocation'] }),
      browser.newContext({ geolocation: { longitude: 116.397428, latitude: 39.909230 }, permissions: ['geolocation'] })
    ])
    const [pageA, pageB] = await Promise.all([contextA.newPage(), contextB.newPage()])

    try {
      await Promise.all([openWorkbench(pageA, sessionA), openWorkbench(pageB, sessionB)])
      const taskA = pageA.getByText(`#${orderId}`).last()
      const taskB = pageB.getByText(`#${orderId}`).last()
      await Promise.all([expect(taskA).toBeVisible(), expect(taskB).toBeVisible()])

      await Promise.all([
        pageA.getByRole('button', { name: '接单配送' }).click(),
        pageB.getByRole('button', { name: '接单配送' }).click()
      ])

      await expect.poll(async () => {
        const states = await Promise.all([
          pageA.getByText('待取餐').count(),
          pageB.getByText('待取餐').count()
        ])
        return states[0] + states[1]
      }).toBe(1)
    } finally {
      await Promise.all([contextA.close(), contextB.close()])
    }
  })
})
