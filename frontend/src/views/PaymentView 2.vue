<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import {
  getBatchPaymentStatus,
  getDealPaymentStatus,
  getPaymentStatus,
  mockPay,
  mockPayBatch,
  payDealOrder
} from '../api/clas'

const route = useRoute()
const router = useRouter()
const isDealPayment = computed(() => route.name === 'DealPayment')
const isBatchPayment = computed(() => route.name === 'BatchPayment')
const orderId = computed(() => Number(route.params.orderId))
const batchOrderIds = computed(() => [...new Set(String(route.query.orderIds || '')
  .split(',')
  .map((value) => Number(value))
  .filter((value) => Number.isInteger(value) && value > 0))])
const paymentInfo = ref(null)
const payMethod = ref('MOCK')
const loading = ref(false)
const message = ref('')

const statusLabel = {
  PENDING: '待支付',
  SUCCESS: '支付成功',
  FAILED: '支付失败'
}

const orderStatusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '订单处理中',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REJECTED: '商家已拒单',
  REFUNDED: '已退款',
  UNUSED: '待使用',
  USED: '已核销'
}

const backTarget = computed(() => (isDealPayment.value ? '/deals' : (isBatchPayment.value ? '/cart' : '/orders')))
const backLabel = computed(() => (isDealPayment.value ? '返回团购' : (isBatchPayment.value ? '返回购物车' : '返回我的订单')))
const pageTitle = computed(() => (isDealPayment.value ? '团购券支付' : (isBatchPayment.value ? '合并付款' : '模拟支付')))
const orderLabel = computed(() => (isDealPayment.value ? '团购订单号' : '订单号'))
const displayAmount = computed(() => isBatchPayment.value ? paymentInfo.value?.totalAmount : paymentInfo.value?.amount)
const hasBatchPayableOrders = computed(() => paymentInfo.value?.payments?.some((item) => item.orderStatus === 'PENDING_PAYMENT'))
const showPaymentForm = computed(() => isBatchPayment.value
  ? hasBatchPayableOrders.value
  : paymentInfo.value?.orderStatus === 'PENDING_PAYMENT')

async function loadStatus() {
  try {
    if (isBatchPayment.value) {
      if (!batchOrderIds.value.length) throw new Error('EMPTY_BATCH')
      paymentInfo.value = await getBatchPaymentStatus(batchOrderIds.value)
    } else {
      paymentInfo.value = isDealPayment.value
        ? await getDealPaymentStatus(orderId.value)
        : await getPaymentStatus(orderId.value)
    }
  } catch (error) {
    message.value = error.response?.data?.message || '加载支付信息失败'
  }
}

async function submitPay() {
  loading.value = true
  message.value = '正在模拟支付，请稍候...'
  try {
    if (isBatchPayment.value) {
      paymentInfo.value = await mockPayBatch({
        orderIds: batchOrderIds.value,
        payMethod: payMethod.value,
        idempotencyKey: `batch-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
      })
    } else {
      paymentInfo.value = isDealPayment.value
        ? await payDealOrder(orderId.value, payMethod.value)
        : await mockPay({ orderId: orderId.value, payMethod: payMethod.value })
    }
    if (paymentInfo.value.paymentStatus === 'FAILED') {
      message.value = '模拟支付失败，请更换方式后重试'
    } else if (paymentInfo.value.paymentStatus === 'PARTIAL') {
      message.value = '部分订单支付成功，请重试剩余待支付订单'
    } else {
      message.value = isDealPayment.value ? '支付成功，团购券已生成' : (isBatchPayment.value ? '全部订单支付成功' : '支付成功')
    }
  } catch (error) {
    message.value = error.response?.data?.message || '支付失败'
  } finally {
    loading.value = false
  }
}

function goNext() {
  router.push(isDealPayment.value ? '/profile' : (isBatchPayment.value ? '/orders?tab=receiving' : '/orders'))
}

onMounted(loadStatus)
</script>

<template>
  <BackButton :to="backTarget" :label="backLabel" />

  <section class="panel narrow">
    <h1>{{ pageTitle }}</h1>
    <p v-if="!isBatchPayment">{{ orderLabel }}：{{ orderId }}</p>
    <p v-else>本次包含 {{ batchOrderIds.length }} 张订单</p>
    <p v-if="paymentInfo">
      应付金额：¥{{ ((displayAmount || 0) / 100).toFixed(2) }}
    </p>
    <p v-if="paymentInfo && !isBatchPayment">
      支付状态：{{ statusLabel[paymentInfo.paymentStatus] || paymentInfo.paymentStatus }}
    </p>
    <p v-if="paymentInfo && !isBatchPayment">
      订单状态：{{ orderStatusLabel[paymentInfo.orderStatus] || paymentInfo.orderStatus }}
    </p>
    <div v-if="isBatchPayment && paymentInfo" class="batch-orders">
      <div v-for="item in paymentInfo.payments" :key="item.orderId" class="batch-order-row">
        <span>订单 #{{ item.orderId }}</span>
        <span>¥{{ ((item.amount || 0) / 100).toFixed(2) }}</span>
        <span>{{ statusLabel[item.paymentStatus] || item.paymentStatus }}</span>
      </div>
    </div>
    <p class="message">{{ message }}</p>

    <label v-if="showPaymentForm">
      支付方式
      <select v-model="payMethod">
        <option value="MOCK">模拟支付</option>
        <option value="WECHAT">微信支付（模拟）</option>
        <option value="ALIPAY">支付宝（模拟）</option>
        <option value="FAIL_MOCK">模拟支付失败</option>
      </select>
    </label>

    <div class="toolbar" v-if="showPaymentForm">
      <button :disabled="loading" @click="submitPay">
        {{ loading ? '支付中...' : '确认支付' }}
      </button>
    </div>
    <div class="toolbar" v-else>
      <button class="secondary" @click="goNext">
        {{ isDealPayment ? '查看我的团购券' : '查看我的订单' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
select {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  min-height: 40px;
  padding: 8px 10px;
  font: inherit;
  background: white;
}
.batch-orders {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  margin: 16px 0;
  overflow: hidden;
}
.batch-order-row {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr auto auto;
  padding: 10px 12px;
}
.batch-order-row + .batch-order-row {
  border-top: 1px solid var(--border-color);
}
</style>
