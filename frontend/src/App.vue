<script setup>
import { onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout, sessionUser } from './api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// version_314: computed 管理用户状态与角色
const user = sessionUser
const role = computed(() => user.value?.role || null)

// version_314: 按角色动态品牌链接
const brandLink = computed(() => {
  if (!user.value) return '/login'
  if (role.value === 'MERCHANT') return '/merchant-console'
  if (role.value === 'ADMIN') return '/admin/dashboard'
  return '/home'
})

function updateUser() {
  // sessionUser 为响应式 computed，路由变化时自动更新
}

// test1: 监听路由变化以确保顶栏用户信息同步
watch(() => route.path, updateUser)
onMounted(updateUser)

// version_314: logout() 接口退出 + test1: ElMessage 提示
async function handleLogout() {
  try {
    await logout()
    ElMessage.success('已安全退出登录')
  } catch {
    ElMessage.error('退出失败，请重试')
  }
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="header-left">
        <RouterLink class="brand" :to="brandLink">CLAS 生活助手</RouterLink>
        <span v-if="user" class="user-welcome">
          欢迎, {{ user.username }}
          <el-tag size="small" type="info" class="role-tag">{{ user.role }}</el-tag>
        </span>
      </div>
      <nav>
        <!-- 所有用户可见 -->
        <RouterLink to="/home">浏览商家</RouterLink>

        <!-- 已登录用户 -->
        <template v-if="user">
          <RouterLink to="/cart">购物车</RouterLink>
          <RouterLink to="/orders">我的订单</RouterLink>
          <!-- version_314: 平台公告入口，按角色显隐 -->
          <RouterLink v-if="role === 'USER'" to="/user/announcements">平台公告</RouterLink>
          <RouterLink v-if="role === 'ADMIN'" to="/admin/announcements">公告管理</RouterLink>
          <RouterLink v-if="role === 'MERCHANT'" to="/merchant/announcements">平台公告</RouterLink>
          <RouterLink v-if="role === 'MERCHANT'" to="/merchant-console">商家工作台</RouterLink>
          <RouterLink v-if="role === 'USER'" to="/merchant-register">商家入驻</RouterLink>
          <RouterLink v-if="role === 'ADMIN'" to="/admin-audit">商家审核</RouterLink>
          <a href="#" @click.prevent="handleLogout" class="logout-link">退出</a>
        </template>

        <!-- 未登录访客 -->
        <template v-else>
          <RouterLink to="/merchant-register">商家入驻</RouterLink>
          <RouterLink to="/login">登录</RouterLink>
        </template>
      </nav>
    </header>
    <main class="main-content">
      <RouterView :key="route.fullPath" />
    </main>
  </div>
</template>

<style scoped>
/* ===== test1 完整布局样式 ===== */
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

/* ===== version_314 独有样式 ===== */
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
