<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminAuditMerchant, adminGetMerchantLogs, adminListMerchants, currentUser } from '../api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const merchants = ref([])
const loading = ref(false)
const logLoading = ref(false)
const auditLogs = ref([])
const selectedMerchant = ref(null)
const auditDialogVisible = ref(false)
const detailVisible = ref(false)

const filters = reactive({
  status: '',
  keyword: ''
})

const auditForm = reactive({
  status: '',
  remarks: ''
})

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已审核', type: 'info' },
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '停业中', type: 'info' },
  BLOCKED: { text: '已禁用', type: 'danger' }
}

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'OPEN', label: '营业中' },
  { value: 'CLOSED', label: '停业中' },
  { value: 'BLOCKED', label: '已禁用' }
]

const auditStatusOptions = statusOptions.filter((item) => item.value)

const filteredMerchants = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return merchants.value.filter((merchant) => {
    const matchesStatus = !filters.status || merchant.status === filters.status
    const haystack = [
      merchant.merchantName,
      merchant.phone,
      merchant.category,
      merchant.address
    ].filter(Boolean).join(' ').toLowerCase()
    return matchesStatus && (!keyword || haystack.includes(keyword))
  })
})

const statusCounts = computed(() => merchants.value.reduce((acc, merchant) => {
  acc[merchant.status] = (acc[merchant.status] || 0) + 1
  return acc
}, {}))

async function load() {
  const user = currentUser()
  if (!user || user.role !== 'ADMIN') {
    ElMessage.warning('权限不足，仅管理员可访问')
    router.push('/home')
    return
  }

  loading.value = true
  try {
    merchants.value = await adminListMerchants()
  } catch {
    ElMessage.error('加载商家审核列表失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
}

function openAuditDialog(merchant) {
  selectedMerchant.value = merchant
  auditForm.status = merchant.status
  auditForm.remarks = merchant.adminRemarks || ''
  auditDialogVisible.value = true
}

async function openDetail(merchant) {
  selectedMerchant.value = merchant
  detailVisible.value = true
  await loadLogs(merchant.id)
}

async function loadLogs(merchantId) {
  logLoading.value = true
  try {
    auditLogs.value = await adminGetMerchantLogs(merchantId)
  } catch {
    ElMessage.error('加载审核记录失败')
    auditLogs.value = []
  } finally {
    logLoading.value = false
  }
}

async function submitAudit() {
  if (!selectedMerchant.value) return
  if (!auditForm.status) {
    ElMessage.warning('请选择目标状态')
    return
  }

  try {
    await adminAuditMerchant(selectedMerchant.value.id, {
      status: auditForm.status,
      remarks: auditForm.remarks.trim()
    })
    ElMessage.success('审核状态已更新')
    auditDialogVisible.value = false
    await load()
    if (detailVisible.value) {
      const refreshed = merchants.value.find((item) => item.id === selectedMerchant.value.id)
      selectedMerchant.value = refreshed || selectedMerchant.value
      await loadLogs(selectedMerchant.value.id)
    }
  } catch {
    ElMessage.error('审核操作失败')
  }
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function formatFen(value) {
  return value ? `¥${(value / 100).toFixed(2)}` : '¥0.00'
}

function maskBankAccount(value) {
  if (!value) return '-'
  return value.length > 4 ? `**** **** ${value.slice(-4)}` : value
}

onMounted(load)
</script>

<template>
  <div class="admin-audit-page">
    <section class="page-head">
      <div>
        <h1>商家审核管理</h1>
        <p>集中处理入驻审核、营业状态流转、禁用和备注追踪。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </section>

    <section class="status-strip">
      <button
        v-for="item in statusOptions"
        :key="item.value || 'all'"
        :class="['status-chip', { active: filters.status === item.value }]"
        type="button"
        @click="filters.status = item.value"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value ? (statusCounts[item.value] || 0) : merchants.length }}</strong>
      </button>
    </section>

    <el-card shadow="never" class="audit-card">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索商家名称、电话、品类或地址"
          style="width: 340px"
        />
        <el-select v-model="filters.status" placeholder="状态筛选" clearable style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table
        :data="filteredMerchants"
        v-loading="loading"
        stripe
        empty-text="暂无匹配商家"
        size="small"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="merchantName" label="商家名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="category" label="品类" width="110" />
        <el-table-column prop="address" label="地址" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入驻时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="adminRemarks" label="管理员备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.adminRemarks || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button type="primary" size="small" @click="openAuditDialog(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" title="商家审核详情" size="520px">
      <div v-if="selectedMerchant" class="detail-panel">
        <header class="merchant-title">
          <div>
            <h2>{{ selectedMerchant.merchantName }}</h2>
            <p>{{ selectedMerchant.category || '未填写品类' }}</p>
          </div>
          <el-tag :type="statusMap[selectedMerchant.status]?.type || 'info'">
            {{ statusMap[selectedMerchant.status]?.text || selectedMerchant.status }}
          </el-tag>
        </header>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="联系电话">{{ selectedMerchant.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商家地址">{{ selectedMerchant.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="营业时间">{{ selectedMerchant.businessHours || '-' }}</el-descriptions-item>
          <el-descriptions-item label="配送费">{{ formatFen(selectedMerchant.deliveryFee) }}</el-descriptions-item>
          <el-descriptions-item label="起送价">{{ formatFen(selectedMerchant.minOrderPrice) }}</el-descriptions-item>
          <el-descriptions-item label="结算周期">{{ selectedMerchant.settlementCycle ? `${selectedMerchant.settlementCycle} 天` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="银行卡">{{ maskBankAccount(selectedMerchant.bankAccount) }}</el-descriptions-item>
          <el-descriptions-item label="管理员备注">{{ selectedMerchant.adminRemarks || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入驻时间">{{ formatTime(selectedMerchant.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(selectedMerchant.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-actions">
          <el-button type="primary" @click="openAuditDialog(selectedMerchant)">更新审核状态</el-button>
        </div>

        <section class="log-section" v-loading="logLoading">
          <h3>审核记录</h3>
          <el-timeline v-if="auditLogs.length">
            <el-timeline-item
              v-for="log in auditLogs"
              :key="log.id"
              :timestamp="formatTime(log.createdAt)"
              placement="top"
            >
              <div class="log-item">
                <strong>
                  {{ statusMap[log.oldStatus]?.text || log.oldStatus || '无' }}
                  →
                  {{ statusMap[log.newStatus]?.text || log.newStatus }}
                </strong>
                <p>{{ log.remarks || '无备注' }}</p>
                <span>管理员：{{ log.adminId || '-' }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核记录" />
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="auditDialogVisible" title="更新商家审核状态" width="520px" destroy-on-close>
      <el-form :model="auditForm" label-width="96px">
        <el-alert
          v-if="selectedMerchant"
          :title="`${selectedMerchant.merchantName} 当前状态：${statusMap[selectedMerchant.status]?.text || selectedMerchant.status}`"
          type="info"
          :closable="false"
          class="audit-alert"
        />
        <el-form-item label="目标状态">
          <el-select v-model="auditForm.status" style="width: 100%">
            <el-option
              v-for="item in auditStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="auditForm.remarks"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="说明审核原因、补充材料或处理依据"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">保存审核结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-audit-page {
  display: grid;
  gap: 16px;
}

.page-head {
  align-items: flex-end;
  display: flex;
  justify-content: space-between;
}

.page-head h1 {
  font-size: 22px;
  margin: 0 0 6px;
}

.page-head p {
  color: var(--text-secondary);
  margin: 0;
}

.status-strip {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.status-chip {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  text-align: left;
}

.status-chip.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-soft);
}

.status-chip strong {
  color: var(--color-primary);
}

.audit-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.toolbar .el-input {
  width: 340px !important;
}
.toolbar .el-input__wrapper {
  width: 100% !important;
  box-sizing: border-box !important;
}

.detail-panel {
  display: grid;
  gap: 18px;
}

.merchant-title {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
}

.merchant-title h2 {
  font-size: 20px;
  margin: 0 0 4px;
}

.merchant-title p {
  color: var(--text-secondary);
  margin: 0;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
}

.log-section h3 {
  font-size: 16px;
  margin: 0 0 14px;
}

.log-item {
  background: var(--bg-soft);
  border-radius: 8px;
  padding: 10px 12px;
}

.log-item p {
  color: var(--text-secondary);
  margin: 6px 0;
}

.log-item span {
  color: var(--text-muted);
  font-size: 12px;
}

.audit-alert {
  margin-bottom: 16px;
}
</style>
