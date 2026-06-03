<script setup>
import { onMounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { adminListMerchants, adminAuditMerchant, adminGetMerchantLogs, currentUser } from '../api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const merchants = ref([])
const loading = ref(true)
const logLoading = ref(false)
const dialogVisible = ref(false)
const logDialogVisible = ref(false)
const auditLogs = ref([])

const selectedMerchant = ref(null)
const auditForm = reactive({
  status: '',
  remarks: ''
})

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已审核', type: 'info' },
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '停业中', type: 'danger' },
  BLOCKED: { text: '已禁用', type: 'danger' }
}

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'OPEN', label: '营业中' },
  { value: 'CLOSED', label: '停业中' },
  { value: 'BLOCKED', label: '已禁用' }
]

async function load() {
  loading.value = true
  const user = currentUser()
  if (!user || user.role !== 'ADMIN') {
    ElMessage.warning('权限不足，仅管理员可访问')
    router.push('/home')
    return
  }

  try {
    merchants.value = await adminListMerchants()
  } catch (error) {
    // Handled globally
  } finally {
    loading.value = false
  }
}

function openAuditDialog(merchant) {
  selectedMerchant.value = merchant
  auditForm.status = merchant.status
  auditForm.remarks = merchant.adminRemarks || ''
  dialogVisible.value = true
}

async function submitAudit() {
  if (!selectedMerchant.value) return

  try {
    await adminAuditMerchant(selectedMerchant.value.id, {
      status: auditForm.status,
      remarks: auditForm.remarks
    })
    ElMessage.success('审核状态更新成功')
    dialogVisible.value = false
    await load()
  } catch (error) {
    // Handled globally
  }
}

async function showLogs(merchant) {
  selectedMerchant.value = merchant
  logLoading.value = true
  logDialogVisible.value = true
  try {
    auditLogs.value = await adminGetMerchantLogs(merchant.id)
  } catch (error) {
    // Handled globally
  } finally {
    logLoading.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(load)
</script>

<template>
  <div class="admin-container" v-loading="loading">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h2>平台商家审核管理</h2>
          <p class="subtitle">管理所有入驻商家的审核状态、限制营业或执行禁用操作</p>
        </div>
      </template>

      <el-table :data="merchants" style="width: 100%" stripe>
        <el-table-column prop="id" label="商户ID" width="80" />
        <el-table-column prop="merchantName" label="商户名称" min-width="150" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="category" label="品类" width="100" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="当前状态" width="110">
          <template #default="scope">
            <el-tag :type="statusMap[scope.row.status]?.type || 'info'" size="small">
              {{ statusMap[scope.row.status]?.text || scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="管理员备注" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <span class="remarks-text">{{ scope.row.adminRemarks || '无' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openAuditDialog(scope.row)">
              更新状态
            </el-button>
            <el-button type="info" size="small" @click="showLogs(scope.row)">
              日志
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Status Audit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="更新商户状态"
      width="500px"
      destroy-on-close
    >
      <el-form :model="auditForm" label-width="80px">
        <el-alert
          v-if="selectedMerchant"
          :title="`当前商户: ${selectedMerchant.merchantName} (当前状态: ${statusMap[selectedMerchant.status]?.text})`"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        />

        <el-form-item label="更新状态">
          <el-select v-model="auditForm.status" placeholder="请选择目标状态" style="width: 100%">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注说明">
          <el-input
            v-model="auditForm.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入操作备注说明（建议说明状态变更原因）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit">确认保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Audit Logs Dialog -->
    <el-dialog
      v-model="logDialogVisible"
      title="商户状态变更日志"
      width="600px"
    >
      <div v-loading="logLoading" style="min-height: 150px; padding: 10px 0;">
        <el-timeline v-if="auditLogs.length > 0">
          <el-timeline-item
            v-for="log in auditLogs"
            :key="log.id"
            :timestamp="formatTime(log.createdAt)"
            placement="top"
            type="primary"
          >
            <el-card class="log-card">
              <h4>状态变更: 
                <el-tag size="small" :type="statusMap[log.oldStatus]?.type || 'info'">
                  {{ statusMap[log.oldStatus]?.text || '无' }}
                </el-tag> 
                ➔ 
                <el-tag size="small" :type="statusMap[log.newStatus]?.type || 'success'">
                  {{ statusMap[log.newStatus]?.text || log.newStatus }}
                </el-tag>
              </h4>
              <p v-if="log.remarks" class="log-remarks">备注: {{ log.remarks }}</p>
              <p class="log-admin">操作人ID: {{ log.adminId }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无该商户的状态变更审计日志" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
}

.box-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header h2 {
  margin: 0;
  color: #303133;
}

.subtitle {
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.remarks-text {
  color: #606266;
  font-style: italic;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.log-card {
  border-radius: 6px;
  box-shadow: none;
  border: 1px solid #e4e7ed;
  background-color: #fafafa;
}

.log-card h4 {
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-remarks {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #606266;
  font-style: italic;
}

.log-admin {
  margin: 0;
  font-size: 11px;
  color: #909399;
}
</style>
