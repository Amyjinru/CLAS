// UC05–UC08 E2E 场景（本地全栈跑测：前端 5173 → 后端 8080 → MySQL/Redis）
// 覆盖：收藏商家/通知跳转、商家入驻审核、商品管理、团购券购买支付核销。
// 辅助逻辑见 ./helpers.js（登录前置 + 固定设备号 + AMap 模拟）。

import { test, expect } from '@playwright/test'
import { BASE, ACCOUNTS, login, installDeviceId, installAmapMock, selectOption } from './helpers.js'

const USER = ACCOUNTS.user
const MERCHANT = ACCOUNTS.merchant
const ADMIN = ACCOUNTS.admin

// 在 LocationSelector 中执行「切换自动定位 → 自动定位 → 确认使用自动定位」。
// 注意：父组件传入空 locationData（source=''）时 onMounted 会归一化到 manual，
// 所以需先点击 el-segmented 的「自动定位」项切回 auto 模式，auto-panel 才会渲染。
async function autoLocate(page) {
  await page.locator('.el-segmented__item', { hasText: '自动定位' }).click()
  await page.locator('.auto-panel').getByRole('button', { name: '自动定位' }).click()
  await page.getByRole('button', { name: '确认使用自动定位' }).click()
}

test.describe('UC05 用户收藏商家 / 业务通知', () => {
  test('E2E-UC05-01 收藏商家并在个人中心取消收藏', async ({ page }) => {
    await login(page, USER)
    await page.goto(`${BASE}/merchant/1`)

    const fav = page.locator('.favorite-button')
    await expect(fav).toBeVisible({ timeout: 15000 })

    // 确保处于「已收藏」状态（幂等：若未收藏则先收藏）
    if (!(await fav.evaluate((el) => el.classList.contains('active')))) {
      await fav.click()
      await expect(fav).toHaveText(/已收藏/)
    }

    // 个人中心 → 收藏 tab，取消收藏
    await page.goto(`${BASE}/profile`)
    await page.getByRole('tab', { name: '收藏' }).click()
    const row = page.locator('.list-row:not(.chat-conversation-row)', { hasText: '校园轻食铺' })
    await expect(row).toBeVisible()
    await row.getByRole('button', { name: '取消收藏' }).click()
    await expect(page.locator('.list-row:not(.chat-conversation-row)', { hasText: '校园轻食铺' })).toHaveCount(0)

    // 回到商家详情确认已取消
    await page.goto(`${BASE}/merchant/1`)
    await expect(page.locator('.favorite-button')).toHaveText(/收藏商家/)
  })

  test('E2E-UC05-02 点击业务通知跳转到目标页并标记已读', async ({ page }) => {
    await login(page, USER)
    await page.goto(`${BASE}/profile/notifications`)

    const head = page.locator('.page-head p')
    await expect(head).toBeVisible()
    const before = Number((await head.textContent()).match(/未读 (\d+) 条/)?.[1] ?? 0)
    expect(before).toBeGreaterThan(0)

    const row = page.locator('.notice-row.clickable').first()
    await expect(row).toBeVisible()
    await row.locator('strong').click()
    await page.waitForURL(/from=notifications/, { timeout: 15000 })

    await page.goto(`${BASE}/profile/notifications`)
    await expect(page.locator('.page-head p')).toBeVisible()
    const after = Number((await page.locator('.page-head p').textContent()).match(/未读 (\d+) 条/)?.[1] ?? 0)
    expect(after).toBe(before - 1)
  })
})

test.describe('UC06 商家入驻审核', () => {
  test('E2E-UC06-01 申请入驻 → 管理员审核 → 查看进度', async ({ browser }) => {
    const TS = Date.now().toString().slice(-8)
    const accountPhone = `139${TS}`
    const merchantName = `E2E测试店铺${TS}`
    // 入驻后该账号同时具备 USER + MERCHANT 身份，登录需选择「普通用户端」查看审核进度
    const applicant = { phone: accountPhone, password: 'Abc123!', portal: '普通用户端', home: /\/home/ }

    // 1) 访问者注册商家
    const visitorCtx = await browser.newContext()
    const page = await visitorCtx.newPage()
    await installAmapMock(page)
    await installDeviceId(page, 'e2e-register-device')
    await page.goto(`${BASE}/merchant-register`)

    // 步骤1：验证账号
    await page.getByPlaceholder('用于登录平台的手机号').fill(accountPhone)
    await page.getByRole('button', { name: '发送验证码' }).click()
    await page.getByPlaceholder('请输入6位短信验证码').fill('123456')
    await page.getByPlaceholder('新账号必填；已注册账号可不填').fill(`店长${TS.slice(-4)}`)
    await page.getByPlaceholder('已有账号请输入当前密码；新账号至少6位').fill('Abc123!')
    await page.getByPlaceholder('新账号请再次输入密码；已注册账号可不填').fill('Abc123!')
    await page.getByRole('button', { name: '下一步' }).click()

    // 步骤2：商家信息
    await page.getByPlaceholder('请输入店铺名称，如：校园轻食铺').fill(merchantName)
    await page.locator('.el-select').click()
    await selectOption(page, '美食')
    await page.getByPlaceholder('客户可见的联系电话').fill(accountPhone)
    await autoLocate(page)
    await page.getByRole('button', { name: '提交入驻申请' }).click()
    await page.waitForURL(/\/login/, { timeout: 20000 })

    // 2) 管理员审核：已审核 → 营业中
    const adminCtx = await browser.newContext()
    const adminPage = await adminCtx.newPage()
    await login(adminPage, ADMIN)
    await adminPage.goto(`${BASE}/admin/audit`)

    const search = adminPage.getByPlaceholder('搜索商家名称、电话、品类或地址')
    await search.fill(merchantName)

    let row = adminPage.locator('.el-table__row', { hasText: merchantName }).first()
    await row.getByRole('button', { name: '审核' }).click()

    let dialog = adminPage.locator('.el-dialog', { hasText: '更新商家审核状态' })
    await dialog.locator('.el-select').click()
    await selectOption(adminPage, '已审核')
    await dialog.getByPlaceholder('说明审核原因、补充材料或处理依据').fill('资质齐全，审核通过')
    await dialog.getByRole('button', { name: '保存审核结果' }).click()

    // 第二次审核 → 营业中
    await search.fill(merchantName)
    row = adminPage.locator('.el-table__row', { hasText: merchantName }).first()
    await row.getByRole('button', { name: '审核' }).click()

    dialog = adminPage.locator('.el-dialog', { hasText: '更新商家审核状态' })
    await dialog.locator('.el-select').click()
    await selectOption(adminPage, '营业中')
    await dialog.getByPlaceholder('说明审核原因、补充材料或处理依据').fill('已开通营业')
    await dialog.getByRole('button', { name: '保存审核结果' }).click()

    // 3) 申请人登录查看进度
    await login(page, applicant)
    await page.goto(`${BASE}/merchant/audit-status`)
    await expect(page.locator('.status-hero h1')).toHaveText('营业中', { timeout: 15000 })
    await expect(page.locator('.timeline-card').first()).toContainText('营业中')
    await expect(page.locator('.timeline-section')).toContainText('已审核')
  })
})

test.describe('UC07 商家店铺商品管理', () => {
  test('E2E-UC07-01 新增/下架/删除商品后用户侧可见性', async ({ browser }) => {
    const TS = Date.now().toString().slice(-6)
    const names = [`E2E商品A${TS}`, `E2E商品B${TS}`, `E2E商品C${TS}`]

    // 商家侧创建 3 件商品
    const merchantCtx = await browser.newContext()
    const mpage = await merchantCtx.newPage()
    await login(mpage, MERCHANT)
    await mpage.goto(`${BASE}/merchant/products`)

    for (const name of names) {
      await mpage.getByRole('button', { name: '新增商品' }).click()
      const dialog = mpage.locator('.el-dialog', { hasText: '新增商品' })
      await dialog.getByPlaceholder('请输入商品名称').fill(name)
      await dialog.getByPlaceholder('单位：元').fill('9.90')
      await dialog.getByPlaceholder('库存量').fill('10')
      await dialog.getByRole('button', { name: '确定' }).click()
      await expect(mpage.locator('.el-table__row', { hasText: name })).toBeVisible()
    }

    // 下架商品 B
    const rowB = mpage.locator('.el-table__row', { hasText: names[1] }).first()
    await rowB.locator('.el-switch').click()
    await expect(mpage.locator('.el-table__row', { hasText: names[1] }).first()).toContainText('下架中')

    // 删除商品 C
    const rowC = mpage.locator('.el-table__row', { hasText: names[2] }).first()
    await rowC.getByRole('button', { name: '删除' }).click()
    await mpage.locator('.el-popconfirm__action').getByRole('button', { name: '确定' }).click()
    await expect(mpage.locator('.el-table__row', { hasText: names[2] })).toHaveCount(0)

    // 用户侧查看商家详情：仅上架商品可见。
    // 新商品未设置分类 → 归入「未分类」，需先切换到该分类 tab 才能看到。
    const userCtx = await browser.newContext()
    const upage = await userCtx.newPage()
    await login(upage, USER)
    await upage.goto(`${BASE}/merchant/1`)
    await upage.getByRole('button', { name: '未分类' }).click()
    await expect(upage.locator('.product-card', { hasText: names[0] })).toBeVisible()
    await expect(upage.locator('.product-card', { hasText: names[1] })).toHaveCount(0)
    await expect(upage.locator('.product-card', { hasText: names[2] })).toHaveCount(0)
  })
})

test.describe('UC08 团购券购买支付核销', () => {
  test('E2E-UC08-01 普通券下单支付（领券 → 结算选券 → 下单 → 支付）', async ({ page }) => {
    await login(page, USER)

    // 加购
    await page.goto(`${BASE}/merchant/1`)
    const card = page.locator('.product-card', { hasText: '鸡胸肉能量碗' }).first()
    await card.locator('button', { hasText: '加入购物车' }).click()

    // 去购物车结算
    await page.goto(`${BASE}/cart`)
    await page.locator('.cart-item', { hasText: '鸡胸肉能量碗' }).first().locator('.el-checkbox').click()

    // 领取优惠券（新用户满减券：全场可用、valid_to 2026-09-08、满 ¥20 减 ¥3）
    const claimRow = page.locator('.claimable-coupons div', { hasText: '新用户满减券' }).first()
    await expect(claimRow).toBeVisible()
    await claimRow.getByRole('button', { name: '领取' }).click()
    await expect(page.locator('.claimable-coupons div', { hasText: '新用户满减券' })).toHaveCount(0)

    // 结算选券（等预览完成、券选项渲染后按 value 选中）
    const couponSelect = page.locator('.merchant-summary select').first()
    const couponOption = couponSelect.locator('option', { hasText: '新用户满减券' })
    await expect(couponOption).toHaveCount(1, { timeout: 15000 })
    await couponSelect.selectOption(await couponOption.getAttribute('value'))

    // 提交订单 → 合并付款
    await page.locator('.checkout-panel .submit-btn').click()
    await page.waitForURL(/\/payment\/batch/, { timeout: 20000 })

    // 支付
    await page.getByRole('button', { name: '确认支付' }).click()
    await page.getByRole('button', { name: '查看我的订单' }).click()
    await page.waitForURL(/\/orders/, { timeout: 15000 })
  })

  test('E2E-UC08-02 团购购买/支付/核销/查看状态', async ({ browser }) => {
    // 用户购买团购并支付
    const userCtx = await browser.newContext()
    const page = await userCtx.newPage()
    await login(page, USER)

    await page.goto(`${BASE}/deals/1`)
    await page.getByRole('button', { name: '购买团购券' }).click()
    await page.waitForURL(/\/payment\/deal\/(\d+)/, { timeout: 15000 })
    const orderId = page.url().match(/\/payment\/deal\/(\d+)/)[1]

    await page.getByRole('button', { name: '确认支付' }).click()
    await page.getByRole('button', { name: '查看我的团购券' }).click()
    await page.waitForURL(/\/profile/, { timeout: 15000 })

    // 读取券码（已支付 → CLAS- 前缀），并确认状态为待使用
    await page.goto(`${BASE}/deal-order/${orderId}`)
    const voucherCode = (await page.locator('.voucher-code').textContent()).trim()
    expect(voucherCode).toMatch(/CLAS-/)
    await expect(page.locator('.deal-order-panel .el-tag')).toHaveText('待使用')

    // 商家工作台核销
    const merchantCtx = await browser.newContext()
    const mpage = await merchantCtx.newPage()
    await login(mpage, MERCHANT)
    await mpage.getByPlaceholder('输入团购券码进行到店核销').fill(voucherCode)
    await mpage.getByRole('button', { name: '核销团购券' }).click()
    await expect(mpage.locator('.el-message').last()).toContainText('核销成功', { timeout: 10000 })

    // 用户侧：状态变为已使用，且收到核销通知
    await page.goto(`${BASE}/deal-order/${orderId}`)
    await expect(page.locator('.deal-order-panel .el-tag')).toHaveText('已使用')
    await page.goto(`${BASE}/profile/notifications`)
    await expect(page.locator('.notifications-page')).toContainText('团购券已核销')
  })
})
