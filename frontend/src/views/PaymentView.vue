<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { getDealPaymentStatus, getPaymentStatus, mockPay, payDealOrder } from '../api/clas'

const route = useRoute()
const router = useRouter()
const isDealPayment = computed(() => route.name === 'DealPayment')
const orderId = computed(() => Number(route.params.orderId))
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
  PAID: '已支付(自动接单中)',
  ACCEPTED: '已支付(自动接单中)',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REJECTED: '商家已拒单',
  REFUNDED: '已退款',
  UNUSED: '待使用',
  USED: '已核销'
}

const backTarget = computed(() => (isDealPayment.value ? '/deals' : '/orders'))
const backLabel = computed(() => (isDealPayment.value ? '返回团购' : '返回我的订单'))
const pageTitle = computed(() => (isDealPayment.value ? '团购券支付' : '模拟支付'))
const orderLabel = computed(() => (isDealPayment.value ? '团购订单号' : '订单号'))

async function loadStatus() {
  try {
    paymentInfo.value = isDealPayment.value
      ? await getDealPaymentStatus(orderId.value)
      : await getPaymentStatus(orderId.value)
  } catch (error) {
    message.value = error.response?.data?.message || '加载支付信息失败'
  }
}

async function submitPay() {
  loading.value = true
  message.value = '正在模拟支付，请稍候...'
  try {
    paymentInfo.value = isDealPayment.value
      ? await payDealOrder(orderId.value, payMethod.value)
      : await mockPay({ orderId: orderId.value, payMethod: payMethod.value })
    if (paymentInfo.value.paymentStatus === 'FAILED') {
      message.value = '模拟支付失败，请更换方式后重试'
    } else {
      message.value = isDealPayment.value ? '支付成功，团购券已生成' : '支付成功'
    }
  } catch (error) {
    message.value = error.response?.data?.message || '支付失败'
  } finally {
    loading.value = false
  }
}

function goNext() {
  router.push(isDealPayment.value ? '/profile' : '/orders')
}

onMounted(loadStatus)
</script>

<template>
  <BackButton :to="backTarget" :label="backLabel" />

  <section class="panel narrow">
    <h1>{{ pageTitle }}</h1>
    <p>{{ orderLabel }}：{{ orderId }}</p>
    <p v-if="paymentInfo">
      应付金额：¥{{ (paymentInfo.amount / 100).toFixed(2) }}
    </p>
    <p v-if="paymentInfo">
      支付状态：{{ statusLabel[paymentInfo.paymentStatus] || paymentInfo.paymentStatus }}
    </p>
    <p v-if="paymentInfo">
      订单状态：{{ orderStatusLabel[paymentInfo.orderStatus] || paymentInfo.orderStatus }}
    </p>
    <p class="message">{{ message }}</p>

    <label v-if="paymentInfo?.orderStatus === 'PENDING_PAYMENT'">
      支付方式
      <select v-model="payMethod">
        <option value="MOCK">模拟支付</option>
        <option value="WECHAT">微信支付（模拟）</option>
        <option value="ALIPAY">支付宝（模拟）</option>
        <option value="FAIL_MOCK">模拟支付失败</option>
      </select>
    </label>

    <div class="toolbar" v-if="paymentInfo?.orderStatus === 'PENDING_PAYMENT'">
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
</style>
