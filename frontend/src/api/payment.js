import { api, unwrap } from './client'
import { currentUserId } from './session'

export const mockPay = (payload) => api.post('/payment/mock', { userId: currentUserId(), ...payload }).then(unwrap)
export const getPaymentStatus = (orderId) => api.get(`/payment/status/${orderId}`).then(unwrap)
export const getBatchPaymentStatus = (orderIds) => api.get('/payment/status/batch', {
  params: { orderIds: orderIds.join(',') }
}).then(unwrap)
export const mockPayBatch = (payload) => api.post('/payment/mock/batch', payload, {
  headers: payload.idempotencyKey ? { 'Idempotency-Key': payload.idempotencyKey } : undefined
}).then(unwrap)
export const listBankCards = () => api.get('/user/bank-cards').then(unwrap)
export const addBankCard = (payload) => api.post('/user/bank-cards', payload).then(unwrap)
export const deleteBankCard = (id) => api.delete(`/user/bank-cards/${id}`).then(unwrap)
