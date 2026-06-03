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
  <section class="panel">
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
    <p class="message">{{ message }}</p>
  </section>

  <section class="list">
    <article class="announcement-card" v-for="item in announcements" :key="item.id">
      <div>
        <h2>{{ item.title }}</h2>
        <p class="announcement-time">{{ item.createTime?.replace('T', ' ') }}</p>
        <p>{{ item.content }}</p>
      </div>
    </article>
    <p v-if="!announcements.length && !message" class="empty">暂无公告</p>
  </section>
</template>

<style scoped>
.announcement-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.announcement-card h2 {
  font-size: 18px;
  margin: 0 0 6px;
}

.announcement-time {
  color: #94a3b8;
  font-size: 13px;
  margin: 0 0 8px;
}

.announcement-card p:last-child {
  color: #667085;
  margin: 0;
}

.empty {
  color: #667085;
  margin: 0;
  text-align: center;
}
</style>
