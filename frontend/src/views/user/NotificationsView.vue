<script setup>

import { computed, onMounted, ref } from 'vue'

import { useRouter } from 'vue-router'

import {

  deleteAllNotifications,

  deleteNotification,

  listNotifications,

  markNotificationRead

} from '../../api/clas'

import { ElMessage, ElMessageBox } from 'element-plus'



const router = useRouter()

const notifications = ref([])

const loading = ref(false)



const unreadCount = computed(() => notifications.value.filter((item) => !item.readFlag).length)



async function load() {

  loading.value = true

  try {

    notifications.value = await listNotifications()

  } finally {

    loading.value = false

  }

}



async function readNotification(id) {

  await markNotificationRead(id)

  await load()

}



async function markAllRead() {

  const unread = notifications.value.filter((item) => !item.readFlag)

  await Promise.all(unread.map((item) => markNotificationRead(item.id)))

  ElMessage.success('已全部标记为已读')

  await load()

}



async function removeNotification(id) {

  try {

    await ElMessageBox.confirm('确定删除这条通知吗？', '删除通知', {

      confirmButtonText: '删除',

      cancelButtonText: '取消',

      type: 'warning'

    })

    await deleteNotification(id)

    ElMessage.success('通知已删除')

    await load()

  } catch (error) {

    if (error !== 'cancel') {

      ElMessage.error('删除失败')

    }

  }

}



async function removeAllNotifications() {

  if (!notifications.value.length) return

  try {

    await ElMessageBox.confirm('确定删除全部通知吗？此操作不可恢复。', '清空通知', {

      confirmButtonText: '全部删除',

      cancelButtonText: '取消',

      type: 'warning'

    })

    await deleteAllNotifications()

    ElMessage.success('已全部删除')

    await load()

  } catch (error) {

    if (error !== 'cancel') {

      ElMessage.error('删除失败')

    }

  }

}



onMounted(load)

</script>



<template>

  <div class="user-page notifications-page-wrap">

    <button class="back-compact" type="button" @click="router.push('/profile')">← 返回</button>



    <section class="panel notifications-page">

      <div class="page-head">

        <div>

          <h1>全部通知</h1>

          <p>共 {{ notifications.length }} 条 · 未读 {{ unreadCount }} 条</p>

        </div>

        <div class="head-actions">

          <el-button v-if="unreadCount" text type="primary" @click="markAllRead">全部标记已读</el-button>

          <el-button v-if="notifications.length" class="btn-delete-soft" size="small" type="danger" @click="removeAllNotifications">删除全部</el-button>

        </div>

      </div>



      <el-skeleton v-if="loading" :rows="4" animated />



      <el-empty v-else-if="!notifications.length" description="暂无通知" />



      <div v-else class="notifications-list">

        <article v-for="item in notifications" :key="item.id" class="notice-row">

          <div>

            <strong>{{ item.title }}</strong>

            <p>{{ item.content }}</p>

          </div>

          <div class="row-actions">

            <el-button v-if="!item.readFlag" text type="primary" @click="readNotification(item.id)">标记已读</el-button>

            <el-tag v-else type="info">已读</el-tag>

            <el-button class="btn-delete-soft" size="small" type="danger" @click="removeNotification(item.id)">删除</el-button>

          </div>

        </article>

      </div>

    </section>

  </div>

</template>



<style scoped>
.notifications-page-wrap {
  display: grid;
  gap: 12px;
}

.back-compact {
  align-self: start;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  justify-self: start;
  min-height: 32px;
  padding: 0 12px;
  width: auto;
}

.back-compact:hover {
  background: var(--color-primary-soft);
  border-color: var(--clas-amber-200);
  color: var(--color-primary);
  transform: none;
}

.notifications-page {
  margin-bottom: 0;
  width: 100%;
}

.page-head {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 0 0 6px;
}

.page-head p {
  color: var(--text-secondary);
  margin: 0;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: flex-end;
}

.notifications-list {
  display: grid;
  gap: 0;
}

.notice-row {
  align-items: center;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
}

.notice-row > div:first-child {
  flex: 1;
  min-width: 0;
}

.notice-row p {
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 6px 0 0;
  max-width: 920px;
}

.row-actions {
  align-items: center;
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  justify-content: flex-end;
  flex-shrink: 0;
  min-width: 144px;
}

.row-actions .btn-delete-soft {
  min-width: 56px;
  flex-shrink: 0;
}

@media (min-width: 1024px) {
  .notifications-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 24px;
  }

  .notice-row {
    align-items: flex-start;
    flex-direction: column;
    min-height: 120px;
  }

  .row-actions {
    justify-content: flex-start;
  }
}
</style>


