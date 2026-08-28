<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Shop } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { toggleMerchantManualClosed, uploadMerchantLogo } from '../../api/clas'
import MerchantProfileEditDialog from './MerchantProfileEditDialog.vue'

const props = defineProps({
  merchant: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  section: {
    type: String,
    default: 'workspace'
  },
  activeModule: {
    type: String,
    default: ''
  },
  showMerchantInfo: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['merchant-updated'])
const router = useRouter()
const logoInputRef = ref(null)
const logoUploading = ref(false)
const profileDialogVisible = ref(false)

const moduleItems = [
  { key: 'orders', label: '接单管理', path: '/merchant-console' },
  { key: 'analytics', label: '经营分析', path: '/merchant/analytics' },
  { key: 'products', label: '商品管理', path: '/merchant/products' },
  { key: 'deals', label: '团购管理', path: '/merchant/deals' },
  { key: 'messages', label: '客户信息', path: '/merchant/messages' }
]

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已审核', type: 'info' },
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '停业中', type: 'danger' },
  BLOCKED: { text: '已禁用', type: 'danger' }
}

const displayStatus = computed(() => {
  const merchant = props.merchant
  if (!merchant) return { text: '-', type: 'info' }
  if (merchant.status === 'OPEN' && merchant.manualClosed) {
    return { text: '已打烊', type: 'info' }
  }
  return statusMap[merchant.status] || { text: merchant.status || '-', type: 'info' }
})

function openLogoPicker() {
  logoInputRef.value?.click()
}

function beforeLogoUpload(file) {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
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
    const nextMerchant = await uploadMerchantLogo(file)
    emit('merchant-updated', nextMerchant)
    ElMessage.success('店铺头像已更新')
  } finally {
    logoUploading.value = false
  }
}

async function toggleManualStatus() {
  const nextMerchant = await toggleMerchantManualClosed()
  emit('merchant-updated', nextMerchant)
  ElMessage.success(nextMerchant.manualClosed ? '已手动打烊' : '已恢复默认营业状态')
}

function onProfileSaved(nextMerchant) {
  emit('merchant-updated', nextMerchant)
}

function openProfileDialog() {
  profileDialogVisible.value = true
}
</script>

<template>
  <div class="merchant-shell" v-loading="loading">
    <el-card v-if="!merchant && !loading" class="welcome-card">
      <div class="welcome-content">
        <el-icon class="welcome-icon" color="var(--color-primary)" :size="80"><Shop /></el-icon>
        <h2>欢迎来到 CLAS 商家中心</h2>
        <p>您目前还没有入驻平台。赶快提交商家入驻资质申请，开始您的商业之旅吧！</p>
        <el-button type="primary" size="large" @click="router.push('/merchant-register')">立即申请入驻</el-button>
      </div>
    </el-card>

    <template v-else-if="merchant">
      <nav v-if="section === 'workspace'" class="module-tabs" aria-label="商家工作台模块导航">
        <button
          v-for="item in moduleItems"
          :key="item.key"
          type="button"
          class="module-tab"
          :class="{ active: activeModule === item.key }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div :class="['workspace-grid', { 'without-shop-info': !showMerchantInfo }]">
        <aside v-if="showMerchantInfo" class="shop-info">
          <div class="shop-profile">
            <button class="store-logo-button" type="button" :disabled="logoUploading" @click="openLogoPicker">
              <img v-if="merchant.logo" :src="merchant.logo" alt="店铺头像" class="store-logo-img" />
              <span v-else class="store-icon">{{ merchant.merchantName?.substring(0, 1) || '店' }}</span>
            </button>
            <input
              ref="logoInputRef"
              class="logo-input"
              type="file"
              accept="image/jpeg,image/png"
              @change="onLogoSelected"
            />
            <h2>{{ merchant.merchantName }}</h2>
            <el-tag :type="displayStatus.type" effect="dark">{{ displayStatus.text }}</el-tag>
            <div class="shop-actions">
              <el-button class="shop-action-btn" size="small" type="primary" :loading="logoUploading" @click="openLogoPicker">上传头像</el-button>
              <el-button
                v-if="merchant.status === 'OPEN'"
                class="shop-action-btn"
                size="small"
                :type="merchant.manualClosed ? 'success' : 'warning'"
                @click="toggleManualStatus"
              >
                {{ merchant.manualClosed ? '恢复营业' : '手动打烊' }}
              </el-button>
            </div>
          </div>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="商家ID">{{ merchant.id }}</el-descriptions-item>
            <el-descriptions-item label="主营品类">{{ merchant.category || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ merchant.phone || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="营业时间">{{ merchant.businessHours || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="店铺地址">{{ merchant.address || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="结算周期">{{ merchant.settlementCycle || '-' }} 天</el-descriptions-item>
            <el-descriptions-item label="综合评分">
              <el-rate v-model="merchant.score" disabled show-score text-color="var(--color-warning)" />
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="merchant.adminRemarks" class="admin-remarks">
            <strong>管理员备注</strong>
            <p>{{ merchant.adminRemarks }}</p>
          </div>
        </aside>

        <main class="workspace-content">
          <slot :merchant="merchant" :open-profile-dialog="openProfileDialog" />
        </main>
      </div>
    </template>

    <MerchantProfileEditDialog
      v-model:visible="profileDialogVisible"
      :merchant="merchant"
      @saved="onProfileSaved"
    />
  </div>
</template>

<style scoped>
.merchant-shell {
  margin: 30px auto;
  max-width: 1200px;
  padding: 0 20px 48px;
}

.welcome-card {
  border-radius: var(--radius-lg);
  padding: 60px 0;
  text-align: center;
}

.welcome-content h2 {
  color: var(--text-primary);
  margin: 20px 0 10px;
}

.welcome-content p {
  color: var(--text-secondary);
  margin: 0 auto 30px;
  max-width: 480px;
}

.module-tabs {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.module-tabs {
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 18px;
  padding-bottom: 14px;
}

.module-tab {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition-duration: 0.2s;
  transition-property: background-color, border-color, color, box-shadow;
  transition-timing-function: ease;
}

.module-tab {
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  min-width: 96px;
  padding: 9px 14px;
}

.module-tab:active {
  transform: scale(0.97);
}

.module-tab.active {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.workspace-grid {
  align-items: start;
  display: grid;
  gap: 24px;
  grid-template-columns: 320px minmax(0, 1fr);
}

.workspace-grid.without-shop-info {
  grid-template-columns: minmax(0, 1fr);
}

.shop-info {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 18px;
  position: sticky;
  top: 18px;
}

.shop-profile {
  align-items: center;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  text-align: center;
}

.shop-profile h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}

.store-logo-button {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  height: 72px;
  justify-content: center;
  padding: 0;
  width: 72px;
}

.store-logo-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.store-logo-img,
.store-icon {
  border: 1px solid var(--border-color);
  border-radius: 50%;
  height: 72px;
  width: 72px;
}

.store-logo-img {
  object-fit: cover;
}

.store-icon {
  align-items: center;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  font-size: 28px;
  font-weight: 800;
  justify-content: center;
}

.logo-input {
  display: none;
}

.shop-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.shop-actions :deep(.shop-action-btn) {
  font-size: 12px;
  min-height: 28px;
  padding: 4px 12px;
}

.admin-remarks {
  background: var(--color-danger-light);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  margin-top: 14px;
  padding: 10px;
}

.admin-remarks p {
  font-size: 13px;
  margin: 6px 0 0;
}

.workspace-content {
  min-width: 0;
}

@media (max-width: 900px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .shop-info {
    position: static;
  }
}

@media (max-width: 640px) {
  .merchant-shell {
    margin-top: 18px;
    padding: 0 14px 36px;
  }

  .module-tabs {
    flex-wrap: nowrap;
    margin-left: -14px;
    margin-right: -14px;
    overflow-x: auto;
    padding: 0 14px 14px;
    scrollbar-width: none;
  }

  .module-tabs::-webkit-scrollbar {
    display: none;
  }

  .module-tab {
    flex: 0 0 auto;
  }
}
</style>
