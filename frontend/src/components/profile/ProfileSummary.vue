<script setup>
defineProps({
  cards: {
    type: Array,
    default: () => []
  },
  activeTab: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select'])
</script>

<template>
  <section class="profile-summary">
    <button
      v-for="item in cards"
      :key="item.label"
      type="button"
      class="summary-item"
      :class="{ active: activeTab === item.targetTab }"
      @click="emit('select', item)"
    >
      <strong>{{ item.value }}</strong>
      <span>{{ item.label }}</span>
    </button>
  </section>
</template>

<style scoped>
.profile-summary {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.summary-item {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 4px;
  padding: 16px;
  text-align: left;
}
.summary-item:hover,
.summary-item:focus-visible {
  border-color: #2563eb;
  outline: none;
  transform: translateY(-1px);
}
.summary-item.active { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }
.summary-item strong { font-size: 24px; }
.summary-item span { color: var(--text-secondary); font-size: 13px; }

@media (max-width: 760px) {
  .profile-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
