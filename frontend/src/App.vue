<script setup>
import { onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout, sessionUser } from './api/clas'
import { ElMessage } from 'element-plus'
import ChatSidebar from './components/ChatSidebar.vue'
import { preferenceState } from './utils/preferences'

const router = useRouter()
const route = useRoute()

// version_314: computed 管理用户状态与角色
const user = sessionUser
const role = computed(() => user.value?.role || null)
const welcomeName = computed(() => user.value?.nickname || user.value?.username || user.value?.phone || '')

const userAvatarInitial = computed(() => {
  const name = welcomeName.value.trim()
  if (!name) return 'U'
  return name.charAt(0).toUpperCase()
})

const userAvatarStyle = computed(() => {
  const avatar = user.value?.avatar
  if (!avatar) return {}
  return {
    backgroundImage: `url(${avatar})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center'
  }
})

function isPrimaryNavActive(targetPath) {
  const path = route.path
  if (targetPath === '/home') return path === '/home'
  if (targetPath === '/profile') return path === '/profile'
  if (targetPath === '/profile/notifications') return path.startsWith('/profile/notifications')
  if (targetPath === '/deals') return path === '/deals' || path.startsWith('/deals/')
  if (targetPath === '/bookings') return path === '/bookings' || path.startsWith('/bookings/')
  if (targetPath === '/settings') return path === '/settings' || path.startsWith('/settings/')
  return path === targetPath || path.startsWith(`${targetPath}/`)
}

// version_314: 按角色动态品牌链接
const brandLink = computed(() => {
  if (!user.value) return '/login'
  if (role.value === 'MERCHANT') return '/merchant-console'
  if (role.value === 'ADMIN') return '/admin/dashboard'
  return '/home'
})
const navLabels = {
  'zh-CN': ['外卖', '团购', '预订/到店', '消息', '个人中心', '设置'],
  en: ['Delivery', 'Deals', 'Booking', 'Messages', 'Profile', 'Settings']
}
const userPrimaryNav = computed(() => {
  const labels = navLabels[preferenceState.language] || navLabels['zh-CN']
  return [
    { label: labels[0], to: '/home' },
    { label: labels[1], to: '/deals' },
    { label: labels[2], to: '/bookings' },
    { label: labels[3], to: '/profile/notifications' },
    { label: labels[4], to: '/profile' },
    { label: labels[5], to: '/settings' }
  ]
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
    <a href="#main-content" class="skip-link">跳到主要内容</a>
    <header class="topbar">
      <div class="header-left">
        <RouterLink class="brand" :to="brandLink">CLAS 生活助手</RouterLink>
        <span v-if="user" class="user-welcome">
          <span class="user-avatar" :style="userAvatarStyle">{{ user.avatar ? '' : userAvatarInitial }}</span>
          <span class="user-welcome-text">
            欢迎, {{ welcomeName }}
            <el-tag size="small" type="info" class="role-tag">{{ user.role }}</el-tag>
          </span>
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
          <RouterLink
            v-for="item in userPrimaryNav"
            :key="item.to"
            class="primary-nav-link"
            :class="{ 'nav-active': isPrimaryNavActive(item.to) }"
            :to="item.to"
          >
            {{ item.label }}
          </RouterLink>
          <a href="#" @click.prevent="handleLogout" class="logout-link">退出</a>
        </template>

        <!-- ===== MERCHANT 商家 ===== -->
        <template v-else-if="role === 'MERCHANT'">
          <RouterLink to="/merchant-console">商家工作台</RouterLink>
          <RouterLink to="/merchant/info">商家信息</RouterLink>
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
    <main id="main-content" class="main-content">
      <RouterView v-slot="{ Component, route: viewRoute }">
        <KeepAlive include="HomeView">
          <component
            :is="Component"
            :key="viewRoute.path === '/home' ? 'HomeView' : viewRoute.fullPath"
          />
        </KeepAlive>
      </RouterView>
    </main>
    <ChatSidebar v-if="sessionUser" />
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
  gap: 12px;
  padding-left: 18px;
  border-left: 1px solid var(--border-color);
}

.user-avatar {
  align-items: center;
  background: linear-gradient(135deg, var(--color-accent), var(--clas-teal-700));
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 1px var(--border-color);
  color: #fff;
  display: flex;
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 800;
  height: 36px;
  justify-content: center;
  width: 36px;
}

.user-welcome-text {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.role-tag {
  font-weight: 600;
  letter-spacing: 0.03em;
}

nav {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

nav a {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  padding: 7px 16px;
  border-radius: var(--radius-sm);
  transition-property: color, background-color;
  transition-duration: var(--transition-fast);
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.02em;
}

nav a:hover {
  color: var(--color-primary);
  background-color: var(--color-primary-light);
}

nav a.router-link-active:not(.primary-nav-link) {
  color: var(--color-primary);
  background-color: var(--color-primary-light);
}

.logout-link {
  color: var(--clas-danger) !important;
  font-weight: 600;
}
.logout-link:hover {
  background-color: var(--clas-danger-light) !important;
}

.primary-nav-link {
  border: 2px solid transparent;
  color: var(--text-primary);
  font-weight: 600;
}

.primary-nav-link.nav-active {
  background-color: var(--color-primary-soft);
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(249, 115, 22, 0.14);
  color: var(--color-primary);
  font-weight: 800;
}

.nav-divider {
  background: var(--border-color);
  height: 20px;
  margin: 0 4px;
  width: 1px;
}

.main-content {
  flex: 1;
}

@media (max-width: 980px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    height: auto;
    padding: 12px 18px;
  }

  .header-left {
    flex-wrap: wrap;
  }

  nav {
    justify-content: flex-start;
    width: 100%;
  }

  nav a {
    padding: 7px 10px;
  }
}

@media (max-width: 640px) {
  .user-welcome {
    border-left: 0;
    padding-left: 0;
  }

  .nav-divider {
    display: none;
  }
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
