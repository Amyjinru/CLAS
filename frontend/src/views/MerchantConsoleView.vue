<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
// ===== test1: 商户审核 API =====
import { getMyMerchant, listMerchantOrders, acceptOrder, currentUser, currentRole, listProducts } from '../api/clas'
import { ElMessage } from 'element-plus'

// ===== version_314: 订单详情组件 =====
import OrderDetailContent from '../components/OrderDetailContent.vue'

const router = useRouter()
const merchant = ref(null)
const orders = ref([])
const loading = ref(true)
const loginUser = ref(null)

// ===== version_314: 订单详情弹窗 & 商品名映射 =====
const productNames = ref({})
const selectedOrder = ref(null)

// ===== test1: 商户审核状态映射 =====
const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已审核', type: 'info' },
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '停业中', type: 'danger' },
  BLOCKED: { text: '已禁用', type: 'danger' }
}

// ===== version_314: 订单状态映射 =====
const orderStatusLabel = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  ACCEPTED: '商家已接单',
  COMPLETED: '已完成'
}

async function load() {
  loading.value = true
  loginUser.value = currentUser()
  if (!loginUser.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const data = await getMyMerchant()
    merchant.value = data

    if (merchant.value && merchant.value.status === 'OPEN') {
      // version_314: 并行加载订单 + 商品名映射
      const merchantId = merchant.value.id
      const [orderList, products] = await Promise.all([
        listMerchantOrders(merchantId),
        listProducts(merchantId)
      ])
      orders.value = orderList
      productNames.value = Object.fromEntries(products.map((p) => [p.id, p.name]))
    }
  } catch (error) {
    // API client handles error messages
  } finally {
    loading.value = false
  }
}

// ===== test1: 原有接单操作 =====
async function handleAccept(orderId) {
  try {
    await acceptOrder(orderId)
    ElMessage.success('已接单')
    if (merchant.value) {
      const [orderList, products] = await Promise.all([
        listMerchantOrders(merchant.value.id),
        listProducts(merchant.value.id)
      ])
      orders.value = orderList
      productNames.value = Object.fromEntries(products.map((p) => [p.id, p.name]))
    }
  } catch (error) {
    // API client handles errors
  }
}

// ===== version_314: 通用订单操作方法 =====
async function operate(action, order) {
  if (action === 'accept') await acceptOrder(order.order.id)
  if (selectedOrder.value?.order.id === order.order.id) {
    selectedOrder.value = orders.value.find((item) => item.order.id === order.order.id) || null
  }
  await load()
}
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
  <div class="console-container" v-loading="loading">
    <!-- No merchant registered yet -->
    <el-card v-if="!merchant && !loading" class="box-card welcome-card">
      <div class="welcome-content">
        <el-icon class="welcome-icon" color="#409eff" :size="80"><Shop /></el-icon>
        <h2>欢迎来到 CLAS 商家中心</h2>
        <p>您目前还没有入驻平台。赶快提交商家入驻资质申请，开始您的商业之旅吧！</p>
        <div class="welcome-actions">
          <el-button type="primary" size="large" @click="router.push('/merchant-register')">
            立即申请入驻
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-else-if="merchant" class="console-layout">
      <!-- Left Info Panel -->
      <div class="sidebar-panel">
        <!-- Navigation Menu -->
        <el-card class="box-card nav-card" style="margin-bottom: 20px;">
          <div class="menu-list">
            <div 
              class="menu-item active"
              @click="router.push('/merchant-console')"
            >
              <el-icon><List /></el-icon>
              <span>接单管理</span>
            </div>
            <div 
              class="menu-item"
              @click="router.push('/merchant/products')"
            >
              <el-icon><Goods /></el-icon>
              <span>商品管理</span>
            </div>
          </div>
        </el-card>

        <el-card class="box-card info-card">
          <template #header>
            <div class="card-header">
              <h3>店铺基本信息</h3>
            </div>
          </template>

          <div class="merchant-badge">
            <div class="store-icon">{{ merchant.merchantName.substring(0, 1) }}</div>
            <h4>{{ merchant.merchantName }}</h4>
            <el-tag :type="statusMap[merchant.status]?.type || 'info'" size="large" effect="dark">
              {{ statusMap[merchant.status]?.text || merchant.status }}
            </el-tag>
          </div>

          <el-divider />

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="商家ID">{{ merchant.id }}</el-descriptions-item>
            <el-descriptions-item label="主营品类">{{ merchant.category }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ merchant.phone }}</el-descriptions-item>
            <el-descriptions-item label="银行账号">{{ merchant.bankAccount }}</el-descriptions-item>
            <el-descriptions-item label="结算周期">{{ merchant.settlementCycle }} 天</el-descriptions-item>
            <el-descriptions-item label="综合评分">
              <el-rate v-model="merchant.score" disabled show-score text-color="#ff9900" />
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="merchant.adminRemarks" class="admin-remarks">
            <h5>管理员备注:</h5>
            <p>{{ merchant.adminRemarks }}</p>
          </div>
        </el-card>
      </div>

      <!-- Right Work Area -->
      <div class="main-work-area">
        <!-- Status Tip Alert -->
        <el-alert
          v-if="merchant.status === 'PENDING'"
          title="您的商家入驻申请正在审核中"
          description="系统管理员会在 1-2 个工作日内处理您的入驻资质。请耐心等待，在此期间您暂时无法上架商品或开始营业。"
          type="warning"
          show-icon
          :closable="false"
          class="status-alert"
        />

        <el-alert
          v-else-if="merchant.status === 'APPROVED'"
          title="您的入驻申请已审核通过！"
          description="您的店铺资质已过审。系统管理员正在为您进行最后的系统接入，开通营业后即可正式开始处理订单。"
          type="info"
          show-icon
          :closable="false"
          class="status-alert"
        />

        <el-alert
          v-else-if="merchant.status === 'CLOSED'"
          title="店铺目前处于停业状态"
          description="店铺已暂停营业。若想重新开业营业，请联系管理员为您更新状态。"
          type="info"
          show-icon
          :closable="false"
          class="status-alert"
        />

        <el-alert
          v-else-if="merchant.status === 'BLOCKED'"
          title="店铺已被系统管理员禁用"
          description="由于违反平台运营规则或资质到期，您的店铺已被管理员禁用。请查阅备注并及时联系管理员申诉或整改。"
          type="error"
          show-icon
          :closable="false"
          class="status-alert"
        />

        <!-- Orders Work List (Only show when status is OPEN) -->
        <el-card v-if="merchant.status === 'OPEN'" class="box-card work-card">
          <template #header>
            <div class="card-header">
              <h3>待接单管理 (营业中)</h3>
            </div>
          </template>

          <el-table :data="orders" style="width: 100%" empty-text="当前没有待处理的订单">
            <el-table-column prop="order.id" label="订单编号" width="100" />
            <el-table-column label="订单金额" width="120">
              <template #default="scope">
                <span class="price-text">¥{{ (scope.row.order.totalPrice / 100).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="order.status" label="订单状态" width="140">
              <template #default="scope">
                <el-tag :type="scope.row.order.status === 'PAID' ? 'success' : 'info'">
                  {{ orderStatusLabel[scope.row.order.status] || scope.row.order.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品清单">
              <template #default="scope">
                <div v-for="item in scope.row.items" :key="item.id" class="order-item-list">
                  {{ productNames[item.productId] || item.name }} x {{ item.quantity }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  @click="openDetail(scope.row)"
                >
                  查看详情
                </el-button>
                <el-button
                  v-if="scope.row.order.status === 'PAID'"
                  type="primary"
                  size="small"
                  @click="handleAccept(scope.row.order.id)"
                >
                  确认接单
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card v-else class="box-card locked-card">
          <div class="locked-content">
            <el-icon :size="50" color="#909399"><Lock /></el-icon>
            <h4>工作台锁定</h4>
            <p>非“营业中”状态的商家无法接入工作台处理订单订单。</p>
          </div>
        </el-card>
      </div>
    </div>
    </div>

    <!-- ===== version_314: 订单详情侧滑弹窗 ===== -->
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
.console-container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
}

.welcome-card {
  text-align: center;
  padding: 60px 0;
  border-radius: 12px;
}

.welcome-content h2 {
  margin: 20px 0 10px 0;
  color: #303133;
}

.welcome-content p {
  color: #606266;
  max-width: 480px;
  margin: 0 auto 30px auto;
}

.console-layout {
  display: flex;
  gap: 24px;
}

.sidebar-panel {
  flex: 1;
  min-width: 320px;
}

.main-work-area {
  flex: 3;
}

.info-card {
  border-radius: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.merchant-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 10px 0;
}

.store-icon {
  width: 64px;
  height: 64px;
  background-color: #409eff;
  color: #fff;
  font-size: 28px;
  font-weight: bold;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.merchant-badge h4 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #303133;
}

.admin-remarks {
  margin-top: 20px;
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 6px;
  padding: 12px;
}

.admin-remarks h5 {
  margin: 0 0 6px 0;
  color: #f56c6c;
}

.admin-remarks p {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.status-alert {
  margin-bottom: 20px;
  border-radius: 8px;
}

.work-card {
  border-radius: 12px;
}

.price-text {
  color: #f56c6c;
  font-weight: bold;
}

.order-item-list {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

.locked-card {
  text-align: center;
  padding: 80px 0;
  border-radius: 12px;
}

.locked-content h4 {
  margin: 12px 0 8px 0;
  color: #303133;
}

.locked-content p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.nav-card {
  border-radius: 12px;
}
.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
}
.menu-item:hover {
  color: #409eff;
  background-color: #f0f7ff;
}
.menu-item.active {
  color: #ffffff;
  background-color: #409eff;
}

/* ===== version_314: 订单卡片 & 详情弹窗样式 ===== */
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
