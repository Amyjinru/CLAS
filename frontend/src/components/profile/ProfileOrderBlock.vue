<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  modules: { type: Array, default: () => [] }
})
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>订单</h2>
        <p>把全部订单、待履约、评价和售后放在一起</p>
      </div>
      <RouterLink class="button secondary" to="/orders">查看全部</RouterLink>
    </div>
    <div class="shortcut-grid">
      <RouterLink v-for="item in modules" :key="item.label" class="shortcut-card" :to="item.to">
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
        <small>{{ item.description }}</small>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.section-head { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 16px; }
.section-head h2 { margin: 0; }
.section-head p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.shortcut-grid { display: grid; gap: 12px; grid-template-columns: repeat(5, minmax(0, 1fr)); }
.shortcut-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  display: grid;
  gap: 8px;
  min-height: 112px;
  padding: 16px;
  text-decoration: none;
}
.shortcut-card:hover { border-color: var(--color-primary); box-shadow: var(--shadow-sm); }
.shortcut-card strong { font-size: 28px; line-height: 1; }
.shortcut-card span { font-weight: 700; }
.shortcut-card small { color: var(--text-secondary); font-size: 13px; }
@media (max-width: 1000px) {
  .shortcut-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .shortcut-grid { grid-template-columns: 1fr; }
  .section-head { display: grid; }
}
</style>
