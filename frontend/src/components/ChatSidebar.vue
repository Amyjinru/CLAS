<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { Close, Message, Promotion, User } from '@element-plus/icons-vue'
import { currentRole } from '../api/session'
import { useChatStore } from '../composables/useChatStore'
import { listMerchantOrdersByUser } from '../api/clas'

const chatStore = useChatStore()
const draft = ref('')
const sending = ref(false)
const bodyRef = ref(null)

const role = computed(() => currentRole())
const isMerchant = computed(() => role.value === 'MERCHANT')
const conversationTab = ref('customer')
const userOrders = ref([])
const userOrdersLoading = ref(false)
const showUserOrders = ref(false)

const activeTitle = computed(() => {
  if (isMerchant.value) return userLabel(chatStore.activeUserId.value)
  const merchant = chatStore.merchantCache.value[chatStore.activeMerchantId.value]
  return merchant?.merchantName || '商家客服'
})
const activeLogo = computed(() => {
  const merchant = chatStore.merchantCache.value[chatStore.activeMerchantId.value]
  return merchant?.logo || ''
})

const orderStatusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '已接单',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REJECTED: '已拒单',
  REFUNDED: '已退款',
  REFUND_PENDING: '退款中'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

function userLabel(userId) {
  if (!userId) return '请选择用户'
  const tail = String(userId).slice(-4)
  return `用户 ${tail}`
}

function itemTitle(item) {
  if (isMerchant.value) return userLabel(item.userId)
  return chatStore.merchantCache.value[item.merchantId]?.merchantName || `商家 #${item.merchantId}`
}

function avatarText(item) {
  if (isMerchant.value) return String(item.userId || 'U').slice(-2)
  return (itemTitle(item) || '店').slice(0, 1)
}

function isActive(item) {
  if (isMerchant.value) return item.userId === chatStore.activeUserId.value
  return item.merchantId === chatStore.activeMerchantId.value
}

function formatPrice(fen) {
  if (fen == null) return '0.00'
  return (fen / 100).toFixed(2)
}

async function send() {
  if (!draft.value.trim() || sending.value) return
  sending.value = true
  try {
    await chatStore.sendActive(draft.value)
    draft.value = ''
    await scrollToBottom()
  } finally {
    sending.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

async function toggleUserOrders(userId) {
  if (showUserOrders.value) {
    showUserOrders.value = false
    return
  }
  showUserOrders.value = true
  await loadUserOrders(userId)
}

async function loadUserOrders(userId) {
  if (!userId) return
  userOrdersLoading.value = true
  try {
    userOrders.value = await listMerchantOrdersByUser(userId)
  } catch {
    userOrders.value = []
  } finally {
    userOrdersLoading.value = false
  }
}

watch(
  () => chatStore.activeUserId.value,
  () => {
    showUserOrders.value = false
    userOrders.value = []
  }
)

watch(
  () => chatStore.activeMessages.value.length,
  () => scrollToBottom()
)
</script>

<template>
  <Teleport to="body">
    <div v-if="chatStore.sidebarOpen.value" class="chat-mask" @click.self="chatStore.closeSidebar">
      <aside class="chat-sidebar">
        <section class="conversation-list">
          <div class="conversation-head">
            <el-icon><Message /></el-icon>
            <strong>{{ isMerchant ? '客户会话' : '客服咨询' }}</strong>
          </div>
          <div v-if="isMerchant" class="conversation-tabs">
            <button
              :class="['tab-btn', { active: conversationTab === 'customer' }]"
              @click="conversationTab = 'customer'"
            >客户消息</button>
            <button
              :class="['tab-btn', { active: conversationTab === 'all' }]"
              @click="conversationTab = 'all'"
            >全部</button>
          </div>
          <button
            v-for="item in chatStore.conversations.value"
            :key="`${item.merchantId}-${item.userId}`"
            type="button"
            :class="['conversation-item', { active: isActive(item) }]"
            @click="chatStore.selectConversation(item)"
          >
            <span
              class="conversation-avatar clickable"
              @click.stop="toggleUserOrders(item.userId)"
              :title="isMerchant ? '查看用户订单' : ''"
            >{{ avatarText(item) }}</span>
            <span class="conversation-meta">
              <strong>{{ itemTitle(item) }}</strong>
              <small>{{ item.lastMessage || '暂无消息' }}</small>
            </span>
          </button>
          <div v-if="!chatStore.conversations.value.length" class="conversation-empty">
            暂无会话
          </div>
        </section>

        <section class="chat-main">
          <header class="chat-main-head">
            <div
              class="active-avatar"
              :class="{ 'has-logo': activeLogo, 'clickable': isMerchant }"
              :style="activeLogo ? { backgroundImage: `url(${activeLogo})` } : null"
              @click="isMerchant && toggleUserOrders(chatStore.activeUserId.value)"
              :title="isMerchant ? '点击查看用户订单历史' : ''"
            >
              <el-icon v-if="!activeLogo && isMerchant"><User /></el-icon>
              <span v-else-if="!activeLogo">{{ activeTitle.slice(0, 1) }}</span>
            </div>
            <div>
              <h3>{{ activeTitle }}</h3>
              <p>{{ isMerchant ? '回复客户咨询' : '与商家客服沟通' }}</p>
            </div>
            <button class="icon-button" type="button" @click="chatStore.closeSidebar" title="关闭">
              <el-icon><Close /></el-icon>
            </button>
          </header>

          <div ref="bodyRef" class="chat-message-body" v-loading="chatStore.loading.value">
            <div v-if="!chatStore.activeMessages.value.length" class="message-empty">
              发送第一条消息开始沟通
            </div>
            <div
              v-for="message in chatStore.activeMessages.value"
              :key="message.id"
              :class="['message-bubble', message.senderRole === role ? 'mine' : 'other']"
            >
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              <p>{{ message.content }}</p>
            </div>
          </div>

          <div v-if="showUserOrders && isMerchant" class="user-orders-panel">
            <header>
              <h4>用户 {{ userLabel(chatStore.activeUserId.value) }} 的订单</h4>
              <button class="icon-button" @click="showUserOrders = false">
                <el-icon><Close /></el-icon>
              </button>
            </header>
            <div v-loading="userOrdersLoading" class="user-orders-body">
              <el-empty v-if="!userOrders.length && !userOrdersLoading" description="该用户暂无订单" />
              <div v-for="entry in userOrders" :key="entry.order.id" class="order-mini-card">
                <div class="order-mini-head">
                  <span class="order-id">订单 #{{ entry.order.id }}</span>
                  <el-tag size="small">{{ orderStatusLabel[entry.order.status] || entry.order.status }}</el-tag>
                  <span class="price">&yen;{{ formatPrice(entry.order.totalPrice) }}</span>
                </div>
                <div class="order-mini-items">
                  <span v-for="item in entry.items" :key="item.id" class="order-item-text">
                    {{ item.productName || item.name }} x{{ item.quantity }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <footer class="chat-input-row">
            <textarea
              v-model="draft"
              rows="2"
              placeholder="输入消息"
              :disabled="sending || (isMerchant && !chatStore.activeUserId.value)"
              @keydown="onKeydown"
            />
            <button class="send-button" type="button" :disabled="!draft.trim() || sending" @click="send">
              <el-icon><Promotion /></el-icon>
            </button>
          </footer>
        </section>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.chat-mask {
  background: rgba(15, 23, 42, 0.3);
  inset: 0;
  position: fixed;
  z-index: 2000;
}

.chat-sidebar {
  background: #fff;
  box-shadow: -16px 0 40px rgba(15, 23, 42, 0.18);
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  height: 100vh;
  margin-left: auto;
  max-width: 92vw;
  width: 430px;
}

.conversation-list {
  background: #f8fafc;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 14px 8px;
}

.conversation-head {
  align-items: center;
  color: #334155;
  display: flex;
  gap: 6px;
  font-size: 13px;
  margin: 0 4px 12px;
}

.conversation-tabs {
  display: flex;
  gap: 4px;
  margin: 0 4px 12px;
}

.tab-btn {
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
  padding: 4px 10px;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: #fff7ed;
  border-color: #f97316;
  color: #f97316;
  font-weight: 600;
}

.conversation-item {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  gap: 8px;
  min-height: 58px;
  padding: 8px;
  text-align: left;
}

.conversation-item:hover,
.conversation-item.active {
  background: #fff7ed;
}

.conversation-avatar,
.active-avatar {
  align-items: center;
  background: linear-gradient(135deg, #f97316, #0f766e);
  border-radius: 50%;
  color: #fff;
  display: flex;
  flex: 0 0 auto;
  font-weight: 700;
  justify-content: center;
}

.conversation-avatar {
  font-size: 12px;
  height: 34px;
  width: 34px;
}

.conversation-meta {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.conversation-meta strong,
.conversation-meta small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-meta strong {
  color: #1f2937;
  font-size: 13px;
}

.conversation-meta small,
.conversation-empty {
  color: #94a3b8;
  font-size: 12px;
}

.conversation-empty {
  padding: 20px 8px;
  text-align: center;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  transform: scale(1.08);
  transition: transform 0.2s ease;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-main-head {
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  gap: 10px;
  min-height: 68px;
  padding: 12px 14px;
}

.active-avatar {
  background-position: center;
  background-size: cover;
  height: 42px;
  width: 42px;
}

.active-avatar.has-logo {
  border: 1px solid #e5e7eb;
}

.chat-main-head h3 {
  font-size: 15px;
  line-height: 1.2;
  margin: 0;
}

.chat-main-head p {
  color: #94a3b8;
  font-size: 12px;
  margin: 4px 0 0;
}

.icon-button,
.send-button {
  align-items: center;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  justify-content: center;
}

.icon-button {
  background: #f1f5f9;
  color: #475569;
  height: 34px;
  margin-left: auto;
  width: 34px;
}

.chat-message-body {
  background: #fff;
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

.message-empty {
  color: #94a3b8;
  font-size: 13px;
  margin-top: 80px;
  text-align: center;
}

.message-bubble {
  display: grid;
  gap: 4px;
  margin-bottom: 12px;
  max-width: 82%;
}

.message-bubble.mine {
  justify-items: end;
  margin-left: auto;
}

.message-bubble.other {
  justify-items: start;
}

.message-time {
  color: #9ca3af;
  font-size: 11px;
}

.message-bubble p {
  background: #f1f5f9;
  border-radius: 12px;
  color: #1f2937;
  line-height: 1.55;
  margin: 0;
  padding: 9px 11px;
  word-break: break-word;
}

.message-bubble.mine p {
  background: #f97316;
  color: #fff;
}

.user-orders-panel {
  background: #fff;
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  max-height: 45%;
  overflow: hidden;
}

.user-orders-panel header {
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  padding: 10px 14px;
}

.user-orders-panel header h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
}

.user-orders-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px;
}

.order-mini-card {
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 8px;
  padding: 10px;
}

.order-mini-head {
  align-items: center;
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.order-mini-head .order-id {
  color: #64748b;
}

.order-mini-head .price {
  color: #f56c6c;
  font-weight: 600;
  margin-left: auto;
}

.order-mini-items {
  color: #94a3b8;
  display: flex;
  flex-wrap: wrap;
  font-size: 12px;
  gap: 6px;
  margin-top: 6px;
}

.order-item-text {
  white-space: nowrap;
}

.chat-input-row {
  align-items: flex-end;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
  padding: 10px;
}

.chat-input-row textarea {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  flex: 1;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.4;
  min-height: 42px;
  outline: none;
  padding: 9px 10px;
  resize: none;
}

.chat-input-row textarea:focus {
  border-color: #f97316;
}

.send-button {
  background: #f97316;
  color: #fff;
  height: 42px;
  width: 42px;
}

.send-button:disabled {
  background: #fed7aa;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .chat-sidebar {
    grid-template-columns: 112px minmax(0, 1fr);
    width: 100vw;
  }
}
</style>
