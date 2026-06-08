import { api, unwrap } from './client'

export const login = (payload) => api.post('/user/login', payload).then(unwrap)
export const register = (payload) => api.post('/user/register', payload).then(unwrap)
export const sendRegisterCode = (payload) => api.post('/user/register/send-code', payload).then(unwrap)
export const sendForgotPasswordCode = (payload) => api.post('/user/forgot-password/send-code', payload).then(unwrap)
export const resetForgotPassword = (payload) => api.post('/user/forgot-password/reset', payload).then(unwrap)
