<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createAddress } from '../api/clas'
import { hasAmapKey, loadAmap } from '../utils/amap'
import { resolveAutoLocationFromAmap } from '../utils/locationFormat'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  },
  saveEnabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const mode = ref('auto')
const locating = ref(false)
const loadingDistrict = ref(false)
const provinces = ref([])
const cities = ref([])
const districts = ref([])
const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')
const current = reactive({
  province: '',
  city: '',
  district: '',
  street: '',
  address: '',
  longitude: null,
  latitude: null,
  source: ''
})
const selectedLocation = ref(null)
const saveForm = reactive({
  contactName: '',
  phone: '',
  isDefault: true
})

let AMapRef = null
const districtCache = new Map()

function syncCurrent(next) {
  if (!next) return
  const normalized = normalizeLocation(next, next.source || 'manual')
  Object.assign(current, normalized)
  mode.value = normalized.source === 'auto' ? 'auto' : 'manual'
  selectedProvince.value = current.province
  selectedCity.value = current.city
  selectedDistrict.value = current.district
  
  // Try to extract street if it's missing but address is present
  if (!current.street && current.address && current.province && current.city && current.district) {
    const prefix = `${current.province}${current.city}${current.district}`
    if (current.address.startsWith(prefix)) {
      current.street = current.address.substring(prefix.length)
    }
  }
  selectedLocation.value = normalizeLocation(current, current.source || 'manual')
}

function updateFullAddress() {
  current.address = `${current.province}${current.city}${current.district}${current.street}`
}

watch(() => current.street, updateFullAddress)

function hasCoordinate(longitude, latitude) {
  return longitude !== null && longitude !== undefined && longitude !== ''
    && latitude !== null && latitude !== undefined && latitude !== ''
}

function normalizeLocation(location, source = mode.value) {
  const normalizedSource = source === 'auto' ? 'auto' : 'manual'
  const province = location?.province || ''
  const city = location?.city || ''
  const district = location?.district || ''
  const street = location?.street || ''
  const address = location?.address || `${province}${city}${district}${street}`
  return {
    province,
    city,
    district,
    street,
    address,
    longitude: location?.longitude ?? null,
    latitude: location?.latitude ?? null,
    source: normalizedSource
  }
}

function emitSelection(location, source, shouldConfirm = false) {
  const next = normalizeLocation(location, source)
  selectedLocation.value = next
  Object.assign(current, next)
  emit('update:modelValue', next)
  if (shouldConfirm) {
    emit('confirm', next)
  }
}

function confirmSelectedLocation() {
  if (!selectedLocation.value?.address || !hasCoordinate(selectedLocation.value.longitude, selectedLocation.value.latitude)) {
    ElMessage.warning('请先生成可用地址')
    return
  }
  emit('confirm', { ...selectedLocation.value })
}

function sortAdministrativeAreas(items) {
  return [...(items || [])].sort((a, b) => (a.name || '').localeCompare(
    b.name || '',
    'zh-Hans-CN-u-co-pinyin',
    { sensitivity: 'base' }
  ))
}

function markManualDraft() {
  mode.value = 'manual'
  current.source = 'manual'
}

async function ensureAmap() {
  if (!hasAmapKey()) {
    throw new Error('AMAP_KEY_MISSING')
  }
  if (!AMapRef) {
    AMapRef = await loadAmap()
  }
  return AMapRef
}

function queryDistrict(keyword, level) {
  const cacheKey = `${level}:${keyword}`
  if (districtCache.has(cacheKey)) {
    return Promise.resolve(districtCache.get(cacheKey))
  }
  return new Promise((resolve) => {
    const search = new AMapRef.DistrictSearch({
      level,
      subdistrict: 1,
      extensions: 'base'
    })
    search.search(keyword, (status, result) => {
      const districts = status === 'complete' ? sortAdministrativeAreas(result?.districtList?.[0]?.districtList || []) : []
      districtCache.set(cacheKey, districts)
      resolve(districts)
    })
  })
}

async function loadProvinces() {
  try {
    await ensureAmap()
    loadingDistrict.value = true
    provinces.value = sortAdministrativeAreas(await queryDistrict('中国', 'country'))
  } catch {
    provinces.value = []
  } finally {
    loadingDistrict.value = false
  }
}

async function selectProvince(province) {
  mode.value = 'manual'
  selectedProvince.value = province.name
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = sortAdministrativeAreas(await queryDistrict(province.adcode, 'province'))
  districts.value = []
  current.province = province.name
  current.city = ''
  current.district = ''
  current.longitude = null
  current.latitude = null
  current.source = 'manual'
  updateFullAddress()
}

async function selectCity(city) {
  mode.value = 'manual'
  selectedCity.value = city.name
  selectedDistrict.value = ''
  districts.value = sortAdministrativeAreas(await queryDistrict(city.adcode, 'city'))
  current.city = city.name
  current.district = ''
  current.longitude = null
  current.latitude = null
  current.source = 'manual'
  updateFullAddress()
}

function selectDistrict(district) {
  mode.value = 'manual'
  selectedDistrict.value = district.name
  current.district = district.name
  current.longitude = null
  current.latitude = null
  current.source = 'manual'
  updateFullAddress()
}

async function locateCurrent() {
  mode.value = 'auto'
  try {
    await ensureAmap()
    locating.value = true
    const geolocation = new AMapRef.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      showButton: false
    })
    geolocation.getCurrentPosition(async (status, result) => {
      locating.value = false
      if (status !== 'complete') {
        ElMessage.warning(current.address ? '自动定位失败，已保留当前收货位置' : '自动定位失败，请手动选择位置')
        return
      }
      
      const autoLocation = await resolveAutoLocationFromAmap(AMapRef, result)
      
      // Update basic fields
      Object.assign(current, autoLocation)
      
      // Set selection state for wheels
      selectedProvince.value = current.province
      selectedCity.value = current.city
      selectedDistrict.value = current.district
      
      // Load wheels data for the UI
      const provinceObj = provinces.value.find(p => p.name === current.province)
      if (provinceObj) {
        cities.value = sortAdministrativeAreas(await queryDistrict(provinceObj.adcode, 'province'))
        const cityObj = cities.value.find(c => c.name === current.city)
        if (cityObj) {
          districts.value = sortAdministrativeAreas(await queryDistrict(cityObj.adcode, 'city'))
        }
      }
      
      updateFullAddress()
      emitSelection(current, 'auto')
      ElMessage.success('已获取当前位置')
    })
  } catch {
    locating.value = false
    ElMessage.warning(current.address ? '未配置或无法加载高德地图，已保留当前收货位置' : '未配置或无法加载高德地图，请手动选择位置')
  }
}

async function geocode() {
  if (!current.address) return
  await ensureAmap()
  return new Promise((resolve) => {
    const geocoder = new AMapRef.Geocoder({ city: '全国' })
    geocoder.getLocation(current.address, (status, result) => {
      if (status === 'complete' && result.geocodes.length > 0) {
        const location = result.geocodes[0].location
        current.longitude = location.lng
        current.latitude = location.lat
        resolve(true)
      } else {
        resolve(false)
      }
    })
  })
}

async function confirmLocation() {
  mode.value = 'manual'
  if (!current.province || !current.city || !current.district) {
    ElMessage.warning('请完整选择省市区')
    return
  }
  if (!current.street) {
    ElMessage.warning('请输入详细街道位置')
    return
  }
  
  const success = await geocode()
  if (!success) {
    ElMessage.warning('无法解析该地址的坐标，请检查地址是否正确')
    return
  }
  
  emitSelection(current, 'manual', true)
}

async function saveAddress() {
  const contactName = saveForm.contactName.trim()
  const phone = saveForm.phone.trim()
  if (!current.province || !current.city || !current.district || !current.street) {
    ElMessage.warning('请完善地址信息')
    return
  }
  if (!contactName) {
    ElMessage.warning('请填写联系人')
    return
  }
  if (!phone) {
    ElMessage.warning('请填写联系电话')
    return
  }
  
  const success = await geocode()
  if (!success) {
    ElMessage.warning('无法解析该地址的坐标，请检查地址是否正确')
    return
  }
  if (!hasCoordinate(current.longitude, current.latitude)) {
    ElMessage.warning('请确认收货地址地图位置')
    return
  }

  await createAddress({
    contactName,
    phone,
    address: current.address,
    longitude: current.longitude,
    latitude: current.latitude,
    isDefault: saveForm.isDefault
  })
  ElMessage.success('收货地址已保存')
}

onMounted(async () => {
  if (props.modelValue) {
    syncCurrent(props.modelValue)
  }
  await loadProvinces()
})
</script>

<template>
  <div class="location-selector">
    <el-segmented
      v-model="mode"
      :options="[
        { label: '自动定位', value: 'auto' },
        { label: '手动选择', value: 'manual' }
      ]"
      class="mode-switch"
    />

    <div v-if="mode === 'auto'" class="mode-panel auto-panel">
      <div class="location-actions top-actions">
        <el-button type="primary" plain :loading="locating" @click="locateCurrent">
          自动定位
        </el-button>
        <el-button
          v-if="selectedLocation?.source === 'auto'"
          type="primary"
          @click="confirmSelectedLocation"
        >
          确认使用自动定位
        </el-button>
        <span class="tip-text">快速获取当前位置并汇成最终地址</span>
      </div>
    </div>

    <div v-else class="mode-panel manual-panel">
      <div class="district-picker-label">手动选择省市区：</div>
      <div class="district-picker" v-loading="loadingDistrict">
        <div class="wheel">
          <button
            v-for="province in provinces"
            :key="province.adcode"
            type="button"
            :class="{ active: selectedProvince === province.name }"
            @click="selectProvince(province)"
          >
            {{ province.name }}
          </button>
        </div>
        <div class="wheel">
          <button
            v-for="city in cities"
            :key="city.adcode"
            type="button"
            :class="{ active: selectedCity === city.name }"
            @click="selectCity(city)"
          >
            {{ city.name }}
          </button>
        </div>
        <div class="wheel">
          <button
            v-for="district in districts"
            :key="district.adcode"
            type="button"
            :class="{ active: selectedDistrict === district.name }"
            @click="selectDistrict(district)"
          >
            {{ district.name }}
          </button>
        </div>
      </div>

      <div class="street-input-box">
        <div class="label">详细街道位置：</div>
        <el-input
          v-model="current.street"
          type="textarea"
          :rows="2"
          placeholder="请输入详细的街道、门牌号等信息"
          @input="markManualDraft"
        />
      </div>

      <div class="location-actions">
        <el-button type="primary" @click="confirmLocation" class="confirm-btn">确定并使用此位置</el-button>
      </div>
    </div>

    <div v-if="selectedLocation?.address" class="final-location-preview">
      <strong>最终地址</strong>
      <dl class="location-fields">
        <div>
          <dt>省</dt>
          <dd>{{ selectedLocation.province || '-' }}</dd>
        </div>
        <div>
          <dt>市</dt>
          <dd>{{ selectedLocation.city || '-' }}</dd>
        </div>
        <div>
          <dt>区</dt>
          <dd>{{ selectedLocation.district || '-' }}</dd>
        </div>
        <div>
          <dt>街道</dt>
          <dd>{{ selectedLocation.street || '-' }}</dd>
        </div>
      </dl>
      <span>{{ selectedLocation.address }}</span>
      <small v-if="hasCoordinate(selectedLocation.longitude, selectedLocation.latitude)">
        {{ selectedLocation.source === 'auto' ? '自动定位' : '手动选择' }} ·
        {{ Number(selectedLocation.longitude).toFixed(6) }}, {{ Number(selectedLocation.latitude).toFixed(6) }}
      </small>
    </div>

    <div v-if="saveEnabled" class="save-address-box">
      <el-divider>保存收货地址</el-divider>
      <div class="save-form">
        <el-input v-model="saveForm.contactName" placeholder="联系人姓名" />
        <el-input v-model="saveForm.phone" placeholder="联系电话" />
        <el-checkbox v-model="saveForm.isDefault">设为默认地址</el-checkbox>
        <el-button type="success" @click="saveAddress">保存地址</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.location-selector {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fff;
  padding: 4px;
}

.mode-switch {
  align-self: flex-start;
}

.mode-panel {
  display: grid;
  gap: 16px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.tip-text {
  font-size: 12px;
  color: #909399;
}

.district-picker-label, .street-input-box .label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.district-picker {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  height: 200px;
  overflow: hidden;
}

.wheel {
  border-right: 1px solid #f2f6fc;
  overflow-y: auto;
  background: #fcfcfc;
}

.wheel:last-child {
  border-right: 0;
}

.wheel button {
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f2f6fc;
  color: #606266;
  cursor: pointer;
  display: block;
  font-size: 14px;
  min-height: 40px;
  padding: 10px;
  text-align: center;
  width: 100%;
  transition: all 0.2s;
}

.wheel button.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: bold;
}

.wheel button:hover:not(.active) {
  background: #f5f7fa;
}

.street-input-box {
  margin-top: 8px;
}

.location-actions {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.confirm-btn {
  width: 100%;
  height: 40px;
}

.save-address-box {
  margin-top: 16px;
}

.final-location-preview {
  background: #f0f9eb;
  border: 1px solid #d1edc4;
  border-radius: 8px;
  color: #529b2e;
  display: grid;
  font-size: 13px;
  gap: 4px;
  line-height: 1.4;
  padding: 10px 12px;
}

.final-location-preview small {
  color: #67c23a;
}

.location-fields {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
}

.location-fields div {
  display: grid;
  gap: 2px;
}

.location-fields dt {
  color: #67c23a;
  font-size: 12px;
}

.location-fields dd {
  color: #3f7f24;
  font-weight: 600;
  margin: 0;
  min-width: 0;
  overflow-wrap: anywhere;
}

.save-form {
  display: grid;
  gap: 12px;
}

@media (max-width: 640px) {
  .district-picker {
    height: 180px;
  }

  .location-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
