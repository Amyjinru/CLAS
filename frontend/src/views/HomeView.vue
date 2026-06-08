<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { currentRole, listAnnouncements, listMerchants } from '../api/clas'

const merchants = ref([])
const announcements = ref([])
const keyword = ref('')
const category = ref('')
const sort = ref('score')
const categories = ['美食', '饮品', '休闲娱乐', '生活服务']

async function load() {
  merchants.value = await listMerchants({
    keyword: keyword.value || undefined,
    category: category.value || undefined,
    sort: sort.value
  })
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
      <p>搜索附近商家、购买外卖与团购券，让吃喝玩乐一键触达。</p>
    </div>
    <div class="hero-actions" v-if="currentRole() === 'USER'">
      <RouterLink class="button" to="/deals">团购到店</RouterLink>
      <RouterLink class="button secondary" to="/orders">我的订单</RouterLink>
    </div>
  </section>

  <section class="panel search-panel">
    <el-input v-model="keyword" placeholder="搜索商家、地点或分类" clearable @keyup.enter="load" />
    <el-select v-model="category" placeholder="全部分类" clearable>
      <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
    </el-select>
    <el-segmented v-model="sort" :options="[
      { label: '评分优先', value: 'score' },
      { label: '人均低价', value: 'price' },
      { label: '最新入驻', value: 'latest' }
    ]" />
    <el-button type="primary" @click="load">搜索</el-button>
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
      <p>评分 {{ merchant.score }} · 人均 ¥{{ ((merchant.averagePrice || 0) / 100).toFixed(0) }} · {{ merchant.businessHours || '营业中' }}</p>
      <p>起送 ¥{{ ((merchant.minOrderPrice || 0) / 100).toFixed(0) }} · 配送费 ¥{{ ((merchant.deliveryFee || 0) / 100).toFixed(0) }}</p>
      <RouterLink class="button secondary" :to="`/merchant/${merchant.id}`">进入店铺</RouterLink>
    </article>
  </section>
</template>

<style scoped>
.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.hero-actions,
.search-panel {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.search-panel :deep(.el-input) {
  max-width: 320px;
}

.search-panel :deep(.el-select) {
  width: 150px;
}
.section-head h2 {
  font-size: 18px;
  margin: 0;
  font-weight: 700;
  color: var(--text-primary);
}
.section-head a {
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
}

.announcement-preview {
  border-top: 1px solid var(--border-light);
  padding-top: 14px;
  margin-top: 14px;
}
.announcement-preview:first-of-type {
  border-top: 0;
  margin-top: 0;
  padding-top: 0;
}
.announcement-preview h3 {
  font-size: 16px;
  margin: 0 0 6px;
  color: var(--text-primary);
}
.announcement-preview p {
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}
</style>
