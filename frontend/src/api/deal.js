import { api, unwrap } from './client'

export const listDeals = (params) => api.get('/deals', { params }).then(unwrap)
export const listMerchantDeals = () => api.get('/deals/merchant').then(unwrap)
export const createDeal = (payload) => api.post('/deals/merchant', payload).then(unwrap)
export const buyDeal = (id) => api.post(`/deals/${id}/buy`).then(unwrap)
export const listMyDealOrders = () => api.get('/deals/mine').then(unwrap)
export const redeemDeal = (voucherCode) => api.post('/deals/redeem', { voucherCode }).then(unwrap)
