import { api, unwrap } from './client'

export const listProducts = (merchantId) => api.get(`/product/list/${merchantId}`).then(unwrap)
export const getMerchantProducts = (params) => api.get('/merchant/products/list', { params }).then(unwrap)
export const createProduct = (data) => api.post('/merchant/products/create', data).then(unwrap)
export const updateProduct = (data) => api.put('/merchant/products/update', data).then(unwrap)
export const updateProductStatus = (productId, status) => api.patch('/merchant/products/status', { productId, status }).then(unwrap)
export const deleteProduct = (productId) => api.delete(`/merchant/products/${productId}`).then(unwrap)
