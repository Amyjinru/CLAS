<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { getPaymentStatus, mockPay } from '../api/clas'

const route = useRoute()
const router = useRouter()
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
  PAID: '已支付',
  ACCEPTED: '商家已接单',
  COMPLETED: '已完成'
}

async function loadStatus() {
  try {
    paymentInfo.value = await getPaymentStatus(orderId.value)
  } catch (error) {
    message.value = error.response?.data?.message || '加载支付信息失败'
  }
}

async function submitPay() {
  loading.value = true
  message.value = '正在模拟支付，请稍候...'
  try {
    paymentInfo.value = await mockPay({ orderId: orderId.value, payMethod: payMethod.value })
    message.value = '支付成功'
  } catch (error) {
    message.value = error.response?.data?.message || '支付失败'
  } finally {
    loading.value = false
  }
}

function goOrders() {
  router.push('/orders')
}

onMounted(loadStatus)
</script>

<template>
  <BackButton to="/orders" label="返回我的订单" />

  <section class="panel narrow">
    <h1>模拟支付</h1>
    <p>订单号：{{ orderId }}</p>
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
      </select>
    </label>

    <div class="toolbar" v-if="paymentInfo?.orderStatus === 'PENDING_PAYMENT'">
      <button :disabled="loading" @click="submitPay">
        {{ loading ? '支付中...' : '确认支付' }}
      </button>
    </div>
    <div class="toolbar" v-else>
      <button class="secondary" @click="goOrders">查看我的订单</button>
    </div>
  </section>
</template>

<style scoped>
select {
  width: 100%;
  border: 1px solid #d8dde8;
  border-radius: 8px;
  min-height: 40px;
  padding: 8px 10px;
  font: inherit;
  background: white;
}
</style>
