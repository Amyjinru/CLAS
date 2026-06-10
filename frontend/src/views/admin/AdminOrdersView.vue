<script setup>
import { onMounted } from 'vue'
import { listAdminOrders } from '../../api/admin'
import { ElMessage } from 'element-plus'
import MoneyText from '../../components/MoneyText.vue'
import StatusTag from '../../components/StatusTag.vue'
import { useTableQuery } from '../../composables/useTableQuery'
import { formatDateTime } from '../../utils/formatters'
import { orderStatusMap } from '../../utils/status'

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待接单', value: 'PAID' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '退款中', value: 'REFUND_REQUESTED' },
  { label: '已退款', value: 'REFUNDED' }
]

const {
  rows: orders,
  total,
  page,
  size,
  loading,
  filters,
  load: loadOrders,
  search,
  reset,
  onPageChange
} = useTableQuery(listAdminOrders, {
  filters: {
    status: '',
    keyword: '',
    dateRange: []
  },
  params: ({ page, size, filters }) => ({
    page,
    size,
    status: filters.status || undefined,
    keyword: filters.keyword.trim() || undefined,
    startDate: filters.dateRange?.[0],
    endDate: filters.dateRange?.[1]
  }),
  rows: (data) => data.records,
  total: (data) => data.total
})

async function load() {
  try {
    await loadOrders()
  } catch {
    ElMessage.error('加载订单列表失败')
  }
}

function resetFilters() {
  reset({ status: '', keyword: '', dateRange: [] }).catch(() => ElMessage.error('加载订单列表失败'))
}

function exportCSV() {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim())
  if (filters.dateRange && filters.dateRange[0]) params.set('startDate', filters.dateRange[0])
  if (filters.dateRange && filters.dateRange[1]) params.set('endDate', filters.dateRange[1])
  window.open('/api/admin/export/orders?' + params.toString(), '_blank')
}

function handleSearch() {
  search().catch(() => ElMessage.error('加载订单列表失败'))
}

function handlePageChange(value) {
  onPageChange(value).catch(() => ElMessage.error('加载订单列表失败'))
}

onMounted(() => load().catch(() => ElMessage.error('加载订单列表失败')))
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <h1>订单管理</h1>
        <p>按状态、时间和关键词查看平台订单；此页面仅用于运营观察。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </section>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索订单号、用户、商家或地址" style="max-width: 300px" @keyup.enter="handleSearch" />
        <el-select v-model="filters.status" placeholder="订单状态" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <el-button @click="exportCSV">导出 CSV</el-button>
      </div>

      <el-table :data="orders" stripe v-loading="loading" size="small" empty-text="暂无匹配订单">
        <el-table-column prop="id" label="订单号" width="90" />
        <el-table-column prop="userId" label="用户" min-width="120" show-overflow-tooltip />
        <el-table-column prop="merchantId" label="商家ID" width="90" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><MoneyText :amount="row.totalPrice" /></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" :map="orderStatusMap" /></template>
        </el-table-column>
        <el-table-column prop="deliveryAddress" label="配送地址" min-width="190" show-overflow-tooltip />
        <el-table-column prop="rejectReason" label="拒单原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="refundStatus" label="退款状态" width="110">
          <template #default="{ row }">{{ row.refundStatus || '-' }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
        class="pager"
      />
    </el-card>
  </div>
</template>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.page-head { align-items: flex-end; display: flex; justify-content: space-between; }
.page-head h1 { font-size: 22px; margin: 0 0 6px; }
.page-head p { color: var(--text-secondary); margin: 0; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.pager { justify-content: flex-end; margin-top: 16px; }
</style>
