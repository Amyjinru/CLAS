<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getMyMerchantStats } from '../api/clas'

const loading = ref(true)
const stats = ref({
  todayOrders: 0,
  todaySales: 0,
  dailySales: [],
  topProducts: []
})
let salesChart = null

function formatFen(value) {
  return ((value || 0) / 100).toFixed(2)
}

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getMyMerchantStats()
  } finally {
    loading.value = false
    await nextTick()
    renderChart()
  }
}

function renderChart() {
  const dom = document.getElementById('merchantSalesChart')
  if (!dom) return
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
</script>

<template>
  <div class="analytics-page" v-loading="loading">
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
  </div>
</template>

<style scoped>
.analytics-page {
  max-width: 1120px;
  margin: 30px auto;
  padding: 0 20px 48px;
}

.page-head h1 {
  color: #303133;
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
  color: #2563eb;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-label {
  color: #606266;
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
