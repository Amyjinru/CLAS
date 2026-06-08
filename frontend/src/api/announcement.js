import { api, unwrap } from './client'

export const listAnnouncements = () => api.get('/announcement/list').then(unwrap)
export const createAnnouncement = (payload) => api.post('/announcement/create', payload, { silent: true }).then(unwrap)
export const updateAnnouncement = (id, payload) => api.put(`/announcement/${id}`, payload, { silent: true }).then(unwrap)
export const deleteAnnouncement = (id) => api.delete(`/announcement/${id}`, { silent: true }).then(unwrap)
