<script setup>
import { ref, onMounted } from 'vue'
import {
  deleteAdminReview,
  listAdminReviews,
  listReviewDeleteRequests,
  processReviewDeleteRequest,
  resolveReviewReport
} from '../../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'

const reviews = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const deleteRequests = ref([])
const requestStatus = ref('PENDING')
const requestLoading = ref(false)

const requestStatusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '全部', value: '' }
]

const requestStatusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'info' }
}

const requestTypeMap = {
  MERCHANT: '商家申请',
  USER: '用户举报'
}

function requestSummary(row) {
  if (row.requestType === 'USER') {
    return `用户 ${row.reporterUserId || '-'} 举报`
  }
  return `商家 #${row.merchantId}`
}

function requestTargetText(row) {
  if (row.replyId) {
    return `评价 #${row.reviewId} / 回复 #${row.replyId}`
  }
  return `评价 #${row.reviewId}`
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function loadDeleteRequests() {
  requestLoading.value = true
  try {
    deleteRequests.value = await listReviewDeleteRequests(requestStatus.value || undefined)
  } catch {
    ElMessage.error('加载删评申请失败')
  } finally {
    requestLoading.value = false
  }
}

async function handleProcessRequest(row, approve) {
  const action = approve ? '通过' : '驳回'
  try {
    const { value: remarks } = await ElMessageBox.prompt(
      `确定${action}${requestSummary(row)} 对 ${requestTargetText(row)} 的删评申请吗？`,
      `${action}删评申请`,
      {
        confirmButtonText: `确认${action}`,
        cancelButtonText: '取消',
        inputPlaceholder: '可选：填写处理备注',
        type: approve ? 'warning' : 'info'
      }
    )
    await processReviewDeleteRequest(row.id, {
      approve,
      remarks: remarks || undefined
    })
    ElMessage.success(`已${action}该删评申请`)
    await Promise.all([loadDeleteRequests(), load()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('处理失败')
    }
  }
}

async function load() {
  loading.value = true
  try {
    const data = await listAdminReviews({ page: page.value, size: size.value })
    reviews.value = data.records
    total.value = data.total
  } catch {
    ElMessage.error('加载评价列表失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(review) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${review.username}" 的评价吗？此操作不可恢复，商家评分将重新计算。`,
      '确认删除', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAdminReview(review.id)
    ElMessage.success('评价已删除，商家评分已重新计算')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function handleResolve(review) {
  try {
    await resolveReviewReport(review.id, 'RESOLVED')
    ElMessage.success('举报已标记处理')
    await load()
  } catch {
    ElMessage.error('处理失败')
  }
}

function onPageChange(p) {
  page.value = p
  load()
}

function onRequestStatusChange() {
  loadDeleteRequests()
}

onMounted(async () => {
  await Promise.all([loadDeleteRequests(), load()])
})
</script>

<template>
  <div class="admin-page">
    <h1 class="page-title">评价管理</h1>

    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>删评申请</h2>
            <p class="card-desc">商家申请删评与用户举报不当评论均会进入此列表，审核通过后删除对应评价或回复。</p>
          </div>
          <el-segmented v-model="requestStatus" :options="requestStatusOptions" @change="onRequestStatusChange" />
        </div>
      </template>

      <el-table :data="deleteRequests" stripe v-loading="requestLoading" size="small" empty-text="暂无删评申请">
        <el-table-column prop="id" label="申请ID" width="80" />
        <el-table-column label="来源" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.requestType === 'USER' ? 'danger' : 'warning'">
              {{ requestTypeMap[row.requestType] || row.requestType || '商家申请' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="160">
          <template #default="{ row }">{{ requestTargetText(row) }}</template>
        </el-table-column>
        <el-table-column prop="merchantId" label="商家ID" width="80" />
        <el-table-column label="发起人" width="120">
          <template #default="{ row }">
            {{ row.requestType === 'USER' ? (row.reporterUserId || '-') : `商家 #${row.merchantId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="理由" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="requestStatusMap[row.status]?.type || 'info'" size="small">
              {{ requestStatusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="adminRemarks" label="处理备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="primary" size="small" @click="handleProcessRequest(row, true)">通过</el-button>
              <el-button size="small" @click="handleProcessRequest(row, false)">驳回</el-button>
            </template>
            <span v-else class="processed-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="hover" class="section-card">
      <template #header>
        <h2>评价列表</h2>
      </template>
      <el-table :data="reviews" stripe v-loading="loading" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="orderId" label="订单ID" width="80" />
        <el-table-column prop="merchantId" label="商家ID" width="80" />
        <el-table-column label="评分" width="80">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="merchantReply" label="商家回复" min-width="140" show-overflow-tooltip />
        <el-table-column label="举报" width="180">
          <template #default="{ row }">
            <el-tag :type="row.reportStatus === 'PENDING' ? 'warning' : 'info'">{{ row.reportStatus }}</el-tag>
            <div class="report-reason" v-if="row.reportReason">{{ row.reportReason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.reportStatus === 'PENDING'" type="primary" size="small" @click="handleResolve(row)">处理</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin: 0 0 20px 0; }
.section-card { margin-bottom: 20px; }
.section-card h2 {
  font-size: 16px;
  margin: 0;
}
.card-header {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: space-between;
}
.card-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 6px 0 0;
}
.report-reason {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 4px;
}
.processed-text {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
