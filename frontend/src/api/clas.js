import { ref } from 'vue'
import { api, unwrap } from './client'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('clas_user') || 'null')
  } catch {
    return null
  }
}

export const sessionUser = ref(readStoredUser())

export function currentUser() {
  return sessionUser.value
}

export function currentRole() {
  return currentUser()?.role || null
}

export function setSessionUser(user) {
  sessionUser.value = user
  if (user) {
    localStorage.setItem('clas_user', JSON.stringify(user))
  } else {
    localStorage.removeItem('clas_user')
  }
}

export function logout() {
  setSessionUser(null)
}

export function currentUserId() {
  const user = currentUser()
  if (!user?.id) {
    throw new Error('NOT_LOGGED_IN')
  }
  return user.id
}

/** MVP 演示：merchant 账号对应 1 号店铺 */
export function currentMerchantId() {
  return 1
}

export const login = (payload) => api.post('/user/login', payload).then(unwrap)
export const register = (payload) => api.post('/user/register', payload).then(unwrap)
export const listMerchants = () => api.get('/merchant/list').then(unwrap)
export const getMerchant = (id) => api.get(`/merchant/${id}`).then(unwrap)
export const listProducts = (merchantId) => api.get(`/product/list/${merchantId}`).then(unwrap)
export const addCart = (payload) => api.post('/cart/add', { userId: currentUserId(), ...payload }).then(unwrap)
export const removeCart = (payload) => api.post('/cart/remove', { userId: currentUserId(), ...payload }).then(unwrap)
export const getCart = () => api.get(`/cart/list/${currentUserId()}`).then(unwrap)
export const clearCart = () => api.delete(`/cart/clear/${currentUserId()}`).then(unwrap)
export const createOrder = (payload) => api.post('/order/create', { userId: currentUserId(), ...payload }).then(unwrap)
export const listOrders = () => api.get(`/order/list/${currentUserId()}`).then(unwrap)
export const payOrder = (orderId) => api.post(`/order/pay/${orderId}`).then(unwrap)
export const mockPay = (payload) => api.post('/payment/mock', { userId: currentUserId(), ...payload }).then(unwrap)
export const getPaymentStatus = (orderId) => api.get(`/payment/status/${orderId}`).then(unwrap)
export const completeOrder = (orderId) => api.post(`/order/complete/${orderId}`).then(unwrap)
export const listMerchantOrders = (merchantId = 1) => api.get(`/order/merchant/${merchantId}`).then(unwrap)
export const acceptOrder = (orderId) => api.post(`/order/accept/${orderId}`).then(unwrap)
export const addReview = (payload) => api.post('/review/add', { userId: currentUserId(), ...payload }).then(unwrap)

// ===== test1: 商户入驻审核 API =====
export const registerMerchant = (payload) => api.post('/merchant/register', payload).then(unwrap)
export const getMyMerchant = () => api.get('/merchant/my').then(unwrap)
export const adminListMerchants = () => api.get('/merchant/admin/list').then(unwrap)
export const adminAuditMerchant = (id, payload) => api.post(`/merchant/admin/audit/${id}`, payload).then(unwrap)
export const adminGetMerchantLogs = (id) => api.get(`/merchant/admin/audit-logs/${id}`).then(unwrap)

// ===== test1: 商户商品管理 API =====
export const getMerchantProducts = (params) => api.get('/merchant/products/list', { params }).then(unwrap)
export const createProduct = (data) => api.post('/merchant/products/create', data).then(unwrap)
export const updateProduct = (data) => api.put('/merchant/products/update', data).then(unwrap)
export const updateProductStatus = (productId, status) => api.patch('/merchant/products/status', { productId, status }).then(unwrap)
export const deleteProduct = (productId) => api.delete(`/merchant/products/${productId}`).then(unwrap)

// ===== version_314: 评论查询 API =====
export const getReviewByOrder = (orderId) => api.get(`/review/order/${orderId}`).then(unwrap)
export const listReviewsByMerchant = (merchantId) => api.get(`/review/merchant/${merchantId}`).then(unwrap)
export const getMerchantRating = (merchantId) => api.get(`/review/rating/${merchantId}`).then(unwrap)

// ===== version_314: 公告管理 API =====
export const listAnnouncements = () => api.get('/announcement/list').then(unwrap)
export const createAnnouncement = (payload) => api.post('/announcement/create', payload).then(unwrap)
export const deleteAnnouncement = (id) => api.delete(`/announcement/${id}`).then(unwrap)

// ===== 同学E: 管理后台 API =====
export const getDashboard = () => api.get('/admin/dashboard').then(unwrap)
export const getOrderStats = () => api.get('/admin/stats/orders').then(unwrap)
export const getSalesOverview = () => api.get('/admin/stats/sales').then(unwrap)
export const getMerchantRanking = () => api.get('/admin/stats/merchants').then(unwrap)
export const getTopProducts = () => api.get('/admin/stats/products').then(unwrap)
export const listAdminOrders = (params) => api.get('/admin/orders', { params }).then(unwrap)
export const listAdminUsers = (params) => api.get('/admin/users', { params }).then(unwrap)
export const toggleUserStatus = (id, enabled) => api.put(`/admin/users/${id}/status`, { enabled }).then(unwrap)
export const listAdminReviews = (params) => api.get('/admin/reviews', { params }).then(unwrap)
export const deleteAdminReview = (id) => api.delete(`/admin/reviews/${id}`).then(unwrap)
