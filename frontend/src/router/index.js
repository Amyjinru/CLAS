import { createRouter, createWebHistory } from 'vue-router'
import { currentRole, currentUser } from '../api/clas'
import { ElMessage } from 'element-plus'

const LoginView = () => import('../views/LoginView.vue')
const ForgotPasswordView = () => import('../views/ForgotPasswordView.vue')
const HomeView = () => import('../views/HomeView.vue')
const MerchantDetailView = () => import('../views/MerchantDetailView.vue')
const CartView = () => import('../views/CartView.vue')
const OrdersView = () => import('../views/OrdersView.vue')
const DealsView = () => import('../views/DealsView.vue')
const BookingsView = () => import('../views/BookingsView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const PaymentView = () => import('../views/PaymentView.vue')
const ReviewView = () => import('../views/ReviewView.vue')
const MerchantRegisterView = () => import('../views/MerchantRegisterView.vue')
const MerchantAuditStatusView = () => import('../views/MerchantAuditStatusView.vue')
const MerchantConsoleView = () => import('../views/MerchantConsoleView.vue')
const MerchantAnalyticsView = () => import('../views/MerchantAnalyticsView.vue')
const MerchantProductsView = () => import('../views/MerchantProductsView.vue')
const MerchantDealsView = () => import('../views/MerchantDealsView.vue')
const MerchantBookingsView = () => import('../views/MerchantBookingsView.vue')
const UserAnnouncementsView = () => import('../views/user/UserAnnouncementsView.vue')
const NotificationsView = () => import('../views/user/NotificationsView.vue')
const MerchantAnnouncementsView = () => import('../views/merchant/MerchantAnnouncementsView.vue')
const AdminLayout = () => import('../views/admin/AdminLayout.vue')
const AdminAuditView = () => import('../views/AdminAuditView.vue')
const AdminAnnouncementsView = () => import('../views/admin/AdminAnnouncementsView.vue')
const AdminDashboardView = () => import('../views/admin/AdminDashboardView.vue')
const AdminUsersView = () => import('../views/admin/AdminUsersView.vue')
const AdminOrdersView = () => import('../views/admin/AdminOrdersView.vue')
const AdminReviewsView = () => import('../views/admin/AdminReviewsView.vue')
const AdminAppealsView = () => import('../views/admin/AdminAppealsView.vue')
const AdminMessagesView = () => import('../views/admin/AdminMessagesView.vue')

function defaultPath() {
  const user = currentUser()
  if (!user) {
    // 未登录 → 跳转到落地页（纯 HTML，零 JS 开销）
    window.location.replace('/landing.html')
    return '/login' // 兜底：如果 replace 失败则到登录页
  }
  const role = currentRole()
  if (role === 'MERCHANT') return '/merchant-console'
  if (role === 'ADMIN') return '/admin/dashboard'
  if (role === 'USER') return '/home'
  return '/login'
}

const routes = [
  { path: '/', redirect: () => defaultPath() },

  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/forgot-password', component: ForgotPasswordView, meta: { public: true } },
  { path: '/home', component: HomeView, meta: { roles: ['USER'], userPortal: true } },
  { path: '/merchant/:id', component: MerchantDetailView, meta: { roles: ['USER'], userPortal: true } },

  { path: '/cart', component: CartView, meta: { roles: ['USER'] } },
  { path: '/deals', component: DealsView, meta: { roles: ['USER'] } },
  { path: '/bookings', component: BookingsView, meta: { roles: ['USER'] } },
  { path: '/profile', component: ProfileView, meta: { roles: ['USER'] } },
  { path: '/profile/notifications', component: NotificationsView, meta: { roles: ['USER'] } },

  { path: '/orders', component: OrdersView, meta: { roles: ['USER'] } },

  { path: '/payment/deal/:orderId', name: 'DealPayment', component: PaymentView, meta: { roles: ['USER'] } },
  { path: '/payment/:orderId', name: 'OrderPayment', component: PaymentView, meta: { roles: ['USER'] } },
  { path: '/review/:orderId', component: ReviewView, meta: { roles: ['USER'] } },

  { path: '/merchant-register', component: MerchantRegisterView },

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
      { path: 'appeals', component: AdminAppealsView },
      { path: 'announcements', component: AdminAnnouncementsView },
      { path: 'messages', component: AdminMessagesView }
    ]
  },

  { path: '/merchant/products', component: MerchantProductsView, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/analytics', component: MerchantAnalyticsView, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/audit-status', component: MerchantAuditStatusView, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/deals', component: MerchantDealsView, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/bookings', component: MerchantBookingsView, meta: { roles: ['MERCHANT'] } },

  { path: '/merchant-console', component: MerchantConsoleView, meta: { roles: ['MERCHANT'] } },

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
