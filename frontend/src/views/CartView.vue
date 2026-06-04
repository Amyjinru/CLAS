<script setup>
import { onMounted, ref } from 'vue'
import { createOrder, getCart } from '../api/clas'

const items = ref([])
const message = ref('')

const total = () => items.value.reduce((sum, item) => sum + item.subtotal, 0)
const merchantIds = () => [...new Set(items.value.map((item) => item.merchantId).filter(Boolean))]

async function load() {
  try {
    items.value = await getCart()
  } catch (error) {
    message.value = '请先登录后查看购物车'
  }
}

async function submit() {
  if (!items.value.length) return
  const merchantId = merchantIds()[0]
  const data = await createOrder({ merchantId })
  message.value = `订单 ${data.order.id} 已创建，库存已扣减，请前往支付`
  await load()
}

onMounted(load)
</script>

<template>
  <div class="cart-container">
    <div class="cart-header">
      <h1>购物车</h1>
      <p v-if="merchantIds().length > 1" class="multi-merchant-warn">
        购物车包含多个商家商品，本次将提交第一个商家的商品
      </p>
    </div>

    <div class="cart-items" v-if="items.length">
      <div class="cart-item" v-for="item in items" :key="item.productId">
        <div class="item-info">
          <h2 class="item-name">{{ item.productName }}</h2>
          <span class="item-qty">× {{ item.quantity }}</span>
        </div>
        <div class="item-price">¥{{ (item.subtotal / 100).toFixed(2) }}</div>
      </div>
    </div>

    <div class="cart-empty" v-else>
      <p>购物车暂无商品</p>
    </div>

    <footer class="checkout-bar" v-if="items.length">
      <div class="checkout-total">
        <span class="total-label">合计</span>
        <span class="total-price">¥{{ (total() / 100).toFixed(2) }}</span>
      </div>
      <button class="submit-btn" @click="submit">提交订单</button>
    </footer>

    <p class="cart-message" v-if="message">{{ message }}</p>
  </div>
</template>

<style scoped>
.cart-container {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px;
}

.cart-header h1 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px 0;
  letter-spacing: 0.04em;
  padding-left: 16px;
  position: relative;
}
.cart-header h1::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  background: linear-gradient(180deg, var(--color-primary), var(--clas-amber-300));
  border-radius: var(--radius-full);
}

.multi-merchant-warn {
  color: var(--clas-warning);
  font-size: 13px;
  background: var(--clas-warning-light);
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  margin: 12px 0 0 16px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 24px;
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 18px 22px;
  transition: all var(--transition-fast);
}
.cart-item:hover {
  border-color: var(--clas-amber-200);
  box-shadow: var(--shadow-sm);
}

.item-info {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.item-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
.item-qty {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 500;
}

.item-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--clas-amber-600);
  white-space: nowrap;
  margin-left: 24px;
}

.cart-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
  font-size: 15px;
}

.checkout-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-top: 20px;
  box-shadow: var(--shadow-md);
  position: sticky;
  bottom: 80px;
}

.checkout-total {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.total-label {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}
.total-price {
  font-size: 26px;
  font-weight: 800;
  color: var(--clas-amber-600);
  letter-spacing: -0.02em;
}

.submit-btn {
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}
.submit-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}
.submit-btn:active {
  transform: scale(0.97);
}

.cart-message {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--clas-success);
  font-weight: 500;
  padding: 10px 16px;
  background: var(--clas-success-light);
  border-radius: var(--radius-sm);
}

@media (max-width: 480px) {
  .cart-container {
    padding: 12px;
  }
  .cart-item {
    padding: 14px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .item-price {
    margin-left: 0;
    align-self: flex-end;
  }
  .checkout-bar {
    flex-direction: column;
    gap: 14px;
    padding: 16px 18px;
  }
  .total-price {
    font-size: 22px;
  }
  .submit-btn {
    width: 100%;
  }
}
</style>
