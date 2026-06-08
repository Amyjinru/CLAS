<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { hasAmapKey, loadAmap } from '../utils/amap'

const props = defineProps({
  address: {
    type: String,
    default: ''
  },
  longitude: {
    type: [Number, String],
    default: null
  },
  latitude: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:address', 'update:longitude', 'update:latitude', 'change'])

const mapEl = ref(null)
const keyword = ref(props.address || '')
const tips = ref([])
const loading = ref(false)
const ready = ref(false)
const errorText = ref('')

let AMapRef = null
let map = null
let marker = null
let placeSearch = null
let geocoder = null

function numberValue(value) {
  if (value === null || value === undefined || value === '') return null
  const next = Number(value)
  return Number.isFinite(next) ? next : null
}

function emitLocation(address, lng, lat) {
  emit('update:address', address)
  emit('update:longitude', lng)
  emit('update:latitude', lat)
  emit('change', { address, longitude: lng, latitude: lat })
}

function setMarker(lng, lat, shouldCenter = true) {
  if (!map || !AMapRef) return
  const position = [lng, lat]
  if (!marker) {
    marker = new AMapRef.Marker({
      position,
      draggable: true,
      cursor: 'move'
    })
    marker.on('dragend', (event) => {
      const position = event.lnglat
      reverseGeocode(position.lng, position.lat)
    })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  if (shouldCenter) {
    map.setCenter(position)
  }
}

function reverseGeocode(lng, lat) {
  if (!geocoder) {
    emitLocation(keyword.value || props.address, lng, lat)
    return
  }
  geocoder.getAddress([lng, lat], (status, result) => {
    const address = status === 'complete'
      ? result?.regeocode?.formattedAddress || keyword.value || props.address
      : keyword.value || props.address
    keyword.value = address
    emitLocation(address, lng, lat)
    setMarker(lng, lat)
  })
}

async function initMap() {
  if (!hasAmapKey()) {
    errorText.value = '未配置高德地图 Key，请设置 VITE_AMAP_KEY'
    return
  }
  try {
    AMapRef = await loadAmap()
    await nextTick()
    map = new AMapRef.Map(mapEl.value, {
      zoom: 15,
      center: [numberValue(props.longitude) || 116.397428, numberValue(props.latitude) || 39.90923]
    })
    map.addControl(new AMapRef.ToolBar())
    map.addControl(new AMapRef.Scale())
    placeSearch = new AMapRef.PlaceSearch({ city: '全国', pageSize: 8 })
    geocoder = new AMapRef.Geocoder({ city: '全国' })
    map.on('click', (event) => reverseGeocode(event.lnglat.lng, event.lnglat.lat))
    const lng = numberValue(props.longitude)
    const lat = numberValue(props.latitude)
    if (lng !== null && lat !== null) {
      setMarker(lng, lat)
    }
    ready.value = true
  } catch {
    errorText.value = '高德地图加载失败，请检查 Key 或网络'
  }
}

function searchPlaces() {
  if (!placeSearch || !keyword.value.trim()) {
    tips.value = []
    return
  }
  loading.value = true
  placeSearch.search(keyword.value.trim(), (status, result) => {
    loading.value = false
    if (status !== 'complete') {
      tips.value = []
      return
    }
    tips.value = (result?.poiList?.pois || [])
      .filter((item) => item.location)
      .map((item) => ({
        id: item.id,
        name: item.name,
        address: item.address || item.pname + item.cityname + item.adname,
        longitude: item.location.lng,
        latitude: item.location.lat
      }))
  })
}

function pickTip(tip) {
  const address = `${tip.name} ${tip.address}`.trim()
  keyword.value = address
  tips.value = []
  setMarker(tip.longitude, tip.latitude)
  emitLocation(address, tip.longitude, tip.latitude)
  ElMessage.success('位置已选择')
}

function centerAddress(address) {
  if (!geocoder || !address) return
  keyword.value = address
  geocoder.getLocation(address, (status, result) => {
    const location = result?.geocodes?.[0]?.location
    if (status === 'complete' && location) {
      setMarker(location.lng, location.lat)
      emitLocation(address, location.lng, location.lat)
    }
  })
}

defineExpose({
  centerAddress
})

watch(
  () => props.address,
  (value) => {
    if (value !== keyword.value) {
      keyword.value = value || ''
    }
  }
)

onMounted(initMap)

onBeforeUnmount(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<template>
  <div class="amap-picker">
    <div v-if="errorText" class="amap-warning">{{ errorText }}</div>
    <div class="picker-search">
      <el-input
        v-model="keyword"
        placeholder="搜索地址或地点"
        clearable
        @keyup.enter="searchPlaces"
      />
      <el-button :loading="loading" @click="searchPlaces">搜索</el-button>
    </div>
    <div v-if="tips.length" class="tip-list">
      <button v-for="tip in tips" :key="tip.id || `${tip.longitude}-${tip.latitude}`" type="button" @click="pickTip(tip)">
        <strong>{{ tip.name }}</strong>
        <span>{{ tip.address }}</span>
      </button>
    </div>
    <div ref="mapEl" class="map-box" :class="{ muted: !ready }"></div>
    <p class="coord-text" v-if="longitude && latitude">
      坐标：{{ Number(longitude).toFixed(6) }}, {{ Number(latitude).toFixed(6) }}
    </p>
  </div>
</template>

<style scoped>
.amap-picker {
  display: grid;
  gap: 10px;
  width: 100%;
}

.picker-search {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto;
}

.tip-list {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  display: grid;
  max-height: 180px;
  overflow: auto;
}

.tip-list button {
  background: #fff;
  border: 0;
  border-bottom: 1px solid var(--border-light);
  color: var(--text-primary);
  cursor: pointer;
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  text-align: left;
}

.tip-list button:last-child {
  border-bottom: 0;
}

.tip-list span {
  color: var(--text-secondary);
  font-size: 12px;
}

.map-box {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  height: 280px;
  overflow: hidden;
  width: 100%;
}

.map-box.muted {
  background: #f8fafc;
}

.coord-text,
.amap-warning {
  color: var(--text-secondary);
  font-size: 12px;
  margin: 0;
}

.amap-warning {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: var(--radius-sm);
  color: #c2410c;
  padding: 8px 10px;
}
</style>
