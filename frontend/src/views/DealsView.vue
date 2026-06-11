<script setup>
import { onMounted, ref, watch } from 'vue'
import { listDeals, listMerchants } from '../api/clas'

const deals = ref([])
const merchants = ref([])
const merchantId = ref(null)
const merchantSearch = ref('')

function yuan(value) {
  return ((value || 0) / 100).toFixed(2)
}

function merchantName(id) {
  return merchants.value.find((item) => item.id === id)?.merchantName || `商家 #${id}`
}

function queryMerchants(queryString, cb) {
  const keyword = queryString.trim().toLowerCase()
  if (!keyword) {
    cb([])
    return
  }
  cb(
    merchants.value.filter((item) =>
      (item.merchantName || '').toLowerCase().includes(keyword)
    )
  )
}

function handleMerchantSelect(item) {
  merchantId.value = item.id
  merchantSearch.value = item.merchantName
  loadDeals()
}

function clearMerchantFilter() {
  merchantId.value = null
  merchantSearch.value = ''
  loadDeals()
}

async function loadMerchants() {
  merchants.value = await listMerchants()
}

async function loadDeals() {
  deals.value = await listDeals({ merchantId: merchantId.value || undefined })
}

watch(merchantSearch, (value) => {
  if (!value?.trim()) {
    merchantId.value = null
    loadDeals()
    return
  }
  const matched = merchants.value.find((item) => item.merchantName === value)
  if (matched && merchantId.value !== matched.id) {
    merchantId.value = matched.id
    loadDeals()
  } else if (!matched && merchantId.value !== null) {
    merchantId.value = null
    loadDeals()
  }
})

onMounted(async () => {
  await loadMerchants()
  await loadDeals()
})
</script>

<template>
  <div class="user-page deals-page">
    <section class="hero deals-hero">
      <div>
        <h1>到店团购</h1>
        <p>先看详情，再买券到店核销。适合下午茶、套餐、娱乐和本地生活服务。</p>
      </div>
    </section>

    <section class="panel filter-bar">
      <p class="filter-tip">搜索商家筛选团购套餐</p>
      <el-autocomplete
        v-model="merchantSearch"
        class="merchant-search"
        clearable
        placeholder="输入商家名称搜索"
        value-key="merchantName"
        :fetch-suggestions="queryMerchants"
        :trigger-on-focus="false"
        @select="handleMerchantSelect"
        @clear="clearMerchantFilter"
      >
        <template #default="{ item }">
          <span class="merchant-suggestion">{{ item.merchantName }}</span>
        </template>
      </el-autocomplete>
    </section>

    <section class="user-page-grid-3 deals-grid">
      <article class="card deal-card" v-for="deal in deals" :key="deal.id">
        <div class="thumb">团购</div>
        <h2>{{ deal.title }}</h2>
        <p>{{ merchantName(deal.merchantId) }}</p>
        <p class="deal-desc">{{ deal.description }}</p>
        <div class="price-row">
          <strong>¥{{ yuan(deal.dealPrice) }}</strong>
          <span>门市价 ¥{{ yuan(deal.originalPrice) }}</span>
        </div>
        <p class="deal-meta">库存 {{ deal.stock }} · 有效期 {{ deal.validDays }} 天</p>
        <div class="deal-actions">
          <RouterLink class="button deal-detail-link" :to="`/deals/${deal.id}`">查看详情</RouterLink>
        </div>
      </article>
    </section>

    <el-empty v-if="!deals.length" description="暂无团购套餐" />
  </div>
</template>

<style scoped>
.deals-page {
  display: grid;
  gap: 20px;
}

.filter-bar {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 0;
}

.filter-tip {
  color: var(--text-secondary);
  margin: 0;
}

.merchant-search {
  width: 320px;
}

.merchant-suggestion {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deal-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.deal-desc {
  flex: 1;
}

.deal-meta {
  margin-bottom: 14px !important;
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

.deal-actions {
  display: flex;
}

.deal-detail-link {
  width: 100%;
}

@media (max-width: 768px) {
  .filter-bar {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .merchant-search {
    width: 100%;
  }
}
</style>
