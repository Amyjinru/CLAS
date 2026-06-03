import { createRouter, createWebHistory } from 'vue-router'
import { currentRole, currentUser } from '../api/clas'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import MerchantDetailView from '../views/MerchantDetailView.vue'
import OrdersView from '../views/OrdersView.vue'
import MerchantConsoleView from '../views/MerchantConsoleView.vue'
import PaymentView from '../views/PaymentView.vue'
import ReviewView from '../views/ReviewView.vue'
import UserAnnouncementsView from '../views/user/UserAnnouncementsView.vue'
import MerchantAnnouncementsView from '../views/merchant/MerchantAnnouncementsView.vue'
import AdminAnnouncementsView from '../views/admin/AdminAnnouncementsView.vue'

function defaultPath() {
  const role = currentRole()
  if (role === 'MERCHANT') return '/merchant-console'
  if (role === 'ADMIN') return '/admin/announcements'
  if (role === 'USER') return '/home'
  return '/login'
}

const routes = [
  { path: '/', redirect: () => defaultPath() },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/home', component: HomeView, meta: { roles: ['USER'], userPortal: true } },
  { path: '/merchant/:id', component: MerchantDetailView, meta: { roles: ['USER'], userPortal: true } },
  { path: '/cart', redirect: '/home' },
  { path: '/orders', component: OrdersView, meta: { roles: ['USER'] } },
  { path: '/payment/:orderId', component: PaymentView, meta: { roles: ['USER'] } },
  { path: '/review/:orderId', component: ReviewView, meta: { roles: ['USER'] } },
  { path: '/user/announcements', component: UserAnnouncementsView, meta: { roles: ['USER'] } },
  { path: '/merchant-console', component: MerchantConsoleView, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/announcements', component: MerchantAnnouncementsView, meta: { roles: ['MERCHANT'] } },
  { path: '/admin/announcements', component: AdminAnnouncementsView, meta: { roles: ['ADMIN'] } },
  { path: '/announcements', redirect: () => defaultPath() }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = currentUser()
  const role = currentRole()

  if (to.meta.roles && (!role || !to.meta.roles.includes(role))) {
    if (!user) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    next(defaultPath())
    return
  }

  if (to.meta.userPortal && role === 'MERCHANT') {
    next('/merchant-console')
    return
  }

  next()
})

export default router
