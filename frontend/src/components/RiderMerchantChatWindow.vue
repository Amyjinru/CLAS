<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getRiderMerchantMessages, sendRiderMerchantMessage } from '../api/rider'

const props = defineProps({ orderId: { type: Number, required: true }, role: { type: String, required: true }, active: { type: Boolean, default: true } })
const messages = ref([])
const input = ref('')
const sending = ref(false)
const panel = ref(null)
let timer = null

async function load() { try { messages.value = await getRiderMerchantMessages(props.orderId); await nextTick(); if (panel.value) panel.value.scrollTop = panel.value.scrollHeight } catch { /* 由请求拦截器提示 */ } }
async function send() { const content = input.value.trim(); if (!content || sending.value || !props.active) return; sending.value = true; try { await sendRiderMerchantMessage(props.orderId, content); input.value = ''; await load() } finally { sending.value = false } }
function keydown(event) { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); send() } }
onMounted(() => { load(); timer = window.setInterval(load, 3000) })
onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })
</script>

<template>
  <section class="partner-chat">
    <header>订单 #{{ orderId }} · {{ role === 'RIDER' ? '联系商家' : '联系骑手' }}<span v-if="!active">（已关闭，仅可查看）</span></header>
    <div ref="panel" class="messages"><p v-if="!messages.length">暂无消息</p><div v-for="message in messages" :key="message.id" :class="['message', message.senderRole === role ? 'mine' : 'other']"><small>{{ message.senderRole === role ? '我' : (role === 'RIDER' ? '商家' : '骑手') }} · {{ String(message.createdAt).replace('T', ' ').slice(0, 16) }}</small><div>{{ message.content }}</div></div></div>
    <footer v-if="active"><textarea v-model="input" rows="2" placeholder="输入消息…" @keydown="keydown" /><button :disabled="sending || !input.trim()" @click="send">{{ sending ? '发送中' : '发送' }}</button></footer>
  </section>
</template>

<style scoped>
.partner-chat{border:1px solid #e5e7eb;border-radius:12px;display:flex;flex-direction:column;min-height:300px;overflow:hidden}.partner-chat header{background:#f8fafc;font-weight:600;padding:12px}.partner-chat header span{color:#9a6700;font-size:12px;font-weight:400}.messages{flex:1;max-height:280px;overflow:auto;padding:12px}.messages>p{color:#94a3b8;text-align:center}.message{margin:8px 0;max-width:82%}.message.mine{margin-left:auto;text-align:right}.message small{color:#94a3b8;display:block;font-size:11px;margin-bottom:3px}.message div{background:#f1f5f9;border-radius:10px;display:inline-block;padding:8px 10px;text-align:left}.message.mine div{background:#0f766e;color:#fff}.partner-chat footer{border-top:1px solid #e5e7eb;display:flex;gap:8px;padding:10px}.partner-chat textarea{flex:1;resize:none}.partner-chat button{width:auto}
</style>
