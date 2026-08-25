<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyForRider, getMyRoleApplications, sessionUser, setSessionUser, switchRole } from '../api/clas'

const loading = ref(false)
const router = useRouter()
const submitting = ref(false)
const applications = ref([])
const form = reactive({ reason: '' })
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

async function submit() {
  if (!form.reason.trim()) {
    ElMessage.warning('请说明申请骑手身份的原因或资质情况')
    return
  }
  submitting.value = true
  try {
    await applyForRider({ reason: form.reason.trim() })
    form.reason = ''
    ElMessage.success('骑手身份申请已提交，请等待管理员审核')
    await load()
  } catch (error) {
    ElMessage.error(error?.message || '提交申请失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
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
          <el-input v-model="form.reason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="例如：配送经验、可服务区域与可工作时段" />
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
.merchant-copy { margin: 0 0 18px; color: #606266; line-height: 1.7; }
.approved-action { display: grid; gap: 10px; margin-top: 10px; justify-items: start; }
</style>
