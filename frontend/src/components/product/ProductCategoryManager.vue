<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createProductCategory, updateProductCategory, deleteProductCategory } from '../../api/clas'

const props = defineProps({
  categories: { type: Array, default: () => [] }
})

const emit = defineEmits(['reload'])

const categoryForm = ref({ name: '', sortOrder: 0 })

async function handleCreate() {
  if (!categoryForm.value.name.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  await createProductCategory({
    name: categoryForm.value.name.trim(),
    sortOrder: categoryForm.value.sortOrder
  })
  ElMessage.success('分类已创建')
  categoryForm.value = { name: '', sortOrder: 0 }
  emit('reload')
}

async function handleUpdate(category) {
  await updateProductCategory({
    id: category.id,
    name: category.name,
    sortOrder: category.sortOrder
  })
  ElMessage.success('分类已保存')
  emit('reload')
}

async function handleDelete(category) {
  await ElMessageBox.confirm(
    `删除分类「${category.name}」后，分类下商品将变为未分类。确定删除吗？`,
    '删除分类',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteProductCategory(category.id)
  ElMessage.success('分类已删除')
  emit('reload', { deletedId: category.id })
}
</script>

<template>
  <el-card class="box-card work-card category-card">
    <template #header>
      <div class="card-header justify-between">
        <h3>分类管理</h3>
      </div>
    </template>

    <div class="category-create-row">
      <el-input v-model="categoryForm.name" placeholder="分类名称，如 主食、饮品、小食" maxlength="50" />
      <el-input-number v-model="categoryForm.sortOrder" :min="0" :step="1" />
      <el-button type="primary" @click="handleCreate">添加分类</el-button>
    </div>

    <div class="category-list" v-if="categories.length">
      <div v-for="category in categories" :key="category.id" class="category-item">
        <el-input v-model="category.name" maxlength="50" />
        <el-input-number v-model="category.sortOrder" :min="0" :step="1" />
        <el-button type="primary" @click="handleUpdate(category)">保存</el-button>
        <el-button type="danger" @click="handleDelete(category)">删除</el-button>
      </div>
    </div>
    <el-empty v-else description="暂无商品分类" />

    <p class="sort-order-hint">数字为分类权重</p>
  </el-card>
</template>

<style scoped>
.work-card { border-radius: 12px; }
.category-card { margin-bottom: 20px; }
.card-header.justify-between { display: flex; align-items: center; justify-content: space-between; }
.card-header h3 { margin: 0; font-size: 16px; color: var(--text-primary); }
.category-create-row,
.category-item {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) 140px auto auto;
  gap: 10px;
  margin-bottom: 10px;
}
.sort-order-hint {
  color: var(--text-muted, #909399);
  font-size: 12px;
  text-align: right;
  margin: 8px 0 0;
}
.category-list { display: grid; gap: 8px; margin-top: 14px; }
</style>
