<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  deleteAdminReview,
  listAdminReviews,
  listReviewDeleteRequests,
  processReviewDeleteRequest,
  resolveReviewReport
} from '../../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const reviews = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const requestLoading = ref(false)
const deleteRequests = ref([])

const reviewFilters = reactive({
  reportStatus: '',
  keyword: ''
})

const requestStatus = ref('PENDING')

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

async function loadReviews() {
  loading.value = true
  try {
    const data = await listAdminReviews({
      page: page.value,
      size: size.value,
      reportStatus: reviewFilters.reportStatus || undefined,
      keyword: reviewFilters.keyword.trim() || undefined
    })
    reviews.value = data.records
    total.value = data.total
  } catch {
    ElMessage.error('加载评价列表失败')
  } finally {
    loading.value = false
  }
}

async function processRequest(row, approve) {
  const action = approve ? '通过' : '驳回'
  try {
    const { value: remarks } = await ElMessageBox.prompt(
      `确定${action}${requestSummary(row)}对${requestTargetText(row)}的处理申请吗？`,
      `${action}删评申请`,
      {
        confirmButtonText: `确认${action}`,
        cancelButtonText: '取消',
        inputPlaceholder: '可选：填写处理备注',
        type: approve ? 'warning' : 'info'
      }
    )
    await processReviewDeleteRequest(row.id, { approve, remarks: remarks || undefined })
    ElMessage.success(`已${action}该申请`)
    await Promise.all([loadDeleteRequests(), loadReviews()])
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('处理失败')
  }
}

async function deleteReview(review) {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${review.username || review.userId}」的评价吗？删除后商家评分会重新计算。`,
      '删除评价',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAdminReview(review.id)
    ElMessage.success('评价已删除')
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

async function resolveReport(review) {
  try {
    await resolveReviewReport(review.id, 'RESOLVED')
    ElMessage.success('举报已标记为已处理')
    await loadReviews()
  } catch {
    ElMessage.error('处理举报失败')
  }
}

function searchReviews() {
  page.value = 1
  loadReviews()
}

function resetReviewFilters() {
  reviewFilters.reportStatus = ''
  reviewFilters.keyword = ''
  searchReviews()
}

async function refreshAll() {
  await Promise.all([loadDeleteRequests(), loadReviews()])
}

function onPageChange(value) {
  page.value = value
  loadReviews()
}

function requestSummary(row) {
  return row.requestType === 'USER'
    ? `用户 ${row.reporterUserId || '-'} 发起`
    : `商家 #${row.merchantId} 发起`
}

function requestTargetText(row) {
  return row.replyId ? `评价 #${row.reviewId} / 回复 #${row.replyId}` : `评价 #${row.reviewId}`
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

onMounted(refreshAll)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <h1>评价治理</h1>
        <p>集中处理用户举报、商家删评申请和违规评价删除。</p>
      </div>
      <el-button :loading="loading || requestLoading" @click="refreshAll">刷新</el-button>
    </section>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>删评与举报申请</h2>
            <p>商家申请删评、用户举报评价或回复后，管理员在此审核处理。</p>
          </div>
          <el-segmented v-model="requestStatus" :options="requestStatusOptions" @change="loadDeleteRequests" />
        </div>
      </template>

      <el-table :data="deleteRequests" stripe v-loading="requestLoading" size="small" empty-text="暂无删评或举报申请">
        <el-table-column prop="id" label="申请ID" width="80" />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.requestType === 'USER' ? 'danger' : 'warning'">
              {{ requestTypeMap[row.requestType] || '商家申请' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="150">
          <template #default="{ row }">{{ requestTargetText(row) }}</template>
        </el-table-column>
        <el-table-column label="发起人" min-width="130">
          <template #default="{ row }">{{ requestSummary(row) }}</template>
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
        <el-table-column prop="adminRemarks" label="处理备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.adminRemarks || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="primary" size="small" @click="processRequest(row, true)">通过</el-button>
              <el-button size="small" @click="processRequest(row, false)">驳回</el-button>
            </template>
            <span v-else class="processed-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>评价列表</h2>
            <p>查看全平台评价和举报状态，可删除明确违规内容。</p>
          </div>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="reviewFilters.keyword" clearable placeholder="搜索用户、评价内容或举报原因" style="max-width: 300px" @keyup.enter="searchReviews" />
        <el-select v-model="reviewFilters.reportStatus" placeholder="举报状态" clearable style="width: 150px" @change="searchReviews">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已处理" value="RESOLVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="searchReviews">查询</el-button>
        <el-button @click="resetReviewFilters">重置</el-button>
      </div>

      <el-table :data="reviews" stripe v-loading="loading" size="small" empty-text="暂无匹配评价">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户" width="120" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单ID" width="90" />
        <el-table-column prop="merchantId" label="商家ID" width="90" />
        <el-table-column label="评分" width="130">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="merchantReply" label="商家回复" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.merchantReply || '-' }}</template>
        </el-table-column>
        <el-table-column label="举报状态" width="150">
          <template #default="{ row }">
            <el-tag :type="row.reportStatus === 'PENDING' ? 'warning' : row.reportStatus === 'RESOLVED' ? 'success' : 'info'" size="small">
              {{ row.reportStatus || '无举报' }}
            </el-tag>
            <div v-if="row.reportReason" class="report-reason">{{ row.reportReason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.reportStatus === 'PENDING'" type="primary" size="small" @click="resolveReport(row)">处理</el-button>
            <el-button type="danger" size="small" @click="deleteReview(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
        class="pager"
      />
    </el-card>
  </div>
</template>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.page-head { align-items: flex-end; display: flex; justify-content: space-between; }
.page-head h1 { font-size: 22px; margin: 0 0 6px; }
.page-head p,
.card-header p { color: var(--text-secondary); margin: 0; }
.section-card h2 { font-size: 16px; margin: 0 0 6px; }
.card-header { align-items: center; display: flex; gap: 16px; justify-content: space-between; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.report-reason { color: var(--text-secondary); font-size: 12px; margin-top: 4px; }
.processed-text { color: var(--text-secondary); font-size: 13px; }
.pager { justify-content: flex-end; margin-top: 16px; }
</style>
