import { api, unwrap } from './client'
import { currentUserId } from './session'

export const addReview = (payload) => api.post('/review/add', { userId: currentUserId(), ...payload }).then(unwrap)
export const getReviewByOrder = (orderId) => api.get(`/review/order/${orderId}`).then(unwrap)
export const listMyReviews = () => api.get('/review/mine').then(unwrap)
export const listReviewsByMerchant = (merchantId) => api.get(`/review/merchant/${merchantId}`).then(unwrap)
export const getMerchantRating = (merchantId) => api.get(`/review/rating/${merchantId}`).then(unwrap)
export const replyReview = (reviewId, reply) => api.post(`/review/${reviewId}/reply`, { reply }).then(unwrap)
export const addReviewComment = (reviewId, payload) => api.post(`/review/${reviewId}/comments`, payload).then(unwrap)
export const voteReviewTarget = (targetType, targetId, voteType) =>
  api.post(`/review/${targetType}/${targetId}/vote`, { targetType, voteType }).then(unwrap)
export const deleteReview = (reviewId) => api.delete(`/review/${reviewId}`).then(unwrap)
export const deleteMerchantReply = (reviewId) => api.delete(`/review/${reviewId}/merchant-reply`).then(unwrap)
export const deleteReviewReply = (replyId) => api.delete(`/review/reply/${replyId}`).then(unwrap)
export const requestReviewDelete = (reviewId, reason) => api.post(`/review/${reviewId}/delete-request`, { reason }).then(unwrap)
export const reportReview = (reviewId, reason) => api.post(`/review/${reviewId}/report`, { reason }).then(unwrap)
export const reportReviewReply = (replyId, reason) => api.post(`/review/reply/${replyId}/report`, { reason }).then(unwrap)

export async function uploadReviewImages(files) {
  const formData = new FormData()
  Array.from(files).forEach((file) => formData.append('files', file))
  const response = await api.post('/review/upload', formData)
  return unwrap(response)
}
