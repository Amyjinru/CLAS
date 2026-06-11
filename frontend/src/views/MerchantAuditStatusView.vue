<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyMerchantAuditStatus } from '../api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(true)
const auditStatus = ref(null)

const statusMap = {
  PENDING: { text: '待审核', type: 'warning', color: '#e6a23c' },
  APPROVED: { text: '已审核', type: 'info', color: '#409eff' },
  OPEN: { text: '营业中', type: 'success', color: '#67c23a' },
  CLOSED: { text: '停业中', type: 'info', color: '#909399' },
  BLOCKED: { text: '已禁用', type: 'danger', color: '#f56c6c' }
}

const currentStatus = computed(() => statusMap[auditStatus.value?.status] || {
  text: auditStatus.value?.status || '未知状态',
  type: 'info',
  color: '#909399'
})

const timeline = computed(() => auditStatus.value?.auditTimeline || [])
const hasRejectReason = computed(() => auditStatus.value?.status === 'APPROVED' && auditStatus.value?.adminRemarks)

function formatStatus(status) {
  return statusMap[status]?.text || status || '-'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadAuditStatus() {
  loading.value = true
  try {
    auditStatus.value = await getMyMerchantAuditStatus()
  } catch (error) {
    ElMessage.error('获取审核进度失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAuditStatus)
</script>

<template>
  <div class="audit-status-page" v-loading="loading">
    <section class="status-hero" v-if="auditStatus">
      <div>
        <p class="eyebrow">商家入驻审核</p>
        <h1>{{ currentStatus.text }}</h1>
        <p class="summary">
          {{
            auditStatus.status === 'PENDING'
              ? '您的入驻申请已提交，请等待管理员审核。'
              : auditStatus.status === 'OPEN'
                ? '店铺已经开通营业，可以进入商家工作台处理业务。'
                : '请关注管理员备注和审核时间线，按需完成后续处理。'
          }}
        </p>
      </div>
      <el-tag :type="currentStatus.type" size="large" effect="dark">
        {{ currentStatus.text }}
      </el-tag>
    </section>

    <el-empty v-else-if="!loading" description="暂无商家入驻信息">
      <el-button type="primary" @click="router.push('/merchant-register')">申请入驻</el-button>
    </el-empty>

    <template v-if="auditStatus">
      <el-alert
        v-if="hasRejectReason"
        title="驳回原因"
        :description="auditStatus.adminRemarks"
        type="error"
        show-icon
        :closable="false"
        class="status-alert"
      />
      <el-alert
        v-else-if="auditStatus.adminRemarks"
        title="管理员备注"
        :description="auditStatus.adminRemarks"
        type="info"
        show-icon
        :closable="false"
        class="status-alert"
      />

      <section class="timeline-section">
        <div class="section-head">
          <h2>审核时间线</h2>
          <el-button type="primary" plain @click="router.push('/merchant-console')">
            返回工作台
          </el-button>
        </div>

        <el-timeline v-if="timeline.length">
          <el-timeline-item
            v-for="item in timeline"
            :key="item.id"
            :timestamp="formatTime(item.createdAt)"
            :color="statusMap[item.newStatus]?.color || '#909399'"
            placement="top"
          >
            <div class="timeline-card">
              <strong>{{ formatStatus(item.oldStatus) }} → {{ formatStatus(item.newStatus) }}</strong>
              <p v-if="item.remarks">{{ item.remarks }}</p>
              <small v-if="item.adminId">操作人：{{ item.adminId }}</small>
            </div>
          </el-timeline-item>
        </el-timeline>

        <div v-else class="empty-timeline">
          <el-icon :size="42"><Clock /></el-icon>
          <p>申请已提交，管理员审核后将在这里显示处理记录。</p>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.audit-status-page {
  max-width: 960px;
  margin: 32px auto;
  padding: 0 20px 48px;
}

.status-hero {
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  display: flex;
  gap: 24px;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 28px;
}

.eyebrow {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0 0 8px;
}

.status-hero h1 {
  color: var(--text-primary);
  font-size: 28px;
  margin: 0;
}

.summary {
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 10px 0 0;
}

.status-alert {
  margin-bottom: 18px;
}

.timeline-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 24px;
}

.section-head {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-head h2 {
  color: var(--text-primary);
  font-size: 18px;
  margin: 0;
}

.timeline-card {
  background: var(--clas-warm-50);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 12px 14px;
}

.timeline-card p {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 8px 0 0;
}

.timeline-card small {
  color: var(--text-muted);
  display: block;
  margin-top: 8px;
}

.empty-timeline {
  align-items: center;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 36px 0 16px;
  text-align: center;
}

@media (max-width: 640px) {
  .status-hero,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
