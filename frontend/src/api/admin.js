import { api, unwrap } from './client'

export const getDashboard = () => api.get('/admin/dashboard').then(unwrap)
export const getOrderStats = () => api.get('/admin/stats/orders').then(unwrap)
export const getSalesOverview = () => api.get('/admin/stats/sales').then(unwrap)
export const getMerchantRanking = () => api.get('/admin/stats/merchants').then(unwrap)
export const getTopProducts = () => api.get('/admin/stats/products').then(unwrap)
export const listAdminOrders = (params) => api.get('/admin/orders', { params }).then(unwrap)
export const listAdminUsers = (params) => api.get('/admin/users', { params }).then(unwrap)
export const toggleUserStatus = (id, enabled) => api.put(`/admin/users/${id}/status`, { enabled }).then(unwrap)
export const listAdminReviews = (params) => api.get('/admin/reviews', { params }).then(unwrap)
export const deleteAdminReview = (id) => api.delete(`/admin/reviews/${id}`).then(unwrap)
export const resolveReviewReport = (id, status) => api.put(`/admin/reviews/${id}/report-status`, { status }).then(unwrap)
