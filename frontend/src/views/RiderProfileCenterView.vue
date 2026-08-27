<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRiderProfile, listRiderDeliveries, listMyRiderMetrics, listMyRiderReviews, listMyRiderSettlements, listMyRiderWithdrawals } from '../api/clas'

const router = useRouter()
const profile = ref(null)
const deliveries = ref([])
const settlements = ref([])
const withdrawals = ref([])
const reviews = ref([])
const metrics = ref([])
const loading = ref(false)

const orderedDeliveries = computed(() => [...deliveries.value].sort((left, right) => deadlineOf(left) - deadlineOf(right)))
const totalIncome = computed(() => settlements.value.reduce((total, item) => total + (Number(item.amount) || 0), 0))
const averageScore = computed(() => {
  if (!reviews.value.length) return '—'
  return (reviews.value.reduce((total, item) => total + (Number(item.score) || 0), 0) / reviews.value.length).toFixed(1)
})
const latestMetric = computed(() => metrics.value[0])

function deadlineOf(order) {
  const value = order.promiseEndAt || order.predictedArrivalAt
  const timestamp = value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER
  return Number.isNaN(timestamp) ? Number.MAX_SAFE_INTEGER : timestamp
}
function isUrgent(order) { const deadline = deadlineOf(order); return deadline !== Number.MAX_SAFE_INTEGER && deadline - Date.now() <= 15 * 60 * 1000 }
function formatMoney(cents) { return `¥${((Number(cents) || 0) / 100).toFixed(2)}` }
function formatTime(value) {
  if (!value) return '暂未计算'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '暂未计算' : date.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function settlementLabel(type) { return ({ COMMISSION: '配送佣金', TIP: '用户打赏', OVERDUE_DEDUCTION: '超时扣减', WITHDRAWAL: '提现' })[type] || type || '收入变动' }

async function load() {
  loading.value = true
  try {
    const [nextProfile, nextDeliveries, nextSettlements, nextWithdrawals, nextReviews, nextMetrics] = await Promise.all([
      getRiderProfile(), listRiderDeliveries(), listMyRiderSettlements(), listMyRiderWithdrawals(), listMyRiderReviews(), listMyRiderMetrics()
    ])
    profile.value = nextProfile
    deliveries.value = nextDeliveries
    settlements.value = nextSettlements
    withdrawals.value = nextWithdrawals
    reviews.value = nextReviews
    metrics.value = nextMetrics
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <main class="rider-center" v-loading="loading">
    <header class="center-hero">
      <div class="hero-grid" aria-hidden="true"></div>
      <div class="hero-content">
        <button class="back-link" type="button" @click="router.push('/rider')">← 返回调度台</button>
        <p class="eyebrow">RIDER ACCOUNT / 账户中心</p>
        <div class="identity-line">
          <div class="avatar-mark">{{ (profile?.realName || '骑').slice(0, 1) }}</div>
          <div><h1>{{ profile?.realName || '骑手' }}</h1><p>{{ profile?.vehicleType || '配送骑手' }} · {{ profile?.serviceArea || '服务区域待完善' }}</p></div>
        </div>
      </div>
      <div class="hero-state"><span class="state-dot" :class="{ active: profile?.onlineStatus }"></span><strong>{{ profile?.onlineStatus ? '在线中' : '离线中' }}</strong><small>{{ profile?.acceptingOrders ? '正在接单' : '暂不接单' }}</small></div>
    </header>

    <section class="metric-strip" aria-label="骑手账户概览">
      <article class="metric-card income"><span>累计流水</span><strong>{{ formatMoney(totalIncome) }}</strong><small>佣金、打赏及结算记录</small></article>
      <article class="metric-card balance"><span>可提现余额</span><strong>{{ formatMoney(profile?.withdrawableBalance) }}</strong><small>冻结中 {{ formatMoney(profile?.frozenBalance) }}</small></article>
      <article class="metric-card rating"><span>用户评价</span><strong>{{ averageScore }}<i v-if="averageScore !== '—'"> / 5</i></strong><small>{{ reviews.length }} 条骑手评价</small></article>
      <article class="metric-card performance"><span>最新表现</span><strong>{{ latestMetric?.finalScore ?? '—' }}<i v-if="latestMetric"> 分</i></strong><small>{{ latestMetric ? `${latestMetric.metricDate} · ${latestMetric.grade}` : '暂无归档数据' }}</small></article>
    </section>

    <section class="center-section delivery-board">
      <header class="section-heading"><div><p>ACTIVE DELIVERIES</p><h2>当前可配送订单</h2></div><button type="button" class="text-action" @click="router.push('/rider')">进入调度台 →</button></header>
      <div v-if="orderedDeliveries.length" class="delivery-grid">
        <article v-for="order in orderedDeliveries" :key="order.id" class="delivery-ticket" :class="{ urgent: isUrgent(order) }">
          <div class="ticket-top"><span>#{{ order.id }}</span><b>{{ order.deliveryStatus === 'DELIVERING' ? '配送中' : '待取餐' }}</b><em v-if="isUrgent(order)">优先处理</em></div>
          <p>{{ order.deliveryAddress }}</p>
          <footer><span>承诺送达</span><strong>{{ formatTime(order.promiseEndAt) }}</strong></footer>
        </article>
      </div>
      <div v-else class="blank-state"><strong>暂无当前配送订单</strong><span>开始接单后，可在调度台领取附近配送任务。</span><button type="button" @click="router.push('/rider')">前往调度台</button></div>
    </section>

    <section class="record-grid">
      <article class="record-card ledger-card"><header><p>SETTLEMENTS</p><h2>收入流水</h2><span>{{ formatMoney(totalIncome) }}</span></header><ul v-if="settlements.length"><li v-for="item in settlements.slice(0, 6)" :key="item.id"><div><strong>{{ settlementLabel(item.settlementType) }}</strong><small>{{ item.balanceType }}</small></div><b :class="{ deduction: item.settlementType === 'OVERDUE_DEDUCTION' }">{{ formatMoney(item.amount) }}</b></li></ul><p v-else class="card-empty">暂无收入流水</p></article>
      <article class="record-card withdrawal-card"><header><p>WITHDRAWALS</p><h2>提现记录</h2><span>{{ withdrawals.length }} 笔</span></header><ul v-if="withdrawals.length"><li v-for="item in withdrawals.slice(0, 6)" :key="item.id"><div><strong>{{ formatMoney(item.amount) }}</strong><small>{{ item.createdAt ? formatTime(item.createdAt) : '提现申请' }}</small></div><b>{{ item.status }}</b></li></ul><p v-else class="card-empty">暂无提现记录</p></article>
      <article class="record-card review-card"><header><p>REVIEWS</p><h2>用户评价</h2><span>{{ averageScore }} 分</span></header><ul v-if="reviews.length"><li v-for="item in reviews.slice(0, 6)" :key="item.id"><div><strong>{{ item.score }} 星</strong><small>{{ item.tags || '未添加评价标签' }}</small></div><b>已评价</b></li></ul><p v-else class="card-empty">暂无用户评价</p></article>
      <article class="record-card metric-card-list"><header><p>PERFORMANCE</p><h2>每日表现</h2><span>{{ latestMetric?.grade || '—' }}</span></header><ul v-if="metrics.length"><li v-for="item in metrics.slice(0, 6)" :key="item.id"><div><strong>{{ item.metricDate }}</strong><small>表现归档</small></div><b>{{ item.finalScore }} 分 · {{ item.grade }}</b></li></ul><p v-else class="card-empty">暂无每日表现归档</p></article>
    </section>
  </main>
</template>

<style scoped>
.rider-center{--ink:#143238;--muted:#6a7c7f;--deep:#10383d;--teal:#007a76;--mint:#dff3ed;--sand:#f8f1e4;--line:#d8e4e1;--orange:#d85a36;max-width:1200px;margin:0 auto;padding:30px 24px 56px;color:var(--ink);font-family:"Noto Sans SC","Microsoft YaHei",sans-serif}.center-hero{min-height:220px;position:relative;isolation:isolate;overflow:hidden;border-radius:26px;padding:30px 34px;background:linear-gradient(125deg,#10383d,#155a59 65%,#0c2e34);box-shadow:0 18px 40px rgba(18,56,59,.18);color:#fff;display:flex;justify-content:space-between;align-items:flex-end}.hero-grid{position:absolute;inset:0;z-index:-1;opacity:.18;background-image:linear-gradient(rgba(255,255,255,.35) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.35) 1px,transparent 1px);background-size:28px 28px;mask-image:linear-gradient(90deg,transparent,black 42%,black)}.hero-content{position:relative}.back-link,.text-action{border:0;background:transparent;color:inherit;cursor:pointer;font:inherit}.back-link{padding:0;color:#bce6dd;font-size:13px}.eyebrow,.section-heading p,.record-card header p{margin:22px 0 7px;color:#85d8c8;font-size:11px;font-weight:800;letter-spacing:.14em}.identity-line{display:flex;align-items:center;gap:14px}.avatar-mark{display:grid;place-items:center;width:52px;height:52px;border:1px solid rgba(255,255,255,.35);border-radius:16px;background:rgba(255,255,255,.12);font-family:Georgia,"Noto Serif SC",serif;font-size:27px}.identity-line h1{font-family:Georgia,"Noto Serif SC",serif;font-size:32px;line-height:1;margin:0}.identity-line p{margin:8px 0 0;color:#c9e9e3;font-size:14px}.hero-state{display:flex;flex-direction:column;align-items:flex-end;gap:5px;padding:12px 0}.hero-state strong{font-size:17px}.hero-state small{color:#b7d9d3}.state-dot{width:9px;height:9px;border-radius:50%;background:#9aafb0}.state-dot.active{background:#76e0ad;box-shadow:0 0 0 5px rgba(118,224,173,.13)}.metric-strip{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:-30px;position:relative;z-index:2;padding:0 22px}.metric-strip article{min-height:117px;display:flex;flex-direction:column;justify-content:center;border-radius:18px;padding:19px;box-shadow:0 10px 24px rgba(29,52,51,.1)}.metric-strip span,.metric-strip small{color:var(--muted);font-size:12px}.metric-strip strong{font-family:Georgia,"Noto Serif SC",serif;font-size:25px;margin:7px 0 4px}.metric-strip i{font-family:"Noto Sans SC",sans-serif;font-size:13px;font-style:normal}.income{background:#fff6e9;border:1px solid #f1d8af}.balance{background:#e7f5ef;border:1px solid #bde4d6}.rating{background:#f0f4f6;border:1px solid #d4e2e7}.performance{background:#f8ece8;border:1px solid #efd0c7}.center-section{margin-top:22px;border:1px solid var(--line);border-radius:22px;background:#fffdf9;overflow:hidden}.section-heading{display:flex;align-items:center;justify-content:space-between;padding:20px 22px 17px;border-bottom:1px solid #e9efed}.section-heading p{margin:0 0 5px;color:var(--teal)}.section-heading h2,.record-card h2{font-family:Georgia,"Noto Serif SC",serif;font-size:22px;margin:0}.text-action{color:var(--teal);font-size:13px;font-weight:700}.delivery-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;padding:16px}.delivery-ticket{border:1px solid #dfe9e6;border-radius:15px;background:linear-gradient(145deg,#fff,#f3f9f7);padding:15px}.delivery-ticket.urgent{border-color:#e6a58f;background:#fff7f3}.ticket-top{display:flex;align-items:center;gap:7px;font-size:13px}.ticket-top span{font-weight:800}.ticket-top b,.ticket-top em{border-radius:999px;padding:3px 7px;font-size:11px;font-style:normal}.ticket-top b{background:#dff3ed;color:#006c67}.ticket-top em{background:#fde1d7;color:#ac3f25}.delivery-ticket>p{margin:13px 0 17px;line-height:1.55;font-size:14px}.delivery-ticket footer{display:flex;justify-content:space-between;color:var(--muted);font-size:12px}.delivery-ticket footer strong{color:var(--ink)}.blank-state{min-height:130px;display:flex;flex-direction:column;gap:7px;align-items:center;justify-content:center;color:var(--muted)}.blank-state strong{color:var(--ink)}.blank-state button{margin-top:5px;border:1px solid #b4d7d1;border-radius:8px;background:#edfaf7;color:var(--teal);padding:7px 12px;cursor:pointer}.record-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;margin-top:18px}.record-card{min-height:240px;border:1px solid var(--line);border-radius:20px;padding:20px;box-shadow:0 10px 24px rgba(24,55,53,.05)}.record-card header{display:flex;align-items:flex-end;justify-content:space-between;padding-bottom:13px;border-bottom:1px solid rgba(37,79,77,.14)}.record-card header p{margin:0 0 5px}.record-card header span{font-size:13px;font-weight:800}.ledger-card{background:#f1faf7}.ledger-card header p,.ledger-card li b{color:#006c67}.withdrawal-card{background:#fbf5ec}.withdrawal-card header p,.withdrawal-card li b{color:#a76b20}.review-card{background:#f2f7fa}.review-card header p,.review-card li b{color:#3e7184}.metric-card-list{background:#fbf1ee}.metric-card-list header p,.metric-card-list li b{color:#b35337}.record-card ul{list-style:none;margin:0;padding:4px 0}.record-card li{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:11px 2px;border-bottom:1px solid rgba(37,79,77,.11)}.record-card li:last-child{border-bottom:0}.record-card li div{display:flex;flex-direction:column;gap:3px;min-width:0}.record-card li strong{font-size:14px}.record-card li small{overflow:hidden;color:var(--muted);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.record-card li b{font-size:12px;white-space:nowrap}.record-card li b.deduction{color:var(--orange)}.card-empty{padding-top:36px;color:var(--muted);text-align:center}@media(max-width:880px){.metric-strip{grid-template-columns:repeat(2,1fr);padding:0 12px}.delivery-grid{grid-template-columns:1fr}.center-hero{align-items:flex-start;flex-direction:column}.hero-state{align-items:flex-start}}@media(max-width:620px){.rider-center{padding:18px 14px 40px}.center-hero{padding:24px 20px;border-radius:20px}.identity-line h1{font-size:27px}.metric-strip{grid-template-columns:1fr;margin-top:14px;padding:0}.record-grid{grid-template-columns:1fr}.section-heading{align-items:flex-start;gap:12px}.section-heading h2{font-size:20px}}
</style>
