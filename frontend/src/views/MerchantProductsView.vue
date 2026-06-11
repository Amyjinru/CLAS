<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMyMerchant,
  getMerchantProducts,
  updateProductStatus,
  deleteProduct,
  listProductCategories,
  currentUser
} from '../api/clas'
import { ElMessage } from 'element-plus'
import MoneyText from '../components/MoneyText.vue'
import StatusTag from '../components/StatusTag.vue'
import ProductStatusAction from '../components/product/ProductStatusAction.vue'
import ProductCategoryManager from '../components/product/ProductCategoryManager.vue'
import ProductFormDialog from '../components/product/ProductFormDialog.vue'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'
import { productStatusMap } from '../utils/status'

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

function onMerchantProfileSaved(nextMerchant) {
  merchant.value = nextMerchant
}

onMounted(loadMerchant)
</script>

<template>
  <MerchantWorkspaceShell
    :merchant="merchant"
    :loading="loading"
    active-module="products"
    @merchant-updated="onMerchantProfileSaved"
  >
    <div v-if="merchant" class="main-work-area">
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
              class="search-input"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
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
              class="category-select"
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

            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <div class="action-stack">
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
                      <el-button class="action-danger-btn" type="danger" size="small" icon="Delete">删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
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

    <ProductFormDialog
      v-model:visible="dialogVisible"
      :is-edit="isEdit"
      :product="selectedProduct"
      :categories="categories"
      @saved="onProductSaved"
    />
  </MerchantWorkspaceShell>
</template>

<style scoped>
.main-work-area {
  min-width: 0;
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

.status-alert {
  margin-bottom: 20px;
  border-radius: 8px;
}

.work-card { border-radius: 12px; }

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
}

.search-input { width: 300px; }

.category-select { width: 180px; }

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

.action-stack {
  align-items: stretch;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 把 popconfirm 行内span强制改成块级，占满整行 */
.action-stack :deep(.el-popconfirm) {
  display: block !important;
  width: 100%;
  margin: 0 !important;
}

/* 两个按钮统一清空外边距、100%宽度铺满 */
.action-stack :deep(.el-button) {
  width: 100%;
  margin: 0 !important;
}
</style>
