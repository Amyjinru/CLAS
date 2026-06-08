<script setup>
import { onMounted, ref } from 'vue'
import { buyDeal, listDeals, listMerchants } from '../api/clas'
import { ElMessage } from 'element-plus'

const deals = ref([])
const merchants = ref([])
const merchantId = ref(null)

function yuan(value) {
  return ((value || 0) / 100).toFixed(2)
}

function merchantName(id) {
  return merchants.value.find((item) => item.id === id)?.merchantName || `商家 #${id}`
}

async function load() {
  const [dealList, merchantList] = await Promise.all([
    listDeals({ merchantId: merchantId.value || undefined }),
    listMerchants()
  ])
  deals.value = dealList
  merchants.value = merchantList
}

async function handleBuy(deal) {
  const order = await buyDeal(deal.id)
  ElMessage.success(`购买成功，券码 ${order.voucherCode}`)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="hero deals-hero">
    <div>
      <h1>到店团购</h1>
      <p>先买券，到店核销。适合下午茶、套餐、娱乐和本地生活服务。</p>
    </div>
  </section>

  <section class="panel filter-bar">
    <el-select v-model="merchantId" clearable placeholder="全部商家" @change="load">
      <el-option v-for="item in merchants" :key="item.id" :label="item.merchantName" :value="item.id" />
    </el-select>
  </section>

  <section class="grid">
    <article class="card deal-card" v-for="deal in deals" :key="deal.id">
      <div class="thumb">团购</div>
      <h2>{{ deal.title }}</h2>
      <p>{{ merchantName(deal.merchantId) }}</p>
      <p>{{ deal.description }}</p>
      <div class="price-row">
        <strong>¥{{ yuan(deal.dealPrice) }}</strong>
        <span>门市价 ¥{{ yuan(deal.originalPrice) }}</span>
      </div>
      <p>库存 {{ deal.stock }} · 有效期 {{ deal.validDays }} 天</p>
      <el-button type="primary" :disabled="deal.stock <= 0" @click="handleBuy(deal)">购买团购券</el-button>
    </article>
  </section>
</template>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: flex-end;
}
.filter-bar :deep(.el-select) {
  width: 220px;
}
.deal-card .price-row {
  align-items: baseline;
  display: flex;
  gap: 10px;
  margin: 12px 0;
}
.deal-card strong {
  color: var(--color-primary);
  font-size: 26px;
}
.deal-card span {
  color: var(--text-tertiary);
  text-decoration: line-through;
}
</style>
