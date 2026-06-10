<script setup>
import { computed } from 'vue'
import { formatFen } from '../utils/formatters'

const props = defineProps({
  amount: {
    type: Number,
    default: 0
  },
  symbol: {
    type: Boolean,
    default: true
  },
  decimals: {
    type: Number,
    default: 2
  },
  negative: {
    type: Boolean,
    default: false
  }
})

const text = computed(() => {
  const value = formatFen(props.amount, { symbol: props.symbol, decimals: props.decimals })
  return props.negative ? `-${value}` : value
})
</script>

<template>
  <span class="money-text">{{ text }}</span>
</template>
