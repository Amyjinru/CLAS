<script setup>
import { onMounted, ref } from 'vue'
import { listAppeals, processAppeal } from '../../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'

const appeals = ref([])
const loading = ref(false)

const statusMap = {
  PENDING: { text: '待处理', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'info' }
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    appeals.value = await listAppeals()
  } catch {
    ElMessage.error('加载申诉列表失败')
  } finally {
    loading.value = false
  }
}

async function handleProcess(row, approve) {
  const action = approve ? '通过' : '驳回'
  try {
    const { value: adminReply } = await ElMessageBox.prompt(
      `确定${action}用户 ${row.userId} 的申诉吗？`,
      `${action}申诉`,
      {
        confirmButtonText: `确认${action}`,
        cancelButtonText: '取消',
        inputPlaceholder: '请填写处理说明',
        type: approve ? 'warning' : 'info'
      }
    )
    await processAppeal(row.id, {
      status: approve ? 'APPROVED' : 'REJECTED',
      adminReply: adminReply || undefined
    })
    ElMessage.success(`已${action}该申诉`)
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('处理失败')
    }
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <h1 class="page-title">申诉管理</h1>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <h2>用户处罚申诉</h2>
            <p class="card-desc">用户在个人中心提交的处罚申诉会出现在此列表，通过后关联处罚将自动撤销。</p>
          </div>
        </div>
      </template>

      <el-table :data="appeals" stripe v-loading="loading" size="small" empty-text="暂无待处理申诉">
        <el-table-column prop="id" label="申诉ID" width="80" />
        <el-table-column prop="userId" label="用户" width="120" />
        <el-table-column prop="penaltyId" label="处罚ID" width="90">
          <template #default="{ row }">{{ row.penaltyId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="申诉内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="primary" size="small" @click="handleProcess(row, true)">通过</el-button>
              <el-button size="small" @click="handleProcess(row, false)">驳回</el-button>
            </template>
            <span v-else class="processed-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin: 0 0 20px 0; }
.card-header h2 { font-size: 16px; margin: 0; }
.card-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 6px 0 0;
}
.processed-text {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
