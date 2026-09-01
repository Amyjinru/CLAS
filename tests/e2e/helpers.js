// 共享 E2E 辅助：登录前置、固定设备号、AMap 模拟（本地全栈跑测用）
// 目标环境为本地前端 dev server（http://localhost:5173），代理 /api → 8080。

export const BASE = 'http://localhost:5173'

// 演示账号（seed 数据，密码统一 Abc123!）：
//  13800000001 = 纯 USER（登录不弹端口选择）
//  13800000002 = USER + MERCHANT
//  13800000003 = USER + ADMIN
export const ACCOUNTS = {
  user: { phone: '13800000001', password: 'Abc123!', portal: null, home: /\/home/ },
  merchant: { phone: '13800000002', password: 'Abc123!', portal: '商家端', home: /\/merchant-console/ },
  admin: { phone: '13800000003', password: 'Abc123!', portal: '管理端', home: /\/admin/ },
}

// 统一固定设备号：避免“异地登录触发验证码（409）”的干扰，
// 也避免登录后重定向到 /login 导致受保护路由用例失败。
export async function installDeviceId(page, id = 'e2e-fixed-device') {
  await page.addInitScript((deviceId) => {
    localStorage.setItem('clas_device_id', deviceId)
  }, id)
}

// 登录前置：填手机号/密码 → 提交 → 多角色账号点对应端口 → 等待落地页。
// 若后端已有其它设备的活跃会话，会先触发验证码（固定 123456），这里兼容处理。
export async function login(page, account) {
  await installDeviceId(page)
  await page.goto(`${BASE}/login`)
  await page.locator('#login-phone').fill(account.phone)
  await page.locator('#login-password').fill(account.password)
  await page.locator('.submit-btn').click()

  // 异地会话触发二次验证：发送验证码并填固定码 123456
  const codeInput = page.locator('#login-code')
  if (await codeInput.isVisible({ timeout: 2500 }).catch(() => false)) {
    await page.locator('button:has-text("获取验证码")').click()
    await codeInput.fill('123456')
    await page.locator('.submit-btn').click()
  }

  if (account.portal) {
    await page.locator('.portal-choice', { hasText: account.portal }).click()
  }
  await page.waitForURL(account.home, { timeout: 15000 })
}

// 模拟高德地图：注入 window.AMap，覆盖 DistrictSearch / Geocoder / Geolocation，
// 使 LocationSelector 的“自动定位/手动选择”无需真实 AMap 即可确定性完成。
export async function installAmapMock(page) {
  await page.addInitScript(() => {
    const position = { lng: 116.397428, lat: 39.90923 }
    const component = {
      province: '北京市',
      city: '北京市',
      district: '海淀区',
      township: '中关村街道',
      street: '中关村大街',
      streetNumber: { street: '中关村大街', number: '1号' },
    }
    const formattedAddress = '北京市北京市海淀区中关村大街1号'
    const PROVINCES = [{ name: '北京市', adcode: '110000' }]
    const CITIES = { 110000: [{ name: '北京市', adcode: '110100' }] }
    const DISTRICTS = { 110100: [{ name: '海淀区', adcode: '110108' }] }

    function DistrictSearch() {
      this.search = function (keyword, cb) {
        let list = []
        if (keyword === '中国') list = PROVINCES
        else if (CITIES[keyword]) list = CITIES[keyword]
        else if (DISTRICTS[keyword]) list = DISTRICTS[keyword]
        else list = [{ name: '海淀区', adcode: '110108' }]
        cb('complete', {
          districtList: [{ name: keyword, adcode: keyword, districtList: list }],
        })
      }
    }
    function Geocoder() {
      this.getLocation = function (address, cb) {
        cb('complete', { geocodes: [{ location: position }] })
      }
      this.getAddress = function (lnglat, cb) {
        cb('complete', { regeocode: { formattedAddress, addressComponent: component } })
      }
    }
    function Geolocation() {
      this.getCurrentPosition = function (cb) {
        cb('complete', { position, formattedAddress, addressComponent: component })
      }
    }
    window.AMap = { plugin: (plugins, cb) => cb && cb(), DistrictSearch, Geocoder, Geolocation }
  })
}

// 在 Element Plus 下拉中点击指定文本的可见选项（dropdown 经 teleport 到 body）
export async function selectOption(page, optionText) {
  await page.locator('.el-select-dropdown__item:visible', { hasText: optionText }).first().click()
}
