<script setup>
import BackButton from '../components/BackButton.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addCart, addFavorite, createOrder, getCart, getDeliveryEstimate, getMerchant, listAddresses, listFavorites, listGroupedProducts, listProducts, removeCart, removeFavorite } from '../api/clas'
import LocationSelector from '../components/LocationSelector.vue'
import MerchantRouteMap from '../components/MerchantRouteMap.vue'
import MerchantReviewSection from '../components/MerchantReviewSection.vue'
import { loadAmap } from '../utils/amap'
import { resolveAutoLocationFromAmap } from '../utils/locationFormat'
import { getCurrentLocation, setCurrentLocation } from '../utils/locationStore'

const route = useRoute()
const router = useRouter()
const merchantId = computed(() => Number(route.params.id))
const merchant = ref(null)
const products = ref([])
const productGroups = ref({})
const activeProductCategory = ref('')
const cartItems = ref([])
const cartOpen = ref(false)
const message = ref('')
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const favoriteLoading = ref(false)
const favoriteMerchantIds = ref(new Set())
const currentLocation = ref(getCurrentLocation())
const deliveryEstimate = ref(null)
const locationDialogVisible = ref(false)

const merchantCartItems = computed(() =>
  cartItems.value.filter((item) => item.merchantId === merchantId.value)
)
const cartCount = computed(() =>
  merchantCartItems.value.reduce((sum, item) => sum + item.quantity, 0)
)
const cartTotal = computed(() =>
  merchantCartItems.value.reduce((sum, item) => sum + item.subtotal, 0)
)
const productCategories = computed(() => Object.keys(productGroups.value))
const visibleProducts = computed(() => {
  if (!productCategories.value.length) return products.value
  return productGroups.value[activeProductCategory.value] || []
})
const isFavorite = computed(() => favoriteMerchantIds.value.has(merchantId.value))
const deliveryDistance = computed(() =>
  deliveryEstimate.value?.routeDistanceMeters || deliveryEstimate.value?.distanceMeters
)
const deliveryStatus = computed(() => {
  if (!currentLocation.value?.longitude || !currentLocation.value?.latitude) {
    return { type: 'info', label: '选择位置查看配送' }
  }
  if (!deliveryEstimate.value) {
    return { type: 'warning', label: '配送估算暂不可用' }
  }
  return deliveryEstimate.value.deliveryAvailable
    ? { type: 'success', label: '可配送' }
    : { type: 'danger', label: '超出配送范围' }
})

function moneyText(amount, fallback = '未设置') {
  if (amount === null || amount === undefined) return fallback
  return `¥${(amount / 100).toFixed(0)}`
}

function priceText(amount) {
  if (amount === null || amount === undefined) return '价格待定'
  return `¥${(amount / 100).toFixed(2)}`
}

function distanceText(distance) {
  if (distance === null || distance === undefined) return '距离未知'
  return distance < 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`
}

function fieldText(value, fallback = '暂无信息') {
  return value || fallback
}

const businessStatus = computed(() => resolveBusinessStatus(merchant.value?.businessHours))

function parseTimeToMinutes(value) {
  const match = /^(\d{2}):(\d{2})$/.exec((value || '').trim())
  if (!match) return null
  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours > 23 || minutes > 59) return null
  return hours * 60 + minutes
}

function resolveBusinessStatus(hoursText) {
  const fallback = { open: true, label: '营业中', type: 'success', nextOpenText: '' }
  if (!hoursText || !hoursText.includes('-')) return fallback
  const [startText, endText] = hoursText.split('-').map((item) => item.trim())
  const start = parseTimeToMinutes(startText)
  const end = parseTimeToMinutes(endText)
  if (start === null || end === null || start === end) return fallback

  const nowDate = new Date()
  const now = nowDate.getHours() * 60 + nowDate.getMinutes()
  const open = start < end
    ? now >= start && now < end
    : now >= start || now < end
  return {
    open,
    label: open ? '营业中' : '已休息',
    type: open ? 'success' : 'info',
    nextOpenText: open ? '' : `${startText} 开始营业`
  }
}

function productImageStyle(product) {
  const image = product.image || product.imageUrl
  if (!image) return {}
  return { backgroundImage: `url("${image}")` }
}

async function loadProducts() {
  merchant.value = await getMerchant(route.params.id)
  try {
    productGroups.value = await listGroupedProducts(route.params.id)
    products.value = Object.values(productGroups.value).flat()
    activeProductCategory.value = Object.keys(productGroups.value)[0] || ''
  } catch {
    productGroups.value = {}
    products.value = await listProducts(route.params.id)
    activeProductCategory.value = ''
  }
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
  loading.value = true
  loadError.value = ''
  message.value = ''
  merchant.value = null
  products.value = []
  productGroups.value = {}
  activeProductCategory.value = ''
  deliveryEstimate.value = null
  try {
    await Promise.all([loadProducts(), loadCart()])
    await ensureLocation()
    await loadDeliveryEstimate()
  } catch (error) {
    loadError.value = error.response?.data?.message || '商家信息加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function ensureLocation() {
  if (currentLocation.value?.longitude && currentLocation.value?.latitude) {
    return
  }
  try {
    const addresses = await listAddresses({ silent: true })
    const address = addresses.find((item) => item.isDefault) || addresses[0]
    if (address?.longitude && address?.latitude) {
      currentLocation.value = {
        province: '',
        city: '',
        district: '',
        street: address.address,
        address: address.address,
        longitude: Number(address.longitude),
        latitude: Number(address.latitude),
        source: 'manual'
      }
      setCurrentLocation(currentLocation.value)
    }
  } catch {
    currentLocation.value = null
  }
}

async function autoLocate() {
  try {
    const AMap = await loadAmap()
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 8000,
      showButton: false
    })
    geolocation.getCurrentPosition(async (status, result) => {
      if (status !== 'complete') {
        message.value = '自动定位失败，请手动选择位置'
        return
      }
      currentLocation.value = await resolveAutoLocationFromAmap(AMap, result)
      setCurrentLocation(currentLocation.value)
      await loadDeliveryEstimate()
    })
  } catch {
    message.value = '自动定位失败，请检查高德配置'
  }
}

async function loadDeliveryEstimate() {
  if (!merchant.value?.id || !currentLocation.value?.longitude || !currentLocation.value?.latitude) {
    deliveryEstimate.value = null
    return
  }
  try {
    deliveryEstimate.value = await getDeliveryEstimate(merchant.value.id, {
      lat: currentLocation.value.latitude,
      lng: currentLocation.value.longitude
    })
  } catch {
    deliveryEstimate.value = null
    message.value = '配送估算暂不可用，可继续浏览商品'
  }
}

function isSoldOut(product) {
  return product.stock <= 0
}

async function add(product) {
  if (isSoldOut(product)) return
  if (!businessStatus.value.open) {
    message.value = `商家已休息，${businessStatus.value.nextOpenText}`
    return
  }
  try {
    await addCart({ productId: product.id, quantity: 1 })
    message.value = `${product.name} 已加入购物车`
    await load()
  } catch (error) {
    message.value = error.response?.data?.message || '请先登录'
  }
}

async function increaseCartItem(item) {
  if (!businessStatus.value.open) {
    message.value = `商家已休息，${businessStatus.value.nextOpenText}`
    return
  }
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
  if (!businessStatus.value.open) {
    message.value = `商家已休息，${businessStatus.value.nextOpenText}`
    return
  }
  submitting.value = true
  message.value = ''
  try {
    const addresses = await listAddresses()
    const address = addresses.find((item) => item.isDefault) || addresses[0]
    if (!address?.id) {
      message.value = '请先在个人中心添加配送地址'
      return
    }
    const data = await createOrder({ merchantId: merchantId.value, addressId: address.id })
    cartOpen.value = false
    await load()
    router.push(`/payment/${data.order.id}`)
  } catch (error) {
    message.value = error.response?.data?.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

function confirmLocation(location) {
  currentLocation.value = location
  setCurrentLocation(location)
  locationDialogVisible.value = false
  loadDeliveryEstimate()
}

function toggleCart() {
  cartOpen.value = !cartOpen.value
}

function closeCart() {
  cartOpen.value = false
}

async function toggleFavorite() {
  if (favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    if (isFavorite.value) {
      await removeFavorite(merchantId.value)
      message.value = '已取消收藏'
    } else {
      await addFavorite(merchantId.value)
      message.value = '已收藏商家'
    }
    await loadProducts()
  } catch (error) {
    message.value = error.response?.data?.message || '收藏操作失败，请先登录'
  } finally {
    favoriteLoading.value = false
  }
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

    <section class="panel loading-panel" v-if="loading">
      <el-skeleton :rows="5" animated />
    </section>

    <section class="panel error-panel" v-else-if="loadError">
      <el-alert :title="loadError" type="error" show-icon :closable="false" />
      <button type="button" @click="load">重新加载</button>
    </section>

    <section class="panel merchant-hero" v-else-if="merchant">
      <div class="merchant-hero-main">
        <div class="merchant-title-row">
          <div>
            <div class="merchant-tags">
              <el-tag effect="plain">{{ fieldText(merchant.category, '生活服务') }}</el-tag>
              <el-tag type="warning" effect="plain">评分 {{ merchant.score ?? '暂无' }}</el-tag>
              <el-tag :type="deliveryStatus.type" effect="plain">{{ deliveryStatus.label }}</el-tag>
            </div>
            <h1>{{ merchant.merchantName }}</h1>
            <p class="merchant-address">{{ fieldText(merchant.address, '暂无地址') }}</p>
          </div>
          <button
            class="favorite-button secondary"
            :class="{ active: isFavorite }"
            :disabled="favoriteLoading"
            @click="toggleFavorite"
          >
            {{ favoriteLoading ? '处理中...' : (isFavorite ? '已收藏' : '收藏商家') }}
          </button>
        </div>

        <div class="merchant-stats">
          <div>
            <span>营业时间</span>
            <strong class="business-hours">
              {{ fieldText(merchant.businessHours, '暂无') }}
              <el-tag :type="businessStatus.type" size="small" effect="plain">
                {{ businessStatus.label }}
              </el-tag>
            </strong>
          </div>
          <div>
            <span>人均消费</span>
            <strong>{{ moneyText(merchant.averagePrice) }}</strong>
          </div>
          <div>
            <span>起送价</span>
            <strong>{{ moneyText(merchant.minOrderPrice, '无起送') }}</strong>
          </div>
          <div>
            <span>配送费</span>
            <strong>{{ moneyText(merchant.deliveryFee, '免配送费') }}</strong>
          </div>
          <div>
            <span>配送范围</span>
            <strong>{{ distanceText(merchant.deliveryRadiusM) }}</strong>
          </div>
        </div>
      </div>

      <div class="delivery-summary">
        <div>
          <span>当前位置</span>
          <strong>{{ currentLocation?.address || '尚未选择位置' }}</strong>
        </div>
        <div>
          <span>配送距离</span>
          <strong>{{ distanceText(deliveryDistance) }}</strong>
        </div>
        <div>
          <span>预计送达</span>
          <strong>{{ deliveryEstimate?.estimatedMinutes ? `${deliveryEstimate.estimatedMinutes} 分钟` : '待估算' }}</strong>
        </div>
        <div class="delivery-actions">
          <button type="button" class="secondary" @click="autoLocate">自动定位</button>
          <button type="button" @click="locationDialogVisible = true">选择位置</button>
        </div>
      </div>
    </section>

    <MerchantRouteMap
      v-if="merchant && !loadError"
      :merchant="merchant"
      :user-location="currentLocation"
      :estimate="deliveryEstimate"
      @locate="autoLocate"
      @select="locationDialogVisible = true"
    />

    <p class="message" v-if="message">{{ message }}</p>

    <section class="panel product-section" v-if="merchant && !loadError">
      <div class="section-head">
        <div>
          <h2>店内商品</h2>
          <p>{{ products.length ? `共 ${products.length} 件可选商品` : '暂无可选商品' }}</p>
        </div>
      </div>

      <div class="category-tabs" v-if="productCategories.length">
        <button
          v-for="category in productCategories"
          :key="category"
          type="button"
          :class="{ active: activeProductCategory === category }"
          @click="activeProductCategory = category"
        >
          {{ category }}
        </button>
      </div>

      <div class="product-grid" v-if="visibleProducts.length">
        <article class="product-card" v-for="product in visibleProducts" :key="product.id">
          <div class="product-thumb" :class="{ placeholder: !(product.image || product.imageUrl) }" :style="productImageStyle(product)">
            <span v-if="!(product.image || product.imageUrl)">{{ product.name?.slice(0, 1) || '品' }}</span>
          </div>
          <div class="product-info">
            <div class="product-title-row">
              <h3>{{ product.name }}</h3>
              <el-tag v-if="isSoldOut(product)" type="danger" effect="plain">已售罄</el-tag>
              <el-tag v-else type="success" effect="plain">库存 {{ product.stock }}</el-tag>
            </div>
            <p class="product-desc">{{ product.description || '暂无商品介绍' }}</p>
            <div class="product-bottom">
              <strong>{{ priceText(product.price) }}</strong>
              <button
                :disabled="isSoldOut(product) || !businessStatus.open"
                :class="{ 'btn-sold-out': isSoldOut(product) || !businessStatus.open }"
                @click="add(product)"
              >
                {{ isSoldOut(product) ? '已售罄' : (businessStatus.open ? '加入购物车' : businessStatus.nextOpenText) }}
              </button>
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="商家暂未上架商品">
        <RouterLink class="button secondary" to="/home">返回首页看看其他商家</RouterLink>
      </el-empty>
    </section>

    <MerchantReviewSection v-if="merchant" :merchant-id="merchantId" />

    <div class="cart-dock" v-if="merchant && !loadError">
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
                <button class="qty-btn" type="button" :disabled="!businessStatus.open" @click="increaseCartItem(item)">+</button>
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
              :disabled="!merchantCartItems.length || submitting || !businessStatus.open"
              @click="submitOrder"
            >
              {{ submitting ? '提交中...' : (businessStatus.open ? '提交订单' : businessStatus.nextOpenText) }}
            </button>
            <RouterLink class="button secondary" to="/orders" @click="closeCart">
              我的订单
            </RouterLink>
          </div>
        </footer>
      </aside>
    </div>

    <el-dialog v-model="locationDialogVisible" title="选择当前位置" width="760px">
      <LocationSelector
        v-model="currentLocation"
        save-enabled
        @confirm="confirmLocation"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.merchant-page {
  padding-bottom: 88px;
}

.loading-panel,
.error-panel {
  display: grid;
  gap: 16px;
}

.error-panel button {
  justify-self: start;
}

.merchant-hero {
  display: grid;
  gap: 22px;
}

.merchant-title-row {
  align-items: flex-start;
  display: flex;
  gap: 18px;
  justify-content: space-between;
}

.merchant-title-row h1 {
  color: var(--text-primary);
  font-size: 30px;
  line-height: 1.2;
  margin: 12px 0 8px;
}

.merchant-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.merchant-address {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

.favorite-button {
  min-width: 108px;
  white-space: nowrap;
}

.favorite-button.active {
  background: #fff7ed;
  border: 1px solid #fb923c;
  color: #ea580c;
}

.merchant-stats,
.delivery-summary {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
}

.merchant-stats div,
.delivery-summary div {
  background: #fffaf4;
  border: 1px solid #f4dfc5;
  border-radius: 8px;
  padding: 12px;
}

.merchant-stats span,
.delivery-summary span {
  color: var(--text-secondary);
  display: block;
  font-size: 12px;
  margin-bottom: 6px;
}

.merchant-stats strong,
.delivery-summary strong {
  color: var(--text-primary);
  font-size: 15px;
}

.business-hours {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.delivery-actions {
  align-items: center;
  display: flex;
  gap: 8px;
}

.delivery-actions button {
  min-height: 36px;
  padding: 0 12px;
}

.message {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  color: #9a3412;
  line-height: 1.5;
  margin: 14px 0;
  padding: 12px 14px;
}

.product-section {
  display: grid;
  gap: 18px;
}

.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.section-head h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}

.section-head p {
  color: var(--text-secondary);
  margin: 6px 0 0;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.category-tabs button {
  background: #fff;
  border: 1px solid #efe4d5;
  color: var(--text-secondary);
  min-height: 36px;
  padding: 0 14px;
  white-space: nowrap;
}

.category-tabs button.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.product-grid {
  display: grid;
  gap: 14px;
}

.product-card {
  align-items: stretch;
  background: #fff;
  border: 1px solid #efe4d5;
  border-radius: 8px;
  display: grid;
  gap: 14px;
  grid-template-columns: 112px 1fr;
  padding: 14px;
}

.product-thumb {
  align-items: center;
  aspect-ratio: 1;
  background-position: center;
  background-size: cover;
  border-radius: 8px;
  color: #9a3412;
  display: flex;
  font-size: 30px;
  font-weight: 800;
  justify-content: center;
  overflow: hidden;
}

.product-thumb.placeholder {
  background: linear-gradient(135deg, #fff7ed, #fde68a);
}

.product-info {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.product-title-row {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.product-title-row h3 {
  color: var(--text-primary);
  font-size: 18px;
  margin: 0;
  min-width: 0;
}

.product-desc {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

.product-bottom {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.product-bottom strong {
  color: #ea580c;
  font-size: 22px;
}

.product-bottom button {
  white-space: nowrap;
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

  .merchant-title-row,
  .product-bottom {
    align-items: stretch;
    flex-direction: column;
  }

  .favorite-button,
  .product-bottom button {
    width: 100%;
  }

  .product-card {
    grid-template-columns: 1fr;
  }

  .product-thumb {
    max-height: 180px;
  }
}
</style>
