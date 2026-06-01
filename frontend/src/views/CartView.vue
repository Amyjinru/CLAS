<script setup>
import { onMounted, ref } from 'vue'
import { createOrder, getCart } from '../api/clas'

const items = ref([])
const message = ref('')

const total = () => items.value.reduce((sum, item) => sum + item.subtotal, 0)
const merchantIds = () => [...new Set(items.value.map((item) => item.merchantId).filter(Boolean))]

async function load() {
  try {
    items.value = await getCart()
  } catch (error) {
    message.value = '请先登录后查看购物车'
  }
}

async function submit() {
  if (!items.value.length) return
  const merchantId = merchantIds()[0]
  const data = await createOrder({ merchantId })
  message.value = `订单 ${data.order.id} 已创建，待支付`
  await load()
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <h1>购物车</h1>
    <p v-if="merchantIds().length > 1">购物车包含多个商家商品，本次会提交第一个商家的商品。</p>
  </section>
  <section class="list">
    <article class="row" v-for="item in items" :key="item.productId">
      <div>
        <h2>{{ item.productName }}</h2>
        <p>数量 {{ item.quantity }}</p>
      </div>
      <strong>¥{{ (item.subtotal / 100).toFixed(2) }}</strong>
    </article>
  </section>
  <footer class="checkout">
    <span>合计 ¥{{ (total() / 100).toFixed(2) }}</span>
    <button @click="submit">提交订单</button>
  </footer>
  <p class="message">{{ message }}</p>
</template>
