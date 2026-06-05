<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register, setSessionUser } from '../api/clas'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: 'user', password: '123456' })
const registerForm = reactive({ username: '', password: '', phone: '' })
const mode = ref('login')
const message = ref('')

const roleHome = {
  USER: '/home',
  MERCHANT: '/merchant-console',
  ADMIN: '/admin/announcements'
}

async function submit() {
  try {
    const data = await login(form)
    setSessionUser(data.user)
    message.value = `已登录：${data.user.username} (${data.user.role})`
    const redirect = route.query.redirect
    const target = typeof redirect === 'string' ? redirect : roleHome[data.user.role] || '/home'
    router.push(target)
  } catch (error) {
    message.value = error.response?.data?.message || '登录失败'
  }
}

async function submitRegister() {
  try {
    // 普通注册不传 role，后端会统一默认成 USER，避免前端伪造管理员或商家角色。
    const payload = {
      username: registerForm.username,
      password: registerForm.password,
      phone: registerForm.phone || null
    }
    const user = await register(payload)
    setSessionUser(user)
    message.value = `注册成功：${user.username}`
    router.push('/home')
  } catch (error) {
    message.value = error.response?.data?.message || '注册失败'
  }
}
</script>

<template>
  <section class="panel narrow">
    <div class="auth-tabs">
      <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
      <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册账号</button>
    </div>

    <template v-if="mode === 'login'">
      <h1>登录</h1>
      <p class="hint">不同角色登录后进入对应端：</p>
      <ul class="hint-list">
        <li>用户端：user / 123456</li>
        <li>商家端：merchant / 123456</li>
        <li>管理端：admin / 123456</li>
      </ul>
      <label>账号<input v-model="form.username" /></label>
      <label>密码<input v-model="form.password" type="password" /></label>
      <button @click="submit">登录</button>
    </template>

    <template v-else>
      <h1>注册账号</h1>
      <p class="hint">注册后默认成为普通用户，可进入首页浏览商家。</p>
      <label>账号<input v-model="registerForm.username" placeholder="请输入账号" /></label>
      <label>密码<input v-model="registerForm.password" type="password" placeholder="请输入密码" /></label>
      <label>手机号<input v-model="registerForm.phone" placeholder="可选，不能与已有账号重复" /></label>
      <button @click="submitRegister">注册并登录</button>
    </template>

    <p>{{ message }}</p>
  </section>
</template>

<style scoped>
.auth-tabs {
  display: grid;
  gap: 8px;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 16px;
}

.auth-tabs button {
  background: #eef2f7;
  color: #475467;
}

.auth-tabs button.active {
  background: #2563eb;
  color: #fff;
}

.hint {
  color: #667085;
  margin: 0 0 8px;
}

.hint-list {
  color: #667085;
  font-size: 14px;
  margin: 0 0 16px;
  padding-left: 18px;
}
</style>
