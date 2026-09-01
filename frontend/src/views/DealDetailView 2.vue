<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { buyDeal, getDeal, listDeals, listMerchants } from '../api/clas'

const route = useRoute()
const router = useRouter()

const deal = ref(null)
const merchants = ref([])
const loading = ref(false)
const buying = ref(false)
const loadError = ref('')

const dealId = computed(() => Number(route.params.id))
const merchant = computed(() => merchants.value.find((item) => item.id === deal.value?.merchantId))
const merchantName = computed(() => merchant.value?.merchantName || (deal.value ? `商家 #${deal.value.merchantId}` : '商家信息加载中'))
const isSoldOut = computed(() => Number(deal.value?.stock || 0) <= 0)
const isOffSale = computed(() => deal.value?.status && deal.value.status !== 'ON_SALE')
const canBuy = computed(() => Boolean(deal.value) && !loading.value && !buying.value && !isSoldOut.value && !isOffSale.value)
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
  return '可购买'
}

function statusType() {
  if (isOffSale.value || isSoldOut.value) return 'info'
  return 'success'
}

async function load() {
  if (!dealId.value) {
    loadError.value = '团购券不存在'
    return
  }
  loading.value = true
  loadError.value = ''
  deal.value = null
  try {
    const [dealInfo, merchantList] = await Promise.all([
      loadDealDetail(),
      listMerchants()
    ])
    deal.value = dealInfo
    merchants.value = merchantList
  } catch (error) {
    loadError.value = error.response?.data?.message || '团购详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadDealDetail() {
  try {
    return await getDeal(dealId.value, { silent: true })
  } catch {
    const dealList = await listDeals(undefined, { silent: true })
    const fallback = dealList.find((item) => Number(item.id) === dealId.value)
    if (fallback) return fallback
    const error = new Error('团购券不存在')
    error.response = { data: { message: '团购券不存在' } }
    throw error
  }
}

async function handleBuy() {
  if (!canBuy.value) return
  buying.value = true
  try {
    const order = await buyDeal(deal.value.id)
    if (!order?.id) {
      ElMessage.error('订单创建失败，请稍后重试')
      return
    }
    await router.push(`/payment/deal/${order.id}`)
  } catch {
    // 错误提示由 axios 拦截器统一弹出，留在详情页便于用户重试。
  } finally {
    buying.value = false
  }
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="user-page deal-detail-page">
    <section class="detail-toolbar">
      <RouterLink class="button secondary" to="/deals">返回团购</RouterLink>
    </section>

    <el-skeleton v-if="loading" :rows="8" animated class="panel" />

    <section v-else-if="loadError" class="panel unavailable-panel">
      <h1>团购详情暂不可用</h1>
      <p>{{ loadError }}</p>
      <RouterLink class="button" to="/deals">浏览其他团购</RouterLink>
    </section>

    <template v-else-if="deal">
      <section class="hero deal-hero">
        <div class="hero-copy">
          <el-tag :type="statusType()">{{ statusLabel() }}</el-tag>
          <h1>{{ deal.title }}</h1>
          <p>{{ merchantName }} · 到店出示券码核销</p>
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
            <p class="deal-description">{{ deal.description || '商家暂未填写详细说明，到店前可联系商家确认适用范围。' }}</p>
          </div>

          <div class="info-grid">
            <div>
              <span>库存</span>
              <strong>{{ deal.stock }} 份</strong>
            </div>
            <div>
              <span>有效期</span>
              <strong>{{ deal.validDays || 30 }} 天</strong>
            </div>
            <div>
              <span>适用商家</span>
              <strong>{{ merchantName }}</strong>
            </div>
          </div>

          <div class="detail-section">
            <p class="section-kicker">使用方式</p>
            <ol class="usage-list">
              <li>购买并完成支付后，系统会生成团购券码。</li>
              <li>到店消费时向商家出示券码，由商家核销。</li>
              <li>请在有效期内使用，过期或已核销状态不可重复使用。</li>
            </ol>
          </div>
        </article>

        <aside class="panel purchase-panel">
          <p class="section-kicker">购买确认</p>
          <div class="purchase-price">
            <span>应付</span>
            <strong>¥{{ yuan(deal.dealPrice) }}</strong>
          </div>
          <p class="purchase-note">支付成功后可在个人中心的券包中查看券码。未使用且未过期的团购券可按现有规则申请退款。</p>
          <el-alert
            v-if="isSoldOut"
            title="该团购已售罄"
            type="info"
            :closable="false"
            show-icon
          />
          <el-alert
            v-else-if="isOffSale"
            title="该团购已下架"
            type="info"
            :closable="false"
            show-icon
          />
          <el-button
            type="primary"
            size="large"
            :disabled="!canBuy"
            :loading="buying"
            @click="handleBuy"
          >
            购买团购券
          </el-button>
        </aside>
      </section>
    </template>
  </div>
</template>

<style scoped>
.deal-detail-page {
  display: grid;
  gap: 18px;
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
.purchase-price span,
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
  grid-template-columns: minmax(0, 1fr) 340px;
}

.deal-main,
.purchase-panel {
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

.purchase-panel {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 88px;
}

.purchase-price {
  align-items: baseline;
  display: flex;
  justify-content: space-between;
}

.purchase-price strong {
  color: var(--color-primary);
  font-size: 30px;
}

.purchase-note {
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

.purchase-panel :deep(.el-button) {
  width: 100%;
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

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .purchase-panel {
    position: static;
  }

  .price-panel {
    text-align: left;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .hero-copy h1 {
    font-size: 26px;
  }
}
</style>
