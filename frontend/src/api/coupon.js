import { api, unwrap } from './client'

export const listClaimableCoupons = () => api.get('/coupon/claimable').then(unwrap)
export const claimCoupon = (couponId) => api.post(`/coupon/claim/${couponId}`).then(unwrap)
export const listMyCoupons = () => api.get('/coupon/mine').then(unwrap)
