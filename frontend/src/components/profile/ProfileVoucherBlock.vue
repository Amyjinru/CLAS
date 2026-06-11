<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  dealOrders: { type: Array, default: () => [] },
  coupons: { type: Array, default: () => [] }
})

function formatMoney(cents) {
  return `¥${((cents || 0) / 100).toFixed(2)}`
}

function dealStatusLabel(status) {
  return {
    PENDING_PAYMENT: '待支付',
    UNUSED: '待使用',
    USED: '已使用',
    EXPIRED: '已过期',
    REFUNDED: '已退款'
  }[status] || status || '未知'
}

function dealStatusType(status) {
  return {
    PENDING_PAYMENT: 'warning',
    UNUSED: 'success',
    USED: 'info',
    EXPIRED: 'warning',
    REFUNDED: 'danger'
  }[status] || 'info'
}

function couponStatusLabel(item) {
  return item.status || (item.used ? '已使用' : '可使用')
}
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>券包</h2>
        <p>团购券和已领取优惠券都会在这里汇总</p>
      </div>
      <RouterLink class="button secondary" to="/deals">去团购</RouterLink>
    </div>

    <el-empty v-if="!dealOrders.length && !coupons.length" description="暂无可展示的券">
      <RouterLink class="button secondary" to="/deals">去团购页看看</RouterLink>
    </el-empty>

    <article v-for="item in dealOrders" v-else :key="`deal-${item.id}`" class="list-row voucher-row">
      <div>
        <strong>{{ item.voucherCode || `团购券 #${item.id}` }}</strong>
        <p>支付金额 {{ formatMoney(item.payAmount) }}</p>
      </div>
      <el-tag :type="dealStatusType(item.status)">{{ dealStatusLabel(item.status) }}</el-tag>
    </article>

    <article v-for="item in coupons" :key="`coupon-${item.id || item.userCouponId}`" class="list-row voucher-row">
      <div>
        <strong>{{ item.name || item.couponName || `优惠券 #${item.couponId || item.id}` }}</strong>
        <p>{{ item.description || item.thresholdText || '平台优惠券' }}</p>
      </div>
      <el-tag type="success">{{ couponStatusLabel(item) }}</el-tag>
    </article>
  </div>
</template>

<style scoped>
.section-head { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 16px; }
.section-head h2 { margin: 0; }
.section-head p,
.list-row p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.list-row { align-items: center; border-top: 1px solid var(--border-light); display: flex; justify-content: space-between; padding: 14px 0; }
.voucher-row strong { font-family: var(--font-mono); }
@media (max-width: 900px) {
  .list-row { align-items: flex-start; flex-direction: column; }
}
@media (max-width: 640px) {
  .section-head { display: grid; }
}
</style>
