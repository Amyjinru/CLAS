<script setup>
import BackButton from '../../components/BackButton.vue'
import AnnouncementList from '../../components/AnnouncementList.vue'
</script>

<template>
  <BackButton to="/merchant-console" label="返回商家工作台" />
  <AnnouncementList title="平台公告" description="商家端可查看平台发布的运营通知与规则说明。" />
</template>
