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

function clearAuthAndRedirect() {
  localStorage.removeItem('clas_user')
  // 刷新页面以重置状态并跳转到登录页
  const currentPath = window.location.pathname
  if (currentPath !== '/login') {
    window.location.href = '/login?redirect=' + encodeURIComponent(currentPath)
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// ===== 请求拦截器（loading + JWT 认证头） =====
api.interceptors.request.use(
  config => {
    // Show loading unless specified otherwise
    if (!config.silent) {
      showLoading()
    }

    // Add Authorization header with JWT Bearer token
    const user = JSON.parse(localStorage.getItem('clas_user') || 'null')
    if (user && user.token) {
      config.headers['Authorization'] = 'Bearer ' + user.token
    }

    return config
  },
  error => {
    hideLoading()
    return Promise.reject(error)
  }
)

// ===== 响应拦截器（401 自动跳转登录 + 错误提示） =====
api.interceptors.response.use(
  response => {
    if (!response.config.silent) {
      hideLoading()
    }

    // Handle standard Result format with code !== 200 as error
    if (response.data && response.data.code && response.data.code !== 200) {
      const msg = response.data.message || '请求失败'
      // 401 清除认证并跳转登录
      if (response.data.code === 401) {
        clearAuthAndRedirect()
      }
      if (!response.config.silent) {
        ElMessage.error(msg)
      }
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

    // HTTP 401 自动清除认证并跳转登录
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      clearAuthAndRedirect()
      return Promise.reject(error)
    }

    const msg = error.response?.data?.message || error.message || '系统异常'
    if (!error.config?.silent) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export function unwrap(response) {
  return response.data.data
}
