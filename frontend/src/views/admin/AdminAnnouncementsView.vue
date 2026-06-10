<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createAnnouncement, deleteAnnouncement, listAdminAnnouncements, updateAnnouncement } from '../../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref([])
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({
  title: '',
  content: '',
  pinned: false,
  startAt: null,
  endAt: null
})

async function load() {
  loading.value = true
  try {
    announcements.value = await listAdminAnnouncements()
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
  form.pinned = false
  form.startAt = null
  form.endAt = null
}

function startEdit(item) {
  editingId.value = item.id
  form.title = item.title
  form.content = item.content
  form.pinned = !!item.pinned
  form.startAt = item.startAt ?? null
  form.endAt = item.endAt ?? null
}

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写公告标题和内容')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      content: form.content.trim(),
      pinned: form.pinned,
      startAt: form.startAt || null,
      endAt: form.endAt || null
    }
    if (editingId.value) {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('公告已更新')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('公告已发布')
    }
    resetForm()
    await load()
  } catch {
    ElMessage.error(editingId.value ? '更新公告失败' : '发布公告失败')
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${item.title}」吗？`, '删除公告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAnnouncement(item.id)
    if (editingId.value === item.id) resetForm()
    ElMessage.success('公告已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除公告失败')
  }
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function isExpired(item) {
  if (!item.endAt) return false
  return new Date(item.endAt) < new Date()
}

function formatEffectiveRange(item) {
  if (!item.startAt && !item.endAt) return '长期有效'
  const parts = []
  if (item.startAt) parts.push(formatTime(item.startAt))
  parts.push('~')
  if (item.endAt) parts.push(formatTime(item.endAt))
  else parts.push('长期')
  return parts.join(' ')
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <h1>公告管理</h1>
        <p>发布平台公告，支持置顶和有效期设置。用户端自动过滤过期公告，置顶公告优先展示。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </section>

    <section class="content-grid">
      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="card-header">
            <h2>公告列表</h2>
            <span>{{ announcements.length }} 条</span>
          </div>
        </template>

        <div v-loading="loading" class="announcement-list">
          <article
            v-for="item in announcements"
            :key="item.id"
            class="announcement-card"
            :class="{ expired: isExpired(item) }"
          >
            <div>
              <header>
                <el-tag v-if="item.pinned" size="small" type="warning" effect="dark">📌 置顶</el-tag>
                <h3>{{ item.title }}</h3>
                <el-tag v-if="isExpired(item)" size="small" type="info">已过期</el-tag>
                <el-tag v-else-if="item.status === 'PUBLISHED'" size="small" type="success">已发布</el-tag>
                <el-tag v-else size="small" type="info">{{ item.status }}</el-tag>
              </header>
              <p class="time">
                {{ formatTime(item.createTime) }}
                <span class="time-divider">|</span>
                <span>{{ formatEffectiveRange(item) }}</span>
              </p>
              <p class="content">{{ item.content }}</p>
            </div>
            <div class="card-actions">
              <el-button size="small" @click="startEdit(item)">编辑</el-button>
              <el-button size="small" type="danger" @click="remove(item)">删除</el-button>
            </div>
          </article>
          <el-empty v-if="!loading && !announcements.length" description="暂无公告" />
        </div>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header>{{ editingId ? '编辑公告' : '发布公告' }}</template>
        <el-form label-position="top">
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="请输入公告标题" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              maxlength="1000"
              show-word-limit
              placeholder="请输入公告内容"
            />
          </el-form-item>
          <el-form-item label="置顶">
            <el-switch v-model="form.pinned" active-text="是" inactive-text="否" />
          </el-form-item>
          <el-form-item label="生效时间">
            <el-date-picker
              v-model="form.startAt"
              type="datetime"
              placeholder="选择生效开始时间（留空=立即生效）"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="失效时间">
            <el-date-picker
              v-model="form.endAt"
              type="datetime"
              placeholder="选择失效时间（留空=长期有效）"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="submit">
              {{ editingId ? '保存修改' : '发布公告' }}
            </el-button>
            <el-button v-if="editingId" @click="resetForm">取消编辑</el-button>
          </div>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.page-head { align-items: flex-end; display: flex; justify-content: space-between; }
.page-head h1 { font-size: 22px; margin: 0 0 6px; }
.page-head p { color: var(--text-secondary); margin: 0; }
.content-grid { display: grid; gap: 16px; grid-template-columns: minmax(0, 1fr) 380px; }
.card-header { align-items: center; display: flex; justify-content: space-between; }
.card-header h2 { font-size: 16px; margin: 0; }
.card-header span { color: var(--text-secondary); font-size: 13px; }
.announcement-list { display: grid; gap: 12px; min-height: 180px; }
.announcement-card {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 14px;
}
.announcement-card.expired {
  opacity: 0.6;
}
.announcement-card header { align-items: center; display: flex; gap: 10px; flex-wrap: wrap; }
.announcement-card h3 { font-size: 16px; margin: 0; }
.time { color: var(--text-muted); font-size: 12px; margin: 6px 0; }
.time-divider { margin: 0 6px; }
.content { color: var(--text-secondary); margin: 0; white-space: pre-wrap; word-break: break-word; }
.card-actions,
.form-actions { display: flex; flex-wrap: wrap; gap: 8px; }

@media (max-width: 980px) {
  .content-grid { grid-template-columns: 1fr; }
}
</style>
