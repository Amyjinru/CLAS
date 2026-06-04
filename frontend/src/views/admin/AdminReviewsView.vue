<script setup>
import { ref, onMounted } from 'vue'
import { api, unwrap } from '../../api/client'
import { ElMessage, ElMessageBox } from 'element-plus'

const reviews = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await api.get('/admin/reviews', { params: { page: page.value, size: size.value } }).then(unwrap)
    reviews.value = data.records
    total.value = data.total
  } catch (e) {
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
    await api.delete(`/admin/reviews/${review.id}`)
    ElMessage.success('评价已删除，商家评分已重新计算')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
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
    <h1 class="page-title">评价管理</h1>
    <el-card shadow="hover">
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
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
</style>
