import { api, unwrap } from './client'

export const listAnnouncements = () => api.get('/announcement/list').then(unwrap)
export const createAnnouncement = (payload) => api.post('/announcement/create', payload).then(unwrap)
export const deleteAnnouncement = (id) => api.delete(`/announcement/${id}`).then(unwrap)
