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
  ADMIN: '/admin/dashboard'
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
.panel {
  max-width: 400px;
  margin: 60px auto;
  padding: 40px;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
}

.auth-tabs {
  display: grid;
  gap: 8px;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 16px;
}

.auth-tabs button {
  background: var(--bg-page, #eef2f7);
  color: var(--text-secondary, #475467);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.auth-tabs button.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.hint {
  color: var(--text-secondary);
  margin: 0 0 8px;
}
h1 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 8px 0;
  color: var(--text-primary);
  letter-spacing: 0.04em;
}
.hint {
  color: var(--text-secondary);
  margin: 0 0 4px;
  font-size: 14px;
}
.hint-list {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0 0 24px;
  padding-left: 18px;
  line-height: 1.8;
}
.hint-list li::marker {
  color: var(--color-primary);
}
label {
  margin: 16px 0;
  font-size: 14px;
  color: var(--text-secondary);
}
input {
  margin-top: 6px;
}
button {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
}
p {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
