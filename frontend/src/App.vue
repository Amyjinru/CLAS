<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { currentUser } from './api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const user = ref(null)

function updateUser() {
  user.value = currentUser()
}

// Watch for route changes to update user info in topbar automatically
watch(() => route.path, updateUser)

onMounted(updateUser)

function handleLogout() {
  localStorage.removeItem('clas_user')
  user.value = null
  ElMessage.success('已安全退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="header-left">
        <RouterLink class="brand" to="/home">CLAS 生活助手</RouterLink>
        <span v-if="user" class="user-welcome">
          欢迎, {{ user.username }} 
          <el-tag size="small" type="info" class="role-tag">{{ user.role }}</el-tag>
        </span>
      </div>
      <nav>
        <RouterLink to="/home">浏览商家</RouterLink>
        
        <!-- Only visible when logged in -->
        <template v-if="user">
          <RouterLink to="/cart">购物车</RouterLink>
          <RouterLink to="/orders">我的订单</RouterLink>
          <RouterLink v-if="user.role === 'MERCHANT'" to="/merchant-console">商家工作台</RouterLink>
          <RouterLink v-if="user.role === 'USER'" to="/merchant-register">商家入驻</RouterLink>
          <RouterLink v-if="user.role === 'ADMIN'" to="/admin-audit">商家审核</RouterLink>
          <a href="#" @click.prevent="handleLogout" class="logout-link">退出</a>
        </template>
        
        <!-- Visible when visitor -->
        <template v-else>
          <RouterLink to="/merchant-register">商家入驻</RouterLink>
          <RouterLink to="/login">登录</RouterLink>
        </template>
      </nav>
    </header>
    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style>
/* Global CSS variables or styles can be adjusted here */
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  padding: 0 40px;
  height: 64px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand {
  font-size: 20px;
  font-weight: 800;
  color: #409eff !important;
  text-decoration: none !important;
}

.user-welcome {
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-tag {
  font-weight: bold;
}

nav {
  display: flex;
  align-items: center;
  gap: 20px;
}

nav a {
  text-decoration: none;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

nav a:hover, nav a.router-link-active {
  color: #409eff;
  background-color: #ecf5ff;
}

.logout-link {
  color: #f56c6c !important;
}

.logout-link:hover {
  background-color: #fef0f0 !important;
}

.main-content {
  flex: 1;
  padding: 20px 0;
}
</style>
