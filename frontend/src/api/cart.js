import { api, unwrap } from './client'
import { currentUserId } from './session'

export const addCart = (payload) => api.post('/cart/add', { userId: currentUserId(), ...payload }).then(unwrap)
export const removeCart = (payload) => api.post('/cart/remove', { userId: currentUserId(), ...payload }).then(unwrap)
export const updateCart = (payload) => api.post('/cart/update', { userId: currentUserId(), ...payload }).then(unwrap)
export const deleteCartItem = (productId) => api.delete(`/cart/item/${currentUserId()}/${productId}`).then(unwrap)
export const getCart = () => api.get(`/cart/list/${currentUserId()}`).then(unwrap)
export const clearCart = () => api.delete(`/cart/clear/${currentUserId()}`).then(unwrap)
