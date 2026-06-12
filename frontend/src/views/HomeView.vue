<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { currentRole, listAddresses, listAnnouncements, listMerchants, currentUser, listOrders } from '../api/clas'
import LocationSelector from '../components/LocationSelector.vue'
import ChatWindow from '../components/ChatWindow.vue'
import { useChatStore } from '../composables/useChatStore'
import { loadAmap } from '../utils/amap'
import { resolveAutoLocationFromAmap } from '../utils/locationFormat'
import { getCurrentLocation, setCurrentLocation } from '../utils/locationStore'
import { formatDistance } from '../utils/formatters'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'HomeView'
})

const merchants = ref([])
const announcements = ref([])
const keyword = ref('')
const category = ref('')
const sort = ref('')
const loading = ref(false)
const onlyDeliverable = ref(false)
const addresses = ref([])
const selectedAddressId = ref('')
const currentLocation = ref(getCurrentLocation())
const filterDialogVisible = ref(false)
const locationPickerVisible = ref(false)
const draftCategory = ref('')
const draftSort = ref('')
const draftOnlyDeliverable = ref(false)
const draftAddressId = ref('')
const draftLocation = ref(null)
const activeOrders = ref([])
const chatOrder = ref(null)
const ordersLoading = ref(false)
const announcementsExpanded = ref(false)
const chatStore = useChatStore()
const categories = ['美食', '饮品', '休闲娱乐', '生活服务']
const DEFAULT_SORT = 'recommend'
const sortOptions = [
  { label: '距离最近', value: 'distance' },
  { label: '评分优先', value: 'score' },
  { label: '人均低价', value: 'price' },
  { label: '最新入驻', value: 'latest' }
]
const sortLabels = {
  recommend: '智能推荐',
  distance: '距离最近',
  score: '评分优先',
  price: '人均低价',
  latest: '最新入驻'
}
const hasSearchLocation = computed(() => Boolean(
  currentLocation.value?.longitude && currentLocation.value?.latitude
))

const draftHasSearchLocation = computed(() => {
  if (draftLocation.value?.longitude && draftLocation.value?.latitude) {
    return true
  }
  if (!draftAddressId.value) {
    return false
  }
  const address = addresses.value.find((item) => item.id === draftAddressId.value)
  return Boolean(address?.longitude && address?.latitude)
})

const appliedFilterCount = computed(() => {
  let count = 0
  if (category.value) count += 1
  if (sort.value) count += 1
  if (onlyDeliverable.value) count += 1
  return count
})

const resultSummary = computed(() => {
  if (loading.value) return '正在查找附近商家'
  return `找到 ${merchants.value.length} 家商家`
})

const activeFilters = computed(() => {
  const filters = []
  if (keyword.value.trim()) {
    filters.push({ key: 'keyword', label: `关键词：${keyword.value.trim()}` })
  }
  if (category.value) {
    filters.push({ key: 'category', label: `分类：${category.value}` })
  }
  if (sort.value) {
    filters.push({ key: 'sort', label: `排序：${sortLabels[sort.value] || sort.value}` })
  }
  if (shouldQueryByLocation() && currentLocation.value?.address) {
    filters.push({ key: 'location', label: `位置：${currentLocation.value.address}` })
  }
  if (onlyDeliverable.value) {
    filters.push({ key: 'deliverable', label: '仅看可配送' })
  }
  return filters
})

function distanceText(distance) {
  if (distance === null || distance === undefined) return '距离未知'
  return formatDistance(distance)
}

function merchantOpenStatus(merchant) {
  if (!merchant || merchant.status !== 'OPEN') return { open: false, label: '未营业', type: 'info' }
  if (merchant.manualClosed) return { open: false, label: '已打烊', type: 'info' }
  const hoursText = merchant.businessHours
  if (!hoursText || !hoursText.includes('-')) return { open: true, label: '营业中', type: 'success' }
  const [startText, endText] = hoursText.split('-').map((item) => item.trim())
  const start = parseBusinessMinutes(startText)
  const end = parseBusinessMinutes(endText)
  if (start === null || end === null || start === end) return { open: true, label: '营业中', type: 'success' }
  const nowDate = new Date()
  const now = nowDate.getHours() * 60 + nowDate.getMinutes()
  const open = start < end ? now >= start && now < end : now >= start || now < end
  return { open, label: open ? '营业中' : '已打烊', type: open ? 'success' : 'info' }
}

function parseBusinessMinutes(value) {
  const match = /^(\d{2}):(\d{2})$/.exec((value || '').trim())
  if (!match) return null
  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours > 23 || minutes > 59) return null
  return hours * 60 + minutes
}

async function load() {
  if (onlyDeliverable.value && !hasSearchLocation.value) {
    onlyDeliverable.value = false
    ElMessage.warning('请先选择当前位置或带坐标的收货地址')
  }
  const params = {
    keyword: keyword.value.trim() || undefined,
    category: category.value || undefined,
    sort: sort.value || DEFAULT_SORT
  }
  if (shouldQueryByLocation() && currentLocation.value?.longitude && currentLocation.value?.latitude) {
    params.lng = currentLocation.value.longitude
    params.lat = currentLocation.value.latitude
  } else if (shouldQueryByLocation() && selectedAddressId.value) {
    params.addressId = selectedAddressId.value
  }
  if (onlyDeliverable.value && hasSearchLocation.value) {
    params.onlyDeliverable = true
  }
  loading.value = true
  try {
    merchants.value = await listMerchants({
      ...params
    })
    try {
      announcements.value = await listAnnouncements()
    } catch {
      announcements.value = []
    }
  } finally {
    loading.value = false
  }
}

function shouldQueryByLocation() {
  return sort.value === 'distance' || onlyDeliverable.value
}

async function loadAddresses() {
  try {
    addresses.value = await listAddresses({ silent: true })
    const defaultAddress = addresses.value.find((item) => item.isDefault) || addresses.value[0]
    selectedAddressId.value = defaultAddress?.id || ''
    if (!currentLocation.value && defaultAddress?.longitude && defaultAddress?.latitude) {
      currentLocation.value = {
        province: '',
        city: '',
        district: '',
        street: defaultAddress.address,
        address: defaultAddress.address,
        longitude: Number(defaultAddress.longitude),
        latitude: Number(defaultAddress.latitude),
        source: 'manual'
      }
    }
  } catch {
    addresses.value = []
  }
}

async function autoLocate() {
  if (currentLocation.value?.longitude && currentLocation.value?.latitude) {
    return
  }
  try {
    const AMap = await loadAmap()
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 3000,
      showButton: false
    })
    await new Promise((resolve) => {
      geolocation.getCurrentPosition(async (status, result) => {
        if (status !== 'complete') {
          resolve()
          return
        }
        const next = await resolveAutoLocationFromAmap(AMap, result)
        currentLocation.value = next
        setCurrentLocation(next)
        selectedAddressId.value = ''
        resolve()
      })
    })
  } catch {
    // Browser permission or key problems should not block the home page.
  }
}

function cloneLocation(location) {
  return location ? { ...location } : null
}

function openFilterDialog() {
  draftCategory.value = category.value
  draftSort.value = sort.value
  draftOnlyDeliverable.value = onlyDeliverable.value
  draftAddressId.value = selectedAddressId.value
  draftLocation.value = cloneLocation(currentLocation.value)
  filterDialogVisible.value = true
}

function toggleDraftCategory(value) {
  draftCategory.value = draftCategory.value === value ? '' : value
}

function toggleDraftSort(value) {
  draftSort.value = draftSort.value === value ? '' : value
}

function toggleDraftDeliverable() {
  if (!draftHasSearchLocation.value) {
    ElMessage.warning('请先选择当前位置或带坐标的收货地址')
    return
  }
  draftOnlyDeliverable.value = !draftOnlyDeliverable.value
}

function onDraftAddressChange(id) {
  draftAddressId.value = id || ''
  if (!id) {
    return
  }
  const address = addresses.value.find((item) => item.id === id)
  if (!address?.longitude || !address?.latitude) {
    draftLocation.value = null
    if (draftOnlyDeliverable.value) {
      draftOnlyDeliverable.value = false
      ElMessage.warning('该地址缺少地图坐标，已关闭可配送筛选')
    }
    return
  }
  draftLocation.value = {
    province: '',
    city: '',
    district: '',
    street: address.address,
    address: address.address,
    longitude: Number(address.longitude),
    latitude: Number(address.latitude),
    source: 'manual'
  }
  if (!draftSort.value) {
    draftSort.value = 'distance'
  }
}

function confirmDraftLocation(location) {
  draftLocation.value = location
  draftAddressId.value = ''
  if (!draftSort.value) {
    draftSort.value = 'distance'
  }
  locationPickerVisible.value = false
  ElMessage.success('位置已选择，点击应用生效')
}

function applyFilterDialog() {
  category.value = draftCategory.value
  sort.value = draftSort.value
  onlyDeliverable.value = draftOnlyDeliverable.value
  selectedAddressId.value = draftAddressId.value
  currentLocation.value = cloneLocation(draftLocation.value)
  if (currentLocation.value) {
    setCurrentLocation(currentLocation.value)
  }
  filterDialogVisible.value = false
  load()
}

function resetFilterDialog() {
  draftCategory.value = ''
  draftSort.value = ''
  draftOnlyDeliverable.value = false
  draftAddressId.value = ''
  draftLocation.value = null
}

function resetAllFilters() {
  keyword.value = ''
  category.value = ''
  sort.value = ''
  onlyDeliverable.value = false
  load()
}

const orderStatusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '已支付(自动接单中)',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REJECTED: '商家已拒单',
  REFUNDED: '已退款',
  REFUND_PENDING: '退款处理中'
}

const deliveryLabel = {
  WAITING: '等待自动接单',
  PREPARING: '商家备餐中',
  DELIVERING: '配送中',
  DELIVERED: '已送达'
}

function openChat(order) {
  chatOrder.value = order
}

function closeChat() {
  chatOrder.value = null
}

async function loadActiveOrders() {
  if (currentRole() !== 'USER') return
  ordersLoading.value = true
  try {
    const all = await listOrders()
    activeOrders.value = all.filter((o) => o.order.status === 'PAID' || o.order.status === 'ACCEPTED')
  } catch {
    activeOrders.value = []
  } finally {
    ordersLoading.value = false
  }
}

function hasActiveOrders() {
  return activeOrders.value.length > 0
}

const showActiveOrdersPanel = computed(() => currentRole() === 'USER' && (ordersLoading.value || hasActiveOrders()))
const showAnnouncementsPanel = computed(() => announcements.value.length > 0)
const hasTopSidePanels = computed(() => showAnnouncementsPanel.value || showActiveOrdersPanel.value)

watch(
  () => [
    currentLocation.value?.longitude,
    currentLocation.value?.latitude
  ],
  ([longitude, latitude], [oldLongitude, oldLatitude]) => {
    if (!longitude || !latitude || (longitude === oldLongitude && latitude === oldLatitude)) {
      return
    }
    if (shouldQueryByLocation()) {
      load()
    }
  }
)

onMounted(async () => {
  await loadAddresses()
  await autoLocate()
  await Promise.all([load(), loadActiveOrders()])
})
</script>

<template>
  <div class="home-layout">
    <!-- ═══ 左侧固定栏 ═══ -->
    <aside class="home-sidebar">
      <section class="hero home-side-hero">
        <div class="home-top-hero-copy">
          <h1>CLAS 综合生活助手平台</h1>
          <p>搜索附近商家、购买外卖与团购券，让吃喝玩乐一键触达。</p>
        </div>
        <div class="hero-actions" v-if="currentRole() === 'USER'">
          <RouterLink class="button" to="/deals">团购到店</RouterLink>
          <RouterLink class="button secondary" to="/orders">我的订单</RouterLink>
        </div>
      </section>

      <section
        v-if="showActiveOrdersPanel"
        class="panel sidebar-panel active-orders-panel"
        v-loading="ordersLoading"
      >
        <div class="section-head">
          <h2>进行中的订单</h2>
          <RouterLink to="/orders">查看全部订单</RouterLink>
        </div>
        <div v-if="hasActiveOrders()" class="sidebar-panel-body">
          <article class="active-order-card" v-for="order in activeOrders.slice(0, 1)" :key="order.order.id">
            <div class="ao-info">
              <span class="ao-id">订单 #{{ order.order.id }}</span>
              <el-tag size="small" :type="order.order.status === 'PAID' ? 'warning' : 'success'">
                {{ orderStatusLabel[order.order.status] || order.order.status }}
              </el-tag>
              <span class="ao-delivery">{{ deliveryLabel[order.order.deliveryStatus] || order.order.deliveryStatus }}</span>
              <span class="ao-price">¥{{ (order.order.totalPrice / 100).toFixed(2) }}</span>
              <span class="ao-items">{{ order.items.length }} 件商品</span>
            </div>
            <div class="ao-actions">
              <button class="button secondary ao-action-btn" type="button" @click="openChat(order)">联系商家</button>
              <RouterLink class="button secondary ao-action-btn" :to="`/orders`">查看详情</RouterLink>
            </div>
          </article>
        </div>
      </section>

      <section v-if="showAnnouncementsPanel" class="panel sidebar-panel announcements-panel">
        <div class="section-head">
          <h2>平台公告</h2>
          <button
            class="collapse-toggle"
            type="button"
            :title="announcementsExpanded ? '收起' : '展开'"
            @click="announcementsExpanded = !announcementsExpanded"
          >
            {{ announcementsExpanded ? '收起 ▲' : '展开 ▼' }}
          </button>
        </div>
        <div class="sidebar-panel-body" :class="{ collapsed: !announcementsExpanded }">
          <article class="announcement-preview" v-for="item in announcements" :key="item.id">
            <h3>{{ item.title }}</h3>
            <p>{{ item.content }}</p>
          </article>
        </div>
      </section>
    </aside>

    <!-- ═══ 右侧滚动内容 ═══ -->
    <div class="home-main">
      <div class="home-main-sticky">
        <section class="panel search-panel">
          <div class="search-row">
            <el-input
              v-model="keyword"
              class="search-input"
              placeholder="搜索商家名称"
              clearable
              @clear="load"
              @keyup.enter="load"
            />
            <el-button type="primary" @click="load">搜索</el-button>
            <el-button @click="openFilterDialog">
              筛选<span v-if="appliedFilterCount" class="filter-count">({{ appliedFilterCount }})</span>
            </el-button>
          </div>
        </section>

        <section class="panel location-panel">
          <strong>当前定位</strong>
          <span>{{ currentLocation?.address || '未定位，请选择位置' }}</span>
          <small v-if="!hasSearchLocation">选择位置后可筛选可配送商家</small>
        </section>

        <section class="panel result-panel">
          <div class="section-head">
            <h2>附近商家</h2>
            <span>{{ resultSummary }}</span>
          </div>
          <div class="filter-tags" v-if="activeFilters.length">
            <el-tag v-for="filter in activeFilters" :key="filter.key" effect="plain">
              {{ filter.label }}
            </el-tag>
          </div>
        </section>
      </div>

      <section class="grid" v-loading="loading" v-if="loading || merchants.length">
        <article class="card" v-for="merchant in merchants" :key="merchant.id">
          <div
            class="merchant-visual"
            :class="{ 'has-logo': merchant.logo }"
          >
            <div
              class="merchant-visual-frame"
              :class="{ 'has-logo': merchant.logo }"
              :style="merchant.logo ? { backgroundImage: `url(${merchant.logo})` } : null"
            ></div>
            <div class="merchant-logo-badge">
              <img v-if="merchant.logo" :src="merchant.logo" alt="商铺图标" class="merchant-logo-img" />
              <span v-else>{{ merchant.category }}</span>
            </div>
          </div>
          <div class="merchant-card-body">
            <h2 class="merchant-name">{{ merchant.merchantName }}</h2>
            <p class="merchant-address">{{ merchant.address }}</p>
            <div class="merchant-meta">
              <span class="merchant-rating">★ {{ Number(merchant.score || 0).toFixed(1) }}</span>
              <span>人均 ¥{{ ((merchant.averagePrice || 0) / 100).toFixed(0) }}</span>
              <span>{{ merchant.businessHours || '营业中' }}</span>
            </div>
            <div class="merchant-info-pills">
              <span>起送 ¥{{ ((merchant.minOrderPrice || 0) / 100).toFixed(0) }}</span>
              <span>配送 ¥{{ ((merchant.deliveryFee || 0) / 100).toFixed(0) }}</span>
              <span>{{ distanceText(merchant.routeDistanceMeters || merchant.distanceMeters) }}</span>
              <span v-if="merchant.estimatedMinutes">约 {{ merchant.estimatedMinutes }} 分钟</span>
              <span v-if="merchant.deliveryRadiusM">{{ distanceText(merchant.deliveryRadiusM) }} 内配送</span>
            </div>
          </div>
          <div class="delivery-status">
            <el-tag :type="merchantOpenStatus(merchant).type" effect="plain">{{ merchantOpenStatus(merchant).label }}</el-tag>
            <el-tag v-if="merchant.deliveryAvailable === true" type="success" effect="plain">可配送</el-tag>
            <el-tag v-else-if="merchant.deliveryAvailable === false" type="danger" effect="plain">超出配送范围</el-tag>
            <el-tag v-else effect="plain">选择位置查看配送</el-tag>
          </div>
          <div class="card-actions">
            <RouterLink class="button secondary merchant-card-btn" :to="`/merchant/${merchant.id}`">进入店铺</RouterLink>
            <button class="button secondary merchant-card-btn" type="button" @click="chatStore.openMerchantChat(merchant.id)">咨询客服</button>
          </div>
        </article>
      </section>

      <section class="panel empty-panel" v-else>
        <el-empty description="没有找到符合条件的商家">
          <div class="empty-actions">
            <el-button type="primary" @click="openFilterDialog">调整筛选</el-button>
            <el-button @click="resetAllFilters">清空条件</el-button>
          </div>
        </el-empty>
      </section>
    </div>
  </div>

  <el-dialog v-model="filterDialogVisible" title="筛选商家" width="640px" class="filter-dialog">
    <div class="filter-section">
      <h3>商家分类</h3>
      <div class="filter-chip-group">
        <button
          v-for="item in categories"
          :key="item"
          type="button"
          class="filter-chip"
          :class="{ active: draftCategory === item }"
          @click="toggleDraftCategory(item)"
        >
          {{ item }}
        </button>
      </div>
    </div>

    <div class="filter-section">
      <h3>排序方式</h3>
      <div class="filter-chip-group">
        <button
          v-for="item in sortOptions"
          :key="item.value"
          type="button"
          class="filter-chip"
          :class="{ active: draftSort === item.value }"
          @click="toggleDraftSort(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="filter-section">
      <h3>参考位置</h3>
      <div class="location-filter-row">
        <el-select
          v-model="draftAddressId"
          placeholder="选择收货地址"
          clearable
          @change="onDraftAddressChange"
          @clear="onDraftAddressChange('')"
        >
          <el-option
            v-for="item in addresses"
            :key="item.id"
            :label="`${item.contactName} · ${item.address}`"
            :value="item.id"
          />
        </el-select>
        <el-button @click="locationPickerVisible = true">选择位置</el-button>
      </div>
      <p class="filter-hint">{{ draftLocation?.address || '未选择位置时将使用默认地址或定位' }}</p>
    </div>

    <div class="filter-section">
      <h3>配送范围</h3>
      <button
        type="button"
        class="filter-chip"
        :class="{ active: draftOnlyDeliverable, disabled: !draftHasSearchLocation }"
        :disabled="!draftHasSearchLocation"
        @click="toggleDraftDeliverable"
      >
        仅看可配送
      </button>
      <p v-if="!draftHasSearchLocation" class="filter-hint">请先选择参考位置后再启用</p>
    </div>

    <template #footer>
      <el-button @click="resetFilterDialog">重置</el-button>
      <el-button type="primary" @click="applyFilterDialog">应用</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="locationPickerVisible" title="选择当前位置" width="760px" append-to-body>
    <LocationSelector
      v-model="draftLocation"
      save-enabled
      @confirm="confirmDraftLocation"
    />
  </el-dialog>

  <!-- 聊天弹窗 -->
  <div v-if="chatOrder" class="order-overlay" @click.self="closeChat">
    <aside class="chat-panel">
      <header class="chat-panel-head">
        <h2>与商家沟通</h2>
        <p class="chat-panel-subtitle">订单 #{{ chatOrder.order.id }}</p>
        <button class="panel-close" type="button" @click="closeChat">×</button>
      </header>
      <div class="chat-panel-body">
        <ChatWindow
          :order-id="chatOrder.order.id"
          :merchant-id="chatOrder.order.merchantId"
          :merchant-name="''"
          role="USER"
          :order-status="chatOrder.order.status"
          :order-number="chatOrder.order.id"
        />
      </div>
    </aside>
  </div>
</template>

<style scoped>
/* ═══ 两栏布局 — 左侧固定侧栏 + 右侧滚动内容 ═══ */
.home-layout {
  align-items: flex-start;
  display: grid;
  gap: 20px;
  grid-template-columns: 360px minmax(0, 1fr);
}

/* ─── 左侧固定侧栏 ─── */
.home-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: sticky;
  top: 84px;
  max-height: calc(100vh - 104px);
  overflow-y: auto;
  overflow-x: hidden;
}

.home-side-hero {
  align-items: flex-start;
  border: 1px solid var(--border-color) !important;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 0;
  flex-shrink: 0;
  min-height: auto;
}

.home-top-hero-copy {
  position: relative;
}

.sidebar-panel {
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
  min-height: 0;
  flex-shrink: 1;
}

.announcements-panel {
  flex-shrink: 0;
  flex: 0 0 auto;
}

.announcements-panel .sidebar-panel-body {
  overflow-y: auto;
  max-height: 260px;
  transition: max-height 0.3s ease;
}

.announcements-panel .sidebar-panel-body.collapsed {
  max-height: 0;
  overflow: hidden;
}

.collapse-toggle {
  background: none;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.2;
  min-height: 28px;
  padding: 4px 10px;
  transition: color 0.2s, border-color 0.2s;
}

.collapse-toggle:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.active-orders-panel {
  flex-shrink: 0;
  flex: 0 0 auto;
}

.sidebar-panel-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.active-orders-panel .sidebar-panel-body {
  display: flex;
}

/* ─── 右侧主内容 ─── */
.home-main {
  min-width: 0;
}

.home-main-sticky {
  position: sticky;
  top: 64px;
  z-index: 3;
  background: #faf7f2;
  padding-bottom: 4px;
  border-bottom: 1px solid transparent;
}

.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.search-panel {
  margin-bottom: 14px;
}

.search-row {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto auto;
}

.search-row :deep(.el-input) {
  margin: 0;
  width: 100%;
}

.search-row :deep(.el-button) {
  margin: 0;
}

.search-input {
  min-width: 0;
}

.filter-count {
  margin-left: 4px;
}

.filter-section {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-section:last-child {
  margin-bottom: 0;
}

.filter-section h3 {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.location-filter-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.location-filter-row :deep(.el-select) {
  flex: 1;
  min-width: 240px;
}

.filter-hint {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
}

.filter-dialog :deep(.el-dialog__footer) {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.filter-chip-group {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.filter-chip {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 999px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  line-height: 1.2;
  padding: 7px 14px;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.filter-chip:hover:not(.disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.filter-chip.active {
  background: rgba(37, 99, 235, 0.08);
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.filter-chip.disabled,
.filter-chip:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.deliverable-chip {
  align-self: center;
}

/* ─── 响应式 ─── */
@media (max-width: 1024px) {
  .home-layout {
    grid-template-columns: 300px minmax(0, 1fr);
  }
}

@media (max-width: 860px) {
  .home-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .home-sidebar {
    position: static;
    max-height: none;
  }
}

@media (max-width: 768px) {
  .search-row {
    grid-template-columns: 1fr 1fr;
  }

  .search-input {
    grid-column: 1 / -1;
  }

  .search-row :deep(.el-button) {
    width: 100%;
  }
}

.location-panel {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.location-panel span {
  color: var(--text-secondary);
}

.location-panel small {
  color: var(--text-muted);
}

.result-panel {
  display: grid;
  gap: 12px;
}

.result-panel .section-head {
  margin-bottom: 0;
}

.result-panel .section-head span {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.grid .card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.merchant-card-body {
  display: grid;
  gap: 8px;
}

.merchant-name {
  color: var(--text-primary);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.25;
  margin: 0;
}

.merchant-address {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.45;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merchant-meta {
  align-items: center;
  color: var(--text-secondary);
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  font-weight: 600;
  gap: 6px 0;
  line-height: 1.3;
}

.merchant-meta span {
  align-items: center;
  display: inline-flex;
}

.merchant-meta span + span::before {
  color: var(--text-muted);
  content: "·";
  font-weight: 600;
  padding: 0 8px;
}

.merchant-rating {
  color: var(--color-warning);
  font-weight: 800;
}

.merchant-info-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 2px;
}

.merchant-info-pills span {
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.1);
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  padding: 6px 9px;
}

.delivery-status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  min-height: 28px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
  padding-top: 16px;
}

.merchant-card-btn {
  flex: 1 1 0;
  font-size: 14px;
  font-weight: 600;
  justify-content: center;
  line-height: 1.2;
  min-height: 40px;
  min-width: 0;
  padding: 0 14px;
  text-align: center;
}

.merchant-visual {
  align-items: center;
  display: flex;
  flex-shrink: 0;
  justify-content: center;
  margin-bottom: 26px;
  min-height: 150px;
  overflow: visible;
  position: relative;
}

.merchant-visual-frame {
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(13, 148, 136, 0.14)),
    var(--bg-subtle);
  background-position: center;
  background-size: cover;
  border-radius: var(--radius-md);
  inset: 0;
  overflow: hidden;
  position: absolute;
}

.merchant-visual-frame.has-logo::before {
  background: inherit;
  content: "";
  filter: blur(2px) saturate(1.02);
  inset: -4px;
  opacity: 0.35;
  position: absolute;
  transform: scale(1.01);
}

.merchant-visual-frame::after {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.04), rgba(15, 23, 42, 0.18));
  content: "";
  inset: 0;
  position: absolute;
}

.merchant-logo-badge {
  align-items: center;
  background: var(--bg-card);
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-radius: 50%;
  box-shadow: var(--shadow-md);
  color: var(--color-primary);
  display: flex;
  font-size: 15px;
  font-weight: 700;
  height: 86px;
  justify-content: center;
  left: 18px;
  line-height: 1.2;
  overflow: hidden;
  position: absolute;
  text-align: center;
  top: calc(100% - 58px);
  width: 86px;
  z-index: 1;
}

.merchant-logo-img {
  border-radius: 50%;
  display: block;
  height: 100%;
  width: 100%;
  object-fit: cover;
  object-position: center;
}

.empty-panel {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 260px;
}

.empty-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}
.section-head h2 {
  font-size: 18px;
  margin: 0;
  font-weight: 700;
  color: var(--text-primary);
}
.section-head a {
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
}

.announcement-preview {
  min-width: 0;
}
.announcement-preview h3 {
  font-size: 16px;
  margin: 0 0 6px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.announcement-preview p {
  color: var(--text-secondary);
  display: -webkit-box;
  line-clamp: 2;
  margin: 0;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.6;
  overflow: hidden;
}

.active-order-list {
  gap: 10px;
}

.active-order-card {
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 10px;
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  padding: 14px 18px;
}

.ao-info {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ao-id {
  font-weight: 700;
  color: var(--text-primary);
}

.ao-delivery {
  color: var(--text-secondary);
  font-size: 13px;
}

.ao-price {
  color: var(--clas-danger, #f56c6c);
  font-weight: 700;
}

.ao-items {
  color: var(--text-muted);
  font-size: 13px;
}

.ao-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ao-action-btn {
  font-size: 14px;
  font-weight: 600;
  min-height: 40px;
  padding: 0 16px;
}

/* 聊天弹窗 */
.order-overlay {
  align-items: center;
  background: rgba(15, 23, 42, 0.28);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 24px;
  position: fixed;
  z-index: 30;
}

.chat-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  height: 520px;
  max-width: 480px;
  width: 100%;
}

.chat-panel-head {
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 18px;
}

.chat-panel-head h2 {
  font-size: 18px;
  margin: 0;
}

.chat-panel-subtitle {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0 0 0 auto;
}

.panel-close {
  background: var(--clas-warm-100);
  border: none;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 18px;
  min-height: 32px;
  min-width: 32px;
  padding: 0;
}

.chat-panel-body {
  flex: 1;
  overflow: hidden;
}

.chat-panel-body :deep(.chat-window) {
  border: none;
  border-radius: 0;
  max-height: none;
}
</style>
