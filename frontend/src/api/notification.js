import { api, unwrap } from './client'

export const listNotifications = () => api.get('/notifications/mine').then(unwrap)
export const markNotificationRead = (id) => api.post(`/notifications/${id}/read`).then(unwrap)
export const deleteNotification = (id) => api.delete(`/notifications/${id}`, { silent: true }).then(unwrap)
export const deleteAllNotifications = () => api.delete('/notifications/all', { silent: true }).then(unwrap)
