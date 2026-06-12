<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createDeal, getMyMerchant, listMerchantDeals } from '../api/clas'
import { ElMessage } from 'element-plus'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'

const router = useRouter()
const deals = ref([])
const merchant = ref(null)
const loading = ref(false)
const creating = ref(false)
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

const dealStats = computed(() => {
  const onSale = deals.value.filter((deal) => deal.status === 'ON_SALE').length
  const stock = deals.value.reduce((sum, deal) => sum + Number(deal.stock || 0), 0)
  return { total: deals.value.length, onSale, stock }
})

function statusMeta(deal) {
  if (deal.status === 'OFF_SALE') return { label: '已下架', type: 'info' }
  if (Number(deal.stock || 0) <= 0) return { label: '已售罄', type: 'warning' }
  return { label: '售卖中', type: 'success' }
}

function normalizeMoney(value) {
  return Math.round(Number(value || 0) * 100) / 100
}

function normalizeInteger(value, min = 0) {
  return Math.max(min, Math.trunc(Number(value || 0)))
}

async function load() {
  loading.value = true
  try {
    const [merchantInfo, dealList] = await Promise.all([getMyMerchant(), listMerchantDeals()])
    merchant.value = merchantInfo
    deals.value = dealList
  } finally {
    loading.value = false
  }
}

async function submit() {
  creating.value = true
  try {
    form.originalPrice = normalizeMoney(form.originalPrice)
    form.dealPrice = normalizeMoney(form.dealPrice)
    form.stock = normalizeInteger(form.stock, 0)
    form.validDays = normalizeInteger(form.validDays, 1)
    const deal = await createDeal({
      ...form,
      originalPrice: Math.round(Number(form.originalPrice) * 100),
      dealPrice: Math.round(Number(form.dealPrice) * 100)
    })
    ElMessage.success('团购套餐已创建')
    Object.assign(form, { title: '', description: '', originalPrice: 0, dealPrice: 0, stock: 20, validDays: 30, status: 'ON_SALE' })
    await load()
    if (deal?.id) {
      router.push(`/merchant/deals/${deal.id}`)
    }
  } finally {
    creating.value = false
  }
}

onMounted(load)

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}
</script>

<template>
  <MerchantWorkspaceShell :merchant="merchant" :loading="loading" active-module="deals" @merchant-updated="onMerchantProfileSaved">
    <main class="merchant-main">
      <section class="hero">
        <div>
          <h1>团购管理</h1>
          <p>维护到店套餐、优惠券价格、库存和有效期。</p>
        </div>
        <div class="stats-strip" aria-label="团购概览">
          <span><strong>{{ dealStats.total }}</strong> 个套餐</span>
          <span><strong>{{ dealStats.onSale }}</strong> 个在售</span>
          <span><strong>{{ dealStats.stock }}</strong> 份库存</span>
        </div>
      </section>

      <section class="deal-layout">
        <div class="panel create-panel">
          <h2>新建团购</h2>
          <el-form :model="form" label-position="top">
            <el-form-item label="套餐名称"><el-input v-model="form.title" /></el-form-item>
            <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
            <div class="form-grid">
              <el-form-item label="门市价（元）"><el-input-number v-model="form.originalPrice" :min="0.01" :precision="2" :step="0.01" /></el-form-item>
              <el-form-item label="团购价（元）"><el-input-number v-model="form.dealPrice" :min="0.01" :precision="2" :step="0.01" /></el-form-item>
              <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" :precision="0" :step="1" step-strictly /></el-form-item>
              <el-form-item label="有效天数"><el-input-number v-model="form.validDays" :min="1" :precision="0" :step="1" step-strictly /></el-form-item>
            </div>
            <el-button type="primary" :loading="creating" @click="submit">创建套餐</el-button>
          </el-form>
        </div>

        <div class="panel deal-list-panel">
          <h2>已有套餐</h2>
          <el-empty v-if="!deals.length" description="暂无团购套餐" />
          <article class="deal-row" v-for="deal in deals" :key="deal.id">
            <div class="deal-copy">
              <div class="deal-title-line">
                <strong>{{ deal.title }}</strong>
                <el-tag :type="statusMeta(deal).type" size="small">{{ statusMeta(deal).label }}</el-tag>
              </div>
              <p>{{ deal.description || '暂无套餐说明' }}</p>
              <div class="deal-meta">
                <span>库存 {{ deal.stock }} 份</span>
                <span>有效 {{ deal.validDays || 30 }} 天</span>
                <span>门市价 ¥{{ yuan(deal.originalPrice) }}</span>
              </div>
            </div>
            <div class="deal-actions">
              <div class="deal-price">¥{{ yuan(deal.dealPrice) }}</div>
              <el-button type="primary" plain @click="router.push(`/merchant/deals/${deal.id}`)">查看/编辑</el-button>
            </div>
          </article>
        </div>
      </section>
    </main>
  </MerchantWorkspaceShell>
</template>

<style scoped>
.merchant-main {
  min-width: 0;
  padding: 0;
}

.stats-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.stats-strip span {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(214, 171, 43, 0.22);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  display: inline-flex;
  gap: 6px;
  padding: 10px 12px;
}

.stats-strip strong {
  color: var(--color-primary);
}

.deal-layout {
  display: grid;
  gap: 18px;
  grid-template-columns: 360px minmax(0, 1fr);
}

.panel h2 {
  margin-top: 0;
}

.create-panel {
  align-self: start;
  position: sticky;
  top: 88px;
}

.form-grid {
  display: grid;
  gap: 0 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.create-panel :deep(.el-input-number) {
  width: 100%;
}

.deal-list-panel {
  min-width: 0;
}

.deal-row {
  align-items: flex-start;
  border-top: 1px solid var(--border-light);
  display: flex;
  gap: 18px;
  justify-content: space-between;
  padding: 16px 0;
}

.deal-title-line {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.deal-row p {
  color: var(--text-secondary);
  margin: 6px 0 0;
}

.deal-copy {
  min-width: 0;
}

.deal-meta {
  color: var(--text-tertiary);
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  gap: 8px 14px;
  margin-top: 10px;
}

.deal-actions {
  align-items: flex-end;
  display: grid;
  gap: 10px;
  justify-items: end;
}

.deal-price {
  color: var(--color-primary);
  font-size: 20px;
  font-weight: 800;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .deal-layout {
    grid-template-columns: 1fr;
  }

  .create-panel {
    position: static;
  }
}

@media (max-width: 640px) {
  .deal-row {
    display: grid;
  }

  .deal-actions {
    justify-items: start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
