import { ref } from 'vue'
import { api, unwrap } from './client'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('clas_user') || 'null')
  } catch {
    return null
  }
}

export const sessionUser = ref(readStoredUser())

export function currentUser() {
  return sessionUser.value
}

export function currentRole() {
  return currentUser()?.role || null
}

export function currentToken() {
  return sessionUser.value?.token || null
}

export function setSessionUser(data) {
  sessionUser.value = data
  if (data) {
    localStorage.setItem('clas_user', JSON.stringify(data))
  } else {
    localStorage.removeItem('clas_user')
  }
}

// 先通知后端清除服务端会话（否则其它设备仍会被要求验证码），再清理本地状态。
// 服务端会话可能已失效（被其它设备覆盖或过期），此时忽略错误仍执行本地清理。
export async function logout() {
  try {
    await api.post('/user/logout', {}, { silent: true }).then(unwrap)
  } catch {
    // 忽略，继续本地清理
  }
  setSessionUser(null)
}

export function currentUserId() {
  const user = currentUser()
  if (!user?.phone) {
    throw new Error('NOT_LOGGED_IN')
  }
  return user.phone
}
