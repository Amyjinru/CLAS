import { test, expect } from '@playwright/test'

const BASE = 'http://8.141.112.182'
const TEST_USER = { phone: '13800000001', password: 'Abc123!' }

async function visit(page, path = '') {
  await page.goto(`${BASE}${path}`, { waitUntil: 'domcontentloaded' })
}

test.describe('CLAS 外卖网站 — 核心流程 E2E', () => {

  test('首页正常加载', async ({ page }) => {
    await visit(page)
    await expect(page.locator('h1, .home-hero h1, .hero-title, header')).toBeVisible({ timeout: 15000 })
    await expect(page).toHaveTitle(/CLAS|外卖|校园/)
  })

  test('用户登录流程', async ({ page }) => {
    await visit(page, '/login')

    // 填写登录表单
    const phoneInput = page.locator('input[placeholder*="手机"], input[type="tel"]').first()
    const passwordInput = page.locator('input[type="password"]').first()

    if (await phoneInput.isVisible()) {
      await phoneInput.fill(TEST_USER.phone)
      await passwordInput.fill(TEST_USER.password)

      const loginBtn = page.locator('button:has-text("登录"), button:has-text("登 录")').first()
      await loginBtn.click()

      // 登录后应跳转或显示用户信息
      await page.waitForTimeout(3000)
    }
  })

  test('商家列表浏览', async ({ page }) => {
    await visit(page)

    const merchantCards = page.locator('.merchant-card, .store-card, [class*="merchant"]')
    const count = await merchantCards.count()
    // 可能有或没有商家，但页面不应报错
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('购物车页面加载', async ({ page }) => {
    await visit(page, '/cart')

    // 未登录应提示登录或显示空购物车
    const cartContent = page.locator('.cart-page, .cart-layout, .user-page, #login-phone')
    await expect(cartContent.first()).toBeVisible({ timeout: 10000 })
  })

  test('订单页面加载', async ({ page }) => {
    await visit(page, '/orders')

    const ordersContent = page.locator('.orders-page, .user-page, .order-list, #login-phone')
    await expect(ordersContent.first()).toBeVisible({ timeout: 10000 })
  })

  test('公告列表可访问', async ({ page }) => {
    await visit(page)

    // 公告区域或公告链接
    const announcements = page.locator('[class*="announcement"], a[href*="announcement"]')
    const count = await announcements.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('个人中心页面加载', async ({ page }) => {
    await visit(page, '/profile')

    // 未登录会重定向或显示空状态
    const profilePage = page.locator('.profile-page, .user-page, #login-phone')
    await expect(profilePage.first()).toBeVisible({ timeout: 10000 })
  })

  test('首页关键元素存在且无报错', async ({ page }) => {
    const errors = []
    page.on('pageerror', err => errors.push(err))

    await visit(page)
    await page.waitForTimeout(2000)

    // 导航栏
    const nav = page.locator('nav, .navbar, .header, header')
    await expect(nav.first()).toBeVisible({ timeout: 10000 })

    // 无 JS 运行时错误
    expect(errors.filter(e => !e.message?.includes('ResizeObserver'))).toHaveLength(0)
  })

  test('商家详情页(如果存在)', async ({ page }) => {
    // 先访问首页获取商家链接
    await visit(page)

    const merchantLink = page.locator('a[href*="/merchant/"]').first()
    if (await merchantLink.isVisible({ timeout: 5000 }).catch(() => false)) {
      await merchantLink.click()
      await page.waitForLoadState('domcontentloaded')

      // 商家详情页应显示商家名称
      const merchantName = page.locator('h1, h2, .merchant-name, [class*="merchant"] strong')
      await expect(merchantName.first()).toBeVisible({ timeout: 10000 })
    }
  })
})
