<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { addReview, getReviewByOrder, reportReview, uploadReviewImages } from '../api/clas'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.orderId))
const score = ref(5)
const content = ref('')
const existingReview = ref(null)
const message = ref('')
const submitting = ref(false)
const reportReason = ref('')
const imageUrls = ref([])
const uploadFiles = ref([])

async function load() {
  try {
    existingReview.value = await getReviewByOrder(orderId.value)
  } catch {
    existingReview.value = null
  }
}

async function handleUpload(option) {
  try {
    const urls = await uploadReviewImages([option.file])
    imageUrls.value = [...imageUrls.value, ...urls].slice(0, 9)
    option.onSuccess(urls)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '图片上传失败')
    option.onError(error)
  }
}

function removeImage(url) {
  imageUrls.value = imageUrls.value.filter((item) => item !== url)
}

async function submit() {
  submitting.value = true
  message.value = ''
  try {
    await addReview({ orderId: orderId.value, score: score.value, content: content.value, images: imageUrls.value })
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

async function report() {
  if (!reportReason.value.trim() || !existingReview.value) return
  await reportReview(existingReview.value.id, reportReason.value.trim())
  message.value = '举报已提交，管理员会尽快处理'
  await load()
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
      <p v-if="existingReview.merchantReply" class="reply">商家回复：{{ existingReview.merchantReply }}</p>
      <p v-if="existingReview.reportStatus !== 'NONE'">举报状态：{{ existingReview.reportStatus }}</p>
      <label>
        举报原因
        <textarea v-model="reportReason" placeholder="例如：评价内容需复核、商家回复不当..." />
      </label>
      <p class="message">{{ message }}</p>
      <button class="secondary" @click="report">提交举报</button>
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
      <div class="upload-block">
        <p>上传图片（最多 9 张）</p>
        <el-upload
          list-type="picture-card"
          :http-request="handleUpload"
          :show-file-list="false"
          accept="image/*"
          :disabled="imageUrls.length >= 9"
        >
          <span v-if="imageUrls.length < 9">+</span>
        </el-upload>
        <div class="preview-list">
          <div v-for="url in imageUrls" :key="url" class="preview-item">
            <img :src="url" alt="预览" loading="lazy" />
            <button type="button" class="remove" @click="removeImage(url)">×</button>
          </div>
        </div>
      </div>
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
.score-display { color: var(--color-accent); font-weight: 700; }
.review-result p { margin-bottom: 10px; }
.reply { background: var(--color-primary-light); border-radius: var(--radius-sm); padding: 10px 12px; }
input[type='range'] { width: 100%; min-height: auto; padding: 0; border: 0; }
.upload-block { margin: 16px 0; }
.preview-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.preview-item { position: relative; }
.preview-item img { border-radius: 8px; height: 88px; object-fit: cover; width: 88px; }
.preview-item .remove {
  background: rgba(0,0,0,.55); border: 0; border-radius: 50%; color: #fff;
  cursor: pointer; height: 22px; position: absolute; right: 4px; top: 4px; width: 22px;
}
</style>
