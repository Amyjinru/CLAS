<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logout, sessionUser } from './api/clas'

const router = useRouter()
const route = useRoute()

const user = sessionUser
const role = computed(() => user.value?.role || null)

const brandLink = computed(() => {
  if (!user.value) return '/login'
  if (role.value === 'MERCHANT') return '/merchant-console'
  if (role.value === 'ADMIN') return '/admin/announcements'
  return '/home'
})

function handleLogout() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <RouterLink class="brand" :to="brandLink">CLAS</RouterLink>
      <nav>
        <template v-if="!user">
          <RouterLink to="/login">登录</RouterLink>
        </template>

        <template v-else-if="role === 'USER'">
          <RouterLink to="/home">首页</RouterLink>
          <RouterLink to="/orders">我的订单</RouterLink>
          <RouterLink to="/user/announcements">平台公告</RouterLink>
          <span class="nav-user">{{ user.username }}</span>
          <button class="nav-logout" type="button" @click="handleLogout">退出</button>
        </template>

        <template v-else-if="role === 'MERCHANT'">
          <RouterLink to="/merchant-console">商家工作台</RouterLink>
          <RouterLink to="/merchant/announcements">平台公告</RouterLink>
          <span class="nav-user">{{ user.username }}</span>
          <button class="nav-logout" type="button" @click="handleLogout">退出</button>
        </template>

        <template v-else-if="role === 'ADMIN'">
          <RouterLink to="/admin/announcements">公告管理</RouterLink>
          <span class="nav-user">{{ user.username }}</span>
          <button class="nav-logout" type="button" @click="handleLogout">退出</button>
        </template>
      </nav>
    </header>
    <main>
      <RouterView :key="route.fullPath" />
    </main>
  </div>
</template>

<style scoped>
.nav-user {
  color: #667085;
  font-size: 14px;
}

.nav-logout {
  background: none;
  border: 0;
  color: #dc2626;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 400;
  min-height: auto;
  padding: 0;
}
</style>
