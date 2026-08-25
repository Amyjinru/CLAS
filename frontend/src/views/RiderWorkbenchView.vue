<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRiderProfile, listRiderDeliveries, listRiderTasks, listMyRiderMetrics, listMyRiderReviews, listMyRiderSettlements, listMyRiderWithdrawals, reportRiderLocation, startAcceptingOrders, stopAcceptingOrders } from '../api/clas'
import RiderChatWindow from '../components/RiderChatWindow.vue'
import { hasAmapKey, loadAmap } from '../utils/amap'

const profile = ref(null)
const tasks = ref([])
const deliveries = ref([])
const loading = ref(false)
const mapHost = ref(null)
const mapMessage = ref('')
const contactOrder = ref(null)
const contactDialogOpen = ref(false)
const income = ref([])
const withdrawals = ref([])
const reviews = ref([])
const metrics = ref([])
let map = null
let locationTimer = null

async function load() {
  loading.value = true
  try {
    const [nextProfile, nextTasks, nextDeliveries, nextIncome, nextWithdrawals, nextReviews, nextMetrics] = await Promise.all([getRiderProfile(), listRiderTasks(), listRiderDeliveries(), listMyRiderSettlements(), listMyRiderWithdrawals(), listMyRiderReviews(), listMyRiderMetrics()])
    profile.value = nextProfile
    tasks.value = nextTasks
    deliveries.value = nextDeliveries
    income.value = nextIncome; withdrawals.value = nextWithdrawals; reviews.value = nextReviews; metrics.value = nextMetrics
  } finally {
    loading.value = false
  }
}

async function updateLocation() {
  if (!profile.value?.onlineStatus || (!profile.value?.acceptingOrders && !deliveries.value.length) || !navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(async (position) => {
    try {
      profile.value = await reportRiderLocation({ longitude: position.coords.longitude, latitude: position.coords.latitude, accuracyMeters: Math.round(position.coords.accuracy || 0) })
    } catch { /* client interceptor has displayed the error */ }
  }, () => { mapMessage.value = '未获取到定位权限，仍可查看和处理已有订单。' }, { enableHighAccuracy: true, timeout: 10000, maximumAge: 10000 })
}

async function initMap() {
  if (!hasAmapKey()) { mapMessage.value = '未配置高德地图密钥，地图已降级；配送流程不受影响。'; return }
  try {
    const AMap = await loadAmap()
    map = new AMap.Map(mapHost.value, { zoom: 14, center: [116.397428, 39.90923] })
    await updateLocation()
  } catch { mapMessage.value = '高德地图暂不可用，已使用无地图模式。' }
}

function syncLocationReporting() {
  if (locationTimer) window.clearInterval(locationTimer)
  locationTimer = window.setInterval(updateLocation, 15000)
}

async function start() {
  profile.value = await startAcceptingOrders()
  ElMessage.success('已开始接单')
  await load()
  await updateLocation()
}

async function stop() {
  profile.value = await stopAcceptingOrders()
  ElMessage.info('已停止接收新订单，手上订单可继续配送')
  await load()
}

function openContact(item) { contactOrder.value = item; contactDialogOpen.value = true }

onMounted(load)
onMounted(async () => { await initMap(); syncLocationReporting() })
onBeforeUnmount(() => { if (locationTimer) window.clearInterval(locationTimer); map?.destroy?.() })
</script>

<template>
  <main class="rider-workbench" v-loading="loading">
    <section class="hero">
      <div>
        <p class="eyebrow">RIDER WORKBENCH</p>
        <h1>{{ profile?.realName || '骑手' }}，{{ profile?.acceptingOrders ? '正在接单' : '暂不接单' }}</h1>
        <p>上线状态：{{ profile?.onlineStatus ? '在线，可上报定位' : '离线' }}。结束接单不会影响正在配送的订单。</p>
      </div>
      <div class="actions">
        <el-button v-if="!profile?.acceptingOrders" type="primary" size="large" @click="start">开始接单</el-button>
        <el-button v-else type="danger" plain size="large" @click="stop">结束接单</el-button>
      </div>
    </section>
    <el-alert v-if="!profile?.onlineStatus" type="warning" :closable="false" show-icon title="当前离线：点击“开始接单”即可上线并领取任务。" />
    <el-alert v-if="mapMessage" type="info" :closable="false" show-icon :title="mapMessage" />
    <section class="map-panel"><div ref="mapHost" class="map-host" /></section>
    <section class="grid">
      <el-card><template #header>配送中的订单（{{ deliveries.length }}）</template><p v-for="item in deliveries" :key="item.id">#{{ item.id }} · {{ item.deliveryStatus }} · {{ item.deliveryAddress }} <el-button text type="primary" @click="openContact(item)">联系用户</el-button></p><p v-if="!deliveries.length">暂无进行中订单</p></el-card>
      <el-card><template #header>附近可接任务（{{ tasks.length }}）</template><p v-for="item in tasks" :key="item.orderId">#{{ item.orderId }} · {{ item.merchantName }} · {{ item.merchantDistanceMeters }}m</p><p v-if="!tasks.length">暂无附近任务</p></el-card>
    </section>
    <section class="grid finance-grid"><el-card><template #header>收入流水</template><p v-for="item in income.slice(0,5)" :key="item.id">{{ item.settlementType }} · {{ item.amount }} 分 · {{ item.balanceType }}</p><p v-if="!income.length">暂无收入流水</p></el-card><el-card><template #header>提现记录</template><p v-for="item in withdrawals.slice(0,5)" :key="item.id">{{ item.amount }} 分 · {{ item.status }}</p><p v-if="!withdrawals.length">暂无提现申请</p></el-card><el-card><template #header>骑手评价</template><p v-for="item in reviews.slice(0,5)" :key="item.id">{{ item.score }} 星 · {{ item.tags || '无标签' }}</p><p v-if="!reviews.length">暂无评价</p></el-card><el-card><template #header>每日表现</template><p v-for="item in metrics.slice(0,5)" :key="item.id">{{ item.metricDate }} · {{ item.finalScore }} 分 · {{ item.grade }}</p><p v-if="!metrics.length">暂无归档数据</p></el-card></section>
    <el-dialog v-model="contactDialogOpen" title="配送沟通" width="min(560px,92vw)"><RiderChatWindow v-if="contactOrder" :order-id="contactOrder.id" role="RIDER" :active="['ASSIGNED_WAITING_MEAL', 'DELIVERING'].includes(contactOrder.deliveryStatus)" /></el-dialog>
  </main>
</template>

<style scoped>
.rider-workbench { margin: 0 auto; max-width: 1080px; padding: 40px 24px; }
.hero { align-items: center; background: linear-gradient(135deg,#102a43,#007c91); border-radius: 20px; color: #fff; display:flex; justify-content:space-between; padding:32px; }
.hero h1 { margin: 6px 0; }.hero p { margin:0; opacity:.86; }.eyebrow { font-size:12px; font-weight:700; letter-spacing:.12em; }.actions { white-space:nowrap; }.grid { display:grid; gap:18px; grid-template-columns:repeat(2,minmax(0,1fr)); margin-top:18px; }.grid p { border-bottom:1px solid #eef2f7; padding:8px 0; } @media(max-width:680px){.hero{align-items:flex-start;flex-direction:column;gap:20px}.grid{grid-template-columns:1fr}}
.map-panel { background:#fff; border:1px solid #e7edf2; border-radius:16px; margin-top:18px; overflow:hidden; }.map-host { height:260px; width:100%; }
</style>
