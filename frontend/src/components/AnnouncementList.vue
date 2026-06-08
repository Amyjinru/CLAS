<script setup>
import { onMounted, ref } from 'vue'
import { listAnnouncements } from '../api/clas'

defineProps({
  title: {
    type: String,
    default: '平台公告'
  },
  description: {
    type: String,
    default: '最新公告按发布时间倒序展示。'
  }
})

const announcements = ref([])
const message = ref('')

onMounted(async () => {
  try {
    announcements.value = await listAnnouncements()
  } catch (error) {
    message.value = error.response?.data?.message || '加载公告失败'
  }
})
</script>

<template>
  <div class="user-page announcements-page">
    <section class="panel announcements-head">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <p class="message">{{ message }}</p>
    </section>

    <section class="user-page-grid-2 announcements-grid">
      <article class="announcement-card" v-for="item in announcements" :key="item.id">
        <h2>{{ item.title }}</h2>
        <p class="announcement-time">{{ item.createTime?.replace('T', ' ') }}</p>
        <p class="announcement-content">{{ item.content }}</p>
      </article>
    </section>

    <el-empty v-if="!announcements.length && !message" description="暂无公告" />
  </div>
</template>

<style scoped>
.announcements-page {
  display: grid;
  gap: 20px;
}

.announcements-head {
  margin-bottom: 0;
}

.announcement-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 22px;
}

.announcement-card h2 {
  font-size: 18px;
  margin: 0 0 6px;
}

.announcement-time {
  color: #94a3b8;
  font-size: 13px;
  margin: 0 0 10px;
}

.announcement-content {
  color: #667085;
  flex: 1;
  line-height: 1.6;
  margin: 0;
}
</style>
