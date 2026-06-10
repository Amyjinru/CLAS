<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitAppeal } from '../../api/clas'

const props = defineProps({
  penalties: { type: Array, default: () => [] },
  appeals: { type: Array, default: () => [] }
})

const emit = defineEmits(['reload'])

const appealForm = reactive({ penaltyId: null, content: '' })

const penaltyTypeLabel = { MUTE: '禁言', BAN: '封禁', SERVICE_STOP: '停止服务' }
const appealStatusLabel = { PENDING: '待处理', APPROVED: '已通过', REJECTED: '已驳回' }
const appealablePenalties = computed(() => props.penalties.filter(item => item.active))

function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

async function sendAppeal() {
  if (!appealForm.content.trim()) {
    ElMessage.warning('请填写申诉内容')
    return
  }
  try {
    await submitAppeal({
      penaltyId: appealForm.penaltyId || undefined,
      content: appealForm.content.trim()
    })
    appealForm.content = ''
    appealForm.penaltyId = null
    ElMessage.success('申诉已提交')
    emit('reload')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申诉提交失败'))
  }
}
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>处罚记录</h2>
        <p>查看平台对账号的限制状态</p>
      </div>
    </div>
    <el-empty v-if="!penalties.length" description="暂无处罚记录" />
    <article v-for="item in penalties" v-else :key="item.id" class="list-row">
      <div>
        <strong>{{ penaltyTypeLabel[item.penaltyType] || item.penaltyType }}</strong>
        <p>{{ item.reason }}</p>
        <p class="muted">
          {{ item.active ? '生效中' : '已失效' }}
          <span v-if="item.endTime"> · 至 {{ formatDateTime(item.endTime) }}</span>
        </p>
      </div>
      <el-tag :type="item.active ? 'danger' : 'info'">{{ item.active ? '生效中' : '已结束' }}</el-tag>
    </article>

    <div class="section-head" style="margin-top: 20px">
      <div>
        <h2>处罚申诉</h2>
        <p>如对禁言、封禁等处理有异议，可提交申诉</p>
      </div>
    </div>
    <div v-if="appealablePenalties.length" class="appeal-penalty-select">
      <span>关联处罚（可选）</span>
      <el-select v-model="appealForm.penaltyId" clearable placeholder="选择要申诉的处罚">
        <el-option
          v-for="item in appealablePenalties" :key="item.id"
          :label="`#${item.id} ${penaltyTypeLabel[item.penaltyType] || item.penaltyType}：${item.reason}`"
          :value="item.id"
        />
      </el-select>
    </div>
    <el-input v-model="appealForm.content" type="textarea" rows="3" placeholder="请描述申诉理由" />
    <el-button type="primary" style="margin-top: 10px" @click="sendAppeal">提交申诉</el-button>

    <article v-for="item in appeals" :key="item.id" class="list-row">
      <div>
        <strong>{{ appealStatusLabel[item.status] || item.status }}</strong>
        <p>{{ item.content }}</p>
        <p v-if="item.adminReply" class="muted">管理员回复：{{ item.adminReply }}</p>
      </div>
    </article>
  </div>
</template>

<style scoped>
.section-head { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 16px; }
.section-head h2 { margin: 0; }
.section-head p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.list-row { align-items: center; border-top: 1px solid var(--border-light); display: flex; justify-content: space-between; padding: 14px 0; }
.muted { color: var(--text-secondary); font-size: 13px; }
.appeal-penalty-select { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 10px; }
.appeal-penalty-select .el-select { min-width: 280px; }
</style>
