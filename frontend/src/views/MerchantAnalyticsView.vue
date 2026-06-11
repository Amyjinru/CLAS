<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { getMyMerchant, getMyMerchantStats } from '../api/clas'
import { formatFen } from '../utils/formatters'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'

const loading = ref(true)
const merchant = ref(null)
const stats = ref({
  todayOrders: 0,
  todaySales: 0,
  dailySales: [],
  topProducts: []
})
let salesChart = null
let echartsModule = null

async function loadStats() {
  loading.value = true
  try {
    const [merchantInfo, statsInfo] = await Promise.all([getMyMerchant(), getMyMerchantStats()])
    merchant.value = merchantInfo
    stats.value = statsInfo
  } finally {
    loading.value = false
    await nextTick()
    await renderChart()
  }
}

async function ensureEcharts() {
  if (!echartsModule) {
    echartsModule = (await import('../utils/echarts')).default
  }
  return echartsModule
}

async function renderChart() {
  const dom = document.getElementById('merchantSalesChart')
  if (!dom) return
  const echarts = await ensureEcharts()
  salesChart?.dispose()
  salesChart = echarts.init(dom)
  const data = stats.value.dailySales || []
  salesChart.setOption({
    color: ['#2563eb', '#16a34a'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 24, top: 36, bottom: 32 },
    xAxis: { type: 'category', data: data.map((item) => item.date) },
    yAxis: [
      { type: 'value', name: '销售额(元)' },
      { type: 'value', name: '订单数' }
    ],
    series: [
      {
        name: '销售额(元)',
        type: 'line',
        smooth: true,
        data: data.map((item) => Number(formatFen(item.amount)))
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: data.map((item) => item.orderCount)
      }
    ]
  })
}

onMounted(loadStats)
onUnmounted(() => salesChart?.dispose())

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}
</script>

<template>
  <MerchantWorkspaceShell
    :merchant="merchant"
    :loading="loading"
    active-module="analytics"
    @merchant-updated="onMerchantProfileSaved"
  >
    <main class="analytics-page">
      <div class="page-head">
        <h1>经营分析</h1>
      </div>

      <div class="stats-grid">
        <el-card shadow="hover">
          <div class="stat-value">{{ stats.todayOrders || 0 }}</div>
          <div class="stat-label">今日订单数</div>
        </el-card>
        <el-card shadow="hover">
          <div class="stat-value">¥{{ formatFen(stats.todaySales) }}</div>
          <div class="stat-label">今日销售额</div>
        </el-card>
      </div>

      <el-card class="chart-card" shadow="hover">
        <template #header>近 7 天销售额趋势</template>
        <div id="merchantSalesChart" class="chart"></div>
      </el-card>

      <el-card shadow="hover">
        <template #header>热销商品 TOP 10</template>
        <el-table :data="stats.topProducts" stripe empty-text="暂无销售数据">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="productName" label="商品名" min-width="180" />
          <el-table-column prop="soldCount" label="销量" width="120" />
          <el-table-column label="销售额" width="140">
            <template #default="{ row }">¥{{ formatFen(row.totalAmount) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </main>
  </MerchantWorkspaceShell>
</template>

<style scoped>
.analytics-page {
  min-width: 0;
}

.page-head h1 {
  color: var(--text-primary);
  font-size: 24px;
  margin: 0 0 18px;
}

.stats-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 18px;
}

.stat-value {
  color: var(--color-accent);
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-label {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 8px;
}

.chart-card {
  margin-bottom: 18px;
}

.chart {
  height: 340px;
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
