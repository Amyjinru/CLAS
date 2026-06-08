import { api, unwrap } from './client'

export const listNotifications = () => api.get('/notifications/mine').then(unwrap)
export const markNotificationRead = (id) => api.post(`/notifications/${id}/read`).then(unwrap)
