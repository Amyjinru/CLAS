<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMerchantBookings, updateBookingStatus } from '../api/clas'

const bookings = ref([])
const loading = ref(false)

const statusMap = {
  PENDING: { text: '待确认', type: 'warning' },
  CONFIRMED: { text: '已确认', type: 'success' },
  CANCELED: { text: '已取消', type: 'info' },
  COMPLETED: { text: '已完成', type: 'primary' }
}

function formatTime(value) {
  if (!value) return '未设置'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    bookings.value = await listMerchantBookings()
  } finally {
    loading.value = false
  }
}

async function setStatus(id, status) {
  await updateBookingStatus(id, status)
  ElMessage.success('预约状态已更新')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <div class="booking-header">
      <div>
        <p>预约管理</p>
        <h1>处理用户到店服务申请</h1>
      </div>
      <el-button type="primary" plain :loading="loading" @click="load">刷新</el-button>
    </div>
    <div class="list">
      <article class="row merchant-booking-row" v-for="booking in bookings" :key="booking.id">
        <div>
          <div class="booking-title">
            <h2>{{ booking.serviceName }}</h2>
            <el-tag :type="statusMap[booking.status]?.type || 'info'">
              {{ statusMap[booking.status]?.text || booking.status }}
            </el-tag>
          </div>
          <p>用户：{{ booking.userId }} · {{ formatTime(booking.appointmentTime) }}</p>
          <p>联系电话：{{ booking.contactPhone }}</p>
          <p v-if="booking.note">备注：{{ booking.note }}</p>
        </div>
        <div class="row-actions">
          <el-button v-if="booking.status === 'PENDING'" type="success" @click="setStatus(booking.id, 'CONFIRMED')">
            确认
          </el-button>
          <el-button v-if="booking.status === 'CONFIRMED'" type="primary" @click="setStatus(booking.id, 'COMPLETED')">
            完成
          </el-button>
          <el-button
            v-if="booking.status !== 'CANCELED' && booking.status !== 'COMPLETED'"
            type="danger"
            plain
            @click="setStatus(booking.id, 'CANCELED')"
          >
            取消
          </el-button>
        </div>
      </article>
      <el-empty v-if="bookings.length === 0" description="暂无预约申请" />
    </div>
  </section>
</template>

<style scoped>
.booking-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.booking-header p {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 800;
  margin: 0 0 6px;
}
.booking-header h1 {
  font-size: 24px;
  margin: 0;
}
.booking-title {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.merchant-booking-row {
  align-items: flex-start;
}
@media (max-width: 768px) {
  .booking-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
