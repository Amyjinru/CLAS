import { api, unwrap } from './client'

export function currentUser() {
  return JSON.parse(localStorage.getItem('clas_user') || 'null')
}

export function currentUserId() {
  const user = JSON.parse(localStorage.getItem('clas_user') || 'null')
  return user?.id || 1
}

export const login = (payload) => api.post('/user/login', payload).then(unwrap)
export const register = (payload) => api.post('/user/register', payload).then(unwrap)
export const listMerchants = () => api.get('/merchant/list').then(unwrap)
export const getMerchant = (id) => api.get(`/merchant/${id}`).then(unwrap)
export const listProducts = (merchantId) => api.get(`/product/list/${merchantId}`).then(unwrap)
export const addCart = (payload) => api.post('/cart/add', { userId: currentUserId(), ...payload }).then(unwrap)
export const getCart = () => api.get(`/cart/list/${currentUserId()}`).then(unwrap)
export const clearCart = () => api.delete(`/cart/clear/${currentUserId()}`).then(unwrap)
export const createOrder = (payload) => api.post('/order/create', { userId: currentUserId(), ...payload }).then(unwrap)
export const listOrders = () => api.get(`/order/list/${currentUserId()}`).then(unwrap)
export const payOrder = (orderId) => api.post(`/order/pay/${orderId}`).then(unwrap)
export const completeOrder = (orderId) => api.post(`/order/complete/${orderId}`).then(unwrap)
export const listMerchantOrders = (merchantId = 1) => api.get(`/order/merchant/${merchantId}`).then(unwrap)
export const acceptOrder = (orderId) => api.post(`/order/accept/${orderId}`).then(unwrap)
export const addReview = (payload) => api.post('/review/add', { userId: currentUserId(), ...payload }).then(unwrap)
