<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  cartItems: { type: Array, default: () => [] },
  pendingPaymentOrders: { type: Array, default: () => [] },
  pendingDealOrders: { type: Array, default: () => [] }
})
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>购物车</h2>
        <p>{{ cartItems.length }} 件商品 · {{ pendingPaymentOrders.length + pendingDealOrders.length }} 个待支付</p>
      </div>
      <RouterLink class="button" to="/cart">进入购物车</RouterLink>
    </div>
    <el-empty v-if="!cartItems.length && !pendingPaymentOrders.length && !pendingDealOrders.length" description="购物车暂无商品">
      <RouterLink class="button secondary" to="/home">去外卖页逛逛</RouterLink>
    </el-empty>
    <div v-else class="cart-summary">
      <article class="metric-card">
        <span>购物车商品</span>
        <strong>{{ cartItems.length }}</strong>
      </article>
      <article class="metric-card">
        <span>外卖待支付</span>
        <strong>{{ pendingPaymentOrders.length }}</strong>
      </article>
      <article class="metric-card">
        <span>团购待支付</span>
        <strong>{{ pendingDealOrders.length }}</strong>
      </article>
    </div>
  </div>
</template>

<style scoped>
.section-head { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 16px; }
.section-head h2 { margin: 0; }
.section-head p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.cart-summary { display: grid; gap: 12px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.metric-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  display: grid;
  gap: 8px;
  min-height: 112px;
  padding: 16px;
}
.metric-card strong { font-size: 28px; line-height: 1; }
.metric-card span { font-weight: 700; }
@media (max-width: 900px) {
  .cart-summary { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .section-head { display: grid; }
}
</style>
