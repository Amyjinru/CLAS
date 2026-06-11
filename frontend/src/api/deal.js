import { api, unwrap } from './client'

export const listDeals = (params, config = {}) => api.get('/deals', { ...config, params }).then(unwrap)
export const getDeal = (id, config = {}) => api.get(`/deals/${id}`, config).then(unwrap)
export const listMerchantDeals = () => api.get('/deals/merchant').then(unwrap)
export const createDeal = (payload) => api.post('/deals/merchant', payload).then(unwrap)
export const buyDeal = (id) => api.post(`/deals/${id}/buy`).then(unwrap)
export const getDealPaymentStatus = (dealOrderId) => api.get(`/deals/orders/${dealOrderId}/payment-status`).then(unwrap)
export const payDealOrder = (dealOrderId, payMethod) =>
  api.post(`/deals/orders/${dealOrderId}/pay`, { payMethod }).then(unwrap)
export const listMyDealOrders = (config = {}) => api.get('/deals/mine', config).then(unwrap)
export const redeemDeal = (voucherCode) => api.post('/deals/redeem', { voucherCode }).then(unwrap)
export const refundDealOrder = (dealOrderId) => api.post(`/deals/orders/${dealOrderId}/refund`).then(unwrap)
export const listDealRedeemLogs = () => api.get('/deals/redeem-logs').then(unwrap)
