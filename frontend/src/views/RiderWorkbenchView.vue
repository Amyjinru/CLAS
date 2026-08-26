<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { claimRiderTask, createRiderCallSession, getRiderProfile, listRiderDeliveries, listRiderTasks, reportRiderLocation, startAcceptingOrders, stopAcceptingOrders } from '../api/clas'
import RiderChatWindow from '../components/RiderChatWindow.vue'
import RiderMerchantChatWindow from '../components/RiderMerchantChatWindow.vue'

const router = useRouter()
const profile = ref(null)
const tasks = ref([])
const deliveries = ref([])
const loading = ref(false)
const locating = ref(false)
const actionOrderId = ref(null)
const taskSort = ref('SMART')
const locationMessage = ref('')
const contactOrder = ref(null)
const contactDialogOpen = ref(false)
const merchantContactOrder = ref(null)
const merchantContactDialogOpen = ref(false)
const callingOrderId = ref(null)
const callSession = ref(null)
const callDialogOpen = ref(false)
let locationTimer = null

const taskSortOptions = [
  { value: 'SMART', label: '智能排序' },
  { value: 'COMMISSION', label: '按佣金' },
  { value: 'DISTANCE', label: '按距离' }
]

const orderedDeliveries = computed(() => [...deliveries.value].sort((left, right) => {
  const leftTime = deadlineOf(left)
  const rightTime = deadlineOf(right)
  if (leftTime !== rightTime) return leftTime - rightTime
  return Number(left.deliverySequence || Number.MAX_SAFE_INTEGER) - Number(right.deliverySequence || Number.MAX_SAFE_INTEGER)
}))

function deadlineOf(order) {
  const value = order.promiseEndAt || order.predictedArrivalAt
  const timestamp = value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER
  return Number.isNaN(timestamp) ? Number.MAX_SAFE_INTEGER : timestamp
}

function isUrgent(order) {
  const deadline = deadlineOf(order)
  return deadline !== Number.MAX_SAFE_INTEGER && deadline - Date.now() <= 15 * 60 * 1000
}

function formatTime(value) {
  if (!value) return '暂未计算'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '暂未计算' : date.toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit', month: 'numeric', day: 'numeric' })
}

function formatMoney(cents) { return `¥${((Number(cents) || 0) / 100).toFixed(2)}` }

function canLoadTasks(riderProfile = profile.value) {
  return Boolean(riderProfile?.onlineStatus && riderProfile.currentLongitude != null && riderProfile.currentLatitude != null)
}

async function loadTasks(riderProfile = profile.value) {
  if (!canLoadTasks(riderProfile)) {
    tasks.value = []
    return
  }
  try { tasks.value = await listRiderTasks(taskSort.value) } catch { tasks.value = [] }
}

async function load() {
  loading.value = true
  try {
    const nextProfile = await getRiderProfile()
    profile.value = nextProfile
    deliveries.value = await listRiderDeliveries()
    await loadTasks(nextProfile)
  } finally { loading.value = false }
}

function browserPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error('当前浏览器不支持定位'))
    navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 })
  })
}

async function refreshLocation({ silent = false } = {}) {
  if (!profile.value?.onlineStatus) {
    if (!silent) ElMessage.warning('请先点击开始接单，上线后即可重新定位')
    return
  }
  locating.value = true
  try {
    const position = await browserPosition()
    profile.value = await reportRiderLocation({ longitude: position.coords.longitude, latitude: position.coords.latitude, accuracyMeters: Math.round(position.coords.accuracy || 0) })
    locationMessage.value = `定位已更新 · 精度约 ${Math.round(position.coords.accuracy || 0)} 米`
    await loadTasks(profile.value)
    if (!silent) ElMessage.success('当前位置已更新，附近任务已刷新')
  } catch {
    locationMessage.value = '重新定位失败：请检查浏览器定位权限后再试。已有配送订单不受影响。'
    if (!silent) ElMessage.warning(locationMessage.value)
  } finally { locating.value = false }
}

async function start() {
  profile.value = await startAcceptingOrders()
  ElMessage.success('已上线并开始接单')
  await refreshLocation()
  await load()
}

async function stop() {
  profile.value = await stopAcceptingOrders()
  ElMessage.info('已停止接收新订单，当前配送任务仍可继续处理')
  await load()
}

async function claimTask(task) {
  actionOrderId.value = task.orderId
  try {
    await claimRiderTask(task.orderId)
    ElMessage.success(`已接订单 #${task.orderId}，请前往商家取餐`)
    await load()
  } finally { actionOrderId.value = null }
}

function openContact(order) { contactOrder.value = order; contactDialogOpen.value = true }
function openMerchantContact(order) { merchantContactOrder.value = order; merchantContactDialogOpen.value = true }

async function callUser(order) {
  callingOrderId.value = order.id
  try {
    callSession.value = await createRiderCallSession(order.id)
    callDialogOpen.value = true
  } finally { callingOrderId.value = null }
}

function formatCallExpiry(value) {
  if (!value) return '10 分钟内有效'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '10 分钟内有效' : `有效至 ${formatTime(value)}`
}

function syncLocationReporting() {
  if (locationTimer) window.clearInterval(locationTimer)
  locationTimer = window.setInterval(() => {
    if (profile.value?.onlineStatus && (profile.value?.acceptingOrders || deliveries.value.length)) refreshLocation({ silent: true })
  }, 15000)
}

onMounted(async () => { await load(); syncLocationReporting() })
onBeforeUnmount(() => { if (locationTimer) window.clearInterval(locationTimer) })
</script>

<template>
  <main class="rider-workbench" v-loading="loading">
    <header class="work-header">
      <div>
        <p class="kicker">CLAS DELIVERY / 调度台</p>
        <h1>{{ profile?.realName || '骑手' }}，{{ profile?.acceptingOrders ? '正在接单' : '待命中' }}</h1>
        <p class="header-note">{{ profile?.onlineStatus ? '当前已上线' : '当前未上线' }} · 结束接单只停止新订单，不影响手中配送。</p>
      </div>
      <div class="header-actions">
        <el-button class="profile-button" plain @click="router.push('/rider/profile')">账户中心</el-button>
        <el-button v-if="!profile?.acceptingOrders" type="primary" size="large" @click="start">开始接单</el-button>
        <el-button v-else type="danger" plain size="large" @click="stop">结束接单</el-button>
      </div>
    </header>

    <section class="location-strip" :class="{ offline: !profile?.onlineStatus }">
      <div>
        <strong>{{ profile?.onlineStatus ? '定位服务已就绪' : '尚未上线' }}</strong>
        <span v-if="locationMessage">{{ locationMessage }}</span>
        <span v-else-if="profile?.locationUpdatedAt">上次定位：{{ formatTime(profile.locationUpdatedAt) }}</span>
        <span v-else>开始接单并定位后，系统将为你展示附近任务。</span>
      </div>
      <el-button :loading="locating" :disabled="!profile?.onlineStatus" @click="refreshLocation">重新定位</el-button>
    </section>

    <section class="dispatch-grid">
      <section class="dispatch-panel deliveries-panel">
        <header class="panel-heading">
          <div><p>IN PROGRESS</p><h2>配送中的订单 <em>{{ orderedDeliveries.length }}</em></h2></div>
          <span>按承诺送达时间排序</span>
        </header>
        <div v-if="orderedDeliveries.length" class="delivery-list">
          <article v-for="order in orderedDeliveries" :key="order.id" class="delivery-card" :class="{ urgent: isUrgent(order) }">
            <div class="order-topline"><strong>#{{ order.id }}</strong><span class="status-chip">{{ order.deliveryStatus === 'DELIVERING' ? '配送中' : '待取餐' }}</span><span v-if="isUrgent(order)" class="urgent-chip">优先处理</span></div>
            <p class="address">{{ order.deliveryAddress }}</p>
            <div class="delivery-meta"><span>承诺送达 {{ formatTime(order.promiseEndAt) }}</span><span>预计 {{ formatTime(order.predictedArrivalAt) }}</span></div>
            <div class="delivery-actions">
              <el-button class="contact-button" type="primary" @click="openContact(order)">联系用户</el-button>
              <el-button class="merchant-contact-button" type="primary" plain @click="openMerchantContact(order)">联系商家</el-button>
              <el-button class="call-button" type="primary" :loading="callingOrderId === order.id" @click="callUser(order)">拨打用户</el-button>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">当前没有配送中的订单。开始接单后，可在右侧领取附近任务。</div>
      </section>

      <section class="dispatch-panel tasks-panel">
        <header class="panel-heading task-heading">
          <div><p>NEARBY TASKS</p><h2>附近可接任务 <em>{{ tasks.length }}</em></h2></div>
          <el-select v-model="taskSort" class="sort-select" aria-label="可接任务排序" @change="loadTasks()"><el-option v-for="option in taskSortOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select>
        </header>
        <div v-if="tasks.length" class="task-list">
          <article v-for="task in tasks" :key="task.orderId" class="task-card">
            <div class="task-main">
              <div class="order-topline"><strong>#{{ task.orderId }}</strong><span class="merchant-name">{{ task.merchantName }}</span></div>
              <p class="address">{{ task.merchantAddress }}</p><p class="address destination">送至：{{ task.deliveryAddress }}</p>
              <div class="task-metrics"><span><b>{{ formatMoney(task.riderCommission) }}</b> 佣金</span><span>{{ task.merchantDistanceMeters }} m 到店</span><span>承诺 {{ formatTime(task.promiseEndAt) }}</span></div>
              <small>{{ task.recommendationReason }}</small>
            </div>
            <el-button type="primary" :loading="actionOrderId === task.orderId" @click="claimTask(task)">接单配送</el-button>
          </article>
        </div>
        <div v-else class="empty-state">{{ profile?.onlineStatus ? '附近暂时没有可接任务，可点击重新定位刷新。' : '请先开始接单并完成定位。' }}</div>
      </section>
    </section>

    <el-dialog v-model="contactDialogOpen" title="配送沟通" width="min(560px,92vw)"><RiderChatWindow v-if="contactOrder" :order-id="contactOrder.id" role="RIDER" :active="['ASSIGNED_WAITING_MEAL', 'DELIVERING'].includes(contactOrder.deliveryStatus)" /></el-dialog>
    <el-dialog v-model="merchantContactDialogOpen" title="商家沟通" width="min(560px,92vw)"><RiderMerchantChatWindow v-if="merchantContactOrder" :order-id="merchantContactOrder.id" role="RIDER" :active="['ASSIGNED_WAITING_MEAL', 'DELIVERING'].includes(merchantContactOrder.deliveryStatus)" /></el-dialog>
    <el-dialog v-model="callDialogOpen" title="隐私通话已准备" width="min(420px,92vw)">
      <section v-if="callSession" class="call-session">
        <p>请使用下方虚拟号码联系订单用户，系统不会展示用户真实手机号。</p>
        <strong>{{ callSession.maskedPhone }}</strong>
        <small>订单 #{{ callSession.orderId || callingOrderId }} · {{ formatCallExpiry(callSession.expiresAt) }}</small>
      </section>
      <template #footer><el-button type="primary" @click="callDialogOpen = false">知道了</el-button></template>
    </el-dialog>
  </main>
</template>

<style scoped>
.rider-workbench { --ink:#16242e; --muted:#6b7880; --line:#dce5e6; --teal:#006b68; --coral:#d94d35; margin:0 auto; max-width:1240px; padding:28px 24px 48px; color:var(--ink); font-family:"Noto Sans SC","Microsoft YaHei",sans-serif; }.work-header { align-items:center; background:#f4f1e8; border:1px solid #e6dfd2; border-radius:22px; display:flex; justify-content:space-between; padding:28px 32px; }.kicker,.panel-heading p { color:var(--teal); font-size:11px; font-weight:800; letter-spacing:.14em; margin:0 0 7px; }.work-header h1 { font-family:Georgia,"Noto Serif SC",serif; font-size:30px; letter-spacing:.02em; margin:0; }.header-note { color:var(--muted); margin:9px 0 0; }.header-actions { display:flex; gap:10px; }.profile-button { border-color:#9aa9aa; color:#253b3c; }.location-strip { align-items:center; background:#edf6f4; border-left:4px solid var(--teal); display:flex; justify-content:space-between; margin:18px 0; padding:13px 16px; }.location-strip div { display:flex; flex-direction:column; gap:3px; }.location-strip span { color:var(--muted); font-size:13px; }.location-strip.offline { background:#f8f2ea; border-color:#b56f2c; }.dispatch-grid { display:grid; gap:18px; grid-template-columns:minmax(0,1fr) minmax(0,1fr); }.dispatch-panel { border:1px solid var(--line); border-radius:18px; min-height:500px; overflow:hidden; }.deliveries-panel { background:#fffdf9; }.tasks-panel { background:#f8fbfb; }.panel-heading { align-items:center; border-bottom:1px solid var(--line); display:flex; justify-content:space-between; padding:22px 22px 16px; }.panel-heading h2 { font-family:Georgia,"Noto Serif SC",serif; font-size:23px; margin:0; }.panel-heading em { color:var(--teal); font-family:"Noto Sans SC",sans-serif; font-size:18px; font-style:normal; margin-left:6px; }.panel-heading>span { color:var(--muted); font-size:12px; }.sort-select { width:120px; }.delivery-list,.task-list { display:flex; flex-direction:column; gap:12px; padding:16px; }.delivery-card,.task-card { background:#fff; border:1px solid #e6ecec; border-radius:13px; padding:16px; }.delivery-card.urgent { border-color:#e4a092; box-shadow:inset 4px 0 0 var(--coral); }.order-topline { align-items:center; display:flex; gap:8px; }.order-topline strong { font-size:16px; }.status-chip,.urgent-chip { border-radius:999px; font-size:12px; padding:3px 8px; }.status-chip { background:#e4f1ef; color:var(--teal); }.urgent-chip { background:#fbe6e1; color:#a43825; }.merchant-name { color:#334a51; font-weight:700; }.address { color:#33424a; line-height:1.5; margin:10px 0 0; }.destination { color:var(--muted); font-size:13px; }.delivery-meta,.task-metrics { color:var(--muted); display:flex; flex-wrap:wrap; font-size:12px; gap:9px 14px; margin-top:12px; }.delivery-actions { align-items:center; border-top:1px solid #edf1f0; display:flex; gap:9px; justify-content:flex-end; margin-top:15px; padding-top:13px; }.delivery-actions .el-button { margin:0; min-width:88px; }.merchant-contact-button { border-color:#d6be7b; color:#806009; background:#fffaf0; }.call-button { box-shadow:0 6px 12px rgba(0,107,104,.18); }.call-session { border:1px solid #cfe4df; border-radius:14px; background:#f4fbf8; display:flex; flex-direction:column; gap:8px; padding:17px; }.call-session p { color:var(--muted); line-height:1.6; margin:0; }.call-session strong { color:var(--teal); font-family:Georgia,"Noto Serif SC",serif; font-size:24px; letter-spacing:.08em; }.call-session small { color:#59716e; }.task-card { align-items:center; display:flex; gap:14px; justify-content:space-between; }.task-main { min-width:0; }.task-metrics b { color:var(--coral); font-size:15px; }.task-main small { color:#73868b; display:block; margin-top:10px; }.empty-state { color:var(--muted); line-height:1.7; padding:38px 22px; text-align:center; }@media (max-width:760px) { .work-header,.location-strip,.task-card { align-items:flex-start; flex-direction:column; }.header-actions { width:100%; }.header-actions .el-button { flex:1; }.location-strip .el-button,.task-card>.el-button { width:100%; }.delivery-actions { justify-content:stretch; width:100%; }.delivery-actions .el-button { flex:1; }.dispatch-grid { grid-template-columns:1fr; }.dispatch-panel { min-height:auto; }.work-header { padding:23px; }.work-header h1 { font-size:25px; } }
</style>
