import { api, unwrap } from './client'

export const listFavorites = (config = {}) => api.get('/favorites/mine', config).then(unwrap)
export const addFavorite = (merchantId) => api.post(`/favorites/${merchantId}`).then(unwrap)
export const removeFavorite = (merchantId) => api.delete(`/favorites/${merchantId}`).then(unwrap)
