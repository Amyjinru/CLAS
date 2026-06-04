<script setup>
import { ref, onMounted } from 'vue'
import { api, unwrap } from '../../api/client'
import { ElMessage } from 'element-plus'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const filterStatus = ref('')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '已支付', value: 'PAID' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '已完成', value: 'COMPLETED' }
]

const statusTagTypes = {
  PENDING_PAYMENT: 'warning', PAID: 'primary',
  ACCEPTED: 'info', COMPLETED: 'success'
}
const statusLabels = {
  PENDING_PAYMENT: '待支付', PAID: '已支付',
  ACCEPTED: '已接单', COMPLETED: '已完成'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterStatus.value) params.status = filterStatus.value
    const data = await api.get('/admin/orders', { params }).then(unwrap)
    orders.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

function formatFen(v) {
  return v ? (v / 100).toFixed(2) : '0.00'
}

function formatTime(t) {
  return t ? new Date(t).toLocaleString('zh-CN') : '-'
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <h1 class="page-title">订单管理</h1>
    <el-card shadow="hover">
      <div class="toolbar">
        <el-select v-model="filterStatus" placeholder="订单状态" clearable @change="onFilterChange" style="width: 160px;">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </div>
      <el-table :data="orders" stripe v-loading="loading" size="small">
        <el-table-column prop="id" label="订单号" width="80" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="merchantId" label="商家ID" width="80" />
        <el-table-column label="金额(元)" width="100">
          <template #default="{ row }">¥{{ formatFen(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagTypes[row.status]" size="small">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin: 0 0 20px 0; }
.toolbar { margin-bottom: 16px; }
</style>
