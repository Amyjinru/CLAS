<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
// ===== test1: 商户审核 API =====
import { getMyMerchant, listMerchantOrders, acceptOrder, currentUser, currentRole, listProducts, rejectOrder, deliverOrder, redeemDeal, listReviewsByMerchant, replyReview, approveRefund, rejectRefund, uploadMerchantLogo, toggleMerchantManualClosed } from '../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'

// ===== version_314: 订单详情组件 =====
import OrderDetailContent from '../components/OrderDetailContent.vue'
import ChatWindow from '../components/ChatWindow.vue'
import MerchantReviewSection from '../components/MerchantReviewSection.vue'
import MerchantSidebar from '../components/merchant/MerchantSidebar.vue'
import MerchantProfileEditDialog from '../components/merchant/MerchantProfileEditDialog.vue'
import { useChatStore } from '../composables/useChatStore'

const router = useRouter()
const merchant = ref(null)
const orders = ref([])
const loading = ref(true)
const loginUser = ref(null)
const voucherCode = ref('')
const reviews = ref([])
const replyDrafts = ref({})
const logoInputRef = ref(null)
const logoUploading = ref(false)
const profileDialogVisible = ref(false)
const chatStore = useChatStore()
const displayStatus = computed(() => resolveDisplayStatus(merchant.value))

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
  COMPLETED: '已完成',
  CANCELED: '已取消',
  REJECTED: '商家已拒单',
  REFUNDED: '已退款',
  REFUND_PENDING: '退款处理中'
}

function resolveDisplayStatus(merchantInfo) {
  if (!merchantInfo) return { text: '-', type: 'info', open: false }
  if (merchantInfo.status !== 'OPEN') {
    const mapped = statusMap[merchantInfo.status] || { text: merchantInfo.status, type: 'info' }
    return { ...mapped, open: false }
  }
  if (merchantInfo.manualClosed) {
    return { text: '已打烊', type: 'info', open: false }
  }
  if (!isWithinBusinessHours(merchantInfo.businessHours)) {
    return { text: '已打烊', type: 'info', open: false }
  }
  return { text: '营业中', type: 'success', open: true }
}

function isWithinBusinessHours(hoursText) {
  if (!hoursText || !hoursText.includes('-')) return true
  const [startText, endText] = hoursText.split('-').map((item) => item.trim())
  const start = parseBusinessMinutes(startText)
  const end = parseBusinessMinutes(endText)
  if (start === null || end === null || start === end) return true
  const nowDate = new Date()
  const now = nowDate.getHours() * 60 + nowDate.getMinutes()
  return start < end ? now >= start && now < end : now >= start || now < end
}

function parseBusinessMinutes(value) {
  const match = /^(\d{2}):(\d{2})$/.exec((value || '').trim())
  if (!match) return null
  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours > 23 || minutes > 59) return null
  return hours * 60 + minutes
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
      const [orderList, products, reviewList] = await Promise.all([
        listMerchantOrders(),
        listProducts(merchantId),
        listReviewsByMerchant(merchantId)
      ])
      orders.value = orderList
      reviews.value = reviewList
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
        listMerchantOrders(),
        listProducts(merchant.value.id)
      ])
      orders.value = orderList
      productNames.value = Object.fromEntries(products.map((p) => [p.id, p.name]))
    }
  } catch (error) {
    // API client handles errors
  }
}

async function handleReject(orderId) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒单理由', '拒单', {
      confirmButtonText: '确认拒单',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：商品售罄、超出配送范围'
    })
    if (!value?.trim()) {
      ElMessage.warning('请填写拒单理由')
      return
    }
    await rejectOrder(orderId, value.trim())
    ElMessage.success('已拒单')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('拒单失败')
    }
  }
}

async function handleDeliver(orderId) {
  await deliverOrder(orderId)
  ElMessage.success('订单已标记为配送中')
  await load()
}

async function handleRefund(orderId, approved) {
  if (approved) {
    await approveRefund(orderId)
    ElMessage.success('已通过退款')
  } else {
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝退款的理由（可选）', '拒绝退款', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：订单已完成配送'
      })
      await rejectRefund(orderId, value?.trim() || undefined)
      ElMessage.success('已拒绝退款')
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('操作失败')
      }
      return
    }
  }
  await load()
}

async function handleRedeem() {
  if (!voucherCode.value.trim()) {
    ElMessage.warning('请输入团购券码')
    return
  }
  const order = await redeemDeal(voucherCode.value.trim())
  ElMessage.success(`核销成功：${order.voucherCode}`)
  voucherCode.value = ''
}

async function handleReply(review) {
  const reply = replyDrafts.value[review.id]
  if (!reply?.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await replyReview(review.id, reply.trim())
  ElMessage.success('回复已发布')
  replyDrafts.value[review.id] = ''
  await load()
}

function openLogoPicker() {
  logoInputRef.value?.click()
}

function beforeLogoUpload(file) {
  const allowed = ['image/jpeg', 'image/png']
  if (!allowed.includes(file.type)) {
    ElMessage.warning('仅支持 jpg/png 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return false
  }
  return true
}

async function onLogoSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || !beforeLogoUpload(file)) return
  logoUploading.value = true
  try {
    merchant.value = await uploadMerchantLogo(file)
    ElMessage.success('店铺头像已更新')
  } finally {
    logoUploading.value = false
  }
}

// ===== version_314: 通用订单操作方法 =====
async function operate(action, order) {
  if (action === 'accept') await acceptOrder(order.order.id)
  if (action === 'reject') {
    await handleReject(order.order.id)
    return
  }
  if (selectedOrder.value?.order.id === order.order.id) {
    selectedOrder.value = orders.value.find((item) => item.order.id === order.order.id) || null
  }
  await load()
}

function openDetail(order) {
  selectedOrder.value = order
}

const chatOrder = ref(null)

function closeDetail() {
  selectedOrder.value = null
}

function openChat(order) {
  chatOrder.value = order
}

function closeChat() {
  chatOrder.value = null
}

async function openReplyPanel() {
  await chatStore.openReplyPanel()
}

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}

async function toggleManualClosed() {
  merchant.value = await toggleMerchantManualClosed()
  ElMessage.success(merchant.value.manualClosed ? '已手动打烊' : '已恢复默认营业状态')
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
        <MerchantSidebar active="orders" @edit-profile="profileDialogVisible = true" />

        <el-card class="box-card info-card">
          <template #header>
            <div class="card-header">
              <h3>店铺基本信息</h3>
            </div>
          </template>

          <div class="merchant-badge">
            <button class="store-logo-button" type="button" :disabled="logoUploading" @click="openLogoPicker">
              <img v-if="merchant.logo" :src="merchant.logo" alt="店铺头像" class="store-logo-img" />
              <span v-else class="store-icon">{{ merchant.merchantName.substring(0, 1) }}</span>
            </button>
            <input
              ref="logoInputRef"
              class="logo-input"
              type="file"
              accept="image/jpeg,image/png"
              @change="onLogoSelected"
            />
            <el-button size="small" type="primary" :loading="logoUploading" @click="openLogoPicker">
              上传店铺头像
            </el-button>
            <h4>{{ merchant.merchantName }}</h4>
            <el-tag :type="displayStatus.type" size="large" effect="dark">
              {{ displayStatus.text }}
            </el-tag>
            <el-button
              v-if="merchant.status === 'OPEN'"
              size="small"
              :type="merchant.manualClosed ? 'success' : 'warning'"
              @click="toggleManualClosed"
            >
              {{ merchant.manualClosed ? '恢复默认状态' : '手动打烊' }}
            </el-button>
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

        <div v-if="merchant.status !== 'OPEN'" class="status-actions">
          <el-button type="primary" plain @click="router.push('/merchant/audit-status')">
            查看审核进度
          </el-button>
        </div>

        <!-- Orders Work List (Only show when status is OPEN) -->
        <el-card v-if="merchant.status === 'OPEN'" class="box-card work-card">
          <template #header>
          <div class="card-header">
            <h3>待接单管理 (营业中)</h3>
            <el-button type="primary" @click="openReplyPanel">回复客户</el-button>
          </div>
          </template>

          <div class="redeem-box">
            <el-input v-model="voucherCode" placeholder="输入团购券码进行到店核销" clearable />
            <el-button type="primary" @click="handleRedeem">核销团购券</el-button>
          </div>

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
            <el-table-column label="配送" width="180">
              <template #default="scope">
                <div>{{ scope.row.order.deliveryStatus || 'WAITING' }}</div>
                <small v-if="scope.row.order.deliveryAddress">{{ scope.row.order.deliveryAddress }}</small>
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
                <el-button
                  v-if="scope.row.order.status === 'PAID'"
                  type="danger"
                  size="small"
                  @click="handleReject(scope.row.order.id)"
                >
                  拒单
                </el-button>
                <el-button
                  v-if="scope.row.order.status === 'ACCEPTED' && scope.row.order.deliveryStatus !== 'DELIVERING'"
                  type="warning"
                  size="small"
                  @click="handleDeliver(scope.row.order.id)"
                >
                  配送中
                </el-button>
                <el-button
                  v-if="scope.row.order.status === 'REFUND_PENDING'"
                  type="primary"
                  size="small"
                  @click="handleRefund(scope.row.order.id, true)"
                >
                  通过退款
                </el-button>
                <el-button
                  v-if="scope.row.order.status === 'REFUND_PENDING'"
                  type="danger"
                  size="small"
                  @click="handleRefund(scope.row.order.id, false)"
                >
                  拒绝退款
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card v-if="merchant.status === 'OPEN'" class="box-card work-card review-card">
          <template #header>
            <div class="card-header">
              <h3>评价回复</h3>
            </div>
          </template>
          <el-table :data="reviews" style="width: 100%" empty-text="暂无评价">
            <el-table-column prop="score" label="评分" width="90" />
            <el-table-column prop="content" label="评价内容" />
            <el-table-column prop="merchantReply" label="商家回复" />
            <el-table-column label="回复" width="260">
              <template #default="scope">
                <el-input v-model="replyDrafts[scope.row.id]" placeholder="输入回复" size="small" />
                <el-button size="small" type="primary" @click="handleReply(scope.row)">发布</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <MerchantReviewSection
          v-if="merchant.status === 'OPEN'"
          :merchant-id="merchant.id"
          :show-merchant-actions="true"
        />

        <el-card v-if="merchant.status !== 'OPEN'" class="box-card locked-card">
          <div class="locked-content">
            <el-icon :size="50" color="#909399"><Lock /></el-icon>
            <h4>工作台锁定</h4>
            <p>非“营业中”状态的商家无法接入工作台处理订单订单。</p>
          </div>
        </el-card>
      </div>
    </div>
    </div>

    <MerchantProfileEditDialog
      v-model:visible="profileDialogVisible"
      :merchant="merchant"
      @saved="onMerchantProfileSaved"
    />

    <!-- Chat overlay -->
    <div v-if="chatOrder" class="order-overlay" @click.self="closeChat">
      <aside class="chat-panel">
        <header class="chat-panel-head">
          <h2>与用户沟通</h2>
          <p class="chat-panel-subtitle">订单 #{{ chatOrder.order.id }}</p>
          <button class="panel-close" type="button" @click="closeChat">×</button>
        </header>
        <div class="chat-panel-body">
          <ChatWindow
            :order-id="chatOrder.order.id"
            :merchant-id="chatOrder.order.merchantId"
            :merchant-name="merchant?.merchantName || ''"
            role="MERCHANT"
            :order-status="chatOrder.order.status"
            :order-number="chatOrder.order.id"
          />
        </div>
      </aside>
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
            v-if="['PAID', 'ACCEPTED'].includes(selectedOrder.order.status)"
            type="button"
            class="secondary"
            @click="openChat(selectedOrder)"
          >
            联系用户
          </button>
          <button
            v-if="selectedOrder.order.status === 'PAID'"
            type="button"
            @click="operate('accept', selectedOrder)"
          >
            接单
          </button>
          <button
            v-if="selectedOrder.order.status === 'PAID'"
            class="secondary"
            type="button"
            @click="operate('reject', selectedOrder)"
          >
            拒单
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

.redeem-box {
  align-items: center;
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.redeem-box :deep(.el-input) {
  max-width: 320px;
}

.review-card {
  margin-top: 20px;
}

.review-card :deep(.cell) {
  display: flex;
  gap: 8px;
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

.store-logo-button {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  height: 64px;
  justify-content: center;
  margin-bottom: 10px;
  padding: 0;
  width: 64px;
}

.store-logo-button:disabled {
  cursor: wait;
  opacity: 0.75;
}

.store-logo-img {
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  height: 64px;
  object-fit: cover;
  width: 64px;
}

.logo-input {
  display: none;
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

.status-actions {
  margin: -8px 0 20px;
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
  transition-property: color, background-color;
  transition-duration: 0.3s;
  transition-timing-function: ease;
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
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xl);
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

/* Chat panel styles */
.console-container .chat-panel {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.16);
  display: flex;
  flex-direction: column;
  height: 520px;
  max-width: 480px;
  width: 100%;
}

.chat-panel-head {
  align-items: center;
  border-bottom: 1px solid #eef2f7;
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
  color: #667085;
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
