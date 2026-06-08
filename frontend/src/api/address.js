import { api, unwrap } from './client'

export const listAddresses = (config = {}) => api.get('/address/mine', config).then(unwrap)
export const createAddress = (payload) => api.post('/address', payload).then(unwrap)
export const setDefaultAddress = (id) => api.post(`/address/${id}/default`).then(unwrap)
export const deleteAddress = (id) => api.delete(`/address/${id}`).then(unwrap)
