<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyMerchant, listMerchantDeals, updateMerchantDeal } from '../api/clas'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'

const route = useRoute()
const router = useRouter()

const merchant = ref(null)
const deal = ref(null)
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')

const form = reactive({
  title: '',
  description: '',
  originalPrice: 0,
  dealPrice: 0,
  stock: 0,
  validDays: 30,
  status: 'ON_SALE'
})

const dealId = computed(() => Number(route.params.id))
const isSoldOut = computed(() => Number(deal.value?.stock || 0) <= 0)
const isOffSale = computed(() => deal.value?.status === 'OFF_SALE')
const discountText = computed(() => {
  if (!deal.value?.originalPrice || !deal.value?.dealPrice) return '优惠待计算'
  const discount = Math.round((deal.value.dealPrice / deal.value.originalPrice) * 100) / 10
  return `${discount} 折`
})

function yuan(value) {
  return ((value || 0) / 100).toFixed(2)
}

function statusLabel() {
  if (!deal.value) return ''
  if (isOffSale.value) return '已下架'
  if (isSoldOut.value) return '已售罄'
  return '售卖中'
}

function statusType() {
  if (isOffSale.value || isSoldOut.value) return 'info'
  return 'success'
}

function normalizeMoney(value) {
  return Math.round(Number(value || 0) * 100) / 100
}

function normalizeInteger(value, min = 0) {
  return Math.max(min, Math.trunc(Number(value || 0)))
}

function copyToForm(nextDeal) {
  Object.assign(form, {
    title: nextDeal.title || '',
    description: nextDeal.description || '',
    originalPrice: Number(nextDeal.originalPrice || 0) / 100,
    dealPrice: Number(nextDeal.dealPrice || 0) / 100,
    stock: Number(nextDeal.stock || 0),
    validDays: Number(nextDeal.validDays || 30),
    status: nextDeal.status || 'ON_SALE'
  })
}

async function load() {
  if (!dealId.value) {
    loadError.value = '团购不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const [merchantInfo, dealList] = await Promise.all([getMyMerchant(), listMerchantDeals()])
    merchant.value = merchantInfo
    const foundDeal = dealList.find((item) => Number(item.id) === dealId.value)
    if (!foundDeal) {
      loadError.value = '该团购不存在，或不属于当前店铺'
      deal.value = null
      return
    }
    deal.value = foundDeal
    copyToForm(foundDeal)
  } catch (error) {
    loadError.value = error.response?.data?.message || '团购详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!deal.value || saving.value) return
  saving.value = true
  try {
    form.originalPrice = normalizeMoney(form.originalPrice)
    form.dealPrice = normalizeMoney(form.dealPrice)
    form.stock = normalizeInteger(form.stock, 0)
    form.validDays = normalizeInteger(form.validDays, 1)
    const nextDeal = await updateMerchantDeal(deal.value.id, {
      title: form.title,
      description: form.description,
      originalPrice: Math.round(Number(form.originalPrice || 0) * 100),
      dealPrice: Math.round(Number(form.dealPrice || 0) * 100),
      stock: Number(form.stock || 0),
      validDays: Number(form.validDays || 1),
      status: form.status
    })
    deal.value = nextDeal
    copyToForm(nextDeal)
    ElMessage.success('团购信息已更新')
  } finally {
    saving.value = false
  }
}

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <MerchantWorkspaceShell :merchant="merchant" :loading="loading" active-module="deals" @merchant-updated="onMerchantProfileSaved">
    <main class="merchant-main merchant-deal-detail">
      <section class="detail-toolbar">
        <el-button @click="router.push('/merchant/deals')">返回团购管理</el-button>
      </section>

      <section v-if="loadError" class="panel unavailable-panel">
        <h1>团购详情暂不可用</h1>
        <p>{{ loadError }}</p>
        <el-button type="primary" @click="router.push('/merchant/deals')">回到列表</el-button>
      </section>

      <template v-else-if="deal">
        <section class="hero deal-hero">
          <div class="hero-copy">
            <el-tag :type="statusType()">{{ statusLabel() }}</el-tag>
            <h1>{{ deal.title }}</h1>
            <p>{{ merchant?.merchantName || `商家 #${deal.merchantId}` }} · 到店出示券码核销</p>
          </div>
          <div class="price-panel" aria-label="团购价格">
            <span>团购价</span>
            <strong>¥{{ yuan(deal.dealPrice) }}</strong>
            <p>门市价 ¥{{ yuan(deal.originalPrice) }} · {{ discountText }}</p>
          </div>
        </section>

        <section class="detail-layout">
          <article class="panel deal-main">
            <div class="detail-section">
              <p class="section-kicker">套餐说明</p>
              <h2>{{ deal.title }}</h2>
              <p class="deal-description">{{ deal.description || '商家暂未填写详细说明。' }}</p>
            </div>

            <div class="info-grid">
              <div>
                <span>剩余份数</span>
                <strong>{{ deal.stock }} 份</strong>
              </div>
              <div>
                <span>有效期</span>
                <strong>{{ deal.validDays || 30 }} 天</strong>
              </div>
              <div>
                <span>当前状态</span>
                <strong>{{ statusLabel() }}</strong>
              </div>
            </div>

            <div class="detail-section">
              <p class="section-kicker">用户端展示</p>
              <ol class="usage-list">
                <li>用户购买并完成支付后，系统会生成团购券码。</li>
                <li>到店消费时向商家出示券码，由商家核销。</li>
                <li>有效期、价格和库存会按照此页保存后的内容展示。</li>
              </ol>
            </div>
          </article>

          <aside class="panel edit-panel">
            <p class="section-kicker">编辑团购</p>
            <el-form :model="form" label-position="top">
              <el-form-item label="套餐名称">
                <el-input v-model="form.title" maxlength="60" show-word-limit />
              </el-form-item>
              <el-form-item label="说明">
                <el-input v-model="form.description" type="textarea" :rows="4" maxlength="300" show-word-limit />
              </el-form-item>
              <div class="form-grid">
                <el-form-item label="门市价（元）">
                  <el-input-number v-model="form.originalPrice" :min="0.01" :precision="2" :step="0.01" />
                </el-form-item>
                <el-form-item label="团购价（元）">
                  <el-input-number v-model="form.dealPrice" :min="0.01" :precision="2" :step="0.01" />
                </el-form-item>
                <el-form-item label="剩余份数">
                  <el-input-number v-model="form.stock" :min="0" :precision="0" :step="1" step-strictly />
                </el-form-item>
                <el-form-item label="有效天数">
                  <el-input-number v-model="form.validDays" :min="1" :precision="0" :step="1" step-strictly />
                </el-form-item>
              </div>
              <el-form-item label="售卖状态">
                <el-select v-model="form.status">
                  <el-option label="售卖中" value="ON_SALE" />
                  <el-option label="已下架" value="OFF_SALE" />
                </el-select>
              </el-form-item>
              <div class="edit-actions">
                <el-button @click="copyToForm(deal)">重置</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
              </div>
            </el-form>
          </aside>
        </section>
      </template>
    </main>
  </MerchantWorkspaceShell>
</template>

<style scoped>
.merchant-main {
  min-width: 0;
  padding: 0;
}

.merchant-deal-detail {
  display: grid;
  gap: 18px;
  max-width: none;
}

.detail-toolbar {
  display: flex;
}

.deal-hero {
  align-items: stretch;
}

.hero-copy {
  display: grid;
  gap: 10px;
  max-width: 720px;
  position: relative;
  z-index: 1;
}

.hero-copy h1 {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

.hero-copy p {
  color: var(--text-secondary);
  margin: 0;
}

.price-panel {
  align-self: center;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(214, 171, 43, 0.28);
  border-radius: var(--radius-md);
  min-width: 240px;
  padding: 20px;
  position: relative;
  text-align: right;
  z-index: 1;
}

.price-panel span,
.info-grid span,
.section-kicker {
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 700;
}

.price-panel strong {
  color: var(--color-primary);
  display: block;
  font-size: 38px;
  line-height: 1.1;
  margin: 6px 0;
}

.price-panel p {
  color: var(--text-secondary);
  margin: 0;
}

.detail-layout {
  align-items: start;
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) 360px;
}

.deal-main,
.edit-panel {
  margin-bottom: 0;
}

.detail-section {
  display: grid;
  gap: 10px;
}

.detail-section + .detail-section,
.info-grid {
  margin-top: 24px;
}

.section-kicker {
  color: var(--color-primary);
  letter-spacing: 0;
  margin: 0;
}

.detail-section h2 {
  font-size: 24px;
  margin: 0;
}

.deal-description {
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0;
}

.info-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.info-grid div {
  background: var(--bg-page);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  display: grid;
  gap: 6px;
  padding: 14px;
}

.info-grid strong {
  color: var(--text-primary);
  font-size: 18px;
}

.usage-list {
  color: var(--text-secondary);
  display: grid;
  gap: 10px;
  line-height: 1.7;
  margin: 0;
  padding-left: 20px;
}

.edit-panel {
  display: grid;
  gap: 14px;
  position: sticky;
  top: 88px;
}

.form-grid {
  display: grid;
  gap: 0 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.edit-panel :deep(.el-input-number),
.edit-panel :deep(.el-select) {
  width: 100%;
}

.edit-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.unavailable-panel {
  display: grid;
  gap: 12px;
  justify-items: start;
}

.unavailable-panel h1,
.unavailable-panel p {
  margin: 0;
}

@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .edit-panel {
    position: static;
  }

  .price-panel {
    text-align: left;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .hero-copy h1 {
    font-size: 26px;
  }

  .info-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .edit-actions {
    flex-direction: column;
  }
}
</style>
