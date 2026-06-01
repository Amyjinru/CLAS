<script setup>
import { reactive, ref } from 'vue'
import { login } from '../api/clas'

const form = reactive({ username: 'user', password: '123456' })
const message = ref('')

async function submit() {
  try {
    const data = await login(form)
    localStorage.setItem('clas_user', JSON.stringify(data.user))
    message.value = `已登录：${data.user.username} (${data.user.role})`
  } catch (error) {
    message.value = error.response?.data?.message || '登录失败'
  }
}
</script>

<template>
  <section class="panel narrow">
    <h1>登录</h1>
    <label>账号<input v-model="form.username" /></label>
    <label>密码<input v-model="form.password" type="password" /></label>
    <button @click="submit">登录</button>
    <p>{{ message }}</p>
  </section>
</template>
