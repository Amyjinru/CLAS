import { api, unwrap } from './client'

export const sendMessage = (orderIdOrPayload, content) => {
  const payload = typeof orderIdOrPayload === 'object'
    ? orderIdOrPayload
    : { orderId: orderIdOrPayload, content }
  return api.post('/chat/send', payload).then(unwrap)
}

export const getOrderMessages = (orderId) =>
  api.get(`/chat/order/${orderId}`).then(unwrap)

export const getMerchantMessages = (merchantId) =>
  api.get(`/chat/merchant/${merchantId}`).then(unwrap)

export const consultMerchant = (merchantId, content) =>
  api.post(`/chat/consult/${merchantId}`, { content }).then(unwrap)

export const getMessagesWithMerchant = (merchantId, userId) =>
  api.get(`/chat/with/${merchantId}`, { params: userId ? { userId } : undefined }).then(unwrap)

export const getConversations = () =>
  api.get('/chat/conversations').then(unwrap)

export const getAdminChatMerchants = () =>
  api.get('/chat/admin/merchants').then(unwrap)

export const getAdminChatUsers = (merchantId) =>
  api.get(`/chat/admin/merchant/${merchantId}/users`).then(unwrap)

export const getAdminChatMessages = (merchantId, userId) =>
  api.get(`/chat/admin/merchant/${merchantId}/user/${userId}`).then(unwrap)
