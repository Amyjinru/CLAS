<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import OrderDetailContent from '../components/OrderDetailContent.vue'
import {
  acceptOrder,
  currentMerchantId,
  currentRole,
  listMerchantOrders,
  listProducts
} from '../api/clas'

const router = useRouter()
const orders = ref([])
const productNames = ref({})
const message = ref('')
const selectedOrder = ref(null)

const statusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '商家已接单',
  COMPLETED: '已完成'
}

async function load() {
  if (currentRole() !== 'MERCHANT') {
    message.value = '请使用商家账号 merchant / 123456 登录'
    orders.value = []
    return
  }
  try {
    const merchantId = currentMerchantId()
    const [orderList, products] = await Promise.all([
      listMerchantOrders(merchantId),
      listProducts(merchantId)
    ])
    orders.value = orderList
    productNames.value = Object.fromEntries(products.map((product) => [product.id, product.name]))
    message.value = ''
  } catch (error) {
    message.value = error.response?.data?.message || '加载订单失败'
  }
}

async function operate(action, order) {
  if (action === 'accept') await acceptOrder(order.order.id)
  if (selectedOrder.value?.order.id === order.order.id) {
    selectedOrder.value = orders.value.find((item) => item.order.id === order.order.id) || null
  }
  await load()
}

function openDetail(order) {
  selectedOrder.value = order
}

function closeDetail() {
  selectedOrder.value = null
}

onMounted(() => {
  if (currentRole() !== 'MERCHANT') {
    router.push('/login')
    return
  }
  load()
})
</script>

<template>
  <section class="panel">
    <h1>商家工作台</h1>
    <p>处理本店已支付订单，完成接单环节。</p>
    <p>{{ message }}</p>
  </section>

  <section class="list">
    <article class="order-card" v-for="order in orders" :key="order.order.id">
      <div class="order-head">
        <div>
          <h2>订单 {{ order.order.id }}</h2>
          <p>{{ statusLabel[order.order.status] || order.order.status }} · ¥{{ (order.order.totalPrice / 100).toFixed(2) }}</p>
        </div>
        <div class="row-actions">
          <button class="secondary" type="button" @click="openDetail(order)">查看详情</button>
          <button v-if="order.order.status === 'PAID'" type="button" @click="operate('accept', order)">接单</button>
        </div>
      </div>

      <OrderDetailContent :order="order" :product-names="productNames" compact />
    </article>
  </section>

  <div v-if="selectedOrder" class="order-overlay" @click.self="closeDetail">
    <aside class="order-panel">
      <header class="order-panel-head">
        <h2>订单详情</h2>
        <button class="panel-close" type="button" @click="closeDetail">×</button>
      </header>

      <div class="order-panel-body">
        <OrderDetailContent :order="selectedOrder" :product-names="productNames" />
      </div>

      <footer class="order-panel-foot">
        <button
          v-if="selectedOrder.order.status === 'PAID'"
          type="button"
          @click="operate('accept', selectedOrder)"
        >
          接单
        </button>
        <button class="secondary" type="button" @click="closeDetail">关闭</button>
      </footer>
    </aside>
  </div>
</template>

<style scoped>
.order-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.order-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.order-head h2 {
  font-size: 18px;
  margin: 0 0 6px;
}

.order-head p {
  color: #667085;
  margin: 0;
}

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

.order-panel {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.16);
  display: flex;
  flex-direction: column;
  max-height: min(80vh, 640px);
  max-width: 520px;
  width: 100%;
}

.order-panel-head,
.order-panel-foot {
  padding: 16px 18px;
}

.order-panel-head {
  align-items: center;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
}

.order-panel-head h2 {
  font-size: 18px;
  margin: 0;
}

.panel-close {
  background: #f3f4f6;
  color: #6b7280;
  min-height: 32px;
  min-width: 32px;
  padding: 0;
}

.order-panel-body {
  overflow: auto;
  padding: 18px;
}

.order-panel-body :deep(.order-detail) {
  background: transparent;
  padding: 0;
}

.order-panel-foot {
  border-top: 1px solid #eef2f7;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .order-head {
    flex-direction: column;
  }

  .order-overlay {
    padding: 14px;
  }
}
</style>
