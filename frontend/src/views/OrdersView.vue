<script setup>
import { onMounted, ref } from 'vue'
import { addReview, completeOrder, listOrders, payOrder } from '../api/clas'

const orders = ref([])
const message = ref('')
const reviewContent = ref('很好吃，配送顺利。')

async function load() {
  try {
    orders.value = await listOrders()
  } catch (error) {
    message.value = '请先登录'
  }
}

async function pay(order) {
  await payOrder(order.order.id)
  await load()
}

async function complete(order) {
  await completeOrder(order.order.id)
  await load()
}

async function review(order) {
  await addReview({ orderId: order.order.id, score: 5, content: reviewContent.value })
  message.value = `订单 ${order.order.id} 已评价`
  await load()
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <h1>订单列表</h1>
    <p>{{ message }}</p>
  </section>
  <section class="list">
    <article class="row" v-for="order in orders" :key="order.order.id">
      <div>
        <h2>订单 {{ order.order.id }}</h2>
        <p>{{ order.order.status }} · ¥{{ (order.order.totalPrice / 100).toFixed(2) }}</p>
        <p>{{ order.items.length }} 件商品</p>
      </div>
      <div class="row-actions">
        <button v-if="order.order.status === 'PENDING_PAYMENT'" @click="pay(order)">模拟支付</button>
        <button v-if="order.order.status === 'ACCEPTED'" @click="complete(order)">确认完成</button>
        <button v-if="order.order.status === 'COMPLETED'" @click="review(order)">评价</button>
      </div>
    </article>
  </section>
</template>
