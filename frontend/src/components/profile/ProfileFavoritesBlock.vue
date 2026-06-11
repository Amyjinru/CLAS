<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  favorites: { type: Array, default: () => [] },
  actionId: { type: [Number, String], default: null }
})

defineEmits(['remove'])
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>收藏</h2>
        <p>常用商家和再次购买入口集中在这里</p>
      </div>
      <RouterLink class="button secondary" to="/home">浏览商家</RouterLink>
    </div>

    <el-empty v-if="!favorites.length" description="暂无收藏商家">
      <RouterLink class="button secondary" to="/home">去首页浏览</RouterLink>
    </el-empty>

    <article v-for="item in favorites" v-else :key="item.id" class="list-row">
      <div>
        <strong>{{ item.merchantName }}</strong>
        <p>{{ item.category || '未分类' }} · {{ item.address || '暂无地址' }}</p>
      </div>
      <div class="row-actions">
        <RouterLink class="button secondary" :to="`/merchant/${item.id}`">进入店铺</RouterLink>
        <el-button text type="danger" :loading="actionId === item.id" @click="$emit('remove', item.id)">取消收藏</el-button>
      </div>
    </article>
  </div>
</template>

<style scoped>
.section-head { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 16px; }
.section-head h2 { margin: 0; }
.section-head p,
.list-row p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.list-row { align-items: center; border-top: 1px solid var(--border-light); display: flex; justify-content: space-between; padding: 14px 0; }
.row-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
@media (max-width: 900px) {
  .list-row { align-items: flex-start; flex-direction: column; }
  .row-actions { justify-content: flex-start; }
}
@media (max-width: 640px) {
  .section-head { display: grid; }
}
</style>
