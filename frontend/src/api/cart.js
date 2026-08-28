import { api, unwrap } from './client'

export const addCart = (payload) => api.post('/cart/add', payload).then(unwrap)
export const removeCart = (payload) => api.post('/cart/remove', payload).then(unwrap)
export const updateCart = (payload) => api.post('/cart/update', payload).then(unwrap)
export const deleteCartItem = (productId) => api.delete(`/cart/me/items/${productId}`).then(unwrap)
export const getCart = () => api.get('/cart/me').then(unwrap)
export const validateCart = () => api.get('/cart/me/validation').then(unwrap)
export const clearInvalidCart = () => api.delete('/cart/me/invalid').then(unwrap)
export const clearCart = () => api.delete('/cart/me').then(unwrap)
