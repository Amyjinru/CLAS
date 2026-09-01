<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { claimRiderOrder, listAvailableRiderOrders, listMyRiderOrders } from '../../api/clas'

const availableOrders = ref([])
const myOrders = ref([])
const loading = ref(false)
const claimingId = ref(null)

const activeOrders = computed(() => myOrders.value.filter(({ order }) => order.deliveryStatus !== 'DELIVERED'))

function money(fen) {
  return `¥${((fen || 0) / 100).toFixed(2)}`
}

function itemCount(items = []) {
  return items.reduce((sum, item) => sum + (item.quantity || 0), 0)
}

function timeText(value) {
  return value ? String(value).replace('T', ' ').slice(5, 16) : '刚刚'
}

async function loadOrders() {
  loading.value = true
  try {
    const [available, mine] = await Promise.all([
      listAvailableRiderOrders(),
      listMyRiderOrders()
    ])
    availableOrders.value = available || []
    myOrders.value = mine || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '骑手订单加载失败')
  } finally {
    loading.value = false
  }
}

async function claim(entry) {
  claimingId.value = entry.order.id
  try {
    await claimRiderOrder(entry.order.id)
    ElMessage.success(`已接下订单 #${entry.order.id}`)
    await loadOrders()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '接单失败')
    await loadOrders()
  } finally {
    claimingId.value = null
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="rider-page" v-loading="loading">
    <section class="rider-hero">
      <div>
        <span class="eyebrow">RIDER MVP</span>
        <h1>骑手工作台</h1>
        <p>当前迭代只验证“查看待配送订单 → 接单”的最小闭环。</p>
      </div>
      <el-tag type="success" effect="dark" round>演示在线</el-tag>
    </section>

    <section class="metric-grid" aria-label="骑手概览">
      <article><span>可抢订单</span><strong>{{ availableOrders.length }}</strong></article>
      <article><span>我的配送</span><strong>{{ activeOrders.length }}</strong></article>
      <article class="placeholder"><span>今日收益</span><strong>待接入</strong></article>
      <article class="placeholder"><span>准时率</span><strong>待接入</strong></article>
    </section>

    <el-tabs class="rider-tabs">
      <el-tab-pane :label="`抢单大厅 ${availableOrders.length}`">
        <el-empty v-if="!availableOrders.length" description="暂无可接的配送单" />
        <div v-else class="order-grid">
          <article v-for="entry in availableOrders" :key="entry.order.id" class="order-card">
            <div class="order-head">
              <div>
                <span>订单 #{{ entry.order.id }}</span>
                <strong>商家 #{{ entry.order.merchantId }}</strong>
              </div>
              <el-tag type="warning">备餐中</el-tag>
            </div>
            <dl>
              <div><dt>送至</dt><dd>{{ entry.order.deliveryAddress || '暂无地址' }}</dd></div>
              <div><dt>距离</dt><dd>{{ entry.order.routeDistanceMeters || entry.order.distanceMeters || '-' }} 米</dd></div>
              <div><dt>商品</dt><dd>{{ itemCount(entry.items) }} 件</dd></div>
              <div><dt>配送费</dt><dd>{{ money(entry.order.deliveryFee) }}</dd></div>
            </dl>
            <div class="order-foot">
              <small>商家接单 {{ timeText(entry.order.acceptedAt) }}</small>
              <el-button type="primary" :loading="claimingId === entry.order.id" @click="claim(entry)">立即接单</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`我的配送 ${myOrders.length}`">
        <el-empty v-if="!myOrders.length" description="还没有接单记录" />
        <div v-else class="order-grid">
          <article v-for="entry in myOrders" :key="entry.order.id" class="order-card assigned">
            <div class="order-head">
              <div><span>订单 #{{ entry.order.id }}</span><strong>{{ entry.order.deliveryAddress }}</strong></div>
              <el-tag type="success">已接单</el-tag>
            </div>
            <p class="next-note">到店、取餐、送达操作将在后续迭代接入订单状态机。</p>
            <div class="stub-actions">
              <el-button disabled>已到店（待实现）</el-button>
              <el-button disabled>确认取餐（待实现）</el-button>
              <el-button disabled>确认送达（待实现）</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="后续优化点">
        <ul class="roadmap-list">
          <li>骑手审核、车辆与健康证资料</li>
          <li>上下线、实时定位、导航与距离排序</li>
          <li>到店、取餐、配送、送达状态机及超时处理</li>
          <li>派单策略、收益流水、绩效与异常上报</li>
        </ul>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.rider-page { width: min(1180px, calc(100% - 32px)); margin: 28px auto 56px; }
.rider-hero { display: flex; justify-content: space-between; align-items: flex-start; gap: 24px; padding: 30px; border-radius: 24px; color: #fff; background: linear-gradient(135deg, #173f35, #0d9488); box-shadow: 0 20px 55px rgba(13, 148, 136, .2); }
.eyebrow { font-size: 12px; font-weight: 900; letter-spacing: .18em; color: #99f6e4; }
.rider-hero h1 { margin: 8px 0; font-size: clamp(28px, 4vw, 42px); }
.rider-hero p { margin: 0; color: rgba(255,255,255,.82); }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 18px 0; }
.metric-grid article { padding: 20px; border: 1px solid #eadfd2; border-radius: 18px; background: rgba(255,255,255,.94); }
.metric-grid span, .metric-grid strong { display: block; }
.metric-grid span { color: #7c6b5b; font-size: 13px; }
.metric-grid strong { margin-top: 8px; font-size: 28px; color: #173f35; }
.metric-grid .placeholder strong { font-size: 17px; color: #a39486; }
.rider-tabs { padding: 22px; border: 1px solid #eadfd2; border-radius: 22px; background: rgba(255,255,255,.95); }
.order-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.order-card { padding: 20px; border: 1px solid #e9dfd5; border-radius: 18px; background: #fffdfa; }
.order-card.assigned { border-color: #99d9cc; background: #f4fffc; }
.order-head, .order-foot { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.order-head span, .order-head strong { display: block; }
.order-head span { color: #8a7765; font-size: 12px; }
.order-head strong { margin-top: 4px; color: #2d251e; }
dl { margin: 18px 0; }
dl div { display: grid; grid-template-columns: 62px 1fr; gap: 12px; padding: 7px 0; border-bottom: 1px dashed #eadfd2; }
dt { color: #907c68; } dd { margin: 0; color: #332b24; text-align: right; }
.order-foot small { color: #927d69; }
.next-note { color: #5f746e; line-height: 1.65; }
.stub-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.roadmap-list { display: grid; gap: 12px; margin: 8px 0; padding-left: 24px; color: #5c5045; line-height: 1.7; }
@media (max-width: 760px) { .metric-grid { grid-template-columns: repeat(2, 1fr); } .order-grid { grid-template-columns: 1fr; } .rider-hero { flex-direction: column; } }
</style>
