<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyDealOrders, listMerchants } from '../api/clas'
import { ElTag } from 'element-plus'

const route = useRoute()
const router = useRouter()
const dealOrderId = computed(() => Number(route.params.orderId))
const fromNotifications = computed(() => route.query.from === 'notifications')

const dealOrder = ref(null)
const merchantName = ref('')
const loading = ref(true)
const error = ref('')

function formatMoney(cents) {
  return `¥${((cents || 0) / 100).toFixed(2)}`
}

function statusLabel(status) {
  return {
    PENDING_PAYMENT: '待支付',
    UNUSED: '待使用',
    USED: '已使用',
    EXPIRED: '已过期',
    REFUNDED: '已退款'
  }[status] || status || '未知'
}

function statusType(status) {
  return {
    PENDING_PAYMENT: 'warning',
    UNUSED: 'success',
    USED: 'info',
    EXPIRED: 'warning',
    REFUNDED: 'danger'
  }[status] || 'info'
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function backLabel() {
  return fromNotifications.value ? '← 返回通知' : '← 返回团购'
}

function goBack() {
  if (fromNotifications.value) {
    router.push('/profile/notifications')
  } else {
    router.push('/deals')
  }
}

onMounted(async () => {
  try {
    const [orders, merchants] = await Promise.all([
      listMyDealOrders({ silent: true }),
      listMerchants()
    ])
    dealOrder.value = orders.find((o) => o.id === dealOrderId.value) || null
    if (dealOrder.value) {
      const merchant = merchants.find((m) => m.id === dealOrder.value.merchantId)
      merchantName.value = merchant?.merchantName || `商家 #${dealOrder.value.merchantId}`
    } else {
      error.value = '团购券不存在或无权查看'
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="user-page deal-order-detail-page">
    <button class="back-compact" type="button" @click="goBack">{{ backLabel() }}</button>

    <el-skeleton v-if="loading" :rows="5" animated />

    <el-empty v-else-if="error" :description="error" />

    <section v-else class="panel deal-order-panel">
      <h1>团购券详情</h1>

      <div class="detail-grid">
        <div class="detail-block">
          <h2>券码</h2>
          <p class="voucher-code">{{ dealOrder.voucherCode }}</p>
        </div>

        <div class="detail-block">
          <h2>状态</h2>
          <p>
            <el-tag :type="statusType(dealOrder.status)">
              {{ statusLabel(dealOrder.status) }}
            </el-tag>
          </p>
        </div>

        <div class="detail-block">
          <h2>商家</h2>
          <p>{{ merchantName }}</p>
        </div>

        <div class="detail-block">
          <h2>支付金额</h2>
          <p class="pay-amount">{{ formatMoney(dealOrder.payAmount) }}</p>
        </div>

        <div class="detail-block">
          <h2>创建时间</h2>
          <p>{{ formatTime(dealOrder.createTime) }}</p>
        </div>

        <div v-if="dealOrder.paidTime" class="detail-block">
          <h2>支付时间</h2>
          <p>{{ formatTime(dealOrder.paidTime) }}</p>
        </div>

        <div v-if="dealOrder.expireTime" class="detail-block">
          <h2>有效期至</h2>
          <p>{{ formatTime(dealOrder.expireTime).slice(0, 10) }}</p>
        </div>

        <div v-if="dealOrder.usedTime" class="detail-block">
          <h2>核销时间</h2>
          <p>{{ formatTime(dealOrder.usedTime) }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.deal-order-detail-page {
  display: grid;
  gap: 16px;
}

.back-compact {
  align-self: start;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  justify-self: start;
  min-height: 32px;
  padding: 0 12px;
  width: auto;
}

.back-compact:hover {
  background: var(--color-primary-soft);
  border-color: var(--clas-amber-200);
  color: var(--color-primary);
  transform: none;
}

.deal-order-panel h1 {
  margin-top: 0;
}

.detail-grid {
  display: grid;
  gap: 20px;
}

.detail-block {
  border-top: 1px solid var(--border-light);
  padding-top: 16px;
}

.detail-block h2 {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin: 0 0 8px;
  text-transform: uppercase;
}

.detail-block p {
  margin: 4px 0;
}

.voucher-code {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
}

.pay-amount {
  color: var(--color-primary);
  font-size: 26px;
  font-weight: 700;
}

@media (min-width: 768px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
