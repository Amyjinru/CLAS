<script setup>
defineProps({
  active: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle'])
</script>

<template>
  <el-switch
    :model-value="active"
    :disabled="disabled"
    active-color="#13ce66"
    inactive-color="#ff4949"
    @change="emit('toggle')"
  />
</template>
