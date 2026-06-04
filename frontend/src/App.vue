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
        <!-- ===== 未登录访客 ===== -->
        <template v-if="!user">
          <RouterLink to="/home">浏览商家</RouterLink>
          <RouterLink to="/merchant-register">商家入驻</RouterLink>
          <RouterLink to="/login">登录</RouterLink>
        </template>

        <!-- ===== USER 普通用户 ===== -->
        <template v-else-if="role === 'USER'">
          <RouterLink to="/home">浏览商家</RouterLink>
          <RouterLink to="/cart">购物车</RouterLink>
          <RouterLink to="/orders">我的订单</RouterLink>
          <RouterLink to="/user/announcements">平台公告</RouterLink>
          <RouterLink to="/merchant-register">商家入驻</RouterLink>
          <a href="#" @click.prevent="handleLogout" class="logout-link">退出</a>
        </template>

        <!-- ===== MERCHANT 商家 ===== -->
        <template v-else-if="role === 'MERCHANT'">
          <RouterLink to="/merchant-console">商家工作台</RouterLink>
          <RouterLink to="/merchant/announcements">平台公告</RouterLink>
          <a href="#" @click.prevent="handleLogout" class="logout-link">退出</a>
        </template>

        <!-- ===== ADMIN 管理员 ===== -->
        <template v-else-if="role === 'ADMIN'">
          <RouterLink to="/admin/dashboard">管理后台</RouterLink>
          <RouterLink to="/admin/audit">商家审核</RouterLink>
          <RouterLink to="/admin/announcements">公告管理</RouterLink>
          <a href="#" @click.prevent="handleLogout" class="logout-link">退出</a>
        </template>
      </nav>
    </header>
    <main class="main-content">
      <RouterView :key="route.fullPath" />
    </main>
  </div>
</template>

<style scoped>
/* ═══════════════ 顶栏 —「暖食」主题 ═══════════════ */
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-page);
  font-family: var(--font-body);
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  padding: 0 32px;
  height: 64px;
  box-shadow: 0 1px 0 var(--border-color), 0 2px 8px rgba(45, 37, 28, 0.04);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand {
  font-size: 21px;
  font-weight: 900;
  color: var(--color-primary) !important;
  text-decoration: none !important;
  letter-spacing: 0.05em;
  transition: color var(--transition-fast);
}
.brand:hover {
  color: var(--color-primary-hover) !important;
}

.user-welcome {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 18px;
  border-left: 1px solid var(--border-color);
}

.role-tag {
  font-weight: 600;
  letter-spacing: 0.03em;
}

nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

nav a {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  padding: 7px 16px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  letter-spacing: 0.02em;
}

nav a:hover,
nav a.router-link-active {
  color: var(--color-primary);
  background-color: var(--color-primary-light);
}

.logout-link {
  color: var(--clas-danger) !important;
  margin-left: 4px;
  font-weight: 600;
}
.logout-link:hover {
  background-color: var(--clas-danger-light) !important;
}

.main-content {
  flex: 1;
}

/* version_314 兼容 */
.nav-user { color: var(--text-secondary); font-size: 14px; }
.nav-logout {
  background: none;
  border: 0;
  color: var(--clas-danger);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  min-height: auto;
  padding: 0;
}
</style>
