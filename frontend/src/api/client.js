import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'

let loadingInstance = null
let requestCount = 0

function showLoading() {
  if (requestCount === 0) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '正在请求中...',
      background: 'rgba(0, 0, 0, 0.5)'
    })
  }
  requestCount++
}

function hideLoading() {
  requestCount--
  if (requestCount <= 0) {
    requestCount = 0
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// ===== test1: 请求拦截器（loading + 认证头） =====
api.interceptors.request.use(
  config => {
    // Show loading unless specified otherwise
    if (!config.silent) {
      showLoading()
    }

    // Add Authorization header
    const user = JSON.parse(localStorage.getItem('clas_user') || 'null')
    if (user && user.phone) {
      config.headers['Authorization'] = user.phone
    }

    return config
  },
  error => {
    hideLoading()
    return Promise.reject(error)
  }
)

// ===== test1: 响应拦截器（ElMessage 错误提示 + 静默模式） =====
api.interceptors.response.use(
  response => {
    if (!response.config.silent) {
      hideLoading()
    }

    // Handle standard Result format with code !== 200 as error
    if (response.data && response.data.code && response.data.code !== 200) {
      const msg = response.data.message || '请求失败'
      ElMessage.error(msg)
      const error = new Error(msg)
      error.response = response
      return Promise.reject(error)
    }

    return response
  },
  error => {
    if (error.config && !error.config.silent) {
      hideLoading()
    }

    const msg = error.response?.data?.message || error.message || '系统异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export function unwrap(response) {
  return response.data.data
}
