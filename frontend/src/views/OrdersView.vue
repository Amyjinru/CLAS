<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BackButton from '../components/BackButton.vue'
import { cancelOrder, completeOrder, getReviewByOrder, listMyDealOrders, listOrders, requestRefund, getOrderMessages } from '../api/clas'
import MoneyText from '../components/MoneyText.vue'
import StatusTag from '../components/StatusTag.vue'
import ChatWindow from '../components/ChatWindow.vue'
import RiderChatWindow from '../components/RiderChatWindow.vue'
import { useConfirmAction } from '../composables/useConfirmAction'
import { orderStatusMap } from '../utils/status'
import { isReceivingDealOrder, isReceivingOrder } from '../utils/orderReceiving'

const orders = ref([])
const dealOrders = ref([])
const message = ref('')
const reviewedOrderIds = ref(new Set())
const { confirmAction } = useConfirmAction()
const chatOrder = ref(null)
const riderChatOrder = ref(null)
const riderChatOpen = ref(false)
const chatHasHistory = ref(new Set())
const refundDialogOpen = ref(false)
const refundTarget = ref(null)
const refundReason = ref('')
const refundSubmitting = ref(false)
const route = useRoute()
const activeTab = ref(route.query.tab || 'all')

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

const dealStatusLabel = {
  PENDING_PAYMENT: '待支付',
  UNUSED: '待使用',
  USED: '已使用',
  EXPIRED: '已过期',
  REFUNDED: '已退款'
}

const orderTabs = [
  { label: '全部订单', name: 'all' },
  { label: '待收货/使用', name: 'receiving' },
  { label: '待评价', name: 'review' },
  { label: '退款/售后', name: 'after-sale' }
]

const visibleEntries = computed(() => {
  let foodEntries = orders.value
  let dealEntries = dealOrders.value
  if (activeTab.value === 'receiving') {
    foodEntries = foodEntries.filter((entry) => isReceivingOrder(entry.order))
    dealEntries = dealEntries.filter(isReceivingDealOrder)
  } else if (activeTab.value === 'review') {
    foodEntries = foodEntries.filter((entry) => entry.order.status === 'COMPLETED' && !hasReview(entry.order.id))
    dealEntries = []
  } else if (activeTab.value === 'after-sale') {
    foodEntries = foodEntries.filter((entry) => entry.order.status === 'REFUND_PENDING' || entry.order.status === 'REFUNDED' || (entry.order.refundStatus && entry.order.refundStatus !== 'NONE'))
    dealEntries = dealEntries.filter((entry) => entry.status === 'REFUNDED')
  }
  return [
    ...foodEntries.map((entry) => ({ key: `food-${entry.order.id}`, kind: 'food', entry, createdAt: entry.order.createTime })),
    ...dealEntries.map((entry) => ({ key: `deal-${entry.id}`, kind: 'deal', entry, createdAt: entry.createTime }))
  ].sort((left, right) => String(right.createdAt || '').localeCompare(String(left.createdAt || '')))
})

async function load() {
  try {
    const [orderList, dealList] = await Promise.all([listOrders(), listMyDealOrders({ silent: true })])
    orders.value = orderList
    dealOrders.value = dealList
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

function productSummary(order, productId) {
  return (order.products || []).find((product) => product.id === productId) || null
}

function itemName(order, item) {
  return productSummary(order, item.productId)?.name || `商品 #${item.productId}`
}

function itemImage(order, item) {
  return productSummary(order, item.productId)?.image || ''
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

function openRefundDialog(order) {
  refundTarget.value = order
  refundReason.value = refundReasonOptions[0]
  refundDialogOpen.value = true
}

function closeRefundDialog() {
  if (refundSubmitting.value) return
  refundDialogOpen.value = false
  refundTarget.value = null
  refundReason.value = ''
}

async function submitRefund() {
  const reason = refundReason.value.trim()
  if (!reason) {
    message.value = '请填写退款原因'
    return
  }
  if (!refundTarget.value) return
  refundSubmitting.value = true
  let submitted = false
  try {
    await requestRefund(refundTarget.value.order.id, reason)
    message.value = '退款申请已提交'
    await load()
    submitted = true
  } catch (error) {
    message.value = error?.response?.data?.message || error?.message || '退款申请提交失败，请稍后重试'
  } finally {
    refundSubmitting.value = false
  }
  if (submitted) closeRefundDialog()
}

function canRequestRefund(order) {
  if (!['PAID', 'ACCEPTED', 'COMPLETED'].includes(order.status) || (order.refundStatus && order.refundStatus !== 'NONE')) return false
  if (order.deliveryStatus !== 'DELIVERED') return true
  const deliveredAt = order.deliveryCompletedAt || order.deliveredAt
  return deliveredAt && Date.now() <= new Date(deliveredAt).getTime() + 15 * 60 * 1000
}

function hasReview(orderId) {
  return reviewedOrderIds.value.has(orderId)
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

    <section class="orders-list">
      <article v-for="display in visibleEntries" :key="display.key" class="order-card">
        <template v-if="display.kind === 'food'">
          <header class="store-header">
            <div class="store-identity">
              <img v-if="display.entry.merchantLogo" :src="display.entry.merchantLogo" :alt="display.entry.merchantName || '店铺'" />
              <span v-else class="store-logo-placeholder">{{ (display.entry.merchantName || '店').slice(0, 1) }}</span>
              <strong>{{ display.entry.merchantName || `店铺 #${display.entry.order.merchantId}` }}</strong>
            </div>
            <StatusTag :status="display.entry.order.status" :map="orderStatusMap" />
          </header>

          <RouterLink class="order-goods" :to="`/order/${display.entry.order.id}`">
            <div v-for="item in display.entry.items" :key="item.id" class="goods-row">
              <div class="goods-image">
                <img v-if="itemImage(display.entry, item)" :src="itemImage(display.entry, item)" :alt="itemName(display.entry, item)" loading="lazy" />
                <span v-else>{{ itemName(display.entry, item).slice(0, 1) }}</span>
              </div>
              <div class="goods-info">
                <h2>{{ itemName(display.entry, item) }}</h2>
                <p>× {{ item.quantity }}</p>
              </div>
              <MoneyText class="goods-price" :amount="item.price * item.quantity" />
            </div>
          </RouterLink>

          <div class="order-summary">
            <span>订单 #{{ display.entry.order.id }} · {{ deliveryLabel[display.entry.order.deliveryStatus] || display.entry.order.deliveryStatus }}</span>
            <strong>实付 <MoneyText :amount="display.entry.order.totalPrice" /></strong>
          </div>
          <div v-if="display.entry.order.remark || display.entry.order.rejectReason || display.entry.order.refundReason" class="order-notes">
            <p v-if="display.entry.order.remark" class="order-note">备注：{{ display.entry.order.remark }}</p>
            <p v-if="display.entry.order.rejectReason" class="order-note warn">拒单理由：{{ display.entry.order.rejectReason }}</p>
            <p v-if="display.entry.order.refundReason" class="order-note">退款申请：{{ display.entry.order.refundReason }}</p>
          </div>
          <div class="row-actions">
            <RouterLink v-if="display.entry.order.status === 'PENDING_PAYMENT'" class="button" :to="`/payment/${display.entry.order.id}`">去支付</RouterLink>
            <button v-if="['PENDING_PAYMENT', 'PAID'].includes(display.entry.order.status)" class="secondary" @click="cancel(display.entry)">取消订单</button>
            <button v-if="display.entry.order.status === 'ACCEPTED'" @click="complete(display.entry)">确认完成</button>
            <button v-if="canRequestRefund(display.entry.order)" class="secondary" type="button" @click="openRefundDialog(display.entry)">申请退款</button>
            <button v-if="['PAID', 'ACCEPTED'].includes(display.entry.order.status)" class="secondary" @click="openChat(display.entry)">联系商家</button>
            <button v-if="hasLiveRiderDelivery(display.entry.order)" class="secondary" @click="openRiderChat(display.entry)">联系骑手</button>
            <button v-if="display.entry.order.status === 'COMPLETED' && chatHasHistory.has(display.entry.order.id)" class="secondary" @click="openChat(display.entry)">查看聊天记录</button>
            <RouterLink v-if="display.entry.order.status === 'COMPLETED' && !hasReview(display.entry.order.id)" class="button secondary" :to="`/review/${display.entry.order.id}`">去评价</RouterLink>
            <span v-if="display.entry.order.status === 'COMPLETED' && hasReview(display.entry.order.id)" class="tag">已评价</span>
          </div>
        </template>

        <template v-else>
          <header class="store-header">
            <div class="store-identity">
              <img v-if="display.entry.merchantLogo" :src="display.entry.merchantLogo" :alt="display.entry.merchantName || '店铺'" />
              <span v-else class="store-logo-placeholder">{{ (display.entry.merchantName || '店').slice(0, 1) }}</span>
              <strong>{{ display.entry.merchantName || `店铺 #${display.entry.merchantId}` }}</strong>
            </div>
            <span class="deal-status">{{ dealStatusLabel[display.entry.status] || display.entry.status }}</span>
          </header>
          <RouterLink class="order-goods" :to="`/deal-order/${display.entry.id}`">
            <div class="goods-row">
              <div class="goods-image deal-image">
                <img v-if="display.entry.merchantLogo" :src="display.entry.merchantLogo" :alt="display.entry.dealTitle || '团购券'" loading="lazy" />
                <span v-else>券</span>
              </div>
              <div class="goods-info">
                <h2>{{ display.entry.dealTitle || `团购套餐 #${display.entry.dealId}` }}</h2>
                <p>到店团购券 · 1 份</p>
              </div>
              <MoneyText class="goods-price" :amount="display.entry.payAmount" />
            </div>
          </RouterLink>
          <div class="order-summary">
            <span>团购订单 #{{ display.entry.id }}</span>
            <strong>实付 <MoneyText :amount="display.entry.payAmount" /></strong>
          </div>
          <div class="row-actions">
            <RouterLink v-if="display.entry.status === 'PENDING_PAYMENT'" class="button" :to="`/payment/deal/${display.entry.id}`">去支付</RouterLink>
            <RouterLink class="button secondary" :to="`/deal-order/${display.entry.id}`">查看券详情</RouterLink>
          </div>
        </template>
      </article>
    </section>

    <el-empty v-if="!visibleEntries.length && !message" description="暂无订单" />

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

    <el-dialog
      v-model="refundDialogOpen"
      title="申请退款"
      width="min(480px,92vw)"
      :close-on-click-modal="!refundSubmitting"
      :close-on-press-escape="!refundSubmitting"
      @closed="closeRefundDialog"
    >
      <p class="refund-dialog-hint">请说明退款原因，商家会据此处理；已送达订单仅可在送达后 15 分钟内申请。</p>
      <el-select v-model="refundReason" filterable allow-create default-first-option placeholder="请选择或输入退款原因" class="refund-reason-select">
        <el-option v-for="reason in refundReasonOptions" :key="reason" :label="reason" :value="reason" />
      </el-select>
      <template #footer>
        <el-button :disabled="refundSubmitting" @click="closeRefundDialog">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
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
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
}

.order-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.store-header {
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 16px;
}

.store-identity {
  align-items: center;
  display: flex;
  gap: 9px;
  min-width: 0;
}

.store-identity img,
.store-logo-placeholder {
  border-radius: 50%;
  height: 30px;
  object-fit: cover;
  width: 30px;
}

.store-logo-placeholder {
  align-items: center;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  font-weight: 700;
  justify-content: center;
}

.store-identity strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deal-status {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.order-goods {
  color: inherit;
  display: block;
  padding: 4px 16px;
  text-decoration: none;
}

.goods-row {
  align-items: center;
  display: grid;
  gap: 12px;
  grid-template-columns: 82px minmax(0, 1fr) auto;
  padding: 12px 0;
}

.goods-row + .goods-row {
  border-top: 1px solid var(--border-light);
}

.goods-image {
  align-items: center;
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  display: flex;
  font-size: 24px;
  font-weight: 700;
  height: 72px;
  justify-content: center;
  overflow: hidden;
  width: 82px;
}

.goods-image img {
  height: 100%;
  object-fit: cover;
  width: 100%;
}

.deal-image img {
  object-fit: contain;
}

.goods-info {
  min-width: 0;
}

.goods-info h2 {
  font-size: 15px;
  margin: 0 0 8px;
}

.goods-info p {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
}

.goods-price {
  align-self: start;
  font-weight: 700;
  white-space: nowrap;
}

.order-summary {
  align-items: center;
  background: var(--bg-page);
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 11px 16px;
}

.order-summary span {
  color: var(--text-muted);
  font-size: 12px;
}

.order-summary strong {
  font-size: 14px;
  white-space: nowrap;
}

.order-notes {
  padding: 8px 16px 0;
}

.order-card .row-actions {
  border-top: 1px solid var(--border-light);
  justify-content: flex-end;
  padding: 12px 16px;
}

.order-note {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 4px;
}

.order-note.warn {
  color: var(--clas-warning);
}

.refund-dialog-hint {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin: 0 0 16px;
}

.refund-reason-select {
  width: 100%;
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

  .goods-row {
    grid-template-columns: 70px minmax(0, 1fr) auto;
  }

  .goods-image {
    height: 64px;
    width: 70px;
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
