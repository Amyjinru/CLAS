<script setup>
import { onMounted, ref } from 'vue'
import {
  addReviewComment,
  deleteReview,
  deleteReviewReply,
  listReviewsByMerchant,
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
const commentDrafts = ref({})
const replyTargets = ref({})
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
  if (!currentRole()) {
    ElMessage.warning('请先登录后再点赞或点踩')
    return
  }
  try {
    await voteReviewTarget(targetType, targetId, voteType)
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '投票失败')
  }
}

async function submitComment(review) {
  const text = commentDrafts.value[review.id]
  if (!text?.trim()) return
  try {
    const target = replyTargets.value[review.id]
    await addReviewComment(review.id, {
      content: text.trim(),
      parentReplyId: target?.parentReplyId ?? null
    })
    commentDrafts.value[review.id] = ''
    delete replyTargets.value[review.id]
    ElMessage.success('评论已发布')
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '评论失败')
  }
}

function selectReplyTarget(review, reply = null) {
  if (!currentRole()) {
    ElMessage.warning('请先登录后再回复')
    return
  }
  const displayName = reply
    ? reply.displayName || reply.userId
    : review.displayName || review.userId
  replyTargets.value = {
    [review.id]: {
      parentReplyId: reply?.id ?? null,
      displayName
    }
  }
}

function clearReplyTarget(reviewId) {
  if (replyTargets.value[reviewId]) {
    delete replyTargets.value[reviewId]
    commentDrafts.value[reviewId] = ''
  }
}

function replyTargetName(review, reply) {
  if (reply.parentReplyId) {
    const parent = (review.replies || []).find((item) => item.id === reply.parentReplyId)
    return parent?.displayName || parent?.userId || null
  }
  return review.displayName || review.userId || null
}

function replyPlaceholder(review) {
  const target = replyTargets.value[review.id]
  return target ? `回复 ${target.displayName}` : '回复这条评论'
}

async function hideOrDelete(review) {
  await deleteReview(review.id)
  ElMessage.success(review.mine ? '评价已删除' : '该评价已对当前账号隐藏')
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
        <img v-for="(img, idx) in review.images" :key="idx" :src="img" alt="评价图片" loading="lazy" />
      </div>
      <div class="actions">
        <el-button text class="vote-action" @click="vote('REVIEW', review.id, 'LIKE')">👍 赞 {{ review.likeCount || 0 }}</el-button>
        <el-button text class="vote-action" @click="vote('REVIEW', review.id, 'DISLIKE')">👎 踩 {{ review.dislikeCount || 0 }}</el-button>
        <el-button text class="danger-action" @click="hideOrDelete(review)">{{ review.mine ? '删除' : '隐藏' }}</el-button>
        <el-button v-if="currentRole() && !review.mine && !showMerchantActions" text class="report-action" @click="reportComment(review)">举报</el-button>
        <el-button v-if="showMerchantActions" text class="report-action" @click="requestDelete(review)">申请删评</el-button>
        <el-button text class="reply-action" @click="selectReplyTarget(review)">回复</el-button>
      </div>

      <div v-for="reply in review.replies || []" :key="reply.id" class="nested-reply">
        <div class="review-head">
          <div class="avatar small" :style="avatarStyle(reply.avatar)">{{ reply.avatar ? '' : avatarText(reply.displayName) }}</div>
          <strong>{{ reply.displayName }}</strong>
        </div>
        <p>
          <span v-if="replyTargetName(review, reply)" class="reply-prefix">回复{{ replyTargetName(review, reply) }}:</span>
          {{ reply.content }}
        </p>
        <div class="actions">
          <el-button text class="vote-action" @click="vote('REPLY', reply.id, 'LIKE')">👍 赞 {{ reply.likeCount || 0 }}</el-button>
          <el-button text class="vote-action" @click="vote('REPLY', reply.id, 'DISLIKE')">👎 踩 {{ reply.dislikeCount || 0 }}</el-button>
          <el-button v-if="reply.mine" text class="danger-action" @click="removeReply(reply)">删除</el-button>
          <el-button v-else-if="currentRole() && !showMerchantActions" text class="report-action" @click="reportReplyComment(reply)">举报</el-button>
          <el-button text class="reply-action" @click="selectReplyTarget(review, reply)">回复</el-button>
        </div>
      </div>

      <div v-if="replyTargets[review.id]" class="reply-box">
        <div class="reply-target">
          <span>回复 {{ replyTargets[review.id].displayName }}</span>
          <el-button text @click="clearReplyTarget(review.id)">取消</el-button>
        </div>
        <el-input v-model="commentDrafts[review.id]" :placeholder="replyPlaceholder(review)" />
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
  align-items: center; background: var(--color-accent-light); border-radius: 50%; color: var(--color-accent);
  display: flex; height: 42px; justify-content: center; width: 42px; font-weight: 700;
}
.avatar.small { height: 32px; width: 32px; font-size: 12px; }
.content { margin: 8px 0; }
.images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.images img { border-radius: var(--radius-sm); height: 88px; object-fit: cover; width: 88px; }
.actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.actions :deep(.el-button.is-text) {
  align-items: center;
  display: inline-flex;
  justify-content: center;
  margin: 0;
  min-height: 32px;
  padding: 0 12px;
}

.actions :deep(.vote-action.el-button.is-text) {
  background: #fffaf4;
  border: 1px solid #f4dfc5;
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-weight: 600;
}

.actions :deep(.vote-action.el-button.is-text:hover),
.actions :deep(.vote-action.el-button.is-text:focus) {
  background: #fff3e0;
  color: var(--text-primary);
}

.actions :deep(.danger-action.el-button.is-text) {
  background: #ef4444;
  border-radius: var(--radius-sm);
  color: #fff;
  font-weight: 700;
}

.actions :deep(.danger-action.el-button.is-text:hover),
.actions :deep(.danger-action.el-button.is-text:focus) {
  background: #dc2626;
  color: #fff;
}

.actions :deep(.report-action.el-button.is-text) {
  background: #f59e0b;
  border-radius: var(--radius-sm);
  color: #fff;
  font-weight: 700;
}

.actions :deep(.report-action.el-button.is-text:hover),
.actions :deep(.report-action.el-button.is-text:focus) {
  background: #d97706;
  color: #fff;
}

.actions :deep(.reply-action.el-button.is-text) {
  background: transparent;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-weight: 600;
  min-height: 32px;
  padding: 0 12px;
}
.reply-action { margin-left: auto; }
.reply-prefix { color: var(--text-secondary); font-weight: 600; margin-right: 4px; }
.nested-reply, .reply-box {
  background: var(--clas-warm-50); border-radius: var(--radius-sm); margin-top: 10px; padding: 10px 12px;
}
.reply-box { display: grid; gap: 8px; }
.reply-target { align-items: center; color: var(--text-secondary); display: flex; justify-content: space-between; }
</style>
