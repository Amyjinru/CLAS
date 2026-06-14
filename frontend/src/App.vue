<script setup>
import { onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout, sessionUser } from './api/clas'
import { ElMessage } from 'element-plus'
import ChatSidebar from './components/ChatSidebar.vue'
import { preferenceState } from './utils/preferences'
import patternBg from './assets/pattern-bg.svg'
import foodLines from './assets/food-lines.svg'

const router = useRouter()
const route = useRoute()

// version_314: computed 管理用户状态与角色
const user = sessionUser
const role = computed(() => user.value?.role || null)
const welcomeName = computed(() => user.value?.nickname || user.value?.username || user.value?.phone || '')

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
  <div class="shell" :style="{ backgroundImage: `url(${patternBg})` }">
    <div class="shell-pattern-overlay" aria-hidden="true"></div>
    <img
      class="food-lines-layer"
      :src="foodLines"
      alt=""
      aria-hidden="true"
    />
    <header class="topbar">
      <div class="header-left">
        <RouterLink class="brand" :to="brandLink">CLAS 生活助手</RouterLink>
        <span v-if="user" class="user-welcome">
          欢迎, {{ welcomeName }}
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
          <RouterLink
            v-for="item in userPrimaryNav"
            :key="item.to"
            class="primary-nav-link"
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
      <RouterView v-slot="{ Component }">
        <Transition
          :name="route.meta.motion || 'page-lift'"
          mode="out-in"
          appear
        >
          <div
            :key="route.fullPath"
            class="route-stage"
          >
            <component :is="Component" />
          </div>
        </Transition>
      </RouterView>
    </main>
    <footer
      class="app-footer"
      aria-label="页脚装饰"
    >
      <div class="footer-overlay">
        <div class="footer-content">
          <span class="footer-brand">CLAS</span>
          <span class="footer-tagline">生活助手 · 温暖每一餐</span>
          <span class="footer-copy">&copy; {{ new Date().getFullYear() }} CLAS Team</span>
        </div>
      </div>
    </footer>
    <ChatSidebar v-if="sessionUser" />
  </div>
</template>

<style scoped>
/* ═══════════════ 顶栏 —「暖食」主题 ═══════════════ */
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  background-repeat: repeat;
  background-size: 360px 240px;
  background-attachment: fixed;
  background-color: #FFFBF5;
  font-family: var(--font-body);
}

/* 整页柔光遮罩 — 让图案融入背景，不干扰内容阅读 */
.shell-pattern-overlay {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    linear-gradient(135deg,
      rgba(255, 251, 245, 0.45) 0%,
      rgba(255, 248, 240, 0.60) 30%,
      rgba(255, 245, 235, 0.70) 60%,
      rgba(255, 242, 230, 0.78) 100%
    );
}

/* ═══════════ 水波流动动效 ═══════════ */
@keyframes rippleFlow1 {
  0%   { opacity: 0.15; transform: scale(1.0) translateY(0); }
  25%  { opacity: 0.35; transform: scale(1.08) translateY(-4px); }
  50%  { opacity: 0.25; transform: scale(1.04) translateY(2px); }
  75%  { opacity: 0.38; transform: scale(1.10) translateY(-2px); }
  100% { opacity: 0.15; transform: scale(1.0) translateY(0); }
}
@keyframes rippleFlow2 {
  0%   { opacity: 0.12; transform: scale(0.97) translateY(0); }
  30%  { opacity: 0.30; transform: scale(1.06) translateY(3px); }
  60%  { opacity: 0.20; transform: scale(1.02) translateY(-3px); }
  100% { opacity: 0.12; transform: scale(0.97) translateY(0); }
}
@keyframes rippleFlow3 {
  0%   { opacity: 0.10; transform: scale(0.94) translateY(0); }
  20%  { opacity: 0.28; transform: scale(1.05) translateY(-5px); }
  50%  { opacity: 0.18; transform: scale(1.0) translateY(3px); }
  80%  { opacity: 0.32; transform: scale(1.07) translateY(-1px); }
  100% { opacity: 0.10; transform: scale(0.94) translateY(0); }
}

/* 水波涟漪元素 — 通过 SVG 中的渐变圆 + CSS 缩放产生流动感 */
.ripple-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-repeat: repeat;
  background-size: 360px 240px;
  background-attachment: fixed;
  opacity: 0.4;
  mix-blend-mode: overlay;
}

/* 在 shell 上叠加一层 */
.shell::after {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image: url('../assets/pattern-bg.svg');
  background-repeat: repeat;
  background-size: 360px 240px;
  background-attachment: fixed;
  opacity: 0.18;
  animation: rippleFlow1 8s ease-in-out infinite;
  filter: blur(3px);
}

/* ═══════════ 食物剪影动画层 ═══════════ */
.food-lines-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.85;
}

/* 暗色模式 — 降低食物线条亮度 */
:root[data-theme='dark'] .food-lines-layer {
  opacity: 0.45;
  filter: brightness(0.8);
}

/* 暗色模式 — 整页遮罩 */
:root[data-theme='dark'] .shell-pattern-overlay {
  background:
    linear-gradient(135deg,
      rgba(30, 27, 24, 0.60) 0%,
      rgba(30, 27, 24, 0.75) 30%,
      rgba(30, 27, 24, 0.85) 60%,
      rgba(30, 27, 24, 0.92) 100%
    );
}

/* 所有交互元素置于遮罩之上 */
.topbar,
.main-content,
.chat-sidebar {
  position: relative;
  z-index: 2;
}
.app-footer {
  position: relative;
  z-index: 0;
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
  flex-wrap: wrap;
  gap: 4px;
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

.primary-nav-link {
  color: var(--text-primary);
  font-weight: 700;
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
