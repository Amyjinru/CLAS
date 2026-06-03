<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyMerchant, listMerchantOrders, acceptOrder, currentUser } from '../api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const merchant = ref(null)
const orders = ref([])
const loading = ref(true)
const loginUser = ref(null)

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已审核', type: 'info' },
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '停业中', type: 'danger' },
  BLOCKED: { text: '已禁用', type: 'danger' }
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
      orders.value = await listMerchantOrders(merchant.value.id)
    }
  } catch (error) {
    // API client handles error messages
  } finally {
    loading.value = false
  }
}

async function handleAccept(orderId) {
  try {
    await acceptOrder(orderId)
    ElMessage.success('已接单')
    orders.value = await listMerchantOrders(merchant.value.id)
  } catch (error) {
    // API client handles errors
  }
}

onMounted(load)
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
            <el-table-column prop="order.status" label="订单状态" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.order.status === 'PAID' ? 'success' : 'info'">
                  {{ scope.row.order.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品清单">
              <template #default="scope">
                <div v-for="item in scope.row.items" :key="item.id" class="order-item-list">
                  {{ item.name }} x {{ item.quantity }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
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
</style>
