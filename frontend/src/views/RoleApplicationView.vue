<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyRoleApplications, sessionUser, setSessionUser, submitRiderApplication, switchRole } from '../api/clas'
import { ensureAmapPlugins, loadAmap } from '../utils/amap'
import { resolveAutoLocationFromAmap } from '../utils/locationFormat'

const loading = ref(false)
const router = useRouter()
const submitting = ref(false)
const locatingServiceArea = ref(false)
const areaOptionsLoading = ref(false)
const provinceOptions = ref([])
const cityOptions = ref([])
const districtOptions = ref([])
const selectedArea = reactive({ province: '', city: '', district: '' })
const applications = ref([])
const form = reactive({ realName: '', idCardNo: '', vehicleType: '', serviceArea: '', emergencyContactName: '', emergencyContactPhone: '', credentialUrls: '' })
const statusText = { PENDING: '审核中', APPROVED: '已通过', REJECTED: '未通过' }
const statusType = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
const businessRole = computed(() => (sessionUser.value?.roles || []).find((role) => role === 'RIDER' || role === 'MERCHANT'))
const businessRoleLabel = computed(() => businessRole.value === 'MERCHANT' ? '商家' : '骑手')
const pendingBusinessRole = computed(() => {
  const riderApplications = Array.isArray(applications.value) ? applications.value : []
  return riderApplications.find((item) => item.status === 'PENDING' && ['RIDER', 'MERCHANT'].includes(item.targetRole))?.targetRole || null
})
const pendingBusinessRoleLabel = computed(() => pendingBusinessRole.value === 'MERCHANT' ? '商家' : '骑手')
const applicationRecords = computed(() => [...(Array.isArray(applications.value) ? applications.value : [])]
  .sort((left, right) => new Date(right.createdAt || 0) - new Date(left.createdAt || 0)))
// 历史申请即使曾获批，也不等同于当前仍持有该身份；注销后应可再次申请。
const resolvedBusinessRole = computed(() => businessRole.value)
const resolvedBusinessRoleLabel = computed(() => resolvedBusinessRole.value === 'MERCHANT' ? '商家' : '骑手')
const canStartBusinessApplication = computed(() => !resolvedBusinessRole.value && !pendingBusinessRole.value)
const switchingRole = ref('')
const hasLoadedInitialRecords = ref(false)
const knownApplicationStatuses = new Map()
let refreshTimer = null

function rememberApplicationStatuses(records) {
  knownApplicationStatuses.clear()
  records.forEach((item) => knownApplicationStatuses.set(item.id, item.status))
}

async function showApprovalNotice(item) {
  const roleLabel = item.targetRole === 'MERCHANT' ? '商家' : '骑手'
  try {
    await ElMessageBox.confirm(
      `您的${roleLabel}身份申请已通过审核。现在可进入${roleLabel}端，也可以继续留在当前页面。`,
      '身份审核已通过',
      {
        confirmButtonText: `进入${roleLabel}端`,
        cancelButtonText: '暂留当前页面',
        type: 'success',
        closeOnClickModal: false
      }
    )
    await enterBusinessPortal(item.targetRole)
  } catch {
    // 用户选择暂不切换时，保留在当前申请页面即可。
  }
}

async function load({ showLoading = true, notifyOnApproval = false } = {}) {
  if (showLoading) loading.value = true
  try {
    const records = await getMyRoleApplications({ silent: !showLoading })
    if (!Array.isArray(records)) {
      throw new Error('身份申请记录格式异常')
    }
    const newlyApproved = notifyOnApproval
      ? records.find((item) => item.status === 'APPROVED' && knownApplicationStatuses.get(item.id) === 'PENDING')
      : null
    applications.value = records
    rememberApplicationStatuses(records)
    if (newlyApproved) {
      await showApprovalNotice(newlyApproved)
    }
  } catch (error) {
    applications.value = []
    if (showLoading) ElMessage.error(error?.message || '身份申请记录加载失败')
  } finally {
    if (showLoading) loading.value = false
  }
}

async function enterBusinessPortal(role) {
  switchingRole.value = role
  try {
    const data = await switchRole(role)
    setSessionUser({ ...data.user, roles: data.roles || data.user.roles || [role], token: data.token })
    ElMessage.success(`已进入${role === 'MERCHANT' ? '商家' : '骑手'}端`)
    await router.replace(role === 'MERCHANT' ? '/merchant-console' : '/rider')
  } catch (error) {
    ElMessage.error(error?.message || '端口切换失败，请重新登录后重试')
  } finally {
    switchingRole.value = ''
  }
}

function serviceAreaText(location) {
  return [location?.province, location?.city, location?.district].filter(Boolean).join('')
}

function sortAreas(areas) {
  return [...(areas || [])].sort((left, right) => (left.name || '').localeCompare(right.name || '', 'zh-Hans-CN-u-co-pinyin'))
}

async function searchDistrict(keyword, level) {
  const AMap = await ensureAmapPlugins(['AMap.DistrictSearch'])
  return new Promise((resolve) => {
    const search = new AMap.DistrictSearch({ level, subdistrict: 1, extensions: 'base' })
    search.search(keyword, (status, result) => {
      resolve(status === 'complete' ? sortAreas(result?.districtList?.[0]?.districtList) : [])
    })
  })
}

async function openManualAreaPicker() {
  if (provinceOptions.value.length) return
  areaOptionsLoading.value = true
  try {
    provinceOptions.value = await searchDistrict('中国', 'country')
  } catch {
    ElMessage.warning('行政区数据加载失败，请检查高德地图配置后重试')
  } finally {
    areaOptionsLoading.value = false
  }
}

async function selectProvince(value) {
  selectedArea.province = value
  selectedArea.city = ''
  selectedArea.district = ''
  cityOptions.value = []
  districtOptions.value = []
  form.serviceArea = ''
  const province = provinceOptions.value.find((item) => item.name === value)
  if (!province) return
  areaOptionsLoading.value = true
  try {
    cityOptions.value = await searchDistrict(province.adcode, 'province')
  } finally {
    areaOptionsLoading.value = false
  }
}

async function selectCity(value) {
  selectedArea.city = value
  selectedArea.district = ''
  districtOptions.value = []
  form.serviceArea = ''
  const city = cityOptions.value.find((item) => item.name === value)
  if (!city) return
  areaOptionsLoading.value = true
  try {
    districtOptions.value = await searchDistrict(city.adcode, 'city')
  } finally {
    areaOptionsLoading.value = false
  }
}

function selectDistrict(value) {
  selectedArea.district = value
  form.serviceArea = serviceAreaText(selectedArea)
}

async function locateServiceArea() {
  locatingServiceArea.value = true
  try {
    const AMap = await loadAmap()
    const result = await new Promise((resolve, reject) => {
      const geolocation = new AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 8000,
        showButton: false
      })
      geolocation.getCurrentPosition((status, locationResult) => {
        if (status === 'complete') resolve(locationResult)
        else reject(new Error('LOCATION_FAILED'))
      })
    })
    const location = await resolveAutoLocationFromAmap(AMap, result)
    const area = serviceAreaText(location)
    if (!location?.province || !location?.city || !location?.district) {
      throw new Error('AREA_INCOMPLETE')
    }
    form.serviceArea = area
    Object.assign(selectedArea, { province: location.province, city: location.city, district: location.district })
    ElMessage.success(`已获取服务区域：${area}`)
  } catch (error) {
    const message = error?.message === 'AMAP_KEY_MISSING'
      ? '未配置高德地图，暂时无法获取服务区域'
      : '定位失败，请允许浏览器定位权限后重试'
    ElMessage.warning(message)
    await openManualAreaPicker()
  } finally {
    locatingServiceArea.value = false
  }
}

async function submit() {
  if (!form.realName || !form.idCardNo || !form.vehicleType || !form.serviceArea || !form.emergencyContactName || !form.emergencyContactPhone) {
    ElMessage.warning('请完整填写骑手实名认证资料')
    return
  }
  submitting.value = true
  try {
    await submitRiderApplication({ ...form })
    Object.keys(form).forEach((key) => { form[key] = '' })
    ElMessage.success('实名骑手申请已提交，请等待管理员审核')
    await load()
  } catch (error) {
    ElMessage.error(error?.message || '提交申请失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  void openManualAreaPicker()
  await load()
  hasLoadedInitialRecords.value = true
  refreshTimer = window.setInterval(() => {
    if (hasLoadedInitialRecords.value) load({ showLoading: false, notifyOnApproval: true })
  }, 15000)
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<template>
  <div class="role-application-page" v-loading="loading">
    <section class="hero">
      <span class="eyebrow">ROLE APPLICATION</span>
      <h1>申请平台身份</h1>
      <p>普通用户可提交申请；审核结果会同步展示在这里，对应端口可随时通过顶部切换器进入。</p>
    </section>

    <el-alert
      v-if="!resolvedBusinessRole"
      class="application-card"
      title="商家与骑手身份为单一业务身份"
      description="您可以根据计划开展的业务选择其中一项申请。任一申请提交并进入审核后，另一项申请会暂时不可提交；审核完成后可继续查看身份状态。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-alert
      v-if="pendingBusinessRole"
      class="application-card"
      :title="`您的${pendingBusinessRoleLabel}身份申请正在审核`"
      :description="`为便于审核与后续服务配置，本次审核完成前暂不开放另一业务身份的申请。`"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-card v-if="canStartBusinessApplication" shadow="never" class="application-card">
      <template #header><h2>申请成为商家</h2></template>
      <p class="merchant-copy">商家申请需要经营信息、联系方式和结算资料。提交后同样由管理员审核，通过后会增加商家端入口，普通用户端仍可保留。</p>
      <el-button type="primary" plain @click="router.push('/merchant-register')">填写商家入驻申请</el-button>
    </el-card>

    <el-alert
      v-else-if="resolvedBusinessRole"
      class="application-card"
      :title="businessRole ? `您已拥有${businessRoleLabel}身份` : `${resolvedBusinessRoleLabel}身份申请已通过`"
      :description="businessRole
        ? `您当前的${businessRoleLabel}身份已满足业务服务条件。如需使用相关功能，可通过顶部端口切换器切换至对应工作台。`
        : `审核结果已生效。普通用户端仍可继续使用，需要时可通过顶部端口切换器切换至对应工作台。`"
      type="success"
      show-icon
      :closable="false"
    />

    <el-card v-if="canStartBusinessApplication" shadow="never" class="application-card">
      <template #header><h2>申请成为骑手</h2></template>
      <el-alert title="当前为最小骑手申请流程" type="info" :closable="false" show-icon>
        请填写基础说明。车辆、健康证及实名认证等材料将在后续版本接入。
      </el-alert>
      <el-form class="application-form" label-position="top">
        <el-form-item label="申请说明">
          <el-input v-model="form.realName" placeholder="真实姓名" />
          <el-input v-model="form.idCardNo" placeholder="18 位身份证号" class="field" />
          <el-input v-model="form.vehicleType" placeholder="交通工具，如电动车" class="field" />
          <el-input :model-value="form.serviceArea" readonly placeholder="请点击右侧按钮获取省、市、区服务区域" class="field">
            <template #append>
              <el-button :loading="locatingServiceArea" @click="locateServiceArea">获取定位</el-button>
            </template>
          </el-input>
          <p class="area-tip">服务区域仅保存省、市、区（县）。也可直接在下方选择，无需等待定位失败。</p>
          <div v-loading="areaOptionsLoading" class="area-picker">
            <el-select :model-value="selectedArea.province" placeholder="请选择省" filterable @update:model-value="selectProvince">
              <el-option v-for="area in provinceOptions" :key="area.adcode" :label="area.name" :value="area.name" />
            </el-select>
            <el-select :model-value="selectedArea.city" placeholder="请选择市" filterable :disabled="!selectedArea.province" @update:model-value="selectCity">
              <el-option v-for="area in cityOptions" :key="area.adcode" :label="area.name" :value="area.name" />
            </el-select>
            <el-select :model-value="selectedArea.district" placeholder="请选择区（县）" filterable :disabled="!selectedArea.city" @update:model-value="selectDistrict">
              <el-option v-for="area in districtOptions" :key="area.adcode" :label="area.name" :value="area.name" />
            </el-select>
          </div>
          <el-input v-model="form.emergencyContactName" placeholder="紧急联系人姓名" class="field" />
          <el-input v-model="form.emergencyContactPhone" placeholder="紧急联系人手机号" class="field" />
          <el-input v-model="form.credentialUrls" type="textarea" :rows="2" placeholder="资质证明链接（可选，多个链接用逗号分隔）" class="field" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交骑手申请</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never" class="application-card">
      <template #header><h2>我的申请记录</h2></template>
      <el-empty v-if="!applicationRecords.length" description="暂无身份申请记录" />
      <el-timeline v-else>
        <el-timeline-item v-for="item in applicationRecords" :key="item.id" :timestamp="item.createdAt" :type="statusType[item.status]">
          <strong>{{ item.targetRole === 'RIDER' ? '骑手身份申请' : '商家入驻申请' }}</strong>
          <el-tag size="small" :type="statusType[item.status]" class="status-tag">{{ statusText[item.status] || item.status }}</el-tag>
          <p>{{ item.reason }}</p>
          <p v-if="item.adminRemarks" class="remarks">审核备注：{{ item.adminRemarks }}</p>
          <div v-if="item.status === 'APPROVED'" class="approved-action">
            <el-alert :title="`${item.targetRole === 'MERCHANT' ? '商家' : '骑手'}身份已通过审核。`" type="success" :closable="false" />
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<style scoped>
.role-application-page { width: min(920px, calc(100% - 32px)); margin: 28px auto 56px; }
.hero { padding: 30px; border-radius: 24px; color: #fff; background: linear-gradient(135deg, #173f35, #0d9488); }
.eyebrow { font-size: 12px; letter-spacing: .12em; opacity: .8; }.hero h1 { margin: 8px 0; }.hero p { margin: 0; opacity: .88; }
.application-card { margin-top: 18px; border-radius: 18px; }.application-card h2 { margin: 0; font-size: 18px; }
.application-form { margin-top: 20px; }.status-tag { margin-left: 10px; }.remarks { color: #606266; }
.area-tip { color: #909399; font-size: 12px; line-height: 1.5; margin: 8px 0 0; }
.area-picker { display: grid; gap: 10px; grid-template-columns: repeat(3, minmax(0, 1fr)); margin-top: 12px; }
@media (max-width: 640px) { .area-picker { grid-template-columns: 1fr; } }
.merchant-copy { margin: 0 0 18px; color: #606266; line-height: 1.7; }
.approved-action { display: grid; gap: 10px; margin-top: 10px; justify-items: start; }
</style>
