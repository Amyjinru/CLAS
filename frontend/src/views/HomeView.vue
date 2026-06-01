<script setup>
import { onMounted, ref } from 'vue'
import { listMerchants } from '../api/clas'

const merchants = ref([])

async function load() {
  merchants.value = await listMerchants()
}

onMounted(load)
</script>

<template>
  <section class="hero">
    <div>
      <h1>CLAS 外卖 MVP</h1>
      <p>浏览商家、选择商品、提交订单、模拟支付、商家接单、确认完成。</p>
    </div>
    <RouterLink class="button" to="/login">登录演示账号</RouterLink>
  </section>

  <section class="grid">
    <article class="card" v-for="merchant in merchants" :key="merchant.id">
      <div class="thumb">{{ merchant.category }}</div>
      <h2>{{ merchant.merchantName }}</h2>
      <p>{{ merchant.address }}</p>
      <p>评分 {{ merchant.score }} · {{ merchant.status }}</p>
      <RouterLink class="button secondary" :to="`/merchant/${merchant.id}`">进入店铺</RouterLink>
    </article>
  </section>
</template>
