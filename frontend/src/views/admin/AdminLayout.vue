<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { sessionUser } from '../../api/clas'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: 'DataAnalysis' },
  { path: '/admin/orders',     label: '订单管理', icon: 'Document' },
  { path: '/admin/users',      label: '用户管理', icon: 'User' },
  { path: '/admin/audit',      label: '商家审核', icon: 'Checked' },
  { path: '/admin/reviews',    label: '评价管理', icon: 'Star' },
  { path: '/admin/announcements', label: '公告管理', icon: 'Notification' }
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
/* ═══════════════ 管理后台侧边栏 —「暖食」主题 ═══════════════ */
.admin-shell {
  display: flex;
  min-height: calc(100vh - 64px);
  background-color: var(--bg-page);
}

.admin-sidebar {
  width: 220px;
  background: linear-gradient(180deg, #2d251c 0%, #1a1510 100%);
  flex-shrink: 0;
  overflow-y: auto;
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
  transition: all var(--transition-fast);
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
  padding: 28px 32px;
  overflow-y: auto;
  background: var(--bg-page);
}
</style>
