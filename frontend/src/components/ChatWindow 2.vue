<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { sendMessage, getOrderMessages } from '../api/chat'

const props = defineProps({
  orderId: { type: Number, required: true },
  merchantId: { type: Number, required: true },
  merchantName: { type: String, default: '' },
  role: { type: String, required: true },
  orderStatus: { type: String, default: '' },
  orderNumber: { type: [Number, String], default: '' }
})

const messages = ref([])
const input = ref('')
const sending = ref(false)
const polling = ref(null)
const chatPanel = ref(null)

const canChat = () => {
  return props.orderStatus === 'PAID' || props.orderStatus === 'ACCEPTED'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function senderLabel(message) {
  if (message.senderRole === props.role) {
    return props.role === 'USER' ? '我' : '我（商家）'
  }
  if (props.role === 'MERCHANT') {
    return `用户（订单#${props.orderNumber}）`
  }
  return props.merchantName || '商家'
}

function isMine(message) {
  return message.senderRole === props.role
}

async function loadMessages() {
  try {
    messages.value = await getOrderMessages(props.orderId)
    await scrollToBottom()
  } catch {
    // silent
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    await sendMessage(props.orderId, text)
    input.value = ''
    await loadMessages()
  } catch {
    // error handled by client interceptor
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
  const el = chatPanel.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function startPolling() {
  stopPolling()
  polling.value = setInterval(loadMessages, 3000)
}

function stopPolling() {
  if (polling.value) {
    clearInterval(polling.value)
    polling.value = null
  }
}

onMounted(() => {
  loadMessages()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="chat-window">
    <div class="chat-header">
      <span v-if="role === 'USER'">{{ merchantName || '商家' }}</span>
      <span v-else>用户（订单#{{ orderNumber }}）</span>
      <span v-if="!canChat()" class="chat-closed-tag">聊天已关闭</span>
    </div>

    <div ref="chatPanel" class="chat-body">
      <div v-if="!messages.length" class="chat-empty">
        暂无消息，{{ canChat() ? '开始聊聊吧' : '暂无聊天记录' }}
      </div>
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['chat-bubble', isMine(message) ? 'mine' : 'other']"
      >
        <div class="bubble-header">
          <span class="bubble-sender">{{ senderLabel(message) }}</span>
          <span class="bubble-time">{{ formatTime(message.createdAt) }}</span>
        </div>
        <div class="bubble-content">{{ message.content }}</div>
      </div>
    </div>

    <div v-if="canChat()" class="chat-input-area">
      <textarea
        v-model="input"
        class="chat-input"
        placeholder="输入消息..."
        :disabled="sending"
        @keydown="onKeydown"
        rows="2"
      />
      <button
        class="chat-send-btn"
        :disabled="!input.trim() || sending"
        @click="send"
      >
        {{ sending ? '发送中...' : '发送' }}
      </button>
    </div>

    <div v-else class="chat-disabled-hint">
      订单状态下不支持发送新消息
    </div>
  </div>
</template>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 360px;
  max-height: 520px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.chat-header {
  align-items: center;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  font-weight: 600;
  gap: 8px;
  padding: 12px 16px;
}

.chat-closed-tag {
  background: #fef3c7;
  border-radius: 4px;
  color: #92400e;
  font-size: 12px;
  font-weight: 500;
  margin-left: auto;
  padding: 2px 8px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.chat-empty {
  color: #9ca3af;
  font-size: 14px;
  margin-top: 40px;
  text-align: center;
}

.chat-bubble {
  margin-bottom: 12px;
  max-width: 80%;
}

.chat-bubble.mine {
  margin-left: auto;
}

.chat-bubble.other {
  margin-right: auto;
}

.bubble-header {
  display: flex;
  font-size: 12px;
  gap: 8px;
  margin-bottom: 4px;
}

.chat-bubble.mine .bubble-header {
  justify-content: flex-end;
}

.bubble-sender {
  color: #6b7280;
}

.bubble-time {
  color: #9ca3af;
}

.bubble-content {
  background: #f3f4f6;
  border-radius: 12px;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.5;
  padding: 8px 12px;
  word-break: break-word;
}

.chat-bubble.mine .bubble-content {
  background: #409eff;
  color: #fff;
}

.chat-input-area {
  align-items: flex-end;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
  padding: 10px 12px;
}

.chat-input {
  border: 1px solid #d1d5db;
  border-radius: 6px;
  flex: 1;
  font-family: inherit;
  font-size: 14px;
  min-height: 36px;
  outline: none;
  padding: 8px 10px;
  resize: none;
}

.chat-input:focus {
  border-color: #409eff;
}

.chat-send-btn {
  background: #409eff;
  border: none;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  min-height: 36px;
  padding: 8px 16px;
  white-space: nowrap;
}

.chat-send-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.chat-disabled-hint {
  border-top: 1px solid #e5e7eb;
  color: #9ca3af;
  font-size: 13px;
  padding: 12px 16px;
  text-align: center;
}
</style>
