import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import MerchantDetailView from '../views/MerchantDetailView.vue'
import CartView from '../views/CartView.vue'
import OrdersView from '../views/OrdersView.vue'
import MerchantConsoleView from '../views/MerchantConsoleView.vue'
import MerchantRegisterView from '../views/MerchantRegisterView.vue'
import AdminAuditView from '../views/AdminAuditView.vue'
import MerchantProductsView from '../views/MerchantProductsView.vue'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: LoginView },
  { path: '/home', component: HomeView },
  { path: '/merchant/:id', component: MerchantDetailView },
  { 
    path: '/cart', 
    component: CartView, 
    meta: { requiresAuth: true } 
  },
  { 
    path: '/orders', 
    component: OrdersView, 
    meta: { requiresAuth: true } 
  },
  { 
    path: '/merchant-console', 
    component: MerchantConsoleView, 
    meta: { requiresAuth: true } 
  },
  { 
    path: '/merchant/products', 
    component: MerchantProductsView, 
    meta: { requiresAuth: true } 
  },
  { 
    path: '/merchant-register', 
    component: MerchantRegisterView 
  },
  { 
    path: '/admin-audit', 
    component: AdminAuditView, 
    meta: { roles: ['ADMIN'] } 
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('clas_user') || 'null')

  // Check roles authorization
  if (to.meta.roles) {
    if (!user) {
      ElMessage.warning('请先登录账号')
      return next('/login')
    }
    const hasRole = to.meta.roles.includes(user.role)
    if (!hasRole) {
      ElMessage.error('权限不足，无法访问该页面')
      return next('/home')
    }
  }

  // Check general authentication
  if (to.meta.requiresAuth && !user) {
    ElMessage.warning('请先登录账号')
    return next('/login')
  }

  next()
})

export default router
