<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  to: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: '← 返回'
  }
})

const router = useRouter()

function goBack() {
  if (props.to) {
    router.push(props.to)
    return
  }
  router.back()
}
</script>

<template>
  <button class="back-btn" type="button" @click="goBack">{{ label }}</button>
</template>

<style scoped>
.back-btn {
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

.back-btn:hover {
  background: var(--color-primary-soft);
  border-color: var(--clas-amber-200);
  color: var(--color-primary);
  transform: none;
  box-shadow: none;
}
</style>
