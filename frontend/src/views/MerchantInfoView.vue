<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { currentRole, currentUser, getMyMerchant } from '../api/clas'
import MerchantWorkspaceShell from '../components/merchant/MerchantWorkspaceShell.vue'

const router = useRouter()
const loading = ref(true)
const merchant = ref(null)

async function loadMerchant() {
  loading.value = true
  const user = currentUser()
  if (!user) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    merchant.value = await getMyMerchant()
  } finally {
    loading.value = false
  }
}

function onMerchantUpdated(nextMerchant) {
  merchant.value = nextMerchant
}

onMounted(() => {
  if (currentRole() !== 'MERCHANT') {
    router.push('/login')
    return
  }
  loadMerchant()
})
</script>

<template>
  <MerchantWorkspaceShell
    :merchant="merchant"
    :loading="loading"
    section="info"
    @merchant-updated="onMerchantUpdated"
  >
    <template #default="{ merchant: shellMerchant, openProfileDialog }">
      <section class="info-panel">
        <div class="info-head">
          <div>
            <h1>商家信息</h1>
            <p>查看店铺资料，并集中维护头像、联系电话、银行卡号、地址和营业时间。</p>
          </div>
          <el-button type="primary" @click="openProfileDialog">修改商家信息</el-button>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="店铺名称">{{ shellMerchant.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ shellMerchant.status }}</el-descriptions-item>
          <el-descriptions-item label="主营品类">{{ shellMerchant.category || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ shellMerchant.phone || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ shellMerchant.bankAccount || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="结算周期">{{ shellMerchant.settlementCycle || '-' }} 天</el-descriptions-item>
          <el-descriptions-item label="营业时间">{{ shellMerchant.businessHours || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="配送半径">{{ shellMerchant.deliveryRadiusM || '-' }} 米</el-descriptions-item>
          <el-descriptions-item label="店铺地址" :span="2">{{ shellMerchant.address || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="地图坐标" :span="2">
            {{ shellMerchant.longitude ?? '-' }}, {{ shellMerchant.latitude ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="shellMerchant.adminRemarks" label="管理员备注" :span="2">
            {{ shellMerchant.adminRemarks }}
          </el-descriptions-item>
        </el-descriptions>
      </section>
    </template>
  </MerchantWorkspaceShell>
</template>

<style scoped>
.info-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 22px;
}

.info-head {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 20px;
}

.info-head h1 {
  color: var(--text-primary);
  font-size: 24px;
  margin: 0 0 6px;
}

.info-head p {
  color: var(--text-secondary);
  margin: 0;
}

@media (max-width: 720px) {
  .info-head {
    flex-direction: column;
  }

  .info-head :deep(.el-button) {
    width: 100%;
  }
}
</style>
