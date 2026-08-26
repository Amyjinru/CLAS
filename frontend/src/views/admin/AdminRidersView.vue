<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditRiderApplication,
  auditRiderInfoChangeRequest,
  auditRiderWithdrawal,
  listRiderApplications,
  listRiderInfoChangeRequests,
  listRiderWithdrawals
} from '../../api/admin'

const applications = ref([])
const withdrawals = ref([])
const infoChanges = ref([])
const loading = ref(false)
const pendingCount = computed(() => applications.value.length + withdrawals.value.length + infoChanges.value.length)

async function load() {
  loading.value = true
  try {
    const [nextApplications, nextWithdrawals, nextInfoChanges] = await Promise.all([
      listRiderApplications(),
      listRiderWithdrawals(),
      listRiderInfoChangeRequests()
    ])
    applications.value = nextApplications
    withdrawals.value = nextWithdrawals
    infoChanges.value = nextInfoChanges
  } finally {
    loading.value = false
  }
}

async function auditApplication(item, decision) {
  await auditRiderApplication(item.id, {
    decision,
    reason: decision === 'APPROVE' ? '审核通过' : '资料不符合要求',
    maxActiveOrders: 3
  })
  ElMessage.success('骑手申请已处理')
  await load()
}

async function auditWithdrawal(item, approved) {
  await auditRiderWithdrawal(item.id, { approved, reason: approved ? '审核通过' : '审核驳回' })
  ElMessage.success('提现申请已处理')
  await load()
}

async function auditInfoChange(item, approved) {
  let reason = '资料核验通过'
  if (!approved) {
    try {
      const result = await ElMessageBox.prompt('请填写驳回原因，将展示给骑手。', '驳回联系电话修改', {
        inputPattern: /.+/,
        inputErrorMessage: '请填写驳回原因'
      })
      reason = result.value
    } catch {
      return
    }
  }
  await auditRiderInfoChangeRequest(item.id, { approved, reason })
  ElMessage.success('资料修改审核已完成')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="rider-operations-page" v-loading="loading">
    <header class="page-head">
      <div>
        <p class="eyebrow">RIDER OPERATIONS</p>
        <h1>骑手运营</h1>
        <p>集中处理身份准入、服务联系方式和资金结算申请。</p>
      </div>
      <div class="head-actions">
        <el-tag v-if="pendingCount" type="warning" effect="light">待处理 {{ pendingCount }} 项</el-tag>
        <el-button plain @click="load">刷新列表</el-button>
      </div>
    </header>

    <section class="operations-grid">
      <el-card class="operation-card">
        <template #header>
          <div class="card-head">
            <div><h2>骑手申请</h2><p>核验资料后决定是否开放接单能力。</p></div>
            <el-tag type="warning">{{ applications.length }} 项</el-tag>
          </div>
        </template>
        <el-table :data="applications" empty-text="暂无待审核的骑手申请">
          <el-table-column prop="realName" label="姓名" min-width="92" />
          <el-table-column prop="vehicleType" label="车辆" min-width="96" />
          <el-table-column prop="serviceArea" label="服务区域" min-width="150" />
          <el-table-column label="操作" width="154" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button size="small" type="success" plain @click="auditApplication(row, 'APPROVE')">通过</el-button>
                <el-button size="small" type="danger" plain @click="auditApplication(row, 'REJECT')">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="operation-card">
        <template #header>
          <div class="card-head">
            <div><h2>服务联系电话修改</h2><p>审核骑手服务号码的变更请求。</p></div>
            <el-tag type="info">{{ infoChanges.length }} 项</el-tag>
          </div>
        </template>
        <el-table :data="infoChanges" empty-text="暂无待审核的资料修改">
          <el-table-column prop="riderId" label="骑手账号" min-width="100" />
          <el-table-column prop="currentPhone" label="当前号码" min-width="124" />
          <el-table-column prop="requestedPhone" label="申请号码" min-width="124" />
          <el-table-column prop="createdAt" label="申请时间" min-width="164" />
          <el-table-column label="操作" width="154" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button size="small" type="success" plain @click="auditInfoChange(row, true)">通过</el-button>
                <el-button size="small" type="danger" plain @click="auditInfoChange(row, false)">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="operation-card withdrawal-card">
        <template #header>
          <div class="card-head">
            <div><h2>提现审核</h2><p>确认可结算资金后再批准提现。</p></div>
            <el-tag type="success">{{ withdrawals.length }} 项</el-tag>
          </div>
        </template>
        <el-table :data="withdrawals" empty-text="暂无待审核的提现申请">
          <el-table-column prop="riderId" label="骑手账号" min-width="108" />
          <el-table-column prop="amount" label="金额（分）" min-width="112" />
          <el-table-column prop="status" label="状态" min-width="96" />
          <el-table-column label="操作" width="154" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button size="small" type="success" plain @click="auditWithdrawal(row, true)">批准</el-button>
                <el-button size="small" type="danger" plain @click="auditWithdrawal(row, false)">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </section>
</template>

<style scoped>
.rider-operations-page { max-width: 1320px; margin: 0 auto; }
.page-head { align-items: flex-end; display: flex; gap: 24px; justify-content: space-between; margin-bottom: 22px; }
.eyebrow { color: var(--clas-teal-700); font-size: 11px; font-weight: 800; letter-spacing: .14em; margin: 0 0 6px; }
.page-head h1 { color: var(--text-primary); font-size: clamp(26px, 4vw, 34px); line-height: 1.2; margin: 0; }
.page-head p:not(.eyebrow) { color: var(--text-secondary); margin: 8px 0 0; }
.head-actions, .table-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; }
.operations-grid { display: grid; gap: 18px; }
.operation-card { overflow: hidden; }
.card-head { align-items: flex-start; display: flex; gap: 16px; justify-content: space-between; }
.card-head h2 { color: var(--text-primary); font-size: 17px; margin: 0; }
.card-head p { color: var(--text-muted); font-size: 12px; line-height: 1.55; margin: 5px 0 0; }
.table-actions :deep(.el-button + .el-button) { margin-left: 0; }
.withdrawal-card { margin-bottom: 24px; }
@media (max-width: 700px) {
  .page-head { align-items: stretch; flex-direction: column; }
  .head-actions > :deep(.el-button) { flex: 1; }
  .operation-card { overflow-x: auto; }
  .table-actions { flex-wrap: nowrap; }
}
</style>
