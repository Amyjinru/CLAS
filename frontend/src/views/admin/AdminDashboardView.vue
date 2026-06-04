<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { api, unwrap } from '../../api/client'

// 仪表盘数据
const stats = ref({
  totalUsers: 0, totalMerchants: 0, totalOrders: 0, totalSales: 0,
  todayOrders: 0, todaySales: 0, pendingPaymentOrders: 0, paidOrders: 0, completedOrders: 0
})
const orderStats = ref({ statusCounts: [], dailyOrders: [] })
const salesOverview = ref({ dailySales: [], totalSales: 0, monthlySales: 0, weeklySales: 0 })
const merchantRanking = ref({ bySales: [], byRating: [] })
const topProducts = ref({ products: [] })

const loading = ref(true)
let salesChart = null
let orderChart = null

onMounted(async () => {
  try {
    const [dash, orders, sales, merchants, products] = await Promise.all([
      api.get('/admin/dashboard').then(unwrap),
      api.get('/admin/stats/orders').then(unwrap),
      api.get('/admin/stats/sales').then(unwrap),
      api.get('/admin/stats/merchants').then(unwrap),
      api.get('/admin/stats/products').then(unwrap)
    ])
    stats.value = dash
    orderStats.value = orders
    salesOverview.value = sales
    merchantRanking.value = merchants
    topProducts.value = products
  } catch (e) {
    console.error('加载仪表盘数据失败:', e)
  } finally {
    loading.value = false
    await nextTick()
    initCharts()
  }
})

function initCharts() {
  initSalesChart()
  initOrderStatusChart()
}

function initSalesChart() {
  const dom = document.getElementById('salesChart')
  if (!dom) return
  if (salesChart) salesChart.dispose()
  salesChart = echarts.init(dom)

  const data = salesOverview.value.dailySales || []
  salesChart.setOption({
    color: ['#f97316', '#0d9488', '#7c3aed', '#059669'],
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#ebe3d5', textStyle: { color: '#1a1510' } },
    legend: { data: ['销售额(元)', '订单数'], textStyle: { color: '#6b5c49' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLine: { lineStyle: { color: '#d6ccb8' } }, axisLabel: { color: '#8c7a64' } },
    yAxis: [
      { type: 'value', name: '销售额(元)', nameTextStyle: { color: '#8c7a64' }, axisLabel: { color: '#8c7a64' }, splitLine: { lineStyle: { color: '#f5f0e8' } } },
      { type: 'value', name: '订单数', nameTextStyle: { color: '#8c7a64' }, axisLabel: { color: '#8c7a64' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '销售额(元)', type: 'bar', barWidth: '40%',
        data: data.map(d => (d.amount / 100).toFixed(2)),
        itemStyle: { borderRadius: [6, 6, 0, 0], color: '#f97316' }
      },
      {
        name: '订单数', type: 'line', yAxisIndex: 1, smooth: true,
        data: data.map(d => d.orderCount),
        lineStyle: { color: '#0d9488', width: 2 },
        itemStyle: { color: '#0d9488' },
        symbol: 'circle', symbolSize: 6
      }
    ]
  })
}

function initOrderStatusChart() {
  const dom = document.getElementById('orderStatusChart')
  if (!dom) return
  if (orderChart) orderChart.dispose()
  orderChart = echarts.init(dom)

  const data = orderStats.value.statusCounts || []
  const statusNames = {
    PENDING_PAYMENT: '待支付', PAID: '已支付',
    ACCEPTED: '已接单', COMPLETED: '已完成'
  }

  orderChart.setOption({
    color: ['#f97316', '#0d9488', '#7c3aed', '#059669'],
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#ebe3d5', textStyle: { color: '#1a1510' } },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '50%'],
      data: data.map(d => ({
        name: statusNames[d.status] || d.status,
        value: d.count
      })),
      label: { color: '#6b5c49', fontSize: 12 },
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.1)' } }
    }]
  })
}

onUnmounted(() => {
  salesChart?.dispose()
  orderChart?.dispose()
})

function formatFen(v) {
  return v ? (v / 100).toFixed(2) : '0.00'
}

const statCards = computed(() => [
  { label: '总订单数', value: stats.value.totalOrders, color: 'var(--clas-amber-500)', bg: 'var(--clas-amber-50)' },
  { label: '总销售额(元)', value: formatFen(stats.value.totalSales), color: 'var(--clas-teal-600)', bg: 'var(--clas-teal-50)' },
  { label: '总用户数', value: stats.value.totalUsers, color: '#7c3aed', bg: '#f5f3ff' },
  { label: '总商家数', value: stats.value.totalMerchants, color: '#059669', bg: '#ecfdf5' }
])
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <h1 class="page-title">仪表盘</h1>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="card-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日数据 -->
    <el-row :gutter="16" class="card-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>今日概览</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="今日新增订单">{{ stats.todayOrders }}</el-descriptions-item>
            <el-descriptions-item label="今日销售额(元)">{{ formatFen(stats.todaySales) }}</el-descriptions-item>
            <el-descriptions-item label="待支付订单">{{ stats.pendingPaymentOrders }}</el-descriptions-item>
            <el-descriptions-item label="待接单数">{{ stats.paidOrders }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>订单状态分布</template>
          <div id="orderStatusChart" style="height: 260px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售额图表 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>近7天销售额趋势</template>
      <div id="salesChart" style="height: 340px;"></div>
    </el-card>

    <!-- 商家排行 -->
    <el-row :gutter="16" class="card-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>商家销售额 TOP 10</template>
          <el-table :data="merchantRanking.bySales" stripe size="small">
            <el-table-column type="index" label="#" width="40" />
            <el-table-column prop="merchantName" label="商家名称" />
            <el-table-column label="销售额(元)" width="120">
              <template #default="{ row }">{{ formatFen(row.totalSales) }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>商家评分 TOP 10</template>
          <el-table :data="merchantRanking.byRating" stripe size="small">
            <el-table-column type="index" label="#" width="40" />
            <el-table-column prop="merchantName" label="商家名称" />
            <el-table-column prop="score" label="评分" width="80" />
            <el-table-column prop="orderCount" label="订单数" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热销商品 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>热销商品 TOP 10</template>
      <el-table :data="topProducts.products" stripe size="small">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="merchantName" label="所属商家" />
        <el-table-column prop="soldCount" label="销量" width="80" />
        <el-table-column label="销售额(元)" width="120">
          <template #default="{ row }">{{ formatFen(row.totalAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 22px;
  margin: 0 0 24px 0;
}
.card-row {
  margin-bottom: 20px;
}
.chart-card {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg) !important;
}
.stat-value {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 8px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-weight: 500;
}
</style>
