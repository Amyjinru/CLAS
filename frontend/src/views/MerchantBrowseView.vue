<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listAddresses, listMerchants, listProducts } from '../api/clas'
import BackButton from '../components/BackButton.vue'
import LocationSelector from '../components/LocationSelector.vue'
import { loadAmap } from '../utils/amap'
import { formatDistance } from '../utils/formatters'
import { resolveAutoLocationFromAmap } from '../utils/locationFormat'
import { getCurrentLocation, setCurrentLocation } from '../utils/locationStore'

defineOptions({
  name: 'MerchantBrowseView'
})

const merchants = ref([])
const merchantProducts = ref({})
const productSlideIndexes = ref({})
const addresses = ref([])
const currentLocation = ref(getCurrentLocation())
const route = useRoute()
const router = useRouter()
const keyword = ref('')
const category = ref('')
const sort = ref('recommend')
const onlyDeliverable = ref(false)
const loading = ref(false)
const productsLoading = ref(false)
const locating = ref(false)
const locationDialogVisible = ref(false)
let productSlideTimer = null

const categories = ['美食', '饮品', '休闲娱乐', '生活服务']
const sortOptions = [
  { label: '智能推荐', value: 'recommend' },
  { label: '距离最近', value: 'distance' },
  { label: '评分优先', value: 'score' },
  { label: '人均低价', value: 'price' },
  { label: '最新入驻', value: 'latest' }
]
const sortValues = new Set(sortOptions.map((item) => item.value))

const hasSearchLocation = computed(() => Boolean(
  currentLocation.value?.longitude && currentLocation.value?.latitude
))

const resultText = computed(() => {
  if (loading.value) return '正在加载店铺'
  return `共 ${merchants.value.length} 家店铺`
})

function distanceText(distance) {
  if (distance === null || distance === undefined) return '距离未知'
  return formatDistance(distance)
}

function merchantInitial(merchant) {
  return (merchant?.merchantName || merchant?.category || '店').slice(0, 1)
}

function merchantOpenStatus(merchant) {
  if (!merchant || merchant.status !== 'OPEN') return { label: '未营业', type: 'info' }
  if (merchant.manualClosed) return { label: '已打烊', type: 'info' }
  const hoursText = merchant.businessHours
  if (!hoursText || !hoursText.includes('-')) return { label: '营业中', type: 'success' }
  const [startText, endText] = hoursText.split('-').map((item) => item.trim())
  const start = parseBusinessMinutes(startText)
  const end = parseBusinessMinutes(endText)
  if (start === null || end === null || start === end) return { label: '营业中', type: 'success' }
  const nowDate = new Date()
  const now = nowDate.getHours() * 60 + nowDate.getMinutes()
  const open = start < end ? now >= start && now < end : now >= start || now < end
  return { label: open ? '营业中' : '已打烊', type: open ? 'success' : 'info' }
}

function parseBusinessMinutes(value) {
  const match = /^(\d{2}):(\d{2})$/.exec((value || '').trim())
  if (!match) return null
  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours > 23 || minutes > 59) return null
  return hours * 60 + minutes
}

function productPrice(product) {
  return `¥${((product?.price || 0) / 100).toFixed(2)}`
}

function activeProduct(merchantId) {
  const products = merchantProducts.value[merchantId] || []
  if (!products.length) return null
  const index = productSlideIndexes.value[merchantId] || 0
  return products[index % products.length]
}

function activeProductPosition(merchantId) {
  const products = merchantProducts.value[merchantId] || []
  if (!products.length) return ''
  const index = (productSlideIndexes.value[merchantId] || 0) % products.length
  return `${index + 1} / ${products.length}`
}

function resetProductSlideIndexes(items) {
  productSlideIndexes.value = Object.fromEntries(items.map((merchant) => [merchant.id, 0]))
}

function startProductSlideTimer() {
  stopProductSlideTimer()
  productSlideTimer = window.setInterval(() => {
    const nextIndexes = { ...productSlideIndexes.value }
    Object.entries(merchantProducts.value).forEach(([merchantId, products]) => {
      if (!Array.isArray(products) || products.length <= 1) return
      nextIndexes[merchantId] = ((nextIndexes[merchantId] || 0) + 1) % products.length
    })
    productSlideIndexes.value = nextIndexes
  }, 3000)
}

function stopProductSlideTimer() {
  if (!productSlideTimer) return
  window.clearInterval(productSlideTimer)
  productSlideTimer = null
}

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : value
}

function restoreFiltersFromRoute() {
  const queryKeyword = firstQueryValue(route.query.keyword)
  const queryCategory = firstQueryValue(route.query.category)
  const querySort = firstQueryValue(route.query.sort)
  const queryOnlyDeliverable = firstQueryValue(route.query.onlyDeliverable)

  keyword.value = typeof queryKeyword === 'string' ? queryKeyword : ''
  category.value = typeof queryCategory === 'string' && categories.includes(queryCategory) ? queryCategory : ''
  sort.value = typeof querySort === 'string' && sortValues.has(querySort) ? querySort : 'recommend'
  onlyDeliverable.value = queryOnlyDeliverable === '1' || queryOnlyDeliverable === 'true'
}

function filterRouteQuery() {
  const query = {}
  const trimmedKeyword = keyword.value.trim()
  if (trimmedKeyword) query.keyword = trimmedKeyword
  if (category.value) query.category = category.value
  if (sort.value && sort.value !== 'recommend') query.sort = sort.value
  if (onlyDeliverable.value) query.onlyDeliverable = '1'
  return query
}

function queryMatchesCurrentRoute(nextQuery) {
  const keys = ['keyword', 'category', 'sort', 'onlyDeliverable']
  return keys.every((key) => {
    const current = firstQueryValue(route.query[key])
    return (current || '') === (nextQuery[key] || '')
  })
}

async function syncFiltersToRoute() {
  const query = filterRouteQuery()
  if (queryMatchesCurrentRoute(query)) return
  await router.replace({ path: '/merchants', query })
}

async function loadAddresses() {
  try {
    addresses.value = await listAddresses({ silent: true })
    if (!currentLocation.value) {
      const defaultAddress = addresses.value.find((item) => item.isDefault) || addresses.value[0]
      if (defaultAddress?.longitude && defaultAddress?.latitude) {
        currentLocation.value = {
          address: defaultAddress.address,
          longitude: Number(defaultAddress.longitude),
          latitude: Number(defaultAddress.latitude),
          source: 'manual'
        }
        setCurrentLocation(currentLocation.value)
      }
    }
  } catch {
    addresses.value = []
  }
}

async function autoLocate() {
  locating.value = true
  try {
    const AMap = await loadAmap()
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 3000,
      showButton: false
    })
    const nextLocation = await new Promise((resolve, reject) => {
      geolocation.getCurrentPosition(async (status, result) => {
        if (status !== 'complete') {
          reject(new Error('locate failed'))
          return
        }
        resolve(await resolveAutoLocationFromAmap(AMap, result))
      })
    })
    currentLocation.value = nextLocation
    setCurrentLocation(nextLocation)
    ElMessage.success('已更新当前位置')
    await load()
  } catch {
    ElMessage.warning('自动定位失败，请手动选择位置')
  } finally {
    locating.value = false
  }
}

async function confirmLocation(location) {
  currentLocation.value = location
  setCurrentLocation(location)
  locationDialogVisible.value = false
  if (!sort.value) {
    sort.value = 'distance'
  }
  ElMessage.success('位置已更新')
  await load()
}

function queryParams() {
  const params = {
    keyword: keyword.value.trim() || undefined,
    category: category.value || undefined,
    sort: sort.value || 'recommend'
  }
  if ((sort.value === 'distance' || onlyDeliverable.value) && hasSearchLocation.value) {
    params.lng = currentLocation.value.longitude
    params.lat = currentLocation.value.latitude
  }
  if (onlyDeliverable.value && hasSearchLocation.value) {
    params.onlyDeliverable = true
  }
  return params
}

async function loadMerchantProducts(items) {
  productsLoading.value = true
  const next = {}
  try {
    await Promise.all(items.map(async (merchant) => {
      try {
        const products = await listProducts(merchant.id)
        next[merchant.id] = products
          .filter((product) => product.status !== 'OFF_SALE')
          .slice(0, 8)
      } catch {
        next[merchant.id] = []
      }
    }))
    merchantProducts.value = next
    resetProductSlideIndexes(items)
  } finally {
    productsLoading.value = false
  }
}

async function load() {
  if (onlyDeliverable.value && !hasSearchLocation.value) {
    onlyDeliverable.value = false
    ElMessage.warning('请先选择带坐标的位置后再筛选可配送')
  }
  await syncFiltersToRoute()
  loading.value = true
  try {
    const data = await listMerchants(queryParams())
    merchants.value = data
    await loadMerchantProducts(data)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  sort.value = 'recommend'
  onlyDeliverable.value = false
  load()
}

onMounted(async () => {
  restoreFiltersFromRoute()
  await loadAddresses()
  await load()
  startProductSlideTimer()
})

onUnmounted(() => {
  stopProductSlideTimer()
})
</script>

<template>
  <div class="merchant-browser">
    <BackButton to="/home" label="返回首页" />

    <section class="browse-toolbar">
      <div class="toolbar-copy">
        <p>{{ resultText }}</p>
        <h1>查看店铺</h1>
      </div>
      <div class="browse-location-panel">
        <div class="location-copy">
          <strong>当前定位</strong>
          <span>{{ currentLocation?.address || '未定位，请选择位置' }}</span>
          <small v-if="!hasSearchLocation">定位后可按距离排序并筛选可配送商家</small>
        </div>
        <div class="location-actions">
          <el-button :loading="locating" @click="autoLocate">自动定位</el-button>
          <el-button @click="locationDialogVisible = true">选择位置</el-button>
        </div>
      </div>
      <div class="toolbar-controls">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          placeholder="搜索店铺名称"
          clearable
          @clear="load"
          @keyup.enter="load"
        />
        <el-select v-model="category" class="toolbar-select" placeholder="分类" clearable @change="load">
          <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="sort" class="toolbar-select" placeholder="排序" @change="load">
          <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-checkbox v-model="onlyDeliverable" :disabled="!hasSearchLocation" @change="load">可配送</el-checkbox>
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
      <p class="location-line">
        参考位置：{{ currentLocation?.address || '未定位' }}
      </p>
    </section>

    <section class="merchant-list" v-loading="loading">
      <article v-for="merchant in merchants" :key="merchant.id" class="merchant-row">
        <div class="merchant-avatar">
          <img v-if="merchant.logo" :src="merchant.logo" :alt="merchant.merchantName" />
          <span v-else>{{ merchantInitial(merchant) }}</span>
        </div>

        <div class="merchant-main">
          <div class="merchant-head">
            <div>
              <h2>{{ merchant.merchantName }}</h2>
              <p>{{ merchant.address }}</p>
            </div>
            <div class="merchant-tags">
              <el-tag :type="merchantOpenStatus(merchant).type" effect="plain">
                {{ merchantOpenStatus(merchant).label }}
              </el-tag>
              <el-tag v-if="merchant.deliveryAvailable === true" type="success" effect="plain">可配送</el-tag>
              <el-tag v-else-if="merchant.deliveryAvailable === false" type="danger" effect="plain">超出范围</el-tag>
            </div>
          </div>

          <div class="merchant-meta">
            <span>评分 {{ Number(merchant.score || 0).toFixed(1) }}</span>
            <span>人均 ¥{{ ((merchant.averagePrice || 0) / 100).toFixed(0) }}</span>
            <span>{{ distanceText(merchant.routeDistanceMeters || merchant.distanceMeters) }}</span>
            <span v-if="merchant.estimatedMinutes">约 {{ merchant.estimatedMinutes }} 分钟</span>
          </div>

          <div class="product-strip" :class="{ loading: productsLoading }">
            <Transition name="product-rotate" mode="out-in">
              <div
                v-if="activeProduct(merchant.id)"
                :key="activeProduct(merchant.id)?.id"
                class="product-chip product-feature"
              >
                <span class="product-label">主售</span>
                <span class="product-name">{{ activeProduct(merchant.id).name }}</span>
                <strong>{{ productPrice(activeProduct(merchant.id)) }}</strong>
                <span class="product-position">{{ activeProductPosition(merchant.id) }}</span>
              </div>
            </Transition>
            <div v-if="!productsLoading && !(merchantProducts[merchant.id] || []).length" class="product-empty">
              暂无上架商品
            </div>
          </div>
        </div>

        <div class="merchant-action">
          <RouterLink class="button enter-button" :to="`/merchant/${merchant.id}`">进入商家</RouterLink>
        </div>
      </article>
    </section>

    <section v-if="!loading && !merchants.length" class="empty-panel">
      <el-empty description="没有找到符合条件的店铺">
        <el-button type="primary" @click="resetFilters">清空条件</el-button>
      </el-empty>
    </section>

    <el-dialog v-model="locationDialogVisible" title="选择当前位置" width="760px" append-to-body>
      <LocationSelector
        v-model="currentLocation"
        save-enabled
        @confirm="confirmLocation"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.merchant-browser {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.browse-toolbar {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 250, 242, 0.92)),
    radial-gradient(circle at top right, rgba(57, 159, 125, 0.15), transparent 38%);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  display: grid;
  gap: 14px;
  padding: 18px;
}

.toolbar-copy p {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 4px;
}

.toolbar-copy h1 {
  color: var(--text-main);
  font-size: 28px;
  letter-spacing: 0;
  margin: 0;
}

.browse-location-panel {
  align-items: center;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  display: flex;
  gap: 14px;
  justify-content: space-between;
  padding: 12px 14px;
}

.location-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.location-copy strong {
  color: var(--text-main);
  font-size: 14px;
}

.location-copy span {
  color: var(--text-secondary);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-copy small {
  color: var(--text-muted);
  font-size: 12px;
}

.location-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.toolbar-controls {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(220px, 1fr) 150px 150px auto auto auto;
}

.toolbar-search,
.toolbar-select {
  width: 100%;
}

.location-line {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
}

.merchant-list {
  display: grid;
  gap: 12px;
  min-height: 240px;
}

.merchant-row {
  align-items: center;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  display: grid;
  gap: 16px;
  grid-template-columns: 92px minmax(0, 1fr) 120px;
  min-height: 144px;
  padding: 16px;
}

.merchant-avatar {
  align-items: center;
  background: linear-gradient(135deg, #f86f45, #36a57f);
  border-radius: 18px;
  color: #fff;
  display: flex;
  font-size: 30px;
  font-weight: 800;
  height: 92px;
  justify-content: center;
  overflow: hidden;
  width: 92px;
}

.merchant-avatar img {
  height: 100%;
  object-fit: cover;
  width: 100%;
}

.merchant-main {
  min-width: 0;
}

.merchant-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.merchant-head h2 {
  color: var(--text-main);
  font-size: 20px;
  letter-spacing: 0;
  margin: 0 0 4px;
}

.merchant-head p {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
}

.merchant-tags {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.merchant-meta {
  color: var(--text-muted);
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  gap: 10px;
  margin-top: 10px;
}

.product-strip {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  min-height: 44px;
  overflow: hidden;
  position: relative;
}

.product-chip {
  align-items: center;
  background: var(--clas-warm-50);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  display: inline-flex;
  flex: 0 0 auto;
  gap: 8px;
  max-width: 220px;
  min-height: 36px;
  padding: 7px 10px;
}

.product-feature {
  box-shadow: inset 0 0 0 1px rgba(249, 115, 22, 0.08);
  max-width: min(100%, 420px);
  min-width: min(100%, 300px);
}

.product-label,
.product-position {
  border-radius: 999px;
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  padding: 5px 7px;
}

.product-label {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.product-position {
  background: #fff;
  border: 1px solid var(--border-light);
  color: var(--text-muted);
  margin-left: auto;
}

.product-name {
  color: var(--text-main);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-chip strong {
  color: var(--color-primary);
  white-space: nowrap;
}

.product-empty {
  align-items: center;
  color: var(--text-muted);
  display: flex;
  font-size: 13px;
  min-height: 36px;
}

.product-rotate-enter-active,
.product-rotate-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.product-rotate-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.product-rotate-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.merchant-action {
  display: flex;
  justify-content: flex-end;
}

.enter-button {
  align-items: center;
  display: inline-flex;
  justify-content: center;
  min-height: 42px;
  min-width: 100px;
}

.empty-panel {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 34px;
}

@media (max-width: 980px) {
  .toolbar-controls {
    grid-template-columns: 1fr 1fr;
  }

  .browse-location-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .merchant-row {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .merchant-avatar {
    border-radius: 14px;
    height: 72px;
    width: 72px;
  }

  .merchant-action {
    grid-column: 2;
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .browse-toolbar {
    padding: 14px;
  }

  .toolbar-controls {
    grid-template-columns: 1fr;
  }

  .location-actions {
    flex-wrap: wrap;
    width: 100%;
  }

  .merchant-row {
    align-items: flex-start;
    grid-template-columns: 58px minmax(0, 1fr);
    padding: 12px;
  }

  .merchant-avatar {
    border-radius: 12px;
    font-size: 22px;
    height: 58px;
    width: 58px;
  }

  .merchant-head {
    flex-direction: column;
  }

  .product-feature {
    min-width: 100%;
  }

  .product-name {
    max-width: 120px;
  }

  .merchant-tags {
    justify-content: flex-start;
  }

  .merchant-action {
    grid-column: 1 / -1;
  }

  .enter-button {
    width: 100%;
  }
}
</style>
