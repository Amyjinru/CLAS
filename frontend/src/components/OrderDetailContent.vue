<script setup>
import MoneyText from './MoneyText.vue'
import { formatCompactDateTime } from '../utils/formatters'

const props = defineProps({
  order: {
    type: Object,
    required: true
  },
  productNames: {
    type: Object,
    default: () => ({})
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const statusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '商家已接单',
  COMPLETED: '已完成'
}

function itemName(productId) {
  return props.productNames[productId] || `商品 ${productId}`
}

</script>

<template>
  <div :class="['order-detail', { compact }]">
    <template v-if="!compact">
      <p class="meta">
        订单号 {{ order.order.id }} · {{ statusLabel[order.order.status] || order.order.status }}
      </p>
      <p v-if="order.order.createTime" class="meta">下单时间 {{ formatCompactDateTime(order.order.createTime) }}</p>
    </template>

    <ul class="item-list">
      <li v-for="item in order.items" :key="item.id">
        <div class="item-main">
          <span class="item-name">{{ itemName(item.productId) }}</span>
          <span class="item-qty">× {{ item.quantity }}</span>
        </div>
        <div class="item-price">
          <span>单价 <MoneyText :amount="item.price" /></span>
          <span v-if="!compact" class="item-subtotal">
            小计 <MoneyText :amount="item.price * item.quantity" />
          </span>
        </div>
      </li>
    </ul>

    <p class="total">合计 <MoneyText :amount="order.order.totalPrice" /></p>
  </div>
</template>

<style scoped>
.order-detail {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
}

.order-detail.compact {
  margin-top: 10px;
  padding: 10px 12px;
}

.meta {
  color: #667085;
  font-size: 13px;
  margin: 0 0 8px;
}

.item-list {
  display: grid;
  gap: 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.item-list li {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.compact .item-list li {
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
}

.item-main {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.item-name {
  font-weight: 600;
}

.item-qty {
  color: #667085;
}

.item-price {
  color: #667085;
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  gap: 10px;
}

.item-subtotal {
  color: #1f2937;
}

.total {
  border-top: 1px solid #eef2f7;
  font-weight: 700;
  margin: 10px 0 0;
  padding-top: 10px;
  text-align: right;
}
</style>
