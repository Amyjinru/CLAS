<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { getMyMerchant, getMyMerchantStats } from '../api/clas'
import { formatFen } from '../utils/formatters'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'

const loading = ref(true)
const merchant = ref(null)
const stats = ref({
  todayOrders: 0,
  todaySales: 0,
  monthSales: 0,
  yearSales: 0,
  totalSales: 0,
  dailySales: [],
  topProducts: []
})
const revenueScope = ref('day')
let salesChart = null
let echartsModule = null

const revenueOptions = [
  { label: '日', value: 'day', field: 'todaySales', hint: '今日营业额' },
  { label: '月', value: 'month', field: 'monthSales', hint: '本月营业额' },
  { label: '年', value: 'year', field: 'yearSales', hint: '本年营业额' },
  { label: '总', value: 'total', field: 'totalSales', hint: '累计营业额' }
]

const activeRevenue = computed(() =>
  revenueOptions.find((item) => item.value === revenueScope.value) || revenueOptions[0]
)

const activeRevenueAmount = computed(() => stats.value?.[activeRevenue.value.field] || 0)

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

      <el-card class="revenue-card" shadow="hover">
        <div class="revenue-head">
          <div>
            <span>营业额查看</span>
            <strong>¥{{ formatFen(activeRevenueAmount) }}</strong>
            <p>{{ activeRevenue.hint }}</p>
          </div>
          <el-segmented
            v-model="revenueScope"
            :options="revenueOptions.map((item) => ({ label: item.label, value: item.value }))"
          />
        </div>
        <div class="revenue-breakdown">
          <button
            v-for="item in revenueOptions"
            :key="item.value"
            type="button"
            :class="{ active: revenueScope === item.value }"
            @click="revenueScope = item.value"
          >
            <span>{{ item.hint }}</span>
            <strong>¥{{ formatFen(stats[item.field]) }}</strong>
          </button>
        </div>
      </el-card>

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

.revenue-card {
  margin-bottom: 18px;
}

.revenue-head {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.revenue-head span {
  color: var(--text-secondary);
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
}

.revenue-head strong {
  color: var(--color-primary);
  display: block;
  font-size: 34px;
  line-height: 1.15;
}

.revenue-head p {
  color: var(--text-muted);
  font-size: 13px;
  margin: 6px 0 0;
}

.revenue-breakdown {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 16px;
}

.revenue-breakdown button {
  background: var(--bg-subtle);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  padding: 12px;
  text-align: left;
  transition: border-color 0.2s, background 0.2s;
}

.revenue-breakdown button.active {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.revenue-breakdown span {
  color: var(--text-secondary);
  display: block;
  font-size: 12px;
  margin-bottom: 6px;
}

.revenue-breakdown strong {
  color: var(--text-primary);
  font-size: 18px;
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

  .revenue-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .revenue-breakdown {
    grid-template-columns: 1fr;
  }
}
</style>
