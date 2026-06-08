<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { currentRole, listAddresses, listAnnouncements, listMerchants } from '../api/clas'
import LocationSelector from '../components/LocationSelector.vue'
import { loadAmap } from '../utils/amap'
import { getCurrentLocation, setCurrentLocation } from '../utils/locationStore'
import { ElMessage } from 'element-plus'

const merchants = ref([])
const announcements = ref([])
const keyword = ref('')
const category = ref('')
const sort = ref('score')
const addresses = ref([])
const selectedAddressId = ref('')
const currentLocation = ref(getCurrentLocation())
const locationDialogVisible = ref(false)
const categories = ['美食', '饮品', '休闲娱乐', '生活服务']

function distanceText(distance) {
  if (distance === null || distance === undefined) return '距离未知'
  return distance < 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`
}

async function load() {
  const params = {
    keyword: keyword.value || undefined,
    category: category.value || undefined,
    sort: sort.value
  }
  if (currentLocation.value?.longitude && currentLocation.value?.latitude) {
    params.lng = currentLocation.value.longitude
    params.lat = currentLocation.value.latitude
  } else if (selectedAddressId.value) {
    params.addressId = selectedAddressId.value
  }
  merchants.value = await listMerchants({
    ...params
  })
  try {
    announcements.value = await listAnnouncements()
  } catch {
    announcements.value = []
  }
}

async function loadAddresses() {
  try {
    addresses.value = await listAddresses({ silent: true })
    const defaultAddress = addresses.value.find((item) => item.isDefault) || addresses.value[0]
    selectedAddressId.value = defaultAddress?.id || ''
    if (!currentLocation.value && defaultAddress?.longitude && defaultAddress?.latitude) {
      currentLocation.value = {
        address: defaultAddress.address,
        longitude: Number(defaultAddress.longitude),
        latitude: Number(defaultAddress.latitude),
        source: 'address'
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
      geolocation.getCurrentPosition((status, result) => {
        if (status !== 'complete') {
          resolve()
          return
        }
        const next = {
          province: result.addressComponent?.province || '',
          city: result.addressComponent?.city || '',
          district: result.addressComponent?.district || '',
          address: result.formattedAddress || '当前位置',
          longitude: result.position.lng,
          latitude: result.position.lat,
          source: 'auto'
        }
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
    load()
    return
  }
  currentLocation.value = {
    address: address.address,
    longitude: Number(address.longitude),
    latitude: Number(address.latitude),
    source: 'address'
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
    <el-input v-model="keyword" placeholder="搜索商家、地点或分类" clearable @keyup.enter="load" />
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
    <el-button type="primary" @click="load">搜索</el-button>
    <el-button @click="locationDialogVisible = true">选择位置</el-button>
  </section>

  <section class="panel location-panel">
    <strong>当前定位</strong>
    <span>{{ currentLocation?.address || '未定位，请选择位置' }}</span>
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

  <section class="grid">
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
        <span v-if="merchant.deliveryAvailable === true"> · 可配送</span>
        <span v-else-if="merchant.deliveryAvailable === false"> · 超出配送范围</span>
      </p>
      <RouterLink class="button secondary" :to="`/merchant/${merchant.id}`">进入店铺</RouterLink>
    </article>
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
  gap: 10px;
}

.location-panel span {
  color: var(--text-secondary);
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
