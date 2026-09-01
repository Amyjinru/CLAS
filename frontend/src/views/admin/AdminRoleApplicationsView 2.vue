<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminAuditRoleApplication, adminListRoleApplications } from '../../api/clas'

const loading = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const selected = ref(null)
const audit = reactive({ status: 'APPROVED', remarks: '' })

async function load() {
  loading.value = true
  try { rows.value = await adminListRoleApplications() } catch { ElMessage.error('身份申请列表加载失败') } finally { loading.value = false }
}
function openAudit(row) {
  selected.value = row
  audit.status = 'APPROVED'
  audit.remarks = ''
  dialogVisible.value = true
}
async function submit() {
  try {
    await adminAuditRoleApplication(selected.value.id, { ...audit })
    ElMessage.success('审核结果已保存')
    dialogVisible.value = false
    await load()
  } catch (error) { ElMessage.error(error?.message || '审核失败') }
}
onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page-head"><h1>身份申请审核</h1><p>审核骑手等业务身份申请；通过后申请人重新登录即可生效。</p></div>
    <el-card shadow="never">
      <el-table :data="rows" stripe empty-text="暂无身份申请">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="申请用户" min-width="130" />
        <el-table-column prop="targetRole" label="目标身份" width="110"><template #default="{ row }">{{ row.targetRole === 'RIDER' ? '骑手' : row.targetRole }}</template></el-table-column>
        <el-table-column prop="reason" label="申请说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="175" />
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button v-if="row.status === 'PENDING'" type="primary" size="small" @click="openAudit(row)">审核</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" title="审核身份申请" width="500px">
      <el-form label-width="88px"><el-form-item label="审核结果"><el-radio-group v-model="audit.status"><el-radio value="APPROVED">通过</el-radio><el-radio value="REJECTED">拒绝</el-radio></el-radio-group></el-form-item><el-form-item label="审核备注"><el-input v-model="audit.remarks" type="textarea" :rows="3" maxlength="255" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确认审核</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.page { padding: 28px; }.page-head { margin-bottom: 20px; }.page-head h1 { margin: 0 0 8px; }.page-head p { margin: 0; color: #909399; }</style>
