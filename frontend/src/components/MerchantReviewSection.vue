<script setup>
import { onMounted, ref } from 'vue'
import {
  addReviewComment,
  deleteMerchantReply,
  deleteReview,
  deleteReviewReply,
  listReviewsByMerchant,
  replyReview,
  reportReview,
  reportReviewReply,
  requestReviewDelete,
  voteReviewTarget
} from '../api/clas'
import { currentRole } from '../api/session'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  merchantId: { type: [Number, String], required: true },
  showMerchantActions: { type: Boolean, default: false }
})

const reviews = ref([])
const replyDrafts = ref({})
const commentDrafts = ref({})
const loading = ref(false)

function avatarText(name) {
  return (name || '?').slice(0, 1).toUpperCase()
}

function avatarStyle(avatar) {
  if (avatar) {
    return { backgroundImage: `url(${avatar})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
}

async function load() {
  loading.value = true
  try {
    reviews.value = await listReviewsByMerchant(props.merchantId)
  } finally {
    loading.value = false
  }
}

async function vote(targetType, targetId, voteType) {
  await voteReviewTarget(targetType, targetId, voteType)
  await load()
}

async function submitMerchantReply(review) {
  const text = replyDrafts.value[review.id]
  if (!text?.trim()) return
  await replyReview(review.id, text.trim())
  replyDrafts.value[review.id] = ''
  ElMessage.success('回复成功')
  await load()
}

async function submitComment(review) {
  const text = commentDrafts.value[review.id]
  if (!text?.trim()) return
  await addReviewComment(review.id, { content: text.trim() })
  commentDrafts.value[review.id] = ''
  await load()
}

async function hideOrDelete(review) {
  await deleteReview(review.id)
  ElMessage.success(review.mine ? '评价已删除' : '该评价已对当前账号隐藏')
  await load()
}

async function removeMerchantReply(review) {
  await deleteMerchantReply(review.id)
  ElMessage.success('商家回复已删除')
  await load()
}

async function requestDelete(review) {
  const reason = window.prompt('请填写申请删除该评价的原因')
  if (!reason?.trim()) return
  await requestReviewDelete(review.id, reason.trim())
  ElMessage.success('删除申请已提交，等待管理员审核')
}

async function reportComment(review) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请描述该评价的不当之处', '举报评价', {
      confirmButtonText: '提交举报',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：辱骂、虚假信息等',
      inputValidator: (value) => !!value?.trim() || '请填写举报原因'
    })
    await reportReview(review.id, reason.trim())
    ElMessage.success('举报已提交，管理员将审核是否删除该评价')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '举报失败')
    }
  }
}

async function reportReplyComment(reply) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请描述该评论的不当之处', '举报评论', {
      confirmButtonText: '提交举报',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：人身攻击、广告引流等',
      inputValidator: (value) => !!value?.trim() || '请填写举报原因'
    })
    await reportReviewReply(reply.id, reason.trim())
    ElMessage.success('举报已提交，管理员将审核是否删除该评论')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '举报失败')
    }
  }
}

async function removeReply(reply) {
  await deleteReviewReply(reply.id)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="review-section panel">
    <div class="section-head">
      <h2>店铺评论</h2>
      <span class="count">{{ reviews.length }} 条</span>
    </div>

    <el-empty v-if="!loading && !reviews.length" description="暂无评论" />

    <article v-for="review in reviews" :key="review.id" class="review-card">
      <div class="review-head">
        <div class="avatar" :style="avatarStyle(review.avatar)">{{ review.avatar ? '' : avatarText(review.displayName) }}</div>
        <div>
          <strong>{{ review.displayName || review.userId }}</strong>
          <p>{{ review.score }} 星 · 订单 #{{ review.orderId }}</p>
        </div>
      </div>
      <p class="content">{{ review.content || '（无文字评价）' }}</p>
      <div v-if="review.images?.length" class="images">
        <img v-for="(img, idx) in review.images" :key="idx" :src="img" alt="评价图片" />
      </div>
      <div class="actions">
        <el-button text @click="vote('REVIEW', review.id, 'LIKE')">赞 {{ review.likeCount || 0 }}</el-button>
        <el-button text @click="vote('REVIEW', review.id, 'DISLIKE')">踩 {{ review.dislikeCount || 0 }}</el-button>
        <el-button text type="danger" @click="hideOrDelete(review)">{{ review.mine ? '删除' : '隐藏' }}</el-button>
        <el-button v-if="currentRole() && !review.mine && !showMerchantActions" text type="warning" @click="reportComment(review)">举报</el-button>
        <el-button v-if="showMerchantActions" text type="warning" @click="requestDelete(review)">申请删评</el-button>
      </div>

      <div v-if="review.merchantReply" class="merchant-reply">
        <strong>商家回复</strong>
        <p>{{ review.merchantReply }}</p>
        <div class="actions">
          <el-button text @click="vote('MERCHANT_REPLY', review.id, 'LIKE')">赞 {{ review.merchantReplyLikeCount || 0 }}</el-button>
          <el-button text @click="vote('MERCHANT_REPLY', review.id, 'DISLIKE')">踩 {{ review.merchantReplyDislikeCount || 0 }}</el-button>
          <el-button v-if="showMerchantActions" text type="danger" @click="removeMerchantReply(review)">删除回复</el-button>
        </div>
      </div>

      <div v-else-if="showMerchantActions" class="reply-box">
        <el-input v-model="replyDrafts[review.id]" placeholder="回复顾客评价" />
        <el-button type="primary" @click="submitMerchantReply(review)">商家回复</el-button>
      </div>

      <div v-for="reply in review.replies || []" :key="reply.id" class="nested-reply">
        <div class="review-head">
          <div class="avatar small" :style="avatarStyle(reply.avatar)">{{ reply.avatar ? '' : avatarText(reply.displayName) }}</div>
          <strong>{{ reply.displayName }}</strong>
        </div>
        <p>{{ reply.content }}</p>
        <div class="actions">
          <el-button text @click="vote('REPLY', reply.id, 'LIKE')">赞 {{ reply.likeCount || 0 }}</el-button>
          <el-button text @click="vote('REPLY', reply.id, 'DISLIKE')">踩 {{ reply.dislikeCount || 0 }}</el-button>
          <el-button v-if="reply.mine" text type="danger" @click="removeReply(reply)">删除</el-button>
          <el-button v-else-if="currentRole() && !showMerchantActions" text type="warning" @click="reportReplyComment(reply)">举报</el-button>
        </div>
      </div>

      <div v-if="currentRole()" class="reply-box">
        <el-input v-model="commentDrafts[review.id]" placeholder="回复这条评论" />
        <el-button @click="submitComment(review)">发表评论</el-button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.review-section { margin-top: 18px; }
.section-head { align-items: center; display: flex; justify-content: space-between; margin-bottom: 12px; }
.count { color: var(--text-secondary); }
.review-card { border-top: 1px solid var(--border-light); padding: 16px 0; }
.review-head { align-items: center; display: flex; gap: 12px; margin-bottom: 8px; }
.avatar {
  align-items: center; background: #dbeafe; border-radius: 50%; color: #1d4ed8;
  display: flex; height: 42px; justify-content: center; width: 42px; font-weight: 700;
}
.avatar.small { height: 32px; width: 32px; font-size: 12px; }
.content { margin: 8px 0; }
.images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.images img { border-radius: 8px; height: 88px; object-fit: cover; width: 88px; }
.actions { display: flex; flex-wrap: wrap; gap: 4px; }
.merchant-reply, .nested-reply, .reply-box {
  background: #f8fafc; border-radius: 10px; margin-top: 10px; padding: 10px 12px;
}
.reply-box { display: grid; gap: 8px; }
</style>
