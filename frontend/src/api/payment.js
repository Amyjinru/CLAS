import { api, unwrap } from './client'
import { currentUserId } from './session'

export const mockPay = (payload) => api.post('/payment/mock', { userId: currentUserId(), ...payload }).then(unwrap)
export const getPaymentStatus = (orderId) => api.get(`/payment/status/${orderId}`).then(unwrap)
