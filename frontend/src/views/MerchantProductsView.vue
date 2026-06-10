<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMyMerchant,
  getMerchantProducts,
  updateProductStatus,
  deleteProduct,
  listProductCategories,
  uploadMerchantLogo,
  currentUser
} from '../api/clas'
import { ElMessage } from 'element-plus'
import MoneyText from '../components/MoneyText.vue'
import StatusTag from '../components/StatusTag.vue'
import ProductStatusAction from '../components/product/ProductStatusAction.vue'
import ProductCategoryManager from '../components/product/ProductCategoryManager.vue'
import ProductFormDialog from '../components/product/ProductFormDialog.vue'
import MerchantSidebar from '../components/merchant/MerchantSidebar.vue'
import MerchantProfileEditDialog from '../components/merchant/MerchantProfileEditDialog.vue'
import { merchantStatusMap, productStatusMap } from '../utils/status'

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
const categoryFilter = ref('')
const categories = ref([])

// Dialog state
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedProduct = ref(null)
const logoInputRef = ref(null)
const logoUploading = ref(false)
const profileDialogVisible = ref(false)

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
      await Promise.all([loadCategories(), loadProducts()])
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
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    }
    if (categoryFilter.value !== '') {
      params.categoryId = categoryFilter.value
    }
    const res = await getMerchantProducts(params)
    products.value = res.list
    total.value = res.total
  } catch (error) {
    // API client handles errors
  }
}

async function loadCategories() {
  categories.value = await listProductCategories()
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
  selectedProduct.value = null
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  selectedProduct.value = row
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

function onCategoryReload({ deletedId } = {}) {
  if (deletedId && categoryFilter.value === deletedId) {
    categoryFilter.value = ''
  }
  Promise.all([loadCategories(), loadProducts()])
}

function onProductSaved() {
  loadProducts()
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

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
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
        <MerchantSidebar active="products" @edit-profile="profileDialogVisible = true" />
        <!-- Navigation Menu -->
        <el-card v-if="false" class="box-card nav-card" style="margin-bottom: 20px;">
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
            <StatusTag :status="merchant.status" :map="merchantStatusMap" size="large" effect="dark" />
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

        <ProductCategoryManager :categories="categories" @reload="onCategoryReload" />

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
            <el-select
              v-model="categoryFilter"
              placeholder="全部分类"
              clearable
              style="width: 180px; margin-left: 12px;"
              @change="handleSearch"
              @clear="handleSearch"
            >
              <el-option label="未分类" :value="0" />
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
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
                <span class="price-text"><MoneyText :amount="scope.row.price" /></span>
              </template>
            </el-table-column>

            <el-table-column prop="stock" label="库存" width="100" />

            <el-table-column label="所属分类" width="120">
              <template #default="scope">
                <el-tag effect="plain">{{ scope.row.categoryName || '未分类' }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="100">
              <template #default="scope">
                <StatusTag :status="scope.row.status" :map="productStatusMap" />
              </template>
            </el-table-column>

            <el-table-column label="上/下架" width="90">
              <template #default="scope">
                <ProductStatusAction
                  :active="scope.row.status === 'ON_SALE'"
                  :disabled="merchant.status === 'PENDING' || merchant.status === 'BLOCKED'"
                  @toggle="handleStatusToggle(scope.row)"
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
                    <el-button type="danger" size="small" icon="Delete">删除</el-button>
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

    <MerchantProfileEditDialog
      v-model:visible="profileDialogVisible"
      :merchant="merchant"
      @saved="onMerchantProfileSaved"
    />

    <ProductFormDialog
      v-model:visible="dialogVisible"
      :is-edit="isEdit"
      :product="selectedProduct"
      :categories="categories"
      @saved="onProductSaved"
    />
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
  color: var(--text-primary);
}

.welcome-content p {
  color: var(--text-secondary);
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
  color: var(--text-primary);
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
  border: 1px solid var(--border-color);
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

.work-card { border-radius: 12px; }

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  transition-property: color, background-color;
  transition-duration: 0.3s;
  transition-timing-function: ease;
}

.menu-item:hover {
  color: var(--color-primary);
  background-color: var(--color-primary-light);
}

.menu-item.active {
  color: var(--text-primary);
  background-color: var(--color-primary);
}
</style>
