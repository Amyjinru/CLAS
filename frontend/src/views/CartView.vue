<script setup>
import { onMounted, ref } from 'vue'
import { createOrder, deleteCartItem, getCart, updateCart } from '../api/clas'

const items = ref([])
const message = ref('')
const updatingProductId = ref(null)

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

async function changeQuantity(item, quantity) {
  const nextQuantity = Number(quantity)
  if (!Number.isInteger(nextQuantity) || nextQuantity < 1) {
    message.value = '数量至少为 1'
    await load()
    return
  }
  updatingProductId.value = item.productId
  try {
    items.value = await updateCart({ productId: item.productId, quantity: nextQuantity })
    message.value = '购物车数量已更新'
  } catch (error) {
    message.value = error.response?.data?.message || '更新数量失败'
    await load()
  } finally {
    updatingProductId.value = null
  }
}

async function deleteItem(item) {
  updatingProductId.value = item.productId
  try {
    items.value = await deleteCartItem(item.productId)
    message.value = '商品已从购物车删除'
  } catch (error) {
    message.value = error.response?.data?.message || '删除商品失败'
  } finally {
    updatingProductId.value = null
  }
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <h1>购物车</h1>
    <p v-if="merchantIds().length > 1">购物车包含多个商家商品，本次会提交第一个商家的商品。</p>
  </section>
  <section class="list">
    <article class="row" v-for="item in items" :key="item.productId">
      <div>
        <h2>{{ item.productName }}</h2>
        <p>库存 {{ item.stock }} · 单价 ¥{{ (item.price / 100).toFixed(2) }}</p>
      </div>
      <div class="cart-actions">
        <label class="quantity-field">
          数量
          <input
            type="number"
            min="1"
            :max="item.stock"
            :value="item.quantity"
            :disabled="updatingProductId === item.productId"
            @change="changeQuantity(item, $event.target.value)"
          />
        </label>
        <strong>¥{{ (item.subtotal / 100).toFixed(2) }}</strong>
        <button
          class="secondary"
          :disabled="updatingProductId === item.productId"
          @click="deleteItem(item)"
        >
          删除
        </button>
      </div>
    </article>
  </section>
  <footer class="checkout">
    <span>合计 ¥{{ (total() / 100).toFixed(2) }}</span>
    <button @click="submit">提交订单</button>
  </footer>
  <p class="message">{{ message }}</p>
</template>

<style scoped>
.cart-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.quantity-field {
  align-items: center;
  display: flex;
  gap: 8px;
  margin: 0;
}

.quantity-field input {
  width: 86px;
}
</style>
