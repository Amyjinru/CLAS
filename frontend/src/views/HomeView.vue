<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { currentRole, listAnnouncements, listMerchants } from '../api/clas'

const merchants = ref([])
const announcements = ref([])

async function load() {
  merchants.value = await listMerchants()
  try {
    announcements.value = await listAnnouncements()
  } catch {
    announcements.value = []
  }
}

onMounted(load)
</script>

<template>
  <section class="hero">
    <div>
      <h1>CLAS 综合生活助手平台</h1>
      <p>浏览商家、选择商品、提交订单、模拟支付、商家接单、确认完成与评价。</p>
    </div>
    <RouterLink v-if="currentRole() === 'USER'" class="button" to="/orders">我的订单</RouterLink>
  </section>

  <section class="panel" v-if="announcements.length">
    <div class="section-head">
      <h2>平台公告</h2>
      <RouterLink to="/user/announcements">查看全部</RouterLink>
    </div>
    <article class="announcement-preview" v-for="item in announcements.slice(0, 2)" :key="item.id">
      <h3>{{ item.title }}</h3>
      <p>{{ item.content }}</p>
    </article>
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

<style scoped>
.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head h2 {
  font-size: 18px;
  margin: 0;
}

.section-head a {
  color: #2563eb;
  font-size: 14px;
}

.announcement-preview {
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
  margin-top: 12px;
}

.announcement-preview:first-of-type {
  border-top: 0;
  margin-top: 0;
  padding-top: 0;
}

.announcement-preview h3 {
  font-size: 16px;
  margin: 0 0 6px;
}

.announcement-preview p {
  color: #667085;
  margin: 0;
}
</style>
