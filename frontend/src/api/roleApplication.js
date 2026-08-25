import { api, unwrap } from './client'

export const applyForRider = (payload) => api.post('/role-applications/rider', payload).then(unwrap)
export const getMyRoleApplications = () => api.get('/role-applications/mine').then(unwrap)
export const adminListRoleApplications = (status) => api.get('/role-applications/admin', { params: { status } }).then(unwrap)
export const adminAuditRoleApplication = (id, payload) => api.post(`/role-applications/admin/${id}/audit`, payload).then(unwrap)
