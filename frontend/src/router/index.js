import { createRouter, createWebHistory } from 'vue-router'
import { currentRole, currentUser } from '../api/clas'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import MerchantDetailView from '../views/MerchantDetailView.vue'
import CartView from '../views/CartView.vue'
import OrdersView from '../views/OrdersView.vue'
import MerchantConsoleView from '../views/MerchantConsoleView.vue'
// ===== test1: 商户入驻/审核/商品管理视图 =====
import MerchantRegisterView from '../views/MerchantRegisterView.vue'
import AdminAuditView from '../views/AdminAuditView.vue'
import MerchantProductsView from '../views/MerchantProductsView.vue'

// ===== version_314: 支付/评价/公告视图 =====
import PaymentView from '../views/PaymentView.vue'
import ReviewView from '../views/ReviewView.vue'
import UserAnnouncementsView from '../views/user/UserAnnouncementsView.vue'
import MerchantAnnouncementsView from '../views/merchant/MerchantAnnouncementsView.vue'
import AdminAnnouncementsView from '../views/admin/AdminAnnouncementsView.vue'

// ===== 同学E: 管理后台新页面 =====
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminUsersView from '../views/admin/AdminUsersView.vue'
import AdminOrdersView from '../views/admin/AdminOrdersView.vue'
import AdminReviewsView from '../views/admin/AdminReviewsView.vue'

// ===== test1: Element Plus 提示 =====
import { ElMessage } from 'element-plus'

// ===== version_314: 按角色动态首页跳转 =====
function defaultPath() {
  const role = currentRole()
  if (role === 'MERCHANT') return '/merchant-console'
  if (role === 'ADMIN') return '/admin/dashboard'
  if (role === 'USER') return '/home'
  return '/login'
}

const routes = [
  // 动态首页重定向（version_314）
  { path: '/', redirect: () => defaultPath() },

  // 公共路由
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/home', component: HomeView, meta: { roles: ['USER'], userPortal: true } },
  { path: '/merchant/:id', component: MerchantDetailView, meta: { roles: ['USER'], userPortal: true } },

  // test1: 购物车
  { path: '/cart', component: CartView, meta: { requiresAuth: true } },

  // 订单（两边共有，保留 test1 的 requiresAuth 语义）
  { path: '/orders', component: OrdersView, meta: { requiresAuth: true, roles: ['USER'] } },

  // version_314: 支付 & 评价
  { path: '/payment/:orderId', component: PaymentView, meta: { roles: ['USER'] } },
  { path: '/review/:orderId', component: ReviewView, meta: { roles: ['USER'] } },

  // test1: 商户入驻
  { path: '/merchant-register', component: MerchantRegisterView },

  // ===== 同学E: 管理后台（已整合到 AdminLayout）=====
  { path: '/admin-audit', redirect: '/admin/audit' },
  { path: '/admin/announcements-old', redirect: '/admin/announcements' },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: AdminDashboardView },
      { path: 'orders', component: AdminOrdersView },
      { path: 'users', component: AdminUsersView },
      { path: 'audit', component: AdminAuditView },
      { path: 'reviews', component: AdminReviewsView },
      { path: 'announcements', component: AdminAnnouncementsView }
    ]
  },

  // test1: 商户商品管理
  { path: '/merchant/products', component: MerchantProductsView, meta: { requiresAuth: true } },

  // 商户工作台（两边共有）
  { path: '/merchant-console', component: MerchantConsoleView, meta: { requiresAuth: true, roles: ['MERCHANT'] } },

  // version_314: 全平台公告
  { path: '/user/announcements', component: UserAnnouncementsView, meta: { roles: ['USER'] } },
  { path: '/merchant/announcements', component: MerchantAnnouncementsView, meta: { roles: ['MERCHANT'] } },
  { path: '/announcements', redirect: () => defaultPath() }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // version_314: 使用 API 获取用户信息（替代直接解析 localStorage）
  const user = currentUser()
  const role = currentRole()

  // 角色权限校验
  if (to.meta.roles && (!role || !to.meta.roles.includes(role))) {
    if (!user) {
      // version_314: 登录后回跳 + test1: ElMessage 提示
      ElMessage.warning('请先登录账号')
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    // test1: 权限不足 ElMessage 提示 + version_314: 跳回默认页
    ElMessage.error('权限不足，无法访问该页面')
    next(defaultPath())
    return
  }

  // version_314: 商户访问用户门户时跳回工作台
  if (to.meta.userPortal && role === 'MERCHANT') {
    next('/merchant-console')
    return
  }

  // test1: 通用登录校验（requiresAuth）
  if (to.meta.requiresAuth && !user) {
    ElMessage.warning('请先登录账号')
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
