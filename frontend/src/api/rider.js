import { api, unwrap } from './client'

export const listAvailableRiderOrders = () => api.get('/rider/orders/available').then(unwrap)
export const listMyRiderOrders = () => api.get('/rider/orders/me').then(unwrap)
export const claimRiderOrder = (orderId) => api.post(`/rider/orders/${orderId}/claim`).then(unwrap)
