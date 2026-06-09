<script setup>
import { onMounted, reactive, ref } from 'vue'
import { applyPenalty } from '../../api/profile'
import { listAdminUsers, toggleUserStatus } from '../../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const filters = reactive({
  role: '',
  enabled: '',
  keyword: ''
})

const roleOptions = [
  { label: '全部角色', value: '' },
  { label: '用户', value: 'USER' },
  { label: '商家', value: 'MERCHANT' },
  { label: '管理员', value: 'ADMIN' }
]

async function load() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      role: filters.role || undefined,
      enabled: filters.enabled === '' ? undefined : filters.enabled,
      keyword: filters.keyword.trim() || undefined
    }
    const data = await listAdminUsers(params)
    users.value = data.records
    total.value = data.total
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.role = ''
  filters.enabled = ''
  filters.keyword = ''
  search()
}

async function changeStatus(user) {
  const action = user.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户「${user.username || user.phone}」吗？`, `${action}用户`, {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning'
    })
    await toggleUserStatus(user.phone, !user.enabled)
    ElMessage.success(`${action}成功`)
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(`${action}失败`)
  }
}

async function applyUserPenalty(user, type) {
  const labels = { MUTE: '禁言', BAN: '封禁', SERVICE_STOP: '停止服务' }
  try {
    const { value: reason } = await ElMessageBox.prompt(`请输入对 ${user.username || user.phone} 执行${labels[type]}的原因`, '处罚用户', {
      confirmButtonText: '继续',
      cancelButtonText: '取消',
      inputValidator: (value) => Boolean(value?.trim()) || '请填写处罚原因'
    })
    let durationHours = 24
    if (type !== 'SERVICE_STOP') {
      const { value: hours } = await ElMessageBox.prompt('请输入处罚时长（小时）', '处罚时长', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: '24',
        inputValidator: (value) => Number(value) > 0 || '请输入大于 0 的小时数'
      })
      durationHours = Number(hours)
    }
    await applyPenalty(user.phone, {
      userId: user.phone,
      penaltyType: type,
      reason: reason.trim(),
      durationHours
    })
    ElMessage.success(`${labels[type]}已生效`)
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('处罚失败')
  }
}

function onPageChange(value) {
  page.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>筛选用户、启停账号，并对异常用户执行处罚。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </section>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索手机号、用户名或昵称" style="max-width: 280px" @keyup.enter="search" />
        <el-select v-model="filters.role" placeholder="角色" clearable style="width: 140px" @change="search">
          <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.enabled" placeholder="账号状态" clearable style="width: 140px" @change="search">
          <el-option label="正常" :value="true" />
          <el-option label="已禁用" :value="false" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="users" stripe v-loading="loading" size="small" empty-text="暂无匹配用户">
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'MERCHANT' ? 'warning' : 'primary'" size="small">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button :type="row.enabled ? 'danger' : 'success'" size="small" @click="changeStatus(row)">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="applyUserPenalty(row, 'MUTE')">禁言</el-button>
            <el-button size="small" type="warning" @click="applyUserPenalty(row, 'BAN')">封禁</el-button>
            <el-button size="small" type="danger" @click="applyUserPenalty(row, 'SERVICE_STOP')">停服</el-button>
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
.admin-page {
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
