import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import MerchantDetailView from '../views/MerchantDetailView.vue'
import CartView from '../views/CartView.vue'
import OrdersView from '../views/OrdersView.vue'
import MerchantConsoleView from '../views/MerchantConsoleView.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: LoginView },
  { path: '/home', component: HomeView },
  { path: '/merchant/:id', component: MerchantDetailView },
  { path: '/cart', component: CartView },
  { path: '/orders', component: OrdersView },
  { path: '/merchant-console', component: MerchantConsoleView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
