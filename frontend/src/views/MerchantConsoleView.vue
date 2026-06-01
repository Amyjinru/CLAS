<script setup>
import { onMounted, ref } from 'vue'
import { acceptOrder, listMerchantOrders } from '../api/clas'

const orders = ref([])
const message = ref('')

async function load() {
  try {
    orders.value = await listMerchantOrders(1)
  } catch (error) {
    message.value = '请使用 merchant / 123456 登录'
  }
}

async function operate(action, order) {
  if (action === 'accept') await acceptOrder(order.order.id)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <h1>商家工作台</h1>
    <p>处理已支付订单，完成 MVP 的商家接单环节。</p>
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
        <button v-if="order.order.status === 'PAID'" @click="operate('accept', order)">接单</button>
      </div>
    </article>
  </section>
</template>
