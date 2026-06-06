<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { cancelOrder, completeOrder, getReviewByOrder, listOrders } from '../api/clas'

const orders = ref([])
const message = ref('')
const reviewedOrderIds = ref(new Set())

const statusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '商家已接单',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REJECTED: '商家已拒单',
  REFUNDED: '已退款'
}

async function load() {
  try {
    orders.value = await listOrders()
    const reviewed = await Promise.all(
      orders.value
        .filter((order) => order.order.status === 'COMPLETED')
        .map(async (order) => {
          try {
            const review = await getReviewByOrder(order.order.id)
            return review ? order.order.id : null
          } catch {
            return null
          }
        })
    )
    reviewedOrderIds.value = new Set(reviewed.filter(Boolean))
  } catch {
    message.value = '请先登录'
  }
}

async function complete(order) {
  await completeOrder(order.order.id)
  await load()
}

async function cancel(order) {
  await cancelOrder(order.order.id)
  await load()
}

function hasReview(orderId) {
  return reviewedOrderIds.value.has(orderId)
}

onMounted(load)
</script>

<template>
  <BackButton to="/home" label="返回首页" />

  <section class="panel">
    <h1>我的订单</h1>
    <p>{{ message }}</p>
  </section>
  <section class="list">
    <article class="row" v-for="order in orders" :key="order.order.id">
      <div>
        <h2>订单 {{ order.order.id }}</h2>
        <p>{{ statusLabel[order.order.status] || order.order.status }} · ¥{{ (order.order.totalPrice / 100).toFixed(2) }}</p>
        <p>{{ order.items.length }} 件商品</p>
      </div>
      <div class="row-actions">
        <RouterLink
          v-if="order.order.status === 'PENDING_PAYMENT'"
          class="button"
          :to="`/payment/${order.order.id}`"
        >
          去支付
        </RouterLink>
        <button
          v-if="['PENDING_PAYMENT', 'PAID'].includes(order.order.status)"
          class="secondary"
          @click="cancel(order)"
        >
          取消订单
        </button>
        <button v-if="order.order.status === 'ACCEPTED'" @click="complete(order)">确认完成</button>
        <RouterLink
          v-if="order.order.status === 'COMPLETED' && !hasReview(order.order.id)"
          class="button secondary"
          :to="`/review/${order.order.id}`"
        >
          去评价
        </RouterLink>
        <span v-if="order.order.status === 'COMPLETED' && hasReview(order.order.id)" class="tag">已评价</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.tag {
  background: #ecfdf5;
  border-radius: 999px;
  color: #047857;
  font-size: 13px;
  font-weight: 700;
  padding: 6px 12px;
}
</style>
