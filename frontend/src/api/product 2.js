import { api, unwrap } from './client'

export const listProducts = (merchantId) => api.get(`/product/list/${merchantId}`).then(unwrap)
export const listGroupedProducts = (merchantId) => api.get('/product/list', { params: { merchantId } }).then(unwrap)
export const listProductCategories = (params) => api.get('/product/categories', { params }).then(unwrap)
export const createProductCategory = (data) => api.post('/product/categories', data).then(unwrap)
export const updateProductCategory = (data) => api.put('/product/categories', data).then(unwrap)
export const deleteProductCategory = (categoryId) => api.delete(`/product/categories/${categoryId}`).then(unwrap)
export const getMerchantProducts = (params) => api.get('/merchant/me/products', { params }).then(unwrap)
export const createProduct = (data) => api.post('/merchant/me/products', data).then(unwrap)
export const updateProduct = (data) => api.put('/merchant/me/products', data).then(unwrap)
export const updateProductStatus = (productId, status) => api.patch('/merchant/me/products/status', { productId, status }).then(unwrap)
export const deleteProduct = (productId) => api.delete(`/merchant/me/products/${productId}`).then(unwrap)
export const uploadProductImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/product/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(unwrap)
}
