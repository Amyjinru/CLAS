import { api, unwrap } from './client'

export const listFavorites = () => api.get('/favorites/mine').then(unwrap)
export const addFavorite = (merchantId) => api.post(`/favorites/${merchantId}`).then(unwrap)
export const removeFavorite = (merchantId) => api.delete(`/favorites/${merchantId}`).then(unwrap)
