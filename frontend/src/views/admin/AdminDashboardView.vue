<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, getMerchantRanking, getOrderStats, getSalesOverview, getTopProducts } from '../../api/admin'
import { ElMessage } from 'element-plus'

const today = new Date()
const start = new Date()
start.setDate(today.getDate() - 6)

const dateRange = ref([toDateInput(start), toDateInput(today)])
const stats = ref({
  totalUsers: 0,
  totalMerchants: 0,
  totalOrders: 0,
  totalSales: 0,
  todayOrders: 0,
  todaySales: 0,
  pendingPaymentOrders: 0,
  paidOrders: 0,
  completedOrders: 0
})
const orderStats = ref({ statusCounts: [], dailyOrders: [] })
const salesOverview = ref({ dailySales: [], totalSales: 0, monthlySales: 0, weeklySales: 0 })
const merchantRanking = ref({ bySales: [], byRating: [] })
const topProducts = ref({ products: [] })
const loading = ref(false)
const fullscreen = ref(false)
const clock = ref('')
let salesChart = null
let orderChart = null
let refreshTimer = null
let clockTimer = null

const rangeParams = computed(() => ({
  startDate: dateRange.value?.[0],
  endDate: dateRange.value?.[1]
}))

const hasSalesData = computed(() => (salesOverview.value.dailySales || []).some((item) => item.amount || item.orderCount))
const hasOrderStatusData = computed(() => (orderStats.value.statusCounts || []).some((item) => item.count))

const statCards = computed(() => [
  { label: '总订单数', value: stats.value.totalOrders, tone: 'orange' },
  { label: '总销售额', value: `¥${formatFen(stats.value.totalSales)}`, tone: 'teal' },
  { label: '总用户数', value: stats.value.totalUsers, tone: 'violet' },
  { label: '总商家数', value: stats.value.totalMerchants, tone: 'green' }
])

const todayCards = computed(() => [
  { label: '筛选区间订单', value: stats.value.todayOrders },
  { label: '筛选区间销售额', value: `¥${formatFen(stats.value.todaySales)}` },
  { label: '待支付订单', value: stats.value.pendingPaymentOrders },
  { label: '待接单订单', value: stats.value.paidOrders }
])

async function load() {
  loading.value = true
  try {
    const params = rangeParams.value
    const [dash, orders, sales, merchants, products] = await Promise.all([
      getDashboard(params),
      getOrderStats(params),
      getSalesOverview(params),
      getMerchantRanking(),
      getTopProducts()
    ])
    stats.value = dash
    orderStats.value = orders
    salesOverview.value = sales
    merchantRanking.value = merchants
    topProducts.value = products
    await nextTick()
    initCharts()
  } catch (error) {
    ElMessage.error('加载仪表盘数据失败')
  } finally {
    loading.value = false
  }
}

function initCharts() {
  initSalesChart()
  initOrderStatusChart()
}

function initSalesChart() {
  const dom = document.getElementById('salesChart')
  if (!dom || !hasSalesData.value) return
  salesChart?.dispose()
  salesChart = echarts.init(dom)
  const data = salesOverview.value.dailySales || []
  salesChart.setOption({
    color: ['#f97316', '#0d9488'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额(元)', '订单数'] },
    grid: { left: 36, right: 36, top: 48, bottom: 28, containLabel: true },
    xAxis: { type: 'category', data: data.map((item) => item.date) },
    yAxis: [
      { type: 'value', name: '销售额' },
      { type: 'value', name: '订单数' }
    ],
    series: [
      {
        name: '销售额(元)',
        type: 'bar',
        barWidth: 22,
        data: data.map((item) => Number((item.amount / 100).toFixed(2))),
        itemStyle: { borderRadius: [5, 5, 0, 0] }
      },
      {
        name: '订单数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: data.map((item) => item.orderCount)
      }
    ]
  })
}

function initOrderStatusChart() {
  const dom = document.getElementById('orderStatusChart')
  if (!dom || !hasOrderStatusData.value) return
  orderChart?.dispose()
  orderChart = echarts.init(dom)
  const labels = {
    PENDING_PAYMENT: '待支付',
    PAID: '待接单',
    ACCEPTED: '已接单',
    COMPLETED: '已完成',
    REFUND_REQUESTED: '退款中',
    REFUNDED: '已退款'
  }
  orderChart.setOption({
    color: ['#f97316', '#0d9488', '#2563eb', '#65a30d', '#d97706', '#dc2626'],
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['48%', '76%'],
      data: (orderStats.value.statusCounts || []).map((item) => ({
        name: labels[item.status] || item.status,
        value: item.count
      }))
    }]
  })
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
  if (fullscreen.value) {
    startAutoRefresh()
    startClock()
  } else {
    stopAutoRefresh()
    stopClock()
  }
  nextTick(() => {
    salesChart?.resize()
    orderChart?.resize()
  })
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => load(), 30000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

function startClock() {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
}

function stopClock() {
  if (clockTimer) {
    clearInterval(clockTimer)
    clockTimer = null
  }
}

function updateClock() {
  const now = new Date()
  clock.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function handleResize() {
  salesChart?.resize()
  orderChart?.resize()
}

function formatFen(value) {
  return value ? (value / 100).toFixed(2) : '0.00'
}

function toDateInput(date) {
  return date.toISOString().slice(0, 10)
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  stopAutoRefresh()
  stopClock()
  salesChart?.dispose()
  orderChart?.dispose()
})
</script>

<template>
  <div :class="['dashboard', { fullscreen }]" v-loading="loading">
    <section class="dashboard-head">
      <div>
        <h1>管理后台仪表盘</h1>
        <p>按时间区间观察平台订单、销售、商家和商品表现。</p>
      </div>
      <div class="head-actions">
        <span v-if="fullscreen" class="live-clock">{{ clock }}</span>
        <span v-if="fullscreen" class="auto-tag">每 30 秒自动刷新</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          style="width: 280px"
          @change="load"
        />
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" plain @click="toggleFullscreen">
          {{ fullscreen ? '退出大屏' : '大屏模式' }}
        </el-button>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in statCards" :key="card.label" :class="['stat-card', card.tone]">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <section class="today-grid">
      <article v-for="card in todayCards" :key="card.label">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <section class="chart-grid">
      <el-card shadow="never" class="chart-card">
        <template #header>订单状态分布</template>
        <div v-if="hasOrderStatusData" id="orderStatusChart" class="chart"></div>
        <el-empty v-else description="当前区间暂无订单状态数据" />
      </el-card>

      <el-card shadow="never" class="chart-card wide">
        <template #header>销售额趋势</template>
        <div v-if="hasSalesData" id="salesChart" class="chart"></div>
        <el-empty v-else description="当前区间暂无销售趋势数据" />
      </el-card>
    </section>

    <section class="rank-grid">
      <el-card shadow="never">
        <template #header>商家销售额 TOP 10</template>
        <el-table :data="merchantRanking.bySales" stripe size="small" empty-text="暂无排行数据">
          <el-table-column type="index" label="#" width="48" />
          <el-table-column prop="merchantName" label="商家名称" show-overflow-tooltip />
          <el-table-column label="销售额" width="110">
            <template #default="{ row }">¥{{ formatFen(row.totalSales) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单" width="80" />
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header>商家评分 TOP 10</template>
        <el-table :data="merchantRanking.byRating" stripe size="small" empty-text="暂无评分数据">
          <el-table-column type="index" label="#" width="48" />
          <el-table-column prop="merchantName" label="商家名称" show-overflow-tooltip />
          <el-table-column prop="score" label="评分" width="80" />
          <el-table-column prop="orderCount" label="订单" width="80" />
        </el-table>
      </el-card>
    </section>

    <el-card shadow="never">
      <template #header>热销商品 TOP 10</template>
      <el-table :data="topProducts.products" stripe size="small" empty-text="暂无商品销量数据">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="productName" label="商品名称" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="所属商家" show-overflow-tooltip />
        <el-table-column prop="soldCount" label="销量" width="90" />
        <el-table-column label="销售额" width="120">
          <template #default="{ row }">¥{{ formatFen(row.totalAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 16px;
}

.dashboard.fullscreen {
  background: var(--bg-page);
  inset: 0;
  overflow: auto;
  padding: 24px;
  position: fixed;
  z-index: 50;
}

.dashboard-head {
  align-items: flex-end;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.dashboard-head h1 {
  font-size: 22px;
  margin: 0 0 6px;
}

.dashboard-head p {
  color: var(--text-secondary);
  margin: 0;
}

.head-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.live-clock {
  background: #1e293b;
  border-radius: 6px;
  color: #0d9488;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  font-weight: 700;
  padding: 6px 14px;
}

.auto-tag {
  background: #ecfdf5;
  border-radius: 4px;
  color: #047857;
  font-size: 12px;
  padding: 4px 10px;
}

.stat-grid,
.today-grid,
.rank-grid,
.chart-grid {
  display: grid;
  gap: 16px;
}

.stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.today-grid,
.rank-grid,
.chart-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stat-card,
.today-grid article {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
}

.stat-card span,
.today-grid span {
  color: var(--text-secondary);
  display: block;
  font-size: 13px;
}

.stat-card strong,
.today-grid strong {
  display: block;
  font-size: 28px;
  margin-top: 8px;
}

.stat-card.orange strong { color: #f97316; }
.stat-card.teal strong { color: #0d9488; }
.stat-card.violet strong { color: #7c3aed; }
.stat-card.green strong { color: #059669; }

.chart-card.wide {
  grid-column: span 1;
}

.chart {
  height: 320px;
}

.fullscreen .chart {
  height: 420px;
}

@media (max-width: 1100px) {
  .stat-grid,
  .today-grid,
  .rank-grid,
  .chart-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .dashboard-head,
  .head-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .stat-grid,
  .today-grid,
  .rank-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
