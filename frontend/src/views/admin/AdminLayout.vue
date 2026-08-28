<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { sessionUser } from '../../api/clas'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: 'DataAnalysis' },
  { path: '/admin/orders',     label: '订单管理', icon: 'Document' },
  { path: '/admin/order-refund-disputes', label: '订单争议', icon: 'WarningFilled' },
  { path: '/admin/users',      label: '用户管理', icon: 'User' },
  { path: '/admin/audit',      label: '商家审核', icon: 'Checked' },
  { path: '/admin/riders',     label: '骑手运营', icon: 'Bicycle' },
  { path: '/admin/reviews',    label: '评价管理', icon: 'Star' },
  { path: '/admin/appeals',    label: '申诉管理', icon: 'ChatLineSquare' },
  { path: '/admin/announcements', label: '公告管理', icon: 'Notification' },
  { path: '/admin/messages', label: '信息管理', icon: 'ChatDotRound' }
]

const activeMenu = computed(() => route.path)

function navigateTo(path) {
  router.push(path)
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <h2>管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="navigateTo"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
/* ═══════════════ 管理后台侧边栏 —「暖食」主题（固定定位） ═══════════════ */
.admin-shell {
  display: flex;
  min-height: calc(100vh - 64px);
  background-color: var(--bg-page);
}

.admin-sidebar {
  width: 220px;
  background: linear-gradient(180deg, #2d251c 0%, #1a1510 100%);
  flex-shrink: 0;
  position: fixed;
  top: 64px;
  left: 0;
  bottom: 0;
  overflow-y: auto;
  z-index: 100;
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.08);
}

.sidebar-header {
  padding: 26px 20px 20px 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-header h2 {
  color: var(--text-inverse);
  font-size: 17px;
  font-weight: 700;
  margin: 0;
  letter-spacing: 0.08em;
}

.sidebar-menu {
  border-right: none !important;
  padding-top: 8px;
}
.sidebar-menu .el-menu-item {
  margin: 2px 10px;
  border-radius: var(--radius-sm);
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.03em;
  transition-property: background-color, color, border-color;
  transition-duration: var(--transition-fast);
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--text-sidebar);
}
.sidebar-menu .el-menu-item:hover {
  background-color: var(--bg-sidebar-hover) !important;
  color: var(--text-inverse) !important;
}
.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.2), rgba(249, 115, 22, 0.08)) !important;
  color: var(--text-sidebar-active) !important;
  font-weight: 600;
  border-left: 3px solid var(--color-primary);
}

.admin-main {
  flex: 1;
  margin-left: 220px;
  padding: 28px 32px;
  overflow-y: auto;
  min-height: calc(100vh - 64px);
  background: var(--bg-page);
}

@media (max-width: 900px) {
  .admin-shell {
    flex-direction: column;
  }

  .admin-sidebar {
    bottom: auto;
    display: flex;
    left: auto;
    overflow: visible;
    position: static;
    top: auto;
    width: 100%;
  }

  .sidebar-header {
    align-items: center;
    border-bottom: 0;
    display: flex;
    flex: 0 0 auto;
    padding: 14px 18px;
  }

  .sidebar-menu {
    display: flex;
    flex: 1;
    min-width: 0;
    overflow-x: auto;
    padding: 8px 10px;
  }

  .sidebar-menu .el-menu-item {
    flex: 0 0 auto;
    margin: 0 3px;
  }

  .sidebar-menu .el-menu-item.is-active {
    border-bottom: 3px solid var(--color-primary);
    border-left: 0;
  }

  .admin-main {
    margin-left: 0;
    min-height: auto;
    padding: 22px 18px 36px;
  }
}

@media (max-width: 560px) {
  .admin-sidebar {
    align-items: stretch;
    flex-direction: column;
  }

  .sidebar-header {
    padding-bottom: 4px;
  }

  .sidebar-menu {
    padding-top: 4px;
  }
}
</style>
