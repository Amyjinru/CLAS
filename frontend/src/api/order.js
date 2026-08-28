import { api, unwrap } from './client'

export const previewOrder = (merchantId, addressId, userCouponId, productIds, location) =>
  api.get('/order/preview', {
    params: {
      merchantId,
      addressId,
      deliveryAddress: addressId ? undefined : location?.address,
      deliveryLongitude: addressId ? undefined : location?.longitude,
      deliveryLatitude: addressId ? undefined : location?.latitude,
      userCouponId,
      productIds: productIds?.length ? productIds.join(',') : undefined
    }
  }).then(unwrap)
export const createOrder = (payload) => api.post('/order/create', payload).then(unwrap)
export const createOrderBatch = (payload) => api.post('/order/create-batch', payload).then(unwrap)
export const listOrders = () => api.get('/order/me').then(unwrap)
export const getOrderDetail = (orderId) => api.get(`/order/${orderId}`).then(unwrap)
export const payOrder = (orderId) => api.post(`/order/pay/${orderId}`).then(unwrap)
export const completeOrder = (orderId) => api.post(`/order/complete/${orderId}`).then(unwrap)
export const tipRider = (orderId, amount, idempotencyKey) => api.post(`/order/${orderId}/rider-tip`, { amount, idempotencyKey }).then(unwrap)
export const cancelOrder = (orderId) => api.post(`/order/cancel/${orderId}`).then(unwrap)
export const acceptOrder = (orderId) => api.post(`/order/accept/${orderId}`).then(unwrap)
export const readyForDispatch = (orderId) => api.post(`/order/ready-for-dispatch/${orderId}`).then(unwrap)
export const getOrderTimeline = (orderId) => api.get(`/order/${orderId}/timeline`).then(unwrap)
export const submitRiderReview = (orderId, payload) => api.post(`/order/${orderId}/rider-review`, payload).then(unwrap)
export const getRiderReview = (orderId) => api.get(`/order/${orderId}/rider-review`).then(unwrap)
export const rejectOrder = (orderId, reason) => api.post(`/order/reject/${orderId}`, { reason }).then(unwrap)
export const deliverOrder = (orderId) => api.post(`/order/deliver/${orderId}`).then(unwrap)
export const requestRefund = (orderId, reason) => api.post(`/order/refund/${orderId}`, { reason }).then(unwrap)
export const approveRefund = (orderId) => api.post(`/order/refund/${orderId}/approve`).then(unwrap)
export const rejectRefund = (orderId, reason) => api.post(`/order/refund/${orderId}/reject`, { reason }).then(unwrap)
export const submitRefundDispute = (orderId, reason) => api.post(`/order/refund/${orderId}/dispute`, { reason }).then(unwrap)
