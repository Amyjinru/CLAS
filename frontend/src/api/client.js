import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

api.interceptors.response.use((response) => {
  const payload = response.data
  if (payload && typeof payload.code === 'number' && payload.code !== 200) {
    return Promise.reject(Object.assign(new Error(payload.message || '请求失败'), { response }))
  }
  return response
})

export function unwrap(response) {
  return response.data.data
}
