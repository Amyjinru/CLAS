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
.admin-shell {
  display: flex;
  min-height: calc(100vh - 64px);
  background-color: #f0f2f5;
}

.admin-sidebar {
  width: 220px;
  background-color: #304156;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.sidebar-header h2 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
}

.admin-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
