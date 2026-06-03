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

// New Merchant Registration and Admin Auditing APIs
export const registerMerchant = (payload) => api.post('/merchant/register', payload).then(unwrap)
export const getMyMerchant = () => api.get('/merchant/my').then(unwrap)
export const adminListMerchants = () => api.get('/merchant/admin/list').then(unwrap)
export const adminAuditMerchant = (id, payload) => api.post(`/merchant/admin/audit/${id}`, payload).then(unwrap)
export const adminGetMerchantLogs = (id) => api.get(`/merchant/admin/audit-logs/${id}`).then(unwrap)

export const getMerchantProducts = (params) => api.get('/merchant/products/list', { params }).then(unwrap)
export const createProduct = (data) => api.post('/merchant/products/create', data).then(unwrap)
export const updateProduct = (data) => api.put('/merchant/products/update', data).then(unwrap)
export const updateProductStatus = (productId, status) => api.patch('/merchant/products/status', { productId, status }).then(unwrap)
export const deleteProduct = (productId) => api.delete(`/merchant/products/${productId}`).then(unwrap)
