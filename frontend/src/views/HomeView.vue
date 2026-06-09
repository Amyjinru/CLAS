<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { currentRole, listAddresses, listAnnouncements, listMerchants } from '../api/clas'
import LocationSelector from '../components/LocationSelector.vue'
import { loadAmap } from '../utils/amap'
import { resolveAutoLocationFromAmap } from '../utils/locationFormat'
import { getCurrentLocation, setCurrentLocation } from '../utils/locationStore'
import { ElMessage } from 'element-plus'

const merchants = ref([])
const announcements = ref([])
const keyword = ref('')
const category = ref('')
const sort = ref('score')
const loading = ref(false)
const onlyDeliverable = ref(false)
const addresses = ref([])
const selectedAddressId = ref('')
const currentLocation = ref(getCurrentLocation())
const locationDialogVisible = ref(false)
const categories = ['美食', '饮品', '休闲娱乐', '生活服务']
const sortLabels = {
  distance: '距离最近',
  score: '评分优先',
  price: '人均低价',
  latest: '最新入驻'
}

const hasSearchLocation = computed(() => Boolean(
  currentLocation.value?.longitude && currentLocation.value?.latitude
))

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
    filters.push({ key: 'sort', label: `排序：${sortLabels[sort.value] || '评分优先'}` })
  }
  if (currentLocation.value?.address) {
    filters.push({ key: 'location', label: `位置：${currentLocation.value.address}` })
  }
  if (onlyDeliverable.value) {
    filters.push({ key: 'deliverable', label: '仅看可配送' })
  }
  return filters
})

function distanceText(distance) {
  if (distance === null || distance === undefined) return '距离未知'
  return distance < 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`
}

async function load() {
  if (onlyDeliverable.value && !hasSearchLocation.value) {
    onlyDeliverable.value = false
    ElMessage.warning('请先选择当前位置或带坐标的收货地址')
  }
  const params = {
    keyword: keyword.value.trim() || undefined,
    category: category.value || undefined,
    sort: sort.value
  }
  if (currentLocation.value?.longitude && currentLocation.value?.latitude) {
    params.lng = currentLocation.value.longitude
    params.lat = currentLocation.value.latitude
  } else if (selectedAddressId.value) {
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
    if (selectedAddressId.value && sort.value === 'score') {
      sort.value = 'distance'
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
      timeout: 8000,
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
        sort.value = 'distance'
        resolve()
      })
    })
  } catch {
    // Browser permission or key problems should not block the home page.
  }
}

function applyAddress(id) {
  const address = addresses.value.find((item) => item.id === id)
  if (!address?.longitude || !address?.latitude) {
    currentLocation.value = null
    if (onlyDeliverable.value) {
      onlyDeliverable.value = false
      ElMessage.warning('该地址缺少地图坐标，已关闭可配送筛选')
    }
    load()
    return
  }
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
  sort.value = 'distance'
  load()
}

function confirmLocation(location) {
  currentLocation.value = location
  setCurrentLocation(location)
  selectedAddressId.value = ''
  sort.value = 'distance'
  locationDialogVisible.value = false
  ElMessage.success('已切换当前位置')
  load()
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  sort.value = 'score'
  onlyDeliverable.value = false
  load()
}

onMounted(async () => {
  await loadAddresses()
  await autoLocate()
  await load()
})
</script>

<template>
  <section class="hero">
    <div>
      <h1>CLAS 综合生活助手平台</h1>
      <p>搜索附近商家、购买外卖与团购券，让吃喝玩乐一键触达。</p>
    </div>
    <div class="hero-actions" v-if="currentRole() === 'USER'">
      <RouterLink class="button" to="/deals">团购到店</RouterLink>
      <RouterLink class="button secondary" to="/orders">我的订单</RouterLink>
    </div>
  </section>

  <section class="panel search-panel">
    <el-input v-model="keyword" placeholder="搜索商家、地点或分类" clearable @clear="load" @keyup.enter="load" />
    <el-select v-model="category" placeholder="全部分类" clearable>
      <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
    </el-select>
    <el-select v-model="selectedAddressId" placeholder="附近地址" clearable @change="applyAddress">
      <el-option v-for="item in addresses" :key="item.id" :label="`${item.contactName} · ${item.address}`" :value="item.id" />
    </el-select>
    <el-segmented v-model="sort" :options="[
      { label: '距离最近', value: 'distance' },
      { label: '评分优先', value: 'score' },
      { label: '人均低价', value: 'price' },
      { label: '最新入驻', value: 'latest' }
    ]" />
    <el-switch
      v-model="onlyDeliverable"
      :disabled="!hasSearchLocation"
      active-text="仅看可配送"
      @change="load"
    />
    <el-button type="primary" @click="load">搜索</el-button>
    <el-button @click="resetFilters">重置</el-button>
    <el-button @click="locationDialogVisible = true">选择位置</el-button>
  </section>

  <section class="panel location-panel">
    <strong>当前定位</strong>
    <span>{{ currentLocation?.address || '未定位，请选择位置' }}</span>
    <small v-if="!hasSearchLocation">选择位置后可筛选可配送商家</small>
  </section>

  <section class="panel" v-if="announcements.length">
    <div class="section-head">
      <h2>平台公告</h2>
      <RouterLink to="/user/announcements">查看全部</RouterLink>
    </div>
    <article class="announcement-preview" v-for="item in announcements.slice(0, 2)" :key="item.id">
      <h3>{{ item.title }}</h3>
      <p>{{ item.content }}</p>
    </article>
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

  <section class="grid" v-loading="loading" v-if="loading || merchants.length">
    <article class="card" v-for="merchant in merchants" :key="merchant.id">
      <div class="thumb">{{ merchant.category }}</div>
      <h2>{{ merchant.merchantName }}</h2>
      <p>{{ merchant.address }}</p>
      <p>评分 {{ merchant.score }} · 人均 ¥{{ ((merchant.averagePrice || 0) / 100).toFixed(0) }} · {{ merchant.businessHours || '营业中' }}</p>
      <p>起送 ¥{{ ((merchant.minOrderPrice || 0) / 100).toFixed(0) }} · 配送费 ¥{{ ((merchant.deliveryFee || 0) / 100).toFixed(0) }}</p>
      <p>
        {{ distanceText(merchant.routeDistanceMeters || merchant.distanceMeters) }}
        <span v-if="merchant.estimatedMinutes"> · 约 {{ merchant.estimatedMinutes }} 分钟</span>
        <span v-if="merchant.deliveryRadiusM"> · {{ distanceText(merchant.deliveryRadiusM) }} 内配送</span>
      </p>
      <p class="delivery-status">
        <el-tag v-if="merchant.deliveryAvailable === true" type="success" effect="plain">可配送</el-tag>
        <el-tag v-else-if="merchant.deliveryAvailable === false" type="danger" effect="plain">超出配送范围</el-tag>
        <el-tag v-else effect="plain">选择位置查看配送</el-tag>
      </p>
      <RouterLink class="button secondary" :to="`/merchant/${merchant.id}`">进入店铺</RouterLink>
    </article>
  </section>

  <section class="panel empty-panel" v-else>
    <el-empty description="没有找到符合条件的商家">
      <div class="empty-actions">
        <el-button type="primary" @click="resetFilters">重置筛选</el-button>
        <el-button @click="locationDialogVisible = true">更换位置</el-button>
      </div>
    </el-empty>
  </section>

  <el-dialog v-model="locationDialogVisible" title="选择当前位置" width="760px">
    <LocationSelector
      v-model="currentLocation"
      save-enabled
      @confirm="confirmLocation"
    />
  </el-dialog>
</template>

<style scoped>
.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.hero-actions,
.search-panel {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.search-panel :deep(.el-input) {
  max-width: 320px;
}

.search-panel :deep(.el-select) {
  width: 150px;
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

.delivery-status {
  min-height: 28px;
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
  border-top: 1px solid var(--border-light);
  padding-top: 14px;
  margin-top: 14px;
}
.announcement-preview:first-of-type {
  border-top: 0;
  margin-top: 0;
  padding-top: 0;
}
.announcement-preview h3 {
  font-size: 16px;
  margin: 0 0 6px;
  color: var(--text-primary);
}
.announcement-preview p {
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}
</style>
