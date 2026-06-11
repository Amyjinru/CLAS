<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '../api/clas'
import MoneyText from '../components/MoneyText.vue'
import StatusTag from '../components/StatusTag.vue'
import { formatCompactDateTime, formatDistance } from '../utils/formatters'
import { orderStatusMap } from '../utils/status'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.orderId))
const fromNotifications = computed(() => route.query.from === 'notifications')

const orderEntry = ref(null)
const loading = ref(true)
const error = ref('')

const deliveryLabel = {
  WAITING: '等待商家接单',
  PREPARING: '商家备餐中',
  DELIVERING: '配送中',
  DELIVERED: '已送达'
}

const refundStatusLabel = {
  PENDING: '待商家审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const orderTimeline = computed(() => {
  const order = orderEntry.value?.order
  if (!order) return []
  return [
    { label: '订单创建', time: order.createTime },
    { label: '支付成功', time: order.paidAt },
    { label: '商家接单', time: order.acceptedAt },
    { label: '配送开始', time: order.deliveredAt },
    { label: '订单完成', time: order.completedAt },
    { label: '订单取消', time: order.canceledAt },
    { label: '商家拒单', time: order.rejectedAt },
    { label: '申请退款', time: order.refundRequestedAt },
    { label: '退款处理', time: order.refundResolvedAt }
  ].filter((item) => item.time)
})

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

onMounted(async () => {
  try {
    orderEntry.value = await getOrderDetail(orderId.value)
    if (!orderEntry.value) {
      error.value = '订单不存在或无权查看'
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载订单失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="user-page order-detail-page">
    <button class="back-compact" type="button" @click="goBack">{{ backLabel() }}</button>

    <el-skeleton v-if="loading" :rows="6" animated />

    <el-empty v-else-if="error" :description="error" />

    <section v-else class="panel order-detail-panel">
      <h1>订单 #{{ orderEntry.order.id }}</h1>

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

.warn {
  color: var(--clas-warning);
}

.order-timeline {
  margin-top: 14px;
  padding-left: 2px;
}

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
