<script setup>
import { ref, onMounted } from 'vue'
import { api, unwrap } from '../../api/client'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await api.get('/admin/users', { params: { page: page.value, size: size.value } }).then(unwrap)
    users.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(user) {
  const action = user.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, '确认操作', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await api.put(`/admin/users/${user.phone}/status`, { enabled: !user.enabled })
    ElMessage.success(`${action}成功`)
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

function onPageChange(p) {
  page.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <h1 class="page-title">用户管理</h1>
    <el-card shadow="hover">
      <el-table :data="users" stripe v-loading="loading" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="phone" label="手机号" />
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
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              :type="row.enabled ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
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
</style>
