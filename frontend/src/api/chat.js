import { api, unwrap } from './client'

export const sendMessage = (orderId, content) =>
  api.post('/chat/send', { orderId, content }).then(unwrap)

export const getOrderMessages = (orderId) =>
  api.get(`/chat/order/${orderId}`).then(unwrap)

export const getMerchantMessages = (merchantId) =>
  api.get(`/chat/merchant/${merchantId}`).then(unwrap)
