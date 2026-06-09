import { ref } from 'vue'

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

export function logout() {
  setSessionUser(null)
}

export function currentUserId() {
  const user = currentUser()
  if (!user?.phone) {
    throw new Error('NOT_LOGGED_IN')
  }
  return user.phone
}
