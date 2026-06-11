<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { Close, Message, Promotion, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getConversations, getMessagesWithMerchant, sendMessage, listMerchantOrdersByUser, getMyMerchant } from '../api/clas'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'

// ── State ──
const merchant = ref(null)
const merchantLoading = ref(true)
const conversations = ref([])
const activeUserId = ref(null)
const messages = ref([])
const loading = ref(false)
const polling = ref(null)
const draft = ref('')
const sending = ref(false)
const bodyRef = ref(null)

// Order history panel
const userOrders = ref([])
const userOrdersLoading = ref(false)
const showUserOrders = ref(false)

const orderStatusLabel = {
  PENDING_PAYMENT: '待支付', PAID: '已支付', ACCEPTED: '已接单',
  COMPLETED: '已完成', CANCELED: '已取消', REJECTED: '已拒单',
  REFUNDED: '已退款', REFUND_PENDING: '退款中'
}

// ── Computed ──
const activeLabel = computed(() => {
  if (!activeUserId.value) return '请选择用户'
  return `用户 ${String(activeUserId.value).slice(-4)}`
})

// ── Init ──
onMounted(async () => {
  try {
    merchant.value = await getMyMerchant()
  } catch {
    merchant.value = null
  } finally {
    merchantLoading.value = false
  }
  await loadConversations()
  startPolling()
})

onUnmounted(() => stopPolling())

// ── Data loading ──
async function loadConversations() {
  try {
    conversations.value = await getConversations()
  } catch {
    conversations.value = []
  }
}

async function loadMessages() {
  if (!merchant.value?.id || !activeUserId.value) {
    messages.value = []
    return
  }
  loading.value = true
  try {
    messages.value = await getMessagesWithMerchant(merchant.value.id, activeUserId.value)
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
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

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}

// ── Polling ──
function startPolling() {
  stopPolling()
  polling.value = window.setInterval(() => {
    loadMessages()
    loadConversations()
  }, 3000)
}

function stopPolling() {
  if (polling.value) {
    window.clearInterval(polling.value)
    polling.value = null
  }
}

// ── Actions ──
async function selectUser(userId) {
  activeUserId.value = userId
  showUserOrders.value = false
  userOrders.value = []
  await loadMessages()
}

async function toggleUserOrders(userId) {
  if (showUserOrders.value) {
    showUserOrders.value = false
    return
  }
  showUserOrders.value = true
  await loadUserOrders(userId)
}

async function send() {
  if (!draft.value.trim() || sending.value || !activeUserId.value || !merchant.value?.id) return
  sending.value = true
  try {
    await sendMessage({
      merchantId: merchant.value.id,
      userId: activeUserId.value,
      content: draft.value.trim()
    })
    draft.value = ''
    await loadMessages()
    await loadConversations()
    await scrollToBottom()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '发送失败')
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

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

function formatPrice(fen) {
  if (fen == null) return '0.00'
  return (fen / 100).toFixed(2)
}

function avatarText(item) {
  return String(item.userId || 'U').slice(-2)
}

function avatarUserInitial(userId) {
  return String(userId || 'U').slice(-2)
}

watch(() => messages.value.length, () => scrollToBottom())
</script>

<template>
  <MerchantWorkspaceShell
    :merchant="merchant"
    :loading="merchantLoading"
    active-module="messages"
    @merchant-updated="onMerchantProfileSaved"
  >
    <div class="main-work-area">
        <div v-if="!merchant" class="panel state-panel">
          <el-skeleton :rows="10" animated />
        </div>

        <div v-else class="chat-panel">
          <!-- Conversation list (left sidebar) -->
          <section class="conversation-pane">
            <div class="pane-head">
              <el-icon><Message /></el-icon>
              <strong>客户消息</strong>
            </div>
            <button
              v-for="item in conversations"
              :key="item.userId"
              type="button"
              :class="['conversation-item', { active: item.userId === activeUserId }]"
              @click="selectUser(item.userId)"
            >
              <span
                class="conversation-avatar clickable"
                @click.stop="toggleUserOrders(item.userId)"
                title="点击查看用户订单"
              >{{ avatarText(item) }}</span>
              <span class="conversation-meta">
                <strong>用户 {{ String(item.userId).slice(-4) }}</strong>
                <small>{{ item.lastMessage || '暂无消息' }}</small>
              </span>
            </button>
            <div v-if="!conversations.length" class="pane-empty">暂无客户消息</div>
          </section>

          <!-- Chat area (right side) -->
          <section class="chat-pane">
            <template v-if="activeUserId">
              <header class="chat-header">
                <div
                  class="active-avatar clickable"
                  @click="toggleUserOrders(activeUserId)"
                  title="点击查看用户订单历史"
                >
                  <el-icon><User /></el-icon>
                </div>
                <div>
                  <h3>{{ activeLabel }}</h3>
                  <p>回复客户咨询</p>
                </div>
              </header>

              <div ref="bodyRef" class="chat-body" v-loading="loading">
                <div v-if="!messages.length" class="chat-empty">发送第一条消息开始沟通</div>
                <div
                  v-for="msg in messages"
                  :key="msg.id"
                  :class="['message-bubble', msg.senderRole === 'MERCHANT' ? 'mine' : 'other']"
                >
                  <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                  <p>{{ msg.content }}</p>
                </div>
              </div>

              <div v-if="showUserOrders" class="orders-panel">
                <header>
                  <h4>{{ activeLabel }} 的订单</h4>
                  <button class="icon-close" @click="showUserOrders = false">
                    <el-icon><Close /></el-icon>
                  </button>
                </header>
                <div v-loading="userOrdersLoading" class="orders-body">
                  <el-empty v-if="!userOrders.length && !userOrdersLoading" description="该用户暂无订单" />
                  <div v-for="entry in userOrders" :key="entry.order.id" class="order-card">
                    <div class="order-head">
                      <span class="order-id">订单 #{{ entry.order.id }}</span>
                      <el-tag size="small">{{ orderStatusLabel[entry.order.status] || entry.order.status }}</el-tag>
                      <span class="order-price">&yen;{{ formatPrice(entry.order.totalPrice) }}</span>
                    </div>
                    <div class="order-items">
                      <span v-for="item in entry.items" :key="item.id">
                        {{ item.productName || item.name }} x{{ item.quantity }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <footer class="chat-footer">
                <textarea
                  v-model="draft"
                  rows="2"
                  placeholder="输入消息"
                  :disabled="sending"
                  @keydown="onKeydown"
                />
                <button class="send-btn" :disabled="!draft.trim() || sending" @click="send">
                  <el-icon><Promotion /></el-icon>
                </button>
              </footer>
            </template>

            <div v-else class="no-selection">
              <el-empty description="请从左侧选择一位客户开始对话" />
            </div>
          </section>
        </div>
    </div>
  </MerchantWorkspaceShell>
</template>

<style scoped>
.main-work-area {
  min-width: 0;
}

.panel {
  border-radius: 12px;
}

.state-panel {
  padding: 20px;
}

/* ── Chat two-panel layout ── */
.chat-panel {
  background: #fff;
  border-radius: 12px;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  height: calc(100vh - 120px);
  min-height: 500px;
  overflow: hidden;
}

/* ── Conversation pane ── */
.conversation-pane {
  background: #f8fafc;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 14px 8px;
}

.pane-head {
  align-items: center;
  color: #334155;
  display: flex;
  gap: 6px;
  font-size: 13px;
  margin: 0 4px 12px;
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
  transition: background .2s;
}

.conversation-item:hover,
.conversation-item.active {
  background: #fff7ed;
}

.conversation-avatar {
  align-items: center;
  background: linear-gradient(135deg, #f97316, #0f766e);
  border-radius: 50%;
  color: #fff;
  display: flex;
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
  height: 34px;
  justify-content: center;
  width: 34px;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  transform: scale(1.08);
  transition: transform 0.2s ease;
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

.conversation-meta small {
  color: #94a3b8;
  font-size: 12px;
}

.pane-empty {
  color: #94a3b8;
  font-size: 12px;
  padding: 20px 8px;
  text-align: center;
}

/* ── Chat pane ── */
.chat-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  gap: 10px;
  min-height: 68px;
  padding: 12px 14px;
}

.active-avatar {
  align-items: center;
  background: linear-gradient(135deg, #f97316, #0f766e);
  background-position: center;
  background-size: cover;
  border-radius: 50%;
  color: #fff;
  display: flex;
  flex: 0 0 auto;
  font-weight: 700;
  height: 42px;
  justify-content: center;
  width: 42px;
}

.chat-header h3 {
  font-size: 15px;
  line-height: 1.2;
  margin: 0;
}

.chat-header p {
  color: #94a3b8;
  font-size: 12px;
  margin: 4px 0 0;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

.chat-empty, .no-selection {
  align-items: center;
  display: flex;
  flex: 1;
  justify-content: center;
}

.chat-empty {
  color: #94a3b8;
  font-size: 13px;
  margin-top: 0;
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

/* ── Orders panel ── */
.orders-panel {
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  max-height: 45%;
  overflow: hidden;
}

.orders-panel header {
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  padding: 10px 14px;
}

.orders-panel header h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
}

.icon-close {
  align-items: center;
  background: #f1f5f9;
  border: 0;
  border-radius: 8px;
  color: #475569;
  cursor: pointer;
  display: inline-flex;
  height: 34px;
  justify-content: center;
  width: 34px;
}

.orders-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px;
}

.order-card {
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 8px;
  padding: 10px;
}

.order-head {
  align-items: center;
  display: flex;
  gap: 8px;
  font-size: 13px;
}

.order-head .order-id {
  color: #64748b;
}

.order-head .order-price {
  color: #f56c6c;
  font-weight: 600;
  margin-left: auto;
}

.order-items {
  color: #94a3b8;
  display: flex;
  flex-wrap: wrap;
  font-size: 12px;
  gap: 6px;
  margin-top: 6px;
}

/* ── Chat input ── */
.chat-footer {
  align-items: flex-end;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
  padding: 10px;
}

.chat-footer textarea {
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

.chat-footer textarea:focus {
  border-color: #f97316;
}

.send-btn {
  align-items: center;
  background: #f97316;
  border: 0;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  height: 42px;
  justify-content: center;
  width: 42px;
}

.send-btn:disabled {
  background: #fed7aa;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .chat-panel {
    grid-template-columns: 120px minmax(0, 1fr);
  }
}
</style>
