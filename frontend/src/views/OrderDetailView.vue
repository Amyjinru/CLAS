<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { cancelOrder, completeOrder, getDeliveryTracking, getMerchant, getOrderDetail, getOrderTimeline, requestRefund } from '../api/clas'
import MoneyText from '../components/MoneyText.vue'
import StatusTag from '../components/StatusTag.vue'
import RiderChatWindow from '../components/RiderChatWindow.vue'
import { useConfirmAction } from '../composables/useConfirmAction'
import { formatCompactDateTime, formatDistance } from '../utils/formatters'
import { orderStatusMap } from '../utils/status'

const route = useRoute()
const router = useRouter()
const { confirmAction } = useConfirmAction()
const orderId = computed(() => Number(route.params.orderId))
const fromNotifications = computed(() => route.query.from === 'notifications')

const orderEntry = ref(null)
const merchant = ref(null)
const loading = ref(true)
const error = ref('')
const actionMessage = ref('')
const deliveryTracking = ref(null)
const lifecycleEvents = ref([])
const riderChatOpen = ref(false)
let trackingTimer = null

const deliveryLabel = {
  WAITING: '等待配送',
  PREPARING: '商家备餐中',
  DELIVERING: '配送中',
  AVAILABLE: '等待骑手接单',
  ASSIGNED_WAITING_MEAL: '骑手已接单，等待取餐',
  DELIVERED: '已送达'
}

const refundStatusLabel = {
  PENDING: '待商家审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const orderTimeline = computed(() => {
  if (lifecycleEvents.value.length) {
    return lifecycleEvents.value.map((event) => ({ label: lifecycleLabel[event.eventType] || event.eventType, time: event.createdAt, remark: event.remark }))
  }
  const order = orderEntry.value?.order
  if (!order) return []
  return [
    { label: '订单创建', time: order.createTime },
    { label: '支付成功', time: order.paidAt },
    { label: '订单已受理', time: order.acceptedAt },
    { label: '骑手接单', time: order.riderAssignedAt },
    { label: '骑手取餐', time: order.pickedUpAt },
    { label: '订单送达', time: order.deliveryCompletedAt || order.deliveredAt },
    { label: '订单完成', time: order.completedAt },
    { label: '订单取消', time: order.canceledAt },
    { label: '商家拒单', time: order.rejectedAt },
    { label: '申请退款', time: order.refundRequestedAt },
    { label: '退款处理', time: order.refundResolvedAt }
  ].filter((item) => item.time)
})

const lifecycleLabel = {
  ORDER_CREATED: '订单创建', PAYMENT_SUCCEEDED: '支付成功', MERCHANT_ACCEPTED: '商家接单，开始制作',
  MERCHANT_READY_FOR_DISPATCH: '餐品制作完成，发布配送', RIDER_CLAIMED: '骑手接单',
  RIDER_PICKED_UP: '骑手取餐', RIDER_DELIVERED: '骑手送达', USER_CONFIRMED_RECEIPT: '确认收货',
  MERCHANT_REVIEWED: '完成商家评价', RIDER_REVIEWED: '完成骑手评价', ORDER_CANCELED: '订单取消', MERCHANT_REJECTED: '商家拒单'
}

const currentStatus = computed(() => orderEntry.value?.order?.status)

function distanceText(distance) {
  if (!distance) return ''
  return formatDistance(distance)
}

function backLabel() {
  return fromNotifications.value ? '← 返回通知' : '← 返回订单'
}

function goBack() {
  if (fromNotifications.value) {
    router.push('/profile/notifications')
  } else {
    router.push('/orders')
  }
}

async function loadDetail() {
  merchant.value = null
  actionMessage.value = ''
  error.value = ''
  try {
    orderEntry.value = await getOrderDetail(orderId.value)
    if (!orderEntry.value) {
      error.value = '订单不存在或无权查看'
      return
    }
    try { lifecycleEvents.value = await getOrderTimeline(orderId.value) } catch { lifecycleEvents.value = [] }
    await refreshTracking()
    const merchantId = orderEntry.value.order?.merchantId
    if (merchantId) {
      try {
        merchant.value = await getMerchant(merchantId)
      } catch (e) {
        merchant.value = null
      }
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载订单失败'
  }
}

function hasLiveDelivery() {
  return ['ASSIGNED_WAITING_MEAL', 'DELIVERING'].includes(orderEntry.value?.order?.deliveryStatus)
}

async function refreshTracking() {
  if (!hasLiveDelivery()) {
    deliveryTracking.value = null
    return
  }
  try {
    deliveryTracking.value = await getDeliveryTracking(orderId.value)
  } catch {
    deliveryTracking.value = null
  }
}

function syncTrackingPolling() {
  if (trackingTimer) {
    window.clearInterval(trackingTimer)
    trackingTimer = null
  }
  if (hasLiveDelivery()) trackingTimer = window.setInterval(refreshTracking, 5000)
}

async function cancelCurrentOrder() {
  await confirmAction('确认取消该订单？', async () => {
    await cancelOrder(orderEntry.value.order.id)
    await loadDetail()
    syncTrackingPolling()
    syncTrackingPolling()
    actionMessage.value = '订单已取消'
  })
}

async function completeCurrentOrder() {
  await confirmAction('确认该订单已经完成？', async () => {
    await completeOrder(orderEntry.value.order.id)
    await loadDetail()
    syncTrackingPolling()
    actionMessage.value = '订单已完成'
  }, { type: 'success' })
}

async function refundCurrentOrder() {
  const reason = window.prompt('请输入退款原因', '配送超时')
  if (!reason) return
  await requestRefund(orderEntry.value.order.id, reason.trim())
  await loadDetail()
  syncTrackingPolling()
  actionMessage.value = '退款申请已提交'
}

onMounted(async () => {
  try {
    await loadDetail()
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => { if (trackingTimer) window.clearInterval(trackingTimer) })
</script>

<template>
  <div class="user-page order-detail-page">
    <button class="back-compact" type="button" @click="goBack">{{ backLabel() }}</button>

    <el-skeleton v-if="loading" :rows="6" animated />

    <el-empty v-else-if="error" :description="error" />

    <section v-else class="panel order-detail-panel">
      <h1>订单 #{{ orderEntry.order.id }}</h1>
      <p v-if="actionMessage" class="action-message">{{ actionMessage }}</p>

      <div class="detail-actions">
        <RouterLink
          v-if="currentStatus === 'PENDING_PAYMENT'"
          class="button"
          :to="`/payment/${orderEntry.order.id}`"
        >
          去支付
        </RouterLink>
        <button v-if="hasLiveDelivery()" class="secondary" type="button" @click="riderChatOpen = true">联系骑手</button>
        <button
          v-if="['PENDING_PAYMENT', 'PAID'].includes(currentStatus)"
          class="secondary"
          type="button"
          @click="cancelCurrentOrder"
        >
          取消订单
        </button>
        <button
          v-if="currentStatus === 'ACCEPTED'"
          type="button"
          @click="completeCurrentOrder"
        >
          确认完成
        </button>
        <button
          v-if="['PAID', 'ACCEPTED', 'COMPLETED'].includes(currentStatus)"
          class="secondary"
          type="button"
          @click="refundCurrentOrder"
        >
          申请退款
        </button>
        <RouterLink
          v-if="currentStatus === 'COMPLETED'"
          class="button secondary"
          :to="`/review/${orderEntry.order.id}`"
        >
          去评价
        </RouterLink>
      </div>

      <div class="detail-grid">
        <div class="detail-block">
          <h2>状态</h2>
          <p>
            <StatusTag :status="orderEntry.order.status" :map="orderStatusMap" />
            <span v-if="orderEntry.order.deliveryStatus"> · {{ deliveryLabel[orderEntry.order.deliveryStatus] || orderEntry.order.deliveryStatus }}</span>
          </p>
          <el-timeline v-if="orderTimeline.length" class="order-timeline">
            <el-timeline-item
              v-for="item in orderTimeline"
              :key="`${item.label}-${item.time}`"
              :timestamp="formatCompactDateTime(item.time).slice(0, 16)"
            >
              {{ item.label }}
              <small v-if="item.remark" class="timeline-remark">{{ item.remark }}</small>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="detail-block">
          <h2>金额</h2>
          <p>商品合计：<MoneyText :amount="orderEntry.order.subtotal" /></p>
          <p>配送费：<MoneyText :amount="orderEntry.order.deliveryFee || 0" /></p>
          <p v-if="orderEntry.order.couponDiscount > 0">优惠券：<MoneyText :amount="orderEntry.order.couponDiscount" negative /></p>
          <p class="total-line">实付：<MoneyText :amount="orderEntry.order.totalPrice" /></p>
        </div>

        <div v-if="merchant" class="detail-block merchant-summary">
          <h2>商家</h2>
          <p class="merchant-name">{{ merchant.merchantName }}</p>
          <p class="merchant-meta">
            <span v-if="merchant.category">{{ merchant.category }}</span>
            <span v-if="merchant.score">评分 {{ Number(merchant.score).toFixed(1) }}</span>
          </p>
          <p v-if="merchant.phone">电话：{{ merchant.phone }}</p>
          <p v-if="merchant.address">地址：{{ merchant.address }}</p>
        </div>

        <div class="detail-block">
          <h2>商品</h2>
          <p v-if="!orderEntry.items || !orderEntry.items.length">暂无商品信息</p>
          <ul v-else class="item-list">
            <li v-for="item in orderEntry.items" :key="item.id">
              {{ item.productName || `商品 #${item.productId}` }}
              <span class="item-qty">×{{ item.quantity }}</span>
              <span class="item-price"><MoneyText :amount="item.price * item.quantity" /></span>
            </li>
          </ul>
        </div>

        <div v-if="orderEntry.order.deliveryAddress" class="detail-block">
          <h2>配送信息</h2>
          <p>送至：{{ orderEntry.order.deliveryAddress }}</p>
          <p>预计 {{ orderEntry.order.estimatedMinutes }} 分钟送达</p>
          <p v-if="orderEntry.order.routeDistanceMeters">路线距离：{{ distanceText(orderEntry.order.routeDistanceMeters) }}</p>
          <p v-else-if="orderEntry.order.distanceMeters">直线距离：{{ distanceText(orderEntry.order.distanceMeters) }}</p>
        </div>

        <div v-if="deliveryTracking" class="detail-block">
          <h2>骑手配送追踪</h2>
          <p>预计剩余：{{ deliveryTracking.remainingMinutes ?? '--' }} 分钟</p>
          <p>承诺送达：{{ formatCompactDateTime(deliveryTracking.promiseStartAt).slice(0, 16) }} 至 {{ formatCompactDateTime(deliveryTracking.promiseEndAt).slice(0, 16) }}</p>
          <p>路线：{{ deliveryTracking.routeSource === 'AMAP' ? '高德地图路线' : '直线估算' }}</p>
          <p v-if="deliveryTracking.liveLocationAvailable">骑手位置已更新：{{ formatCompactDateTime(deliveryTracking.locationUpdatedAt).slice(0, 16) }}</p>
          <p v-else>骑手位置暂不可用或已过期</p>
        </div>

        <div v-if="orderEntry.order.remark" class="detail-block">
          <h2>备注</h2>
          <p>{{ orderEntry.order.remark }}</p>
        </div>

        <div v-if="orderEntry.order.rejectReason" class="detail-block">
          <h2>拒单理由</h2>
          <p class="warn">{{ orderEntry.order.rejectReason }}</p>
        </div>

        <div v-if="orderEntry.order.refundReason" class="detail-block">
          <h2>退款信息</h2>
          <p>申请理由：{{ orderEntry.order.refundReason }}</p>
          <p v-if="orderEntry.order.refundStatus && orderEntry.order.refundStatus !== 'NONE'">
            进度：{{ refundStatusLabel[orderEntry.order.refundStatus] || orderEntry.order.refundStatus }}
          </p>
          <p v-if="orderEntry.order.refundRequestedAt">
            申请时间：{{ formatCompactDateTime(orderEntry.order.refundRequestedAt).slice(0, 16) }}
          </p>
          <p v-if="orderEntry.order.refundResolvedAt">
            处理时间：{{ formatCompactDateTime(orderEntry.order.refundResolvedAt).slice(0, 16) }}
          </p>
          <p v-if="orderEntry.order.refundRejectReason" class="warn">
            拒绝理由：{{ orderEntry.order.refundRejectReason }}
          </p>
        </div>
      </div>
    </section>
    <el-dialog v-model="riderChatOpen" title="配送沟通" width="min(560px,92vw)"><RiderChatWindow v-if="riderChatOpen" :order-id="orderEntry.order.id" role="USER" :active="hasLiveDelivery()" /></el-dialog>
  </div>
</template>

<style scoped>
.order-detail-page {
  display: grid;
  gap: 16px;
}

.back-compact {
  align-self: start;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  justify-self: start;
  min-height: 32px;
  padding: 0 12px;
  width: auto;
}

.back-compact:hover {
  background: var(--color-primary-soft);
  border-color: var(--clas-amber-200);
  color: var(--color-primary);
  transform: none;
}

.order-detail-panel h1 {
  margin-top: 0;
}

.action-message {
  color: var(--clas-success);
  font-weight: 600;
  margin: 0 0 12px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}

.detail-grid {
  display: grid;
  gap: 20px;
}

.detail-block {
  border-top: 1px solid var(--border-light);
  padding-top: 16px;
}

.detail-block h2 {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin: 0 0 8px;
  text-transform: uppercase;
}

.detail-block p {
  margin: 4px 0;
}

.total-line {
  font-weight: 700;
  font-size: 16px;
  margin-top: 8px;
}

.merchant-name {
  font-size: 16px;
  font-weight: 700;
}

.merchant-meta {
  color: var(--text-secondary);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.warn {
  color: var(--clas-warning);
}

.order-timeline {
  margin-top: 14px;
  padding-left: 2px;
}

.timeline-remark { color: var(--text-tertiary); display: block; font-size: 12px; margin-top: 3px; }

.item-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.item-list li {
  align-items: center;
  display: flex;
  gap: 8px;
  padding: 6px 0;
}

.item-list li + li {
  border-top: 1px solid var(--border-light);
}

.item-qty {
  color: var(--text-tertiary);
  font-size: 13px;
}

.item-price {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: auto;
}

@media (min-width: 768px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
