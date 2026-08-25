import { createRouter, createWebHistory } from 'vue-router'
import { currentRole, currentUser } from '../api/clas'
import { ElMessage } from 'element-plus'

const LoginView = () => import('../views/LoginView.vue')
const ForgotPasswordView = () => import('../views/ForgotPasswordView.vue')
const HomeView = () => import('../views/HomeView.vue')
const MerchantBrowseView = () => import('../views/MerchantBrowseView.vue')
const MerchantDetailView = () => import('../views/MerchantDetailView.vue')
const CartView = () => import('../views/CartView.vue')
const OrdersView = () => import('../views/OrdersView.vue')
const DealsView = () => import('../views/DealsView.vue')
const DealDetailView = () => import('../views/DealDetailView.vue')
const DealOrderDetailView = () => import('../views/DealOrderDetailView.vue')
const OrderDetailView = () => import('../views/OrderDetailView.vue')
const BookingsView = () => import('../views/BookingsView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const UserSettingsView = () => import('../views/UserSettingsView.vue')
const PaymentView = () => import('../views/PaymentView.vue')
const ReviewView = () => import('../views/ReviewView.vue')
const MerchantRegisterView = () => import('../views/MerchantRegisterView.vue')
const MerchantAuditStatusView = () => import('../views/MerchantAuditStatusView.vue')
const MerchantConsoleView = () => import('../views/MerchantConsoleView.vue')
const MerchantInfoView = () => import('../views/MerchantInfoView.vue')
const MerchantAnalyticsView = () => import('../views/MerchantAnalyticsView.vue')
const MerchantProductsView = () => import('../views/MerchantProductsView.vue')
const MerchantMessagesView = () => import('../views/MerchantMessagesView.vue')
const RiderWorkbenchView = () => import('../views/RiderWorkbenchView.vue')
const MerchantDealsView = () => import('../views/MerchantDealsView.vue')
const MerchantDealDetailView = () => import('../views/MerchantDealDetailView.vue')
const MerchantBookingsView = () => import('../views/MerchantBookingsView.vue')
const RoleApplicationView = () => import('../views/RoleApplicationView.vue')
const UserAnnouncementsView = () => import('../views/user/UserAnnouncementsView.vue')
const NotificationsView = () => import('../views/user/NotificationsView.vue')
const MerchantAnnouncementsView = () => import('../views/merchant/MerchantAnnouncementsView.vue')
const AdminLayout = () => import('../views/admin/AdminLayout.vue')
const AdminAuditView = () => import('../views/AdminAuditView.vue')
const AdminRoleApplicationsView = () => import('../views/admin/AdminRoleApplicationsView.vue')
const AdminAnnouncementsView = () => import('../views/admin/AdminAnnouncementsView.vue')
const AdminDashboardView = () => import('../views/admin/AdminDashboardView.vue')
const AdminUsersView = () => import('../views/admin/AdminUsersView.vue')
const AdminOrdersView = () => import('../views/admin/AdminOrdersView.vue')
const AdminReviewsView = () => import('../views/admin/AdminReviewsView.vue')
const AdminAppealsView = () => import('../views/admin/AdminAppealsView.vue')
const AdminMessagesView = () => import('../views/admin/AdminMessagesView.vue')
const AdminRidersView = () => import('../views/admin/AdminRidersView.vue')

function defaultPath() {
  const user = currentUser()
  if (!user) {
    // 未登录 → 跳转到落地页（纯 HTML，零 JS 开销）
    window.location.replace('/landing.html')
    return '/login' // 兜底：如果 replace 失败则到登录页
  }
  const role = currentRole()
  if (role === 'MERCHANT') return '/merchant-console'
  if (role === 'RIDER') return '/rider'
  if (role === 'ADMIN') return '/admin/dashboard'
  if (role === 'USER') return '/home'
  return '/login'
}

const routes = [
  { path: '/', redirect: () => defaultPath() },

  { path: '/login', component: LoginView, meta: { public: true, title: '登录', motion: 'page-none' } },
  { path: '/forgot-password', component: ForgotPasswordView, meta: { public: true, title: '忘记密码', motion: 'page-pop' } },
  { path: '/home', component: HomeView, meta: { roles: ['USER'], userPortal: true, title: '浏览商家', motion: 'page-market' } },
  { path: '/merchants', component: MerchantBrowseView, meta: { roles: ['USER'], userPortal: true, title: '查看店铺', motion: 'page-market' } },
  { path: '/merchant/:id', component: MerchantDetailView, meta: { roles: ['USER'], userPortal: true, title: '商家详情', motion: 'page-focus' } },

  { path: '/cart', component: CartView, meta: { roles: ['USER'], title: '购物车', motion: 'page-slide' } },
  { path: '/deals', component: DealsView, meta: { roles: ['USER'], title: '团购', motion: 'page-market' } },
  { path: '/deals/:id', component: DealDetailView, meta: { roles: ['USER'], title: '团购详情', motion: 'page-focus' } },
  { path: '/deal-order/:orderId', component: DealOrderDetailView, meta: { roles: ['USER'], title: '团购券详情', motion: 'page-slide' } },
  { path: '/bookings', component: BookingsView, meta: { roles: ['USER'], title: '到店预约', motion: 'page-slide' } },
  { path: '/profile', component: ProfileView, meta: { roles: ['USER'], title: '个人中心', motion: 'page-lift' } },
  { path: '/settings', component: UserSettingsView, meta: { roles: ['USER'], userPortal: true, title: '设置', motion: 'page-pop' } },
  { path: '/profile/notifications', component: NotificationsView, meta: { roles: ['USER'], title: '通知中心', motion: 'page-slide' } },
  { path: '/role-applications', component: RoleApplicationView, meta: { roles: ['USER', 'RIDER', 'MERCHANT'], title: '身份申请', motion: 'page-pop' } },

  { path: '/orders', component: OrdersView, meta: { roles: ['USER'], title: '我的订单', motion: 'page-slide' } },
  { path: '/order/:orderId', component: OrderDetailView, meta: { roles: ['USER'], title: '订单详情', motion: 'page-focus' } },

  { path: '/payment/deal/:orderId', name: 'DealPayment', component: PaymentView, meta: { roles: ['USER'], title: '支付', motion: 'page-pop' } },
  { path: '/payment/:orderId', name: 'OrderPayment', component: PaymentView, meta: { roles: ['USER'], title: '支付', motion: 'page-pop' } },
  { path: '/review/:orderId', component: ReviewView, meta: { roles: ['USER'], title: '评价', motion: 'page-pop' } },

  { path: '/merchant-register', component: MerchantRegisterView, meta: { motion: 'page-none' } },

  { path: '/admin-audit', redirect: '/admin/audit' },
  { path: '/admin/announcements-old', redirect: '/admin/announcements' },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { roles: ['ADMIN'], motion: 'page-admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: AdminDashboardView, meta: { title: '管理后台', motion: 'page-admin' } },
      { path: 'orders', component: AdminOrdersView, meta: { title: '订单管理', motion: 'page-admin' } },
      { path: 'users', component: AdminUsersView, meta: { title: '用户管理', motion: 'page-admin' } },
      { path: 'audit', component: AdminAuditView, meta: { title: '商家审核', motion: 'page-admin' } },
      { path: 'role-applications', component: AdminRoleApplicationsView, meta: { title: '身份审核', motion: 'page-admin' } },
      { path: 'reviews', component: AdminReviewsView, meta: { title: '评价管理', motion: 'page-admin' } },
      { path: 'appeals', component: AdminAppealsView, meta: { title: '申诉处理', motion: 'page-admin' } },
      { path: 'announcements', component: AdminAnnouncementsView, meta: { title: '公告管理', motion: 'page-admin' } },
      { path: 'messages', component: AdminMessagesView, meta: { title: '消息管理', motion: 'page-admin' } }
      ,{ path: 'riders', component: AdminRidersView, meta: { title: '骑手运营', motion: 'page-admin' } }
    ]
  },

  { path: '/merchant/products', component: MerchantProductsView, meta: { roles: ['MERCHANT'], title: '商品管理', motion: 'page-workbench' } },
  { path: '/merchant/messages', component: MerchantMessagesView, meta: { roles: ['MERCHANT'], title: '客户信息', motion: 'page-slide' } },
  { path: '/merchant/analytics', component: MerchantAnalyticsView, meta: { roles: ['MERCHANT'], title: '数据分析', motion: 'page-workbench' } },
  { path: '/merchant/info', component: MerchantInfoView, meta: { roles: ['MERCHANT'], title: '商家信息', motion: 'page-pop' } },
  { path: '/merchant/audit-status', component: MerchantAuditStatusView, meta: { roles: ['USER', 'MERCHANT'], title: '审核状态', motion: 'page-pop' } },
  { path: '/merchant/deals', component: MerchantDealsView, meta: { roles: ['MERCHANT'], title: '团购管理', motion: 'page-workbench' } },
  { path: '/merchant/deals/:id', component: MerchantDealDetailView, meta: { roles: ['MERCHANT'], title: '团购详情', motion: 'page-focus' } },
  { path: '/merchant/bookings', component: MerchantBookingsView, meta: { roles: ['MERCHANT'], title: '预约管理', motion: 'page-slide' } },

  { path: '/merchant-console', component: MerchantConsoleView, meta: { roles: ['MERCHANT'], title: '商家工作台', motion: 'page-workbench' } },
  { path: '/rider-workbench', component: RiderWorkbenchView, meta: { roles: ['RIDER'], title: '骑手工作台', motion: 'page-workbench' } },

  { path: '/rider', component: RiderWorkbenchView, meta: { roles: ['RIDER'], title: '骑手工作台', motion: 'page-workbench' } },

  { path: '/user/announcements', component: UserAnnouncementsView, meta: { roles: ['USER'], title: '平台公告', motion: 'page-lift' } },
  { path: '/merchant/announcements', component: MerchantAnnouncementsView, meta: { roles: ['MERCHANT'], title: '平台公告', motion: 'page-lift' } },
  { path: '/announcements', redirect: () => defaultPath() }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 动态设置页面标题
router.afterEach((to) => {
  const title = to.meta.title
  document.title = title ? `${title} — CLAS` : 'CLAS 综合生活助手平台'
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
