<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMyMerchant,
  getMerchantProducts,
  createProduct,
  updateProduct,
  updateProductStatus,
  deleteProduct,
  currentUser
} from '../api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const merchant = ref(null)
const products = ref([])
const loading = ref(true)
const loginUser = ref(null)

// Pagination & Search
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

// Dialog state
const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const formRef = ref(null)
const isEdit = ref(false)

const form = ref({
  id: null,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  imageUrl: ''
})

const rules = {
  name: [
    { required: true, message: '商品名称不能为空', trigger: 'blur' },
    { max: 50, message: '商品名称不能超过50个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '商品价格不能为空', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于 0', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '库存不能为空', trigger: 'blur' },
    { type: 'integer', min: 0, message: '库存不能小于 0', trigger: 'blur' }
  ]
}

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已审核', type: 'info' },
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '停业中', type: 'danger' },
  BLOCKED: { text: '已禁用', type: 'danger' }
}

async function loadMerchant() {
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
    if (merchant.value) {
      await loadProducts()
    }
  } catch (error) {
    // API client handles errors
  } finally {
    loading.value = false
  }
}

async function loadProducts() {
  if (!merchant.value) return
  try {
    const res = await getMerchantProducts({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    products.value = res.list
    total.value = res.total
  } catch (error) {
    // API client handles errors
  }
}

function handleSearch() {
  currentPage.value = 1
  loadProducts()
}

function handlePageChange(page) {
  currentPage.value = page
  loadProducts()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadProducts()
}

function openAddDialog() {
  isEdit.value = false
  dialogTitle.value = '新增商品'
  form.value = {
    id: null,
    name: '',
    description: '',
    price: 0,
    stock: 0,
    imageUrl: ''
  }
  dialogVisible.value = true
  if (formRef.value) formRef.value.resetFields()
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑商品'
  form.value = {
    id: row.id,
    name: row.name,
    description: row.description || '',
    price: row.price / 100, // Convert cents to Yuan for display
    stock: row.stock,
    imageUrl: row.imageUrl || ''
  }
  dialogVisible.value = true
}

async function handleStatusToggle(row) {
  const nextStatus = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  try {
    await updateProductStatus(row.id, nextStatus)
    ElMessage.success(nextStatus === 'ON_SALE' ? '商品已上架' : '商品已下架')
    await loadProducts()
  } catch (error) {
    // Keep switch in original state if error occurs
    row.status = row.status // Vue will re-render
  }
}

async function handleDelete(id) {
  try {
    await deleteProduct(id)
    ElMessage.success('商品已删除')
    // If we delete the last item on the page, go to previous page
    if (products.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    await loadProducts()
  } catch (error) {
    // handled
  }
}

function submitForm() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    
    // Prepare payload (convert Yuan back to cents/fen for price)
    const payload = {
      ...form.value,
      price: Math.round(form.value.price * 100)
    }

    try {
      if (isEdit.value) {
        await updateProduct(payload)
        ElMessage.success('商品修改成功')
      } else {
        await createProduct(payload)
        ElMessage.success('商品添加成功，默认为下架状态')
      }
      dialogVisible.value = false
      await loadProducts()
    } catch (error) {
      // handled
    }
  })
}

onMounted(loadMerchant)
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
              class="menu-item"
              @click="router.push('/merchant-console')"
            >
              <el-icon><List /></el-icon>
              <span>接单管理</span>
            </div>
            <div 
              class="menu-item active"
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
        <!-- Status Tip Alert (Show alerts for non-active states) -->
        <el-alert
          v-if="merchant.status === 'PENDING'"
          title="您的商家入驻申请正在审核中"
          description="在此期间，您的商品可以编辑添加，但暂时无法上架销售。"
          type="warning"
          show-icon
          :closable="false"
          class="status-alert"
        />
        <el-alert
          v-else-if="merchant.status === 'BLOCKED'"
          title="店铺已被系统管理员禁用"
          description="您的店铺目前处于禁用状态，商品已被锁定下架，无法执行上架操作。"
          type="error"
          show-icon
          :closable="false"
          class="status-alert"
        />

        <el-card class="box-card work-card">
          <template #header>
            <div class="card-header justify-between">
              <h3>商品管理</h3>
              <el-button 
                type="success" 
                icon="Plus" 
                @click="openAddDialog"
                :disabled="merchant.status === 'BLOCKED'"
              >
                新增商品
              </el-button>
            </div>
          </template>

          <!-- Search Bar -->
          <div class="search-bar">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称/描述"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
              style="width: 300px; margin-right: 12px;"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          </div>

          <!-- Product Table -->
          <el-table :data="products" style="width: 100%" empty-text="未找到相关商品">
            <el-table-column label="商品图片" width="100">
              <template #default="scope">
                <el-image
                  :src="scope.row.imageUrl || '/images/default-product.png'"
                  fit="cover"
                  class="product-table-img"
                >
                  <template #error>
                    <div class="image-slot">
                      <el-icon :size="24" color="#909399"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </template>
            </el-table-column>

            <el-table-column prop="name" label="商品名称" min-width="150" show-overflow-tooltip>
              <template #default="scope">
                <span class="product-name">{{ scope.row.name }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="description" label="商品描述" min-width="180" show-overflow-tooltip>
              <template #default="scope">
                <span class="product-desc">{{ scope.row.description || '暂无描述' }}</span>
              </template>
            </el-table-column>

            <el-table-column label="价格" width="110">
              <template #default="scope">
                <span class="price-text">¥{{ (scope.row.price / 100).toFixed(2) }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="stock" label="库存" width="100" />

            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ON_SALE' ? 'success' : 'info'">
                  {{ scope.row.status === 'ON_SALE' ? '上架中' : '下架中' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="上/下架" width="90">
              <template #default="scope">
                <el-switch
                  :model-value="scope.row.status === 'ON_SALE'"
                  @change="handleStatusToggle(scope.row)"
                  :disabled="merchant.status === 'PENDING' || merchant.status === 'BLOCKED'"
                  active-color="#13ce66"
                  inactive-color="#ff4949"
                />
              </template>
            </el-table-column>

            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <el-button 
                  type="primary" 
                  size="small" 
                  icon="Edit" 
                  @click="openEditDialog(scope.row)"
                  :disabled="merchant.status === 'BLOCKED'"
                  text
                >
                  编辑
                </el-button>
                <el-popconfirm
                  title="确定要删除这件商品吗？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleDelete(scope.row.id)"
                >
                  <template #reference>
                    <el-button type="danger" size="small" icon="Delete" text>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <!-- Pagination -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[5, 10, 20, 50]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-card>
      </div>
    </div>

    <!-- Product Form Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        style="padding: 10px 20px 0 0;"
      >
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxLength="50" />
        </el-form-item>

        <el-form-item label="商品价格" prop="price">
          <el-input-number 
            v-model="form.price" 
            :precision="2" 
            :step="0.5" 
            :min="0" 
            placeholder="单位：元" 
            style="width: 180px;"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 13px;">单位：元</span>
        </el-form-item>

        <el-form-item label="商品库存" prop="stock">
          <el-input-number 
            v-model="form.stock" 
            :min="0" 
            :step="1" 
            placeholder="库存量" 
            style="width: 180px;"
          />
        </el-form-item>

        <el-form-item label="商品图片" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="请输入图片URL或相对路径，如 /images/product.jpg" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品详细描述"
            maxLength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
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

.card-header {
  display: flex;
  align-items: center;
}

.card-header.justify-between {
  justify-content: space-between;
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

.search-bar {
  display: flex;
  margin-bottom: 20px;
}

.product-table-img {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
}

.product-name {
  font-weight: bold;
  color: #303133;
}

.product-desc {
  color: #909399;
  font-size: 13px;
}

.price-text {
  color: #f56c6c;
  font-weight: bold;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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
