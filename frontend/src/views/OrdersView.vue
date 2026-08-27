<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { cancelOrder, completeOrder, getReviewByOrder, listOrders, requestRefund, getOrderMessages } from '../api/clas'
import MoneyText from '../components/MoneyText.vue'
import StatusTag from '../components/StatusTag.vue'
import ChatWindow from '../components/ChatWindow.vue'
import RiderChatWindow from '../components/RiderChatWindow.vue'
import { useConfirmAction } from '../composables/useConfirmAction'
import { formatCompactDateTime, formatDistance } from '../utils/formatters'
import { orderStatusMap } from '../utils/status'

const orders = ref([])
const message = ref('')
const reviewedOrderIds = ref(new Set())
const { confirmAction } = useConfirmAction()
const chatOrder = ref(null)
const riderChatOrder = ref(null)
const riderChatOpen = ref(false)
const chatHasHistory = ref(new Set())
const route = useRoute()
const activeTab = ref(route.query.tab || 'all')

const refundStatusLabel = {
  PENDING: '待商家审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const refundReasonOptions = [
  '行程变化，无法收货',
  '下错单/重复下单',
  '商品质量问题',
  '配送超时',
  '其他原因'
]

const deliveryLabel = {
  WAITING: '等待商家接单',
  PREPARING: '商家备餐中',
  AVAILABLE: '等待骑手接单',
  ASSIGNED_WAITING_MEAL: '骑手已接单，等待取餐',
  DELIVERING: '配送中',
  DELIVERED: '已送达'
}

const orderTabs = [
  { label: '全部订单', name: 'all' },
  { label: '待收货/使用', name: 'receiving' },
  { label: '待评价', name: 'review' },
  { label: '退款/售后', name: 'after-sale' }
]

const filteredOrders = computed(() => {
  if (activeTab.value === 'receiving') {
    return orders.value.filter((entry) => {
      const { status, deliveryStatus, refundStatus } = entry.order
      // 明确排除退款、取消和拒单订单
      if (['REFUNDED', 'REFUND_PENDING', 'CANCELED', 'REJECTED'].includes(status)) return false
      if (refundStatus && refundStatus !== 'NONE') return false
      // 自动接单且已配送/已送达（未确认收货）
      if (status === 'ACCEPTED' && ['DELIVERING', 'DELIVERED'].includes(deliveryStatus)) return true
      return false
    })
  }
  if (activeTab.value === 'review') {
    return orders.value.filter((entry) => entry.order.status === 'COMPLETED' && !hasReview(entry.order.id))
  }
  if (activeTab.value === 'after-sale') {
    return orders.value.filter((entry) => entry.order.status === 'REFUND_PENDING' || entry.order.status === 'REFUNDED' || (entry.order.refundStatus && entry.order.refundStatus !== 'NONE'))
  }
  return orders.value
})

async function load() {
  try {
    orders.value = await listOrders()
    const reviewed = await Promise.all(
      orders.value
        .filter((order) => order.order.status === 'COMPLETED')
        .map(async (order) => {
          try {
            const review = await getReviewByOrder(order.order.id)
            return review ? order.order.id : null
          } catch {
            return null
          }
        })
    )
    reviewedOrderIds.value = new Set(reviewed.filter(Boolean))
  } catch {
    message.value = '请先登录'
  }
}

async function complete(order) {
  await confirmAction('确认该订单已经完成？', async () => {
    await completeOrder(order.order.id)
    await load()
  }, { type: 'success' })
}

async function cancel(order) {
  await confirmAction('确认取消该订单？', async () => {
    await cancelOrder(order.order.id)
    await load()
  })
}

async function refund(order) {
  const hint = refundReasonOptions.map((item, index) => `${index + 1}. ${item}`).join('\n')
  const reason = window.prompt(`请选择或输入退款原因：\n${hint}`, refundReasonOptions[0])
  if (!reason) return
  await requestRefund(order.order.id, reason.trim())
  message.value = '退款申请已提交'
  await load()
}

function hasReview(orderId) {
  return reviewedOrderIds.value.has(orderId)
}

function distanceText(distance) {
  if (!distance) return ''
  return formatDistance(distance)
}

function openChat(order) {
  chatOrder.value = order
}

function closeChat() {
  chatOrder.value = null
}

function hasLiveRiderDelivery(order) {
  return ['ASSIGNED_WAITING_MEAL', 'DELIVERING'].includes(order.deliveryStatus)
}

function openRiderChat(order) {
  riderChatOrder.value = order
  riderChatOpen.value = true
}

function closeRiderChat() {
  riderChatOpen.value = false
  riderChatOrder.value = null
}

async function checkChatHistory(order) {
  try {
    const messages = await getOrderMessages(order.order.id)
    return messages && messages.length > 0
  } catch {
    return false
  }
}

function openOrderById(orderId) {
  const id = Number(orderId)
  if (!id) return
  const order = orders.value.find((o) => o.order.id === id)
  if (order) {
    openChat(order)
  }
}

onMounted(async () => {
  await load()
  // Check chat history for completed orders
  for (const order of orders.value) {
    if (order.order.status === 'COMPLETED') {
      const hasHistory = await checkChatHistory(order)
      if (hasHistory) {
        chatHasHistory.value = new Set([...chatHasHistory.value, order.order.id])
      }
    }
  }
})

watch(() => route.query.tab, (tab) => {
  activeTab.value = tab || 'all'
})
</script>

<template>
  <div class="user-page orders-page">
    <BackButton to="/home" />

    <section class="panel orders-head">
      <h1>我的订单</h1>
      <p>{{ message }}</p>
      <el-tabs v-model="activeTab" class="order-tabs">
        <el-tab-pane v-for="tab in orderTabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>
    </section>

    <section class="user-page-grid-2 orders-list">
      <article class="row order-card" v-for="order in filteredOrders" :key="order.order.id">
        <div class="order-body">
          <h2>订单 {{ order.order.id }}</h2>
          <p><StatusTag :status="order.order.status" :map="orderStatusMap" /> · {{ deliveryLabel[order.order.deliveryStatus] || order.order.deliveryStatus }} · <MoneyText :amount="order.order.totalPrice" /></p>
          <p v-if="order.order.subtotal != null">
            商品 <MoneyText :amount="order.order.subtotal" />
            · 配送费 <MoneyText :amount="order.order.deliveryFee || 0" />
            <span v-if="order.order.couponDiscount > 0">
              · 优惠 <MoneyText :amount="order.order.couponDiscount" negative />
            </span>
          </p>
          <p v-if="order.order.remark" class="order-note">备注：{{ order.order.remark }}</p>
          <p v-if="order.order.rejectReason" class="order-note warn">拒单理由：{{ order.order.rejectReason }}</p>
          <p v-if="order.order.refundReason" class="order-note">退款申请：{{ order.order.refundReason }}</p>
          <p v-if="order.order.refundStatus && order.order.refundStatus !== 'NONE'" class="order-note">
            退款进度：{{ refundStatusLabel[order.order.refundStatus] || order.order.refundStatus }}
            <span v-if="order.order.refundRequestedAt"> · 申请于 {{ formatCompactDateTime(order.order.refundRequestedAt).slice(0, 16) }}</span>
            <span v-if="order.order.refundResolvedAt"> · 处理于 {{ formatCompactDateTime(order.order.refundResolvedAt).slice(0, 16) }}</span>
          </p>
          <p v-if="order.order.refundRejectReason" class="order-note warn">
            退款拒绝理由：{{ order.order.refundRejectReason }}
          </p>
          <p v-if="order.order.deliveryAddress">
            送至 {{ order.order.deliveryAddress }} · 约 {{ order.order.estimatedMinutes }} 分钟
            <span v-if="order.order.routeDistanceMeters"> · 路线 {{ distanceText(order.order.routeDistanceMeters) }}</span>
            <span v-else-if="order.order.distanceMeters"> · 直线 {{ distanceText(order.order.distanceMeters) }}</span>
          </p>
          <p>{{ order.items.length }} 件商品</p>
        </div>
        <div class="row-actions">
          <RouterLink
            v-if="order.order.status === 'PENDING_PAYMENT'"
            class="button"
            :to="`/payment/${order.order.id}`"
          >
            去支付
          </RouterLink>
          <button
            v-if="['PENDING_PAYMENT', 'PAID'].includes(order.order.status)"
            class="secondary"
            @click="cancel(order)"
          >
            取消订单
          </button>
          <button v-if="order.order.status === 'ACCEPTED'" @click="complete(order)">确认完成</button>
          <button
            v-if="['PAID', 'ACCEPTED', 'COMPLETED'].includes(order.order.status)"
            class="secondary"
            @click="refund(order)"
          >
            申请退款
          </button>
          <button
            v-if="['PAID', 'ACCEPTED'].includes(order.order.status)"
            class="secondary"
            @click="openChat(order)"
          >
            联系商家
          </button>
          <button
            v-if="hasLiveRiderDelivery(order.order)"
            class="secondary"
            @click="openRiderChat(order)"
          >
            联系骑手
          </button>
          <button
            v-if="order.order.status === 'COMPLETED' && chatHasHistory.has(order.order.id)"
            class="secondary"
            @click="openChat(order)"
          >
            查看聊天记录
          </button>
          <RouterLink
            v-if="order.order.status === 'COMPLETED' && !hasReview(order.order.id)"
            class="button secondary"
            :to="`/review/${order.order.id}`"
          >
            去评价
          </RouterLink>
          <span v-if="order.order.status === 'COMPLETED' && hasReview(order.order.id)" class="tag">已评价</span>
        </div>
      </article>
    </section>

    <el-empty v-if="!filteredOrders.length && !message" description="暂无订单" />

    <!-- Chat overlay -->
    <div v-if="chatOrder" class="order-overlay" @click.self="closeChat">
      <aside class="chat-panel">
        <header class="chat-panel-head">
          <h2>与商家沟通</h2>
          <p class="chat-panel-subtitle">订单 #{{ chatOrder.order.id }}</p>
          <button class="panel-close" type="button" @click="closeChat">×</button>
        </header>
        <div class="chat-panel-body">
          <ChatWindow
            :order-id="chatOrder.order.id"
            :merchant-id="chatOrder.order.merchantId"
            :merchant-name="chatOrder.merchantName || ''"
            role="USER"
            :order-status="chatOrder.order.status"
            :order-number="chatOrder.order.id"
          />
        </div>
      </aside>
    </div>

    <el-dialog v-model="riderChatOpen" title="配送沟通" width="min(560px,92vw)" @closed="closeRiderChat">
      <RiderChatWindow
        v-if="riderChatOrder"
        :order-id="riderChatOrder.order.id"
        role="USER"
        :active="hasLiveRiderDelivery(riderChatOrder.order)"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.orders-page {
  display: grid;
  gap: 20px;
}

.orders-head {
  margin-bottom: 0;
}

.order-tabs {
  margin-top: 14px;
}

.orders-list {
  gap: 16px;
}

.order-card {
  align-items: flex-start;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.order-body {
  width: 100%;
}

.order-card .row-actions {
  justify-content: flex-start;
  width: 100%;
}

.order-note {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 4px;
}

.order-note.warn {
  color: var(--clas-warning);
}

.tag {
  background: var(--clas-success-light);
  border-radius: var(--radius-full);
  color: var(--clas-success);
  font-size: 13px;
  font-weight: 700;
  padding: 6px 12px;
}

@media (min-width: 1024px) {
  .orders-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .orders-list {
    grid-template-columns: 1fr;
  }
}

/* Chat panel styles */
.order-overlay {
  align-items: center;
  background: rgba(15, 23, 42, 0.28);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 24px;
  position: fixed;
  z-index: 30;
}

.chat-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  height: 520px;
  max-width: 480px;
  width: 100%;
}

.chat-panel-head {
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 18px;
}

.chat-panel-head h2 {
  font-size: 18px;
  margin: 0;
}

.chat-panel-subtitle {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
  margin-left: auto;
}

.chat-panel-body {
  flex: 1;
  overflow: hidden;
}

.chat-panel-body :deep(.chat-window) {
  border: none;
  border-radius: 0;
  max-height: none;
}
</style>
