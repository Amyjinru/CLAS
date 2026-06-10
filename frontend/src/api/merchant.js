import { api, unwrap } from './client'

export const listMerchants = (params) => api.get('/merchant/list', { params }).then(unwrap)
export const getMerchant = (id) => api.get(`/merchant/${id}`).then(unwrap)
export const getDeliveryEstimate = (id, params) => api.get(`/merchant/${id}/delivery-estimate`, { params }).then(unwrap)
export const registerMerchant = (payload) => api.post('/merchant/register', payload).then(unwrap)
export const getMyMerchant = () => api.get('/merchant/my').then(unwrap)
export const getMyMerchantAuditStatus = () => api.get('/merchant/my/audit-status').then(unwrap)
export const getMyMerchantStats = () => api.get('/merchant/my/stats').then(unwrap)
export const sendMerchantProfileCode = (payload) => api.post('/merchant/my/profile/send-code', payload).then(unwrap)
export const updateMyMerchantProfile = (payload) => api.put('/merchant/my/profile', payload).then(unwrap)
export const toggleMerchantManualClosed = () => api.post('/merchant/my/manual-closed/toggle').then(unwrap)
export const adminListMerchants = () => api.get('/merchant/admin/list').then(unwrap)
export const adminAuditMerchant = (id, payload) => api.post(`/merchant/admin/audit/${id}`, payload).then(unwrap)
export const adminGetMerchantLogs = (id) => api.get(`/merchant/admin/audit-logs/${id}`).then(unwrap)
export const uploadMerchantLogo = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/merchant/my/logo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(unwrap)
}

export async function currentMerchantId() {
  const merchant = await getMyMerchant()
  if (!merchant?.id) {
    throw new Error('NO_MERCHANT')
  }
  return merchant.id
}

export async function listMerchantOrders() {
  return api.get('/order/merchant/me').then(unwrap)
}
