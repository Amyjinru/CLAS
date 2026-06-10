<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createDeal, getMyMerchant, listMerchantDeals } from '../api/clas'
import { ElMessage } from 'element-plus'
import MerchantSidebar from '../components/merchant/MerchantSidebar.vue'
import MerchantProfileEditDialog from '../components/merchant/MerchantProfileEditDialog.vue'

const deals = ref([])
const merchant = ref(null)
const profileDialogVisible = ref(false)
const form = reactive({
  title: '',
  description: '',
  originalPrice: 0,
  dealPrice: 0,
  stock: 20,
  validDays: 30,
  status: 'ON_SALE'
})

function yuan(value) {
  return ((value || 0) / 100).toFixed(2)
}

async function load() {
  const [merchantInfo, dealList] = await Promise.all([getMyMerchant(), listMerchantDeals()])
  merchant.value = merchantInfo
  deals.value = dealList
}

async function submit() {
  await createDeal({
    ...form,
    originalPrice: Math.round(Number(form.originalPrice) * 100),
    dealPrice: Math.round(Number(form.dealPrice) * 100)
  })
  ElMessage.success('团购套餐已创建')
  Object.assign(form, { title: '', description: '', originalPrice: 0, dealPrice: 0, stock: 20, validDays: 30, status: 'ON_SALE' })
  await load()
}

onMounted(load)

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}
</script>

<template>
  <div class="merchant-page">
    <aside class="sidebar-panel">
      <MerchantSidebar active="deals" @edit-profile="profileDialogVisible = true" />
    </aside>
    <main class="merchant-main">
  <section class="hero">
    <div>
      <h1>团购管理</h1>
      <p>维护到店套餐、优惠券库存和有效期。</p>
    </div>
  </section>

  <section class="deal-layout">
    <div class="panel">
      <h2>新建团购</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="套餐名称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="门市价（元）"><el-input-number v-model="form.originalPrice" :min="0" /></el-form-item>
        <el-form-item label="团购价（元）"><el-input-number v-model="form.dealPrice" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="有效天数"><el-input-number v-model="form.validDays" :min="1" /></el-form-item>
        <el-button type="primary" @click="submit">创建套餐</el-button>
      </el-form>
    </div>

    <div class="panel">
      <h2>已有套餐</h2>
      <article class="deal-row" v-for="deal in deals" :key="deal.id">
        <div>
          <strong>{{ deal.title }}</strong>
          <p>{{ deal.description }}</p>
        </div>
        <div class="deal-price">¥{{ yuan(deal.dealPrice) }}</div>
      </article>
    </div>
  </section>
    </main>
    <MerchantProfileEditDialog
      v-model:visible="profileDialogVisible"
      :merchant="merchant"
      @saved="onMerchantProfileSaved"
    />
  </div>
</template>

<style scoped>
.merchant-page {
  display: flex;
  gap: 24px;
  margin: 30px auto;
  max-width: 1200px;
  padding: 0 20px 48px;
}

.sidebar-panel {
  flex: 1;
  min-width: 260px;
}

.merchant-main {
  flex: 3;
  min-width: 0;
}

.deal-layout {
  display: grid;
  gap: 18px;
  grid-template-columns: 360px minmax(0, 1fr);
}
.panel h2 {
  margin-top: 0;
}
.deal-row {
  align-items: center;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  padding: 16px 0;
}
.deal-row p {
  color: var(--text-secondary);
  margin: 6px 0 0;
}
.deal-price {
  color: var(--color-primary);
  font-size: 20px;
  font-weight: 800;
}
@media (max-width: 900px) {
  .merchant-page {
    flex-direction: column;
  }

  .deal-layout {
    grid-template-columns: 1fr;
  }
}
</style>
