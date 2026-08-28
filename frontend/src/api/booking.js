import { api, unwrap } from './client'

export const createBooking = (payload) => api.post('/bookings', payload).then(unwrap)
export const listMyBookings = () => api.get('/bookings/mine').then(unwrap)
export const cancelBooking = (id) => api.post(`/bookings/${id}/cancel`).then(unwrap)
export const listMerchantBookings = () => api.get('/bookings/merchant').then(unwrap)
export const updateBookingStatus = (id, status) => api.post(`/bookings/${id}/status`, { status }).then(unwrap)
