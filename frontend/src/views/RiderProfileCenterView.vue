<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRiderProfile, listMyRiderMetrics, listMyRiderReviews, listMyRiderSettlements, listMyRiderWithdrawals } from '../api/clas'

const router = useRouter()
const profile = ref(null)
const settlements = ref([])
const withdrawals = ref([])
const reviews = ref([])
const metrics = ref([])
const loading = ref(false)

function formatMoney(cents) { return `¥${((Number(cents) || 0) / 100).toFixed(2)}` }

async function load() {
  loading.value = true
  try {
    const [nextProfile, nextSettlements, nextWithdrawals, nextReviews, nextMetrics] = await Promise.all([
      getRiderProfile(), listMyRiderSettlements(), listMyRiderWithdrawals(), listMyRiderReviews(), listMyRiderMetrics()
    ])
    profile.value = nextProfile; settlements.value = nextSettlements; withdrawals.value = nextWithdrawals; reviews.value = nextReviews; metrics.value = nextMetrics
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <main class="rider-center" v-loading="loading">
    <header><el-button text @click="router.push('/rider')">← 返回调度台</el-button><p>RIDER PROFILE</p><h1>个人中心</h1></header>
    <section class="profile-card"><div><span>骑手</span><h2>{{ profile?.realName || '—' }}</h2><p>{{ profile?.vehicleType || '—' }} · {{ profile?.serviceArea || '—' }}</p></div><div class="balances"><div><span>可提现</span><strong>{{ formatMoney(profile?.withdrawableBalance) }}</strong></div><div><span>冻结中</span><strong>{{ formatMoney(profile?.frozenBalance) }}</strong></div></div></section>
    <section class="data-grid"><article><h2>收入流水</h2><p v-for="item in settlements.slice(0, 6)" :key="item.id">{{ item.settlementType }} · {{ formatMoney(item.amount) }} · {{ item.balanceType }}</p><p v-if="!settlements.length" class="empty">暂无收入流水</p></article><article><h2>提现记录</h2><p v-for="item in withdrawals.slice(0, 6)" :key="item.id">{{ formatMoney(item.amount) }} · {{ item.status }}</p><p v-if="!withdrawals.length" class="empty">暂无提现记录</p></article><article><h2>用户评价</h2><p v-for="item in reviews.slice(0, 6)" :key="item.id">{{ item.score }} 星 · {{ item.tags || '未添加标签' }}</p><p v-if="!reviews.length" class="empty">暂无评价</p></article><article><h2>每日表现</h2><p v-for="item in metrics.slice(0, 6)" :key="item.id">{{ item.metricDate }} · {{ item.finalScore }} 分 · {{ item.grade }}</p><p v-if="!metrics.length" class="empty">暂无表现归档</p></article></section>
  </main>
</template>

<style scoped>
.rider-center { margin:0 auto; max-width:1020px; padding:30px 24px 48px; color:#16242e; font-family:"Noto Sans SC","Microsoft YaHei",sans-serif; }.rider-center header p { color:#006b68; font-size:11px; font-weight:800; letter-spacing:.14em; margin:20px 0 6px; }.rider-center h1,.profile-card h2,.data-grid h2 { font-family:Georgia,"Noto Serif SC",serif; }.rider-center h1 { font-size:32px; margin:0; }.profile-card { align-items:center; background:#f4f1e8; border:1px solid #e6dfd2; border-radius:20px; display:flex; justify-content:space-between; margin-top:22px; padding:28px; }.profile-card span,.profile-card p,.balances span,.empty { color:#6b7880; }.profile-card h2 { margin:5px 0; }.balances { display:flex; gap:32px; }.balances div { display:flex; flex-direction:column; gap:5px; }.balances strong { color:#006b68; font-size:22px; }.data-grid { display:grid; gap:16px; grid-template-columns:repeat(2,minmax(0,1fr)); margin-top:18px; }.data-grid article { border:1px solid #dce5e6; border-radius:16px; padding:18px; }.data-grid h2 { font-size:20px; margin:0 0 14px; }.data-grid p { border-bottom:1px solid #edf0f0; font-size:14px; margin:0; padding:10px 0; }.data-grid p:last-child { border:0; }@media(max-width:680px){.profile-card{align-items:flex-start;flex-direction:column;gap:22px}.data-grid{grid-template-columns:1fr}.balances{gap:22px}}
</style>
