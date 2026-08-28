<script setup>
import { onMounted, ref } from 'vue'
import { exportAdminOrders, getAdminOrderTimeline, listAdminOrders } from '../../api/admin'
import { ElMessage } from 'element-plus'
import MoneyText from '../../components/MoneyText.vue'
import StatusTag from '../../components/StatusTag.vue'
import { useTableQuery } from '../../composables/useTableQuery'
import { formatDateTime } from '../../utils/formatters'
import { orderStatusMap } from '../../utils/status'

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待商家接单', value: 'PAID' },
  { label: '履约中', value: 'ACCEPTED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '退款中', value: 'REFUND_PENDING' },
  { label: '已退款', value: 'REFUNDED' }
]

const deliveryStatusOptions = [
  { label: '全部配送进度', value: '' }, { label: '待商家处理', value: 'WAITING' },
  { label: '制作中', value: 'PREPARING' }, { label: '待骑手接单', value: 'AVAILABLE' },
  { label: '待取餐', value: 'ASSIGNED_WAITING_MEAL' }, { label: '配送中', value: 'DELIVERING' }, { label: '已送达', value: 'DELIVERED' }
]
const deliveryLabel = Object.fromEntries(deliveryStatusOptions.map((item) => [item.value, item.label]))
const selectedOrder = ref(null)
const timeline = ref([])
const timelineLoading = ref(false)
const timelineVisible = ref(false)

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
    deliveryStatus: '',
    keyword: '',
    dateRange: []
  },
  params: ({ page, size, filters }) => ({
    page,
    size,
    status: filters.status || undefined,
    deliveryStatus: filters.deliveryStatus || undefined,
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
  reset({ status: '', deliveryStatus: '', keyword: '', dateRange: [] }).catch(() => ElMessage.error('加载订单列表失败'))
}

async function exportCSV() {
  try {
    await exportAdminOrders({
      status: filters.status || undefined,
      deliveryStatus: filters.deliveryStatus || undefined,
      keyword: filters.keyword.trim() || undefined,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1]
    })
    ElMessage.success('CSV 导出成功')
  } catch {
    ElMessage.error('CSV 导出失败')
  }
}

async function openTimeline(order) {
  selectedOrder.value = order
  timelineVisible.value = true
  timeline.value = []
  timelineLoading.value = true
  try { timeline.value = await getAdminOrderTimeline(order.id) } catch { ElMessage.error('加载订单流程记录失败') } finally { timelineLoading.value = false }
}

const lifecycleLabel = { ORDER_CREATED: '订单创建', PAYMENT_SUCCEEDED: '支付成功', MERCHANT_ACCEPTED: '商家接单，开始制作', MERCHANT_READY_FOR_DISPATCH: '餐品制作完成，发布配送', RIDER_CLAIMED: '骑手接单', RIDER_PICKED_UP: '骑手取餐', RIDER_DELIVERED: '骑手送达', USER_CONFIRMED_RECEIPT: '用户确认收货', MERCHANT_REVIEWED: '完成商家评价', RIDER_REVIEWED: '完成骑手评价', ORDER_CANCELED: '订单取消', MERCHANT_REJECTED: '商家拒单', REFUND_REQUESTED: '用户申请退款', REFUND_REJECTED: '商家提交退款处理意见', REFUND_APPROVED: '退款已通过', REFUND_DISPUTE_SUBMITTED: '提交平台退款争议', REFUND_DISPUTE_AUTO_SUBMITTED: '退款自动转入平台审核', REFUND_DISPUTE_REJECTED: '平台驳回退款争议' }

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
        <el-select v-model="filters.deliveryStatus" placeholder="配送进度" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="item in deliveryStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column label="配送进度" width="130"><template #default="{ row }">{{ deliveryLabel[row.deliveryStatus] || row.deliveryStatus || '-' }}</template></el-table-column>
        <el-table-column prop="riderId" label="骑手" width="120"><template #default="{ row }">{{ row.riderId || '-' }}</template></el-table-column>
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
        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openTimeline(row)">详细信息</el-button></template></el-table-column>
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
    <el-dialog v-model="timelineVisible" :title="selectedOrder ? `订单 #${selectedOrder.id} 详细信息` : '订单详细信息'" width="min(620px, 92vw)">
      <el-timeline v-loading="timelineLoading"><el-timeline-item v-for="event in timeline" :key="event.id" :timestamp="formatDateTime(event.createdAt)"><strong>{{ lifecycleLabel[event.eventType] || event.eventType }}</strong><p v-if="event.remark">{{ event.remark }}</p><small>{{ event.actorRole }}</small></el-timeline-item></el-timeline>
      <el-empty v-if="!timelineLoading && !timeline.length" description="暂无流程记录" />
    </el-dialog>
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
