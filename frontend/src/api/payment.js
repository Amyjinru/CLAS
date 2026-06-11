import { api, unwrap } from './client'
import { currentUserId } from './session'

export const mockPay = (payload) => api.post('/payment/mock', { userId: currentUserId(), ...payload }).then(unwrap)
export const getPaymentStatus = (orderId) => api.get(`/payment/status/${orderId}`).then(unwrap)
export const listBankCards = () => api.get('/user/bank-cards').then(unwrap)
export const addBankCard = (payload) => api.post('/user/bank-cards', payload).then(unwrap)
export const deleteBankCard = (id) => api.delete(`/user/bank-cards/${id}`).then(unwrap)
