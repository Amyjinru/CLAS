import { api, unwrap } from './client'
import { currentUserId } from './session'

export const createOrder = (payload) => api.post('/order/create', { userId: currentUserId(), ...payload }).then(unwrap)
export const listOrders = () => api.get(`/order/list/${currentUserId()}`).then(unwrap)
export const payOrder = (orderId) => api.post(`/order/pay/${orderId}`).then(unwrap)
export const completeOrder = (orderId) => api.post(`/order/complete/${orderId}`).then(unwrap)
export const cancelOrder = (orderId) => api.post(`/order/cancel/${orderId}`).then(unwrap)
export const acceptOrder = (orderId) => api.post(`/order/accept/${orderId}`).then(unwrap)
export const rejectOrder = (orderId) => api.post(`/order/reject/${orderId}`).then(unwrap)
