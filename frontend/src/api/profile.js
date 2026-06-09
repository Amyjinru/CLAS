import { api, unwrap } from './client'

export const getProfile = (config = {}) => api.get('/user/profile', config).then(unwrap)
export const updateProfile = (payload) => api.put('/user/profile', payload).then(unwrap)
export async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post('/user/profile/avatar', formData, { silent: true })
  return unwrap(response)
}
export const listMyPenalties = () => api.get('/user/penalties/mine', { silent: true }).then(unwrap)
export const submitAppeal = (payload) => api.post('/user/appeals', payload, { silent: true }).then(unwrap)
export const listMyAppeals = () => api.get('/user/appeals/mine', { silent: true }).then(unwrap)

export const applyPenalty = (phone, payload) => api.post(`/admin/users/${phone}/penalties`, payload, { silent: true }).then(unwrap)
export const revokePenalty = (id) => api.post(`/admin/penalties/${id}/revoke`, null, { silent: true }).then(unwrap)
