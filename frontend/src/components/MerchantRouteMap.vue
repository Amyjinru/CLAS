<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ensureAmapPlugins, hasAmapKey } from '../utils/amap'

const props = defineProps({
  merchant: {
    type: Object,
    required: true
  },
  userLocation: {
    type: Object,
    default: null
  },
  estimate: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['locate', 'select'])

const mapEl = ref(null)
const errorText = ref('')

let AMapRef = null
let map = null
let driving = null
let markers = []

function numberValue(value) {
  const next = Number(value)
  return Number.isFinite(next) ? next : null
}

function distanceText(distance) {
  if (!distance) return '距离未知'
  return distance < 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`
}

function clearMap() {
  if (map && markers.length) {
    map.remove(markers)
  }
  markers = []
  if (driving) {
    driving.clear()
  }
}

async function initMap() {
  if (!hasAmapKey()) {
    errorText.value = '未配置高德地图 Key'
    return
  }
  AMapRef = await ensureAmapPlugins(['AMap.ToolBar', 'AMap.Scale', 'AMap.Driving'])
  await nextTick()
  if (!map) {
    const lng = numberValue(props.merchant.longitude) || 116.397428
    const lat = numberValue(props.merchant.latitude) || 39.90923
    map = new AMapRef.Map(mapEl.value, {
      zoom: 14,
      center: [lng, lat]
    })
    map.addControl(new AMapRef.ToolBar())
    map.addControl(new AMapRef.Scale())
    driving = new AMapRef.Driving({
      map,
      hideMarkers: true
    })
  }
  renderRoute()
}

function renderRoute() {
  if (!map || !AMapRef || !props.merchant) return
  clearMap()
  const merchantLng = numberValue(props.merchant.longitude)
  const merchantLat = numberValue(props.merchant.latitude)
  if (merchantLng === null || merchantLat === null) {
    errorText.value = '商家缺少地图坐标'
    return
  }
  const merchantPosition = [merchantLng, merchantLat]
  const merchantMarker = new AMapRef.Marker({
    position: merchantPosition,
    title: props.merchant.merchantName,
    label: { content: '商家', direction: 'top' }
  })
  markers.push(merchantMarker)

  const userLng = numberValue(props.userLocation?.longitude)
  const userLat = numberValue(props.userLocation?.latitude)
  if (userLng === null || userLat === null) {
    map.add(markers)
    map.setFitView(markers)
    return
  }

  const userPosition = [userLng, userLat]
  const userMarker = new AMapRef.Marker({
    position: userPosition,
    title: props.userLocation.address || '当前位置',
    label: { content: '你', direction: 'top' }
  })
  markers.push(userMarker)
  map.add(markers)
  driving.search(userPosition, merchantPosition, (status) => {
    if (status !== 'complete') {
      map.setFitView(markers)
    }
  })
}

function openNavigation() {
  const userLng = numberValue(props.userLocation?.longitude)
  const userLat = numberValue(props.userLocation?.latitude)
  const merchantLng = numberValue(props.merchant.longitude)
  const merchantLat = numberValue(props.merchant.latitude)
  if ([userLng, userLat, merchantLng, merchantLat].some((item) => item === null)) {
    ElMessage.warning('请先选择当前位置')
    return
  }
  const url = `https://uri.amap.com/navigation?from=${userLng},${userLat},我的位置&to=${merchantLng},${merchantLat},${encodeURIComponent(props.merchant.merchantName)}&mode=car&policy=1&src=clas&coordinate=gaode&callnative=0`
  window.open(url, '_blank')
}

watch(
  () => [props.merchant?.id, props.userLocation?.longitude, props.userLocation?.latitude],
  () => {
    if (map) renderRoute()
  }
)

watch(
  () => props.merchant,
  () => initMap(),
  { immediate: true }
)

onBeforeUnmount(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<template>
  <section class="route-panel">
    <div class="route-head">
      <div>
        <h2>配送导航</h2>
        <p v-if="userLocation?.address">{{ userLocation.address }}</p>
        <p v-else>请选择当前位置查看配送路线</p>
      </div>
      <div class="route-actions">
        <el-button @click="emit('locate')">重新定位</el-button>
        <el-button @click="emit('select')">选择位置</el-button>
        <el-button type="primary" @click="openNavigation">打开高德导航</el-button>
      </div>
    </div>

    <div class="route-stats">
      <span>{{ distanceText(estimate?.routeDistanceMeters || estimate?.distanceMeters) }}</span>
      <span v-if="estimate?.estimatedMinutes">约 {{ estimate.estimatedMinutes }} 分钟</span>
      <span v-if="estimate?.deliveryRadiusM">{{ distanceText(estimate.deliveryRadiusM) }} 内配送</span>
      <span v-if="estimate?.deliveryAvailable === false" class="danger">超出配送范围</span>
      <span v-else-if="estimate?.deliveryAvailable === true" class="success">可配送</span>
    </div>

    <div v-if="errorText" class="route-warning">{{ errorText }}</div>
    <div ref="mapEl" class="route-map"></div>
  </section>
</template>

<style scoped>
.route-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  display: grid;
  gap: 12px;
  padding: 16px;
}

.route-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.route-head h2 {
  font-size: 18px;
  margin: 0 0 6px;
}

.route-head p {
  color: var(--text-secondary);
  margin: 0;
}

.route-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.route-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.route-stats span {
  background: #f8fafc;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 13px;
  padding: 6px 10px;
}

.route-stats .danger {
  background: #fef2f2;
  color: #dc2626;
}

.route-stats .success {
  background: #ecfdf5;
  color: #047857;
}

.route-warning {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: var(--radius-sm);
  color: #c2410c;
  font-size: 13px;
  padding: 8px 10px;
}

.route-map {
  border-radius: var(--radius-sm);
  height: 320px;
  overflow: hidden;
}

@media (max-width: 720px) {
  .route-head {
    display: grid;
  }

  .route-actions {
    justify-content: flex-start;
  }
}
</style>
