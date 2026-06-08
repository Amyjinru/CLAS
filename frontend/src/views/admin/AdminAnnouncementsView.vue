<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createAnnouncement, deleteAnnouncement, listAnnouncements, updateAnnouncement } from '../../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref([])
const loading = ref(false)
const form = reactive({ title: '', content: '' })
const editingId = ref(null)

async function load() {
  loading.value = true
  try {
    announcements.value = await listAnnouncements()
  } catch {
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.title = ''
  form.content = ''
}

function startEdit(item) {
  editingId.value = item.id
  form.title = item.title
  form.content = item.content
}

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, { title: form.title.trim(), content: form.content.trim() })
      ElMessage.success('公告已更新')
    } else {
      await createAnnouncement({ title: form.title.trim(), content: form.content.trim() })
      ElMessage.success('公告发布成功')
    }
    resetForm()
    await load()
  } catch {
    ElMessage.error(editingId.value ? '更新失败' : '发布失败')
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定删除这条公告吗？', '删除公告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAnnouncement(id)
    if (editingId.value === id) resetForm()
    ElMessage.success('公告已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="panel">
      <h1>公告管理</h1>
      <p>管理平台公告，用户端与商家端同步展示。</p>
    </section>

    <section class="list">
      <article class="announcement-card" v-for="item in announcements" :key="item.id">
        <div>
          <h2>{{ item.title }}</h2>
          <p class="announcement-time">{{ item.createTime?.replace('T', ' ') }}</p>
          <p>{{ item.content }}</p>
        </div>
        <div class="card-actions">
          <el-button size="small" @click="startEdit(item)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(item.id)">删除</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && !announcements.length" description="暂无公告" />
    </section>

    <section class="panel narrow">
      <h2>{{ editingId ? '编辑公告' : '发布公告' }}</h2>
      <label>
        标题
        <input v-model="form.title" placeholder="请输入公告标题" />
      </label>
      <label>
        内容
        <textarea v-model="form.content" placeholder="请输入公告内容" />
      </label>
      <div class="form-actions">
        <button @click="submit">{{ editingId ? '保存修改' : '发布公告' }}</button>
        <button v-if="editingId" class="secondary" type="button" @click="resetForm">取消编辑</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.announcement-card {
  align-items: flex-start;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 16px;
}

.announcement-card h2 {
  font-size: 18px;
  margin: 0 0 6px;
}

.announcement-time {
  color: #94a3b8;
  font-size: 13px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
