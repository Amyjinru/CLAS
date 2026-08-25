<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyForRider, getMyRoleApplications, sessionUser } from '../api/clas'

const loading = ref(false)
const router = useRouter()
const submitting = ref(false)
const applications = ref([])
const form = reactive({ reason: '' })
const statusText = { PENDING: '审核中', APPROVED: '已通过', REJECTED: '未通过' }
const statusType = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
const businessRole = computed(() => (sessionUser.value?.roles || []).find((role) => role === 'RIDER' || role === 'MERCHANT'))
const businessRoleLabel = computed(() => businessRole.value === 'MERCHANT' ? '商家' : '骑手')

async function load() {
  loading.value = true
  try {
    applications.value = await getMyRoleApplications()
  } catch {
    ElMessage.error('身份申请记录加载失败')
  } finally {
    loading.value = false
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

onMounted(load)
</script>

<template>
  <div class="role-application-page" v-loading="loading">
    <section class="hero">
      <span class="eyebrow">ROLE APPLICATION</span>
      <h1>申请平台身份</h1>
      <p>普通用户可提交申请；审核通过后，重新登录即可使用对应工作台。</p>
    </section>

    <el-card v-if="!businessRole" shadow="never" class="application-card">
      <template #header><h2>申请成为商家</h2></template>
      <p class="merchant-copy">商家申请需要经营信息、联系方式和结算资料。提交后同样由管理员审核，通过后会增加商家端入口，普通用户端仍可保留。</p>
      <el-button type="primary" plain @click="router.push('/merchant-register')">填写商家入驻申请</el-button>
    </el-card>

    <el-alert
      v-else
      class="application-card"
      :title="`您已拥有${businessRoleLabel}身份`"
      :description="`您当前的${businessRoleLabel}身份已满足业务服务条件。如需使用相关功能，可通过顶部端口切换器进入${businessRoleLabel}端。`"
      type="success"
      show-icon
      :closable="false"
    />

    <el-card v-if="!businessRole" shadow="never" class="application-card">
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
      <el-empty v-if="!applications.length" description="暂无身份申请记录" />
      <el-timeline v-else>
        <el-timeline-item v-for="item in applications" :key="item.id" :timestamp="item.createdAt" :type="statusType[item.status]">
          <strong>{{ item.targetRole === 'RIDER' ? '骑手身份申请' : item.targetRole }}</strong>
          <el-tag size="small" :type="statusType[item.status]" class="status-tag">{{ statusText[item.status] || item.status }}</el-tag>
          <p>{{ item.reason }}</p>
          <p v-if="item.adminRemarks" class="remarks">审核备注：{{ item.adminRemarks }}</p>
          <el-alert v-if="item.status === 'APPROVED'" title="请退出并重新登录，以刷新骑手身份。" type="success" :closable="false" />
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
</style>
