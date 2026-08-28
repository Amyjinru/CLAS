<script setup>
import BackButton from '../../components/BackButton.vue'
import AnnouncementList from '../../components/AnnouncementList.vue'
</script>

<template>
  <BackButton to="/home" label="返回首页" />
  <AnnouncementList title="平台公告" description="查看平台最新通知与活动信息。" />
</template>
