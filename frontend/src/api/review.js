import { api, unwrap } from './client'
import { currentUserId } from './session'

export const addReview = (payload) => api.post('/review/add', { userId: currentUserId(), ...payload }).then(unwrap)
export const getReviewByOrder = (orderId) => api.get(`/review/order/${orderId}`).then(unwrap)
export const listReviewsByMerchant = (merchantId) => api.get(`/review/merchant/${merchantId}`).then(unwrap)
export const getMerchantRating = (merchantId) => api.get(`/review/rating/${merchantId}`).then(unwrap)
