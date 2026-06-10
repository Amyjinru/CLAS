<script setup>
import { computed } from 'vue'
import { orderStatusMap, statusMeta } from '../utils/status'

const props = defineProps({
  status: {
    type: String,
    default: ''
  },
  map: {
    type: Object,
    default: () => orderStatusMap
  },
  size: {
    type: String,
    default: 'small'
  },
  effect: {
    type: String,
    default: 'light'
  }
})

const meta = computed(() => statusMeta(props.status, props.map))
</script>

<template>
  <el-tag :type="meta.type" :size="size" :effect="effect">
    {{ meta.text }}
  </el-tag>
</template>
