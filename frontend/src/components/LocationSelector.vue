<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createAddress } from '../api/clas'
import { hasAmapKey, loadAmap } from '../utils/amap'

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
  latitude: null
})
const saveForm = reactive({
  contactName: '',
  phone: '',
  isDefault: true
})

let AMapRef = null
const districtCache = new Map()

function syncCurrent(next) {
  if (!next) return
  Object.assign(current, next)
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
}

function updateFullAddress() {
  current.address = `${current.province}${current.city}${current.district}${current.street}`
  emit('update:modelValue', { ...current })
}

watch(() => current.street, updateFullAddress)

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
      const districts = status === 'complete' ? result?.districtList?.[0]?.districtList || [] : []
      districtCache.set(cacheKey, districts)
      resolve(districts)
    })
  })
}

async function loadProvinces() {
  try {
    await ensureAmap()
    loadingDistrict.value = true
    provinces.value = await queryDistrict('中国', 'country')
  } catch {
    provinces.value = []
  } finally {
    loadingDistrict.value = false
  }
}

async function selectProvince(province) {
  selectedProvince.value = province.name
  selectedCity.value = ''
  selectedDistrict.value = ''
  cities.value = await queryDistrict(province.adcode, 'province')
  districts.value = []
  current.province = province.name
  current.city = ''
  current.district = ''
  updateFullAddress()
}

async function selectCity(city) {
  selectedCity.value = city.name
  selectedDistrict.value = ''
  districts.value = await queryDistrict(city.adcode, 'city')
  current.city = city.name
  current.district = ''
  updateFullAddress()
}

function selectDistrict(district) {
  selectedDistrict.value = district.name
  current.district = district.name
  updateFullAddress()
}

async function locateCurrent() {
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
        ElMessage.warning('自动定位失败，请手动选择位置')
        return
      }
      
      const component = result.addressComponent || {}
      const provinceName = component.province || ''
      const cityName = Array.isArray(component.city) ? component.province || '' : component.city || component.province || ''
      const districtName = component.district || ''
      
      // Update basic fields
      current.province = provinceName
      current.city = cityName
      current.district = districtName
      current.longitude = result.position.lng
      current.latitude = result.position.lat
      
      // Set selection state for wheels
      selectedProvince.value = provinceName
      selectedCity.value = cityName
      selectedDistrict.value = districtName
      
      // Load wheels data for the UI
      const provinceObj = provinces.value.find(p => p.name === provinceName)
      if (provinceObj) {
        cities.value = await queryDistrict(provinceObj.adcode, 'province')
        const cityObj = cities.value.find(c => c.name === cityName)
        if (cityObj) {
          districts.value = await queryDistrict(cityObj.adcode, 'city')
        }
      }

      // Calculate street
      const fullAddress = result.formattedAddress || ''
      const prefix = `${provinceName}${cityName}${districtName}`
      if (fullAddress.startsWith(prefix)) {
        current.street = fullAddress.substring(prefix.length)
      } else {
        current.street = fullAddress
      }
      
      updateFullAddress()
      ElMessage.success('已获取当前位置')
    })
  } catch {
    locating.value = false
    ElMessage.warning('未配置或无法加载高德地图')
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
  
  emit('confirm', { ...current })
}

async function saveAddress() {
  if (!current.province || !current.city || !current.district || !current.street) {
    ElMessage.warning('请完善地址信息')
    return
  }
  if (!saveForm.contactName || !saveForm.phone) {
    ElMessage.warning('请填写联系人和电话')
    return
  }
  
  const success = await geocode()
  if (!success) {
    ElMessage.warning('无法解析该地址的坐标，请检查地址是否正确')
    return
  }

  await createAddress({
    contactName: saveForm.contactName,
    phone: saveForm.phone,
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
    <div class="location-actions top-actions">
      <el-button type="primary" plain :loading="locating" @click="locateCurrent">
        自动定位
      </el-button>
      <span class="tip-text">快速获取当前位置</span>
    </div>

    <div class="district-picker-label">或手动选择省市区：</div>
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
      />
    </div>

    <div class="location-actions">
      <el-button type="primary" @click="confirmLocation" class="confirm-btn">确定并使用此位置</el-button>
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

.save-form {
  display: grid;
  gap: 12px;
}

@media (max-width: 640px) {
  .district-picker {
    height: 180px;
  }
}
</style>
