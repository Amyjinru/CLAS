<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { addReview, getReviewByOrder } from '../api/clas'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.orderId))
const score = ref(5)
const content = ref('')
const existingReview = ref(null)
const message = ref('')
const submitting = ref(false)

async function load() {
  try {
    existingReview.value = await getReviewByOrder(orderId.value)
  } catch {
    existingReview.value = null
  }
}

async function submit() {
  submitting.value = true
  message.value = ''
  try {
    await addReview({ orderId: orderId.value, score: score.value, content: content.value })
    message.value = '评价提交成功，商家评分已更新'
    await load()
  } catch (error) {
    message.value = error.response?.data?.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

function goOrders() {
  router.push('/orders')
}

onMounted(load)
</script>

<template>
  <BackButton to="/orders" label="返回我的订单" />

  <section class="panel narrow">
    <h1>订单评价</h1>
    <p>订单号：{{ orderId }}</p>

    <div v-if="existingReview" class="review-result">
      <p>您已评价过该订单</p>
      <p>评分：{{ '★'.repeat(existingReview.score) }}{{ '☆'.repeat(5 - existingReview.score) }}</p>
      <p>{{ existingReview.content || '（无文字评价）' }}</p>
      <button class="secondary" @click="goOrders">返回订单列表</button>
    </div>

    <template v-else>
      <label>
        评分（1-5 星）
        <input v-model.number="score" type="range" min="1" max="5" step="1" />
        <span class="score-display">{{ score }} 星</span>
      </label>
      <label>
        评价内容
        <textarea v-model="content" placeholder="分享您的用餐体验..." />
      </label>
      <p class="message">{{ message }}</p>
      <div class="toolbar">
        <button :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '提交评价' }}
        </button>
        <button class="secondary" @click="goOrders">取消</button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.score-display {
  color: #2563eb;
  font-weight: 700;
}

.review-result p {
  margin-bottom: 10px;
}

input[type='range'] {
  width: 100%;
  min-height: auto;
  padding: 0;
  border: 0;
}
</style>
