<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  deleteNotification,
  deleteAllNotifications,
  getProfile,
  listFavorites,
  listAddresses,
  listMyAppeals,
  listMyDealOrders,
  listMyPenalties,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  removeFavorite,
  sessionUser,
  setSessionUser,
  updateProfile,
  uploadAvatar
} from '../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProfileHero from '../components/profile/ProfileHero.vue'
import ProfileSummary from '../components/profile/ProfileSummary.vue'
import ProfileAddressSection from '../components/profile/ProfileAddressSection.vue'
import ProfilePenaltySection from '../components/profile/ProfilePenaltySection.vue'

const addresses = ref([])
const dealOrders = ref([])
const favorites = ref([])
const notifications = ref([])
const penalties = ref([])
const appeals = ref([])
const loading = ref(false)
const loadError = ref('')
const favoriteActionId = ref(null)
const notificationActionId = ref(null)
const markingAllRead = ref(false)
const deletingAllNotifications = ref(false)
const activeProfileTab = ref('transactions')
const profileForm = reactive({ nickname: '', avatar: '' })
const avatarUploading = ref(false)
const nicknameSaving = ref(false)

const currentUser = computed(() => sessionUser.value || {})
const unreadCount = computed(() => notifications.value.filter(item => !item.readFlag).length)
const summaryCards = computed(() => [
  { label: '收货地址', value: addresses.value.length, targetTab: 'addresses' },
  { label: '收藏店铺', value: favorites.value.length, targetTab: 'shopping' },
  { label: '券包', value: dealOrders.value.length, targetTab: 'vouchers' },
  { label: '未读通知', value: unreadCount.value, targetTab: 'messages' }
])
const transactionShortcuts = computed(() => [
  { label: '全部订单', value: '查看外卖与到店订单', to: '/orders', type: 'primary' },
  { label: '购物车', value: '继续结算已选商品', to: '/cart', type: 'success' },
  { label: '生活预约', value: '查看预约记录', to: '/bookings', type: 'warning' }
])
const displayName = computed(() => profileForm.nickname || currentUser.value?.username || currentUser.value?.phone || '未命名用户')

function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function openSummaryCard(item) {
  activeProfileTab.value = item.targetTab
}

const silentConfig = { silent: true }

async function loadProfile() {
  try {
    const profile = await getProfile(silentConfig)
    profileForm.nickname = profile.nickname || profile.username || ''
    profileForm.avatar = profile.avatar || ''
    if (profile && sessionUser.value) {
      setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    }
  } catch {
    // 资料接口失败时不阻塞个人中心其余内容加载
  }
}

async function saveProfile() {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  nicknameSaving.value = true
  try {
    const profile = await updateProfile({ nickname: profileForm.nickname.trim() })
    profileForm.nickname = profile.nickname || profile.username || ''
    setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    ElMessage.success('昵称已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '昵称更新失败'))
  } finally {
    nicknameSaving.value = false
  }
}

async function onAvatarSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 2MB')
    return
  }
  avatarUploading.value = true
  try {
    const profile = await uploadAvatar(file)
    profileForm.avatar = profile.avatar || ''
    setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '头像上传失败'))
  } finally {
    avatarUploading.value = false
  }
}

function avatarText() {
  return (displayName.value || '?').slice(0, 1).toUpperCase()
}

async function load() {
  loading.value = true
  loadError.value = ''
  const results = await Promise.allSettled([
    listAddresses(silentConfig),
    listMyDealOrders(silentConfig),
    listFavorites(silentConfig),
    listNotifications(silentConfig),
    listMyPenalties(silentConfig),
    listMyAppeals(silentConfig)
  ])
  addresses.value = results[0].status === 'fulfilled' ? results[0].value : []
  dealOrders.value = results[1].status === 'fulfilled' ? results[1].value : []
  favorites.value = results[2].status === 'fulfilled' ? results[2].value : []
  notifications.value = results[3].status === 'fulfilled' ? results[3].value : []
  penalties.value = results[4].status === 'fulfilled' ? results[4].value : []
  appeals.value = results[5].status === 'fulfilled' ? results[5].value : []
  const rejected = results.filter((item) => item.status === 'rejected')
  const allAre401 = rejected.length > 0 && rejected.every((item) => item.reason?.response?.data?.code === 401 || item.reason?.response?.status === 401)
  if (rejected.length === results.length && !allAre401) {
    loadError.value = '个人中心数据加载失败，请确认已登录后重试'
  } else if (rejected.length > 0 && !allAre401) {
    ElMessage.warning('部分个人中心数据加载失败')
  }
  loading.value = false
}

async function removeFavoriteMerchant(id) {
  favoriteActionId.value = id
  try {
    await removeFavorite(id)
    ElMessage.success('已取消收藏')
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '取消收藏失败'))
  } finally {
    favoriteActionId.value = null
  }
}

async function readNotification(id) {
  notificationActionId.value = id
  try {
    await markNotificationRead(id)
    ElMessage.success('通知已标记为已读')
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '标记已读失败'))
  } finally {
    notificationActionId.value = null
  }
}

async function readAllNotifications() {
  if (!unreadCount.value) return
  markingAllRead.value = true
  try {
    await markAllNotificationsRead()
    ElMessage.success('全部通知已标记为已读')
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '全部已读失败'))
  } finally {
    markingAllRead.value = false
  }
}

async function removeNotification(id) {
  try {
    await ElMessageBox.confirm('确定删除这条通知吗？', '删除通知', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteNotification(id)
    ElMessage.success('通知已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '删除失败'))
    }
  }
}

async function clearAllNotifications() {
  if (!notifications.value.length) return
  try {
    await ElMessageBox.confirm('确定清空全部通知吗？', '清空通知', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    deletingAllNotifications.value = true
    await deleteAllNotifications()
    ElMessage.success('通知已清空')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '清空失败'))
    }
  } finally {
    deletingAllNotifications.value = false
  }
}

function formatMoney(cents) {
  return `¥${((cents || 0) / 100).toFixed(2)}`
}

function dealStatusLabel(status) {
  return {
    UNUSED: '待使用',
    USED: '已使用',
    EXPIRED: '已过期',
    REFUNDED: '已退款'
  }[status] || status || '未知'
}

function dealStatusType(status) {
  return {
    UNUSED: 'success',
    USED: 'info',
    EXPIRED: 'warning',
    REFUNDED: 'danger'
  }[status] || 'info'
}

onMounted(async () => {
  await Promise.all([loadProfile(), load()])
})
</script>

<template>
  <div class="user-page profile-page">
  <ProfileHero
    v-model:nickname="profileForm.nickname"
    :display-name="displayName"
    :phone="currentUser.phone"
    :avatar="profileForm.avatar"
    :avatar-text="avatarText()"
    :avatar-uploading="avatarUploading"
    :nickname-saving="nicknameSaving"
    @avatar-selected="onAvatarSelected"
    @save-profile="saveProfile"
  />

  <ProfileSummary
    :cards="summaryCards"
    :active-tab="activeProfileTab"
    @select="openSummaryCard"
  />

  <section v-if="loading" class="panel state-panel">
    <el-skeleton :rows="8" animated />
  </section>

  <section v-else-if="loadError" class="panel state-panel">
    <el-alert :title="loadError" type="error" show-icon :closable="false" />
    <el-button type="primary" plain @click="load">重新加载</el-button>
  </section>

  <section v-else class="panel profile-workspace">
    <el-tabs v-model="activeProfileTab" class="profile-tabs">
      <el-tab-pane label="我的交易" name="transactions">
        <div class="shortcut-grid">
          <RouterLink
            v-for="item in transactionShortcuts"
            :key="item.label"
            class="shortcut-card"
            :to="item.to"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </RouterLink>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的购物" name="shopping">
        <div class="section-head">
          <div>
            <h2>我的收藏</h2>
            <p>常用商家和购物入口集中在这里</p>
          </div>
          <RouterLink class="button secondary" to="/cart">查看购物车</RouterLink>
        </div>

        <el-empty v-if="!favorites.length" description="暂无收藏商家">
          <RouterLink class="button secondary" to="/">去首页浏览</RouterLink>
        </el-empty>

        <article v-for="item in favorites" v-else :key="item.id" class="list-row">
          <div>
            <strong>{{ item.merchantName }}</strong>
            <p>{{ item.category || '未分类' }} · {{ item.address || '暂无地址' }}</p>
          </div>
          <div class="row-actions">
            <RouterLink class="button secondary" :to="`/merchant/${item.id}`">进入店铺</RouterLink>
            <el-button
              text
              type="danger"
              :loading="favoriteActionId === item.id"
              @click="removeFavoriteMerchant(item.id)"
            >
              取消收藏
            </el-button>
          </div>
        </article>
      </el-tab-pane>

      <el-tab-pane label="我的券包" name="vouchers">
        <div class="section-head">
          <div>
            <h2>优惠券 / 团购券</h2>
            <p>当前展示已购买团购券，后续可接入平台优惠券</p>
          </div>
          <el-tag type="warning">{{ dealOrders.length }} 张</el-tag>
        </div>

        <el-empty v-if="!dealOrders.length" description="暂无团购券">
          <RouterLink class="button secondary" to="/deals">去团购页看看</RouterLink>
        </el-empty>

        <article v-for="item in dealOrders" v-else :key="item.id" class="list-row voucher-row">
          <div>
            <strong>{{ item.voucherCode }}</strong>
            <p>支付金额 {{ formatMoney(item.payAmount) }}</p>
          </div>
          <el-tag :type="dealStatusType(item.status)">
            {{ dealStatusLabel(item.status) }}
          </el-tag>
        </article>
      </el-tab-pane>

      <el-tab-pane label="账号与申诉" name="account">
        <ProfilePenaltySection :penalties="penalties" :appeals="appeals" @reload="load" />
      </el-tab-pane>

      <el-tab-pane label="地址与资料" name="addresses" lazy>
        <ProfileAddressSection :addresses="addresses" @reload="load" />
      </el-tab-pane>

      <el-tab-pane label="消息与服务" name="messages">
        <div class="section-head">
          <div>
            <h2>通知中心</h2>
            <p>{{ unreadCount }} 条未读</p>
          </div>
          <div class="row-actions">
            <RouterLink class="button secondary" to="/user/announcements">平台公告</RouterLink>
            <el-button
              type="primary"
              plain
              :disabled="!unreadCount"
              :loading="markingAllRead"
              @click="readAllNotifications"
            >
              全部已读
            </el-button>
            <el-button
              type="danger"
              plain
              :disabled="!notifications.length"
              :loading="deletingAllNotifications"
              @click="clearAllNotifications"
            >
              清空通知
            </el-button>
          </div>
        </div>

        <el-empty v-if="!notifications.length" description="暂无通知" />

        <article
          v-for="item in notifications"
          v-else
          :key="item.id"
          class="list-row notification-row"
          :class="{ unread: !item.readFlag }"
        >
          <div>
            <div class="row-title">
              <strong>{{ item.title }}</strong>
              <el-tag v-if="!item.readFlag" type="danger" size="small">未读</el-tag>
              <el-tag v-else type="info" size="small">已读</el-tag>
            </div>
            <p>{{ item.content }}</p>
          </div>
          <el-button
            v-if="!item.readFlag"
            text
            type="primary"
            :loading="notificationActionId === item.id"
            @click="readNotification(item.id)"
          >
            标记已读
          </el-button>
          <el-button text type="danger" @click="removeNotification(item.id)">删除</el-button>
        </article>
      </el-tab-pane>
    </el-tabs>
  </section>
  </div>
</template>

<style scoped>
.profile-workspace {
  overflow: hidden;
}
.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}
.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
.shortcut-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.shortcut-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  color: var(--text-primary);
  display: grid;
  gap: 8px;
  min-height: 104px;
  padding: 16px;
  text-decoration: none;
}
.shortcut-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}
.shortcut-card strong {
  font-size: 18px;
}
.shortcut-card span {
  color: var(--text-secondary);
  font-size: 13px;
}
.state-panel {
  display: grid;
  gap: 16px;
}
.section-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-head h2 { margin: 0; }
.section-head p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.list-row {
  align-items: center; border-top: 1px solid var(--border-light);
  display: flex; justify-content: space-between; padding: 14px 0;
}
.notification-fold p { color: var(--text-secondary); margin: 0; }
.row-title { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; }
.row-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
.notification-row.unread {
  background: #fff7f0; margin-left: -12px; margin-right: -12px;
  padding-left: 12px; padding-right: 12px;
}
.voucher-row strong { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
@media (max-width: 900px) {
  .shortcut-grid { grid-template-columns: 1fr; }
  .list-row { align-items: flex-start; flex-direction: column; }
  .row-actions { justify-content: flex-start; }
}
</style>
