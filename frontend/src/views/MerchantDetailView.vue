<script setup>
import BackButton from '../components/BackButton.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addCart, addFavorite, createOrder, getCart, getMerchant, listFavorites, listProducts, removeCart, removeFavorite } from '../api/clas'

const route = useRoute()
const router = useRouter()
const merchantId = computed(() => Number(route.params.id))
const merchant = ref(null)
const products = ref([])
const cartItems = ref([])
const cartOpen = ref(false)
const message = ref('')
const submitting = ref(false)
const favoriteMerchantIds = ref(new Set())

const merchantCartItems = computed(() =>
  cartItems.value.filter((item) => item.merchantId === merchantId.value)
)
const cartCount = computed(() =>
  merchantCartItems.value.reduce((sum, item) => sum + item.quantity, 0)
)
const cartTotal = computed(() =>
  merchantCartItems.value.reduce((sum, item) => sum + item.subtotal, 0)
)

async function loadProducts() {
  merchant.value = await getMerchant(route.params.id)
  products.value = await listProducts(route.params.id)
  try {
    const favorites = await listFavorites()
    favoriteMerchantIds.value = new Set(favorites.map((item) => item.id))
  } catch {
    favoriteMerchantIds.value = new Set()
  }
}

async function loadCart() {
  try {
    cartItems.value = await getCart()
  } catch {
    cartItems.value = []
  }
}

async function load() {
  await Promise.all([loadProducts(), loadCart()])
}

function isSoldOut(product) {
  return product.stock <= 0
}

async function add(product) {
  if (isSoldOut(product)) return
  try {
    await addCart({ productId: product.id, quantity: 1 })
    message.value = `${product.name} 已加入购物车`
    await load()
  } catch (error) {
    message.value = error.response?.data?.message || '请先登录'
  }
}

async function increaseCartItem(item) {
  try {
    await addCart({ productId: item.productId, quantity: 1 })
    await load()
  } catch (error) {
    message.value = error.response?.data?.message || '操作失败'
  }
}

async function decreaseCartItem(item) {
  try {
    await removeCart({ productId: item.productId, quantity: 1 })
    await load()
  } catch (error) {
    message.value = error.response?.data?.message || '操作失败'
  }
}

async function removeCartItem(item) {
  try {
    await removeCart({ productId: item.productId, quantity: item.quantity })
    await load()
  } catch (error) {
    message.value = error.response?.data?.message || '操作失败'
  }
}

async function submitOrder() {
  if (!merchantCartItems.value.length) return
  submitting.value = true
  message.value = ''
  try {
    const data = await createOrder({ merchantId: merchantId.value })
    cartOpen.value = false
    await load()
    router.push(`/payment/${data.order.id}`)
  } catch (error) {
    message.value = error.response?.data?.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

function toggleCart() {
  cartOpen.value = !cartOpen.value
}

function closeCart() {
  cartOpen.value = false
}

async function toggleFavorite() {
  if (favoriteMerchantIds.value.has(merchantId.value)) {
    await removeFavorite(merchantId.value)
    message.value = '已取消收藏'
  } else {
    await addFavorite(merchantId.value)
    message.value = '已收藏商家'
  }
  await loadProducts()
}

onMounted(load)

watch(
  () => route.fullPath,
  (newPath, oldPath) => {
    if (newPath !== oldPath && newPath.startsWith('/merchant/')) {
      cartOpen.value = false
      load()
    }
  }
)
</script>

<template>
  <div class="merchant-page">
    <BackButton to="/home" label="返回首页" />

    <section class="panel" v-if="merchant">
      <h1>{{ merchant.merchantName }}</h1>
      <p>{{ merchant.category }} · {{ merchant.address }} · {{ merchant.score }} 分</p>
      <p>{{ merchant.businessHours }} · 起送 ¥{{ ((merchant.minOrderPrice || 0) / 100).toFixed(0) }} · 配送费 ¥{{ ((merchant.deliveryFee || 0) / 100).toFixed(0) }}</p>
      <button class="secondary" @click="toggleFavorite">
        {{ favoriteMerchantIds.has(merchantId) ? '取消收藏' : '收藏商家' }}
      </button>
    </section>

    <p class="message">{{ message }}</p>

    <section class="list">
      <article class="row" v-for="product in products" :key="product.id">
        <div>
          <h2>{{ product.name }}</h2>
          <p>¥{{ (product.price / 100).toFixed(2) }}</p>
        </div>
        <div class="row-actions">
          <button
            :disabled="isSoldOut(product)"
            :class="{ 'btn-sold-out': isSoldOut(product) }"
            @click="add(product)"
          >
            {{ isSoldOut(product) ? '已售罄' : '加入' }}
          </button>
        </div>
      </article>
    </section>

    <div class="cart-dock">
      <button class="cart-dock-toggle" type="button" @click="toggleCart">
        本店购物车
        <span v-if="cartCount" class="cart-badge">{{ cartCount }}</span>
      </button>
    </div>

    <div v-if="cartOpen" class="cart-overlay" @click.self="closeCart">
      <aside class="cart-panel">
        <header class="cart-panel-head">
          <h2>本店已选商品</h2>
          <button class="cart-close" type="button" @click="closeCart">×</button>
        </header>

        <div v-if="merchantCartItems.length" class="cart-panel-list">
          <article class="cart-panel-item" v-for="item in merchantCartItems" :key="item.productId">
            <div class="cart-panel-item-main">
              <h3>{{ item.productName }}</h3>
              <div class="cart-qty-control">
                <button class="qty-btn" type="button" @click="decreaseCartItem(item)">−</button>
                <span>{{ item.quantity }}</span>
                <button class="qty-btn" type="button" @click="increaseCartItem(item)">+</button>
              </div>
            </div>
            <div class="cart-panel-item-side">
              <strong>¥{{ (item.subtotal / 100).toFixed(2) }}</strong>
              <button class="remove-btn" type="button" @click="removeCartItem(item)">移除</button>
            </div>
          </article>
        </div>
        <p v-else class="cart-empty">还没有选择商品，点击「加入」开始选购。</p>

        <footer class="cart-panel-foot">
          <div class="cart-panel-total">
            <span>合计</span>
            <strong>¥{{ (cartTotal / 100).toFixed(2) }}</strong>
          </div>
          <div class="cart-panel-actions">
            <button
              :disabled="!merchantCartItems.length || submitting"
              @click="submitOrder"
            >
              {{ submitting ? '提交中...' : '提交订单' }}
            </button>
            <RouterLink class="button secondary" to="/orders" @click="closeCart">
              我的订单
            </RouterLink>
          </div>
        </footer>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.merchant-page {
  padding-bottom: 88px;
}

.btn-sold-out:disabled,
button:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.cart-dock {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 20;
}

.cart-dock-toggle {
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.25);
  gap: 8px;
  min-width: 140px;
}

.cart-badge {
  align-items: center;
  background: white;
  border-radius: 999px;
  color: #2563eb;
  display: inline-flex;
  font-size: 12px;
  justify-content: center;
  min-width: 22px;
  padding: 2px 8px;
}

.cart-overlay {
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.28);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 24px;
  position: fixed;
  z-index: 30;
}

.cart-panel {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.16);
  display: flex;
  flex-direction: column;
  max-height: min(72vh, 560px);
  max-width: 420px;
  width: 100%;
}

.cart-panel-head,
.cart-panel-foot {
  padding: 16px 18px;
}

.cart-panel-head {
  align-items: center;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
}

.cart-panel-head h2 {
  font-size: 18px;
  margin: 0;
}

.cart-close {
  background: #f3f4f6;
  color: #6b7280;
  min-height: 32px;
  min-width: 32px;
  padding: 0;
}

.cart-panel-list {
  display: grid;
  gap: 10px;
  overflow: auto;
  padding: 12px 18px;
}

.cart-panel-item {
  align-items: center;
  background: #fafafa;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 12px;
}

.cart-panel-item-main {
  flex: 1;
  min-width: 0;
}

.cart-panel-item-side {
  align-items: flex-end;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cart-qty-control {
  align-items: center;
  display: inline-flex;
  gap: 8px;
  margin-top: 8px;
}

.qty-btn {
  background: white;
  border: 1px solid #d8dde8;
  color: #1f2937;
  min-height: 32px;
  min-width: 32px;
  padding: 0;
}

.qty-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.remove-btn {
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  min-height: auto;
  padding: 0;
}

.remove-btn:hover {
  color: #dc2626;
}

.cart-panel-item h3 {
  font-size: 15px;
  margin: 0 0 4px;
}

.cart-panel-item p {
  color: #667085;
  font-size: 13px;
  margin: 0;
}

.cart-empty {
  color: #667085;
  margin: 0;
  padding: 28px 18px;
  text-align: center;
}

.cart-panel-foot {
  border-top: 1px solid #eef2f7;
}

.cart-panel-total {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.cart-panel-total strong {
  color: #2563eb;
  font-size: 20px;
}

.cart-panel-actions {
  display: grid;
  gap: 8px;
}

@media (max-width: 640px) {
  .cart-dock {
    left: 14px;
    right: 14px;
  }

  .cart-dock-toggle {
    width: 100%;
  }

  .cart-overlay {
    padding: 14px;
  }
}
</style>
