<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, setSessionUser } from '../api/clas'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: 'user', password: '123456' })
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
</script>

<template>
  <section class="panel narrow">
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
    <p>{{ message }}</p>
  </section>
</template>

<style scoped>
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
