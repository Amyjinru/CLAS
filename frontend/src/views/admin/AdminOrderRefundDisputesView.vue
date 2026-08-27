<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditOrderRefundDispute, listOrderRefundDisputes } from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const disputes = ref([])
const loading = ref(false)
const handlingId = ref(null)
const filter = ref('PENDING')

const visibleDisputes = computed(() => filter.value
  ? disputes.value.filter((item) => item.status === filter.value)
  : disputes.value)

const statusLabel = { PENDING: '待审核', APPROVED: '支持退款', REJECTED: '维持原处理' }

async function load() {
  loading.value = true
  try { disputes.value = await listOrderRefundDisputes() } catch { ElMessage.error('加载退款争议失败') } finally { loading.value = false }
}

async function audit(dispute, approved) {
  try {
    const { value } = await ElMessageBox.prompt(
      approved ? '请填写支持退款的裁定依据' : '请填写维持商家处理的裁定依据',
      approved ? '通过退款争议' : '驳回退款争议',
      {
        inputPlaceholder: '裁定依据将同步给用户、商家和骑手',
        inputValidator: (value) => value?.trim() ? true : '请填写裁定依据',
        confirmButtonText: approved ? '确认支持退款' : '确认维持原处理',
        cancelButtonText: '取消'
      }
    )
    handlingId.value = dispute.id
    await auditOrderRefundDispute(dispute.id, { approved, reason: value.trim() })
    ElMessage.success(approved ? '已裁定退款' : '已裁定维持原处理')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.message || '审核失败')
  } finally { handlingId.value = null }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div><h1>订单争议</h1><p>处理商家拒绝退款后自动转入的平台裁定申请。</p></div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </section>

    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="filter"><el-radio-button label="PENDING">待审核</el-radio-button><el-radio-button label="APPROVED">支持退款</el-radio-button><el-radio-button label="REJECTED">维持原处理</el-radio-button><el-radio-button label="">全部</el-radio-button></el-radio-group>
      </div>
      <el-table :data="visibleDisputes" v-loading="loading" stripe empty-text="暂无退款争议">
        <el-table-column prop="id" label="争议号" width="90" />
        <el-table-column prop="orderId" label="订单号" width="90" />
        <el-table-column prop="userId" label="用户" width="120" />
        <el-table-column prop="merchantId" label="商家ID" width="90" />
        <el-table-column prop="riderId" label="骑手" width="120"><template #default="{ row }">{{ row.riderId || '未指派' }}</template></el-table-column>
        <el-table-column label="用户争议理由" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ row.userReason }}</template></el-table-column>
        <el-table-column label="商家拒绝理由" min-width="180" show-overflow-tooltip><template #default="{ row }">{{ row.merchantRejectReason || '未填写' }}</template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'info'">{{ statusLabel[row.status] || row.status }}</el-tag></template></el-table-column>
        <el-table-column label="提交时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right"><template #default="{ row }"><div v-if="row.status === 'PENDING'" class="decision-actions"><el-button class="decision-button approve-button" type="success" :loading="handlingId === row.id" @click="audit(row, true)">支持退款</el-button><el-button class="decision-button" link type="danger" :loading="handlingId === row.id" @click="audit(row, false)">维持原处理</el-button></div><span v-else>{{ row.adminReason || '-' }}</span></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.admin-page { display:grid; gap:16px; }.page-head { align-items:flex-end; display:flex; justify-content:space-between; }.page-head h1 { font-size:22px; margin:0 0 6px; }.page-head p { color:var(--text-secondary); margin:0; }.toolbar { margin-bottom:14px; }.decision-actions { align-items:center; display:flex; flex-wrap:nowrap; gap:4px; white-space:nowrap; }.decision-button { font-size:13px; font-weight:600; min-width:96px; }.approve-button { --el-button-active-text-color:#fff; --el-button-hover-text-color:#fff; --el-button-text-color:#fff; }
</style>
