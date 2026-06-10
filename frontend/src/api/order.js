import { api, unwrap } from './client'

export const previewOrder = (merchantId, addressId, userCouponId) =>
  api.get('/order/preview', { params: { merchantId, addressId, userCouponId } }).then(unwrap)
export const createOrder = (payload) => api.post('/order/create', payload).then(unwrap)
export const listOrders = () => api.get('/order/me').then(unwrap)
export const payOrder = (orderId) => api.post(`/order/pay/${orderId}`).then(unwrap)
export const completeOrder = (orderId) => api.post(`/order/complete/${orderId}`).then(unwrap)
export const cancelOrder = (orderId) => api.post(`/order/cancel/${orderId}`).then(unwrap)
export const acceptOrder = (orderId) => api.post(`/order/accept/${orderId}`).then(unwrap)
export const rejectOrder = (orderId, reason) => api.post(`/order/reject/${orderId}`, { reason }).then(unwrap)
export const deliverOrder = (orderId) => api.post(`/order/deliver/${orderId}`).then(unwrap)
export const requestRefund = (orderId, reason) => api.post(`/order/refund/${orderId}`, { reason }).then(unwrap)
export const approveRefund = (orderId) => api.post(`/order/refund/${orderId}/approve`).then(unwrap)
export const rejectRefund = (orderId, reason) => api.post(`/order/refund/${orderId}/reject`, { reason }).then(unwrap)
