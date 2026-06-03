<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  to: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: '返回'
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
  <button class="back-btn secondary" type="button" @click="goBack">{{ label }}</button>
</template>

<style scoped>
.back-btn {
  margin-bottom: 14px;
}
</style>
