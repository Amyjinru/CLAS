<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  active: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['edit-profile'])
const router = useRouter()

const items = [
  { key: 'orders', label: '接单管理', icon: 'List', path: '/merchant-console' },
  { key: 'analytics', label: '经营分析', icon: 'TrendCharts', path: '/merchant/analytics' },
  { key: 'products', label: '商品管理', icon: 'Goods', path: '/merchant/products' },
  { key: 'deals', label: '团购管理', icon: 'Ticket', path: '/merchant/deals' }
]
</script>

<template>
  <el-card class="nav-card">
    <div class="menu-list">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="menu-item"
        :class="{ active: props.active === item.key }"
        @click="router.push(item.path)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
      <button type="button" class="menu-item" @click="emit('edit-profile')">
        <el-icon><Edit /></el-icon>
        <span>信息修改</span>
      </button>
    </div>
  </el-card>
</template>

<style scoped>
.nav-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 8px;
  color: #606266;
  cursor: pointer;
  display: flex;
  font-size: 15px;
  font-weight: 500;
  gap: 12px;
  padding: 12px 16px;
  text-align: left;
  transition-property: background-color, color, border-color;
  transition-duration: 0.3s;
  transition-timing-function: ease;
  width: 100%;
}

.menu-item:hover {
  background-color: #f0f7ff;
  color: #409eff;
}

.menu-item.active {
  background-color: #409eff;
  color: #ffffff;
}
</style>
