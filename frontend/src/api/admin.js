import { api, unwrap } from './client'

function downloadCsv(path, params, filename) {
  return api.get(path, { params, responseType: 'blob' }).then((response) => {
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'text/csv;charset=utf-8'
    })
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(objectUrl)
  })
}

export const getDashboard = (params) => api.get('/admin/dashboard', { params }).then(unwrap)
export const getOrderStats = (params) => api.get('/admin/stats/orders', { params }).then(unwrap)
export const getSalesOverview = (params) => api.get('/admin/stats/sales', { params }).then(unwrap)
export const getMerchantRanking = () => api.get('/admin/stats/merchants').then(unwrap)
export const getTopProducts = () => api.get('/admin/stats/products').then(unwrap)
export const listAdminOrders = (params) => api.get('/admin/orders', { params }).then(unwrap)
export const exportAdminOrders = (params) => downloadCsv('/admin/export/orders', params, 'orders.csv')
export const getAdminOrderTimeline = (orderId) => api.get(`/order/${orderId}/timeline`).then(unwrap)
export const listOrderRefundDisputes = (status) => api.get('/admin/order-refund-disputes', { params: { status } }).then(unwrap)
export const auditOrderRefundDispute = (id, payload) => api.patch(`/admin/order-refund-disputes/${id}`, payload).then(unwrap)
export const listAdminUsers = (params) => api.get('/admin/users', { params }).then(unwrap)
export const exportAdminUsers = (params) => downloadCsv('/admin/export/users', params, 'users.csv')
export const toggleUserStatus = (id, enabled) => api.put(`/admin/users/${id}/status`, { enabled }).then(unwrap)
export const listAdminReviews = (params) => api.get('/admin/reviews', { params, silent: true }).then(unwrap)
export const exportAdminReviews = (params) => downloadCsv('/admin/export/reviews', params, 'reviews.csv')
export const deleteAdminReview = (id) => api.delete(`/admin/reviews/${id}`, { silent: true }).then(unwrap)
export const resolveReviewReport = (id, status) => api.put(`/admin/reviews/${id}/report-status`, { status }, { silent: true }).then(unwrap)
export const listReviewDeleteRequests = (status) => api.get('/admin/reviews/delete-requests', { params: { status }, silent: true }).then(unwrap)
export const processReviewDeleteRequest = (id, payload) => api.post(`/admin/reviews/delete-requests/${id}/process`, payload, { silent: true }).then(unwrap)
export const listDeletedReviewBackups = () => api.get('/admin/reviews/deleted-backups', { silent: true }).then(unwrap)
export const listAppeals = () => api.get('/admin/appeals', { silent: true }).then(unwrap)
export const processAppeal = (id, payload) => api.post(`/admin/appeals/${id}/process`, payload, { silent: true }).then(unwrap)
export const listRiderApplications = () => api.get('/rider/admin/applications').then(unwrap)
export const auditRiderApplication = (id, payload) => api.patch(`/rider/admin/applications/${id}`, payload).then(unwrap)
export const listRiderWithdrawals = () => api.get('/rider/admin/withdrawals').then(unwrap)
export const auditRiderWithdrawal = (id, payload) => api.patch(`/rider/admin/withdrawals/${id}`, payload).then(unwrap)
export const listRiderInfoChangeRequests = () => api.get('/rider/admin/info-change-requests').then(unwrap)
export const auditRiderInfoChangeRequest = (id, payload) => api.patch(`/rider/admin/info-change-requests/${id}`, payload).then(unwrap)
