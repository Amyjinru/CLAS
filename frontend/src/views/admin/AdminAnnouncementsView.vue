<script setup>
import { onMounted, ref } from 'vue'
import { createAnnouncement, deleteAnnouncement, listAnnouncements } from '../../api/clas'

const announcements = ref([])
const title = ref('')
const content = ref('')
const message = ref('')

async function load() {
  announcements.value = await listAnnouncements()
}

async function submit() {
  if (!title.value.trim() || !content.value.trim()) {
    message.value = '请填写标题和内容'
    return
  }
  await createAnnouncement({ title: title.value, content: content.value })
  title.value = ''
  content.value = ''
  message.value = '公告发布成功'
  await load()
}

async function remove(id) {
  await deleteAnnouncement(id)
  message.value = '公告已删除'
  await load()
}

onMounted(load)
</script>

<template>
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
      <button class="secondary" @click="remove(item.id)">删除</button>
    </article>
  </section>

  <section class="panel narrow">
    <h2>发布公告</h2>
    <label>
      标题
      <input v-model="title" placeholder="请输入公告标题" />
    </label>
    <label>
      内容
      <textarea v-model="content" placeholder="请输入公告内容" />
    </label>
    <p class="message">{{ message }}</p>
    <button @click="submit">发布公告</button>
  </section>
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
</style>
