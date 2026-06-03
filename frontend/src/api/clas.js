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
export const getReviewByOrder = (orderId) => api.get(`/review/order/${orderId}`).then(unwrap)
export const listReviewsByMerchant = (merchantId) => api.get(`/review/merchant/${merchantId}`).then(unwrap)
export const getMerchantRating = (merchantId) => api.get(`/review/rating/${merchantId}`).then(unwrap)
export const listAnnouncements = () => api.get('/announcement/list').then(unwrap)
export const createAnnouncement = (payload) => api.post('/announcement/create', payload).then(unwrap)
export const deleteAnnouncement = (id) => api.delete(`/announcement/${id}`).then(unwrap)
