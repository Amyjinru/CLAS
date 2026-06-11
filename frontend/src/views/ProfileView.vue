<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteAllNotifications,
  deleteNotification,
  getCart,
  getConversations,
  getMerchant,
  getProfile,
  getReviewByOrder,
  listFavorites,
  listMyCoupons,
  listMyDealOrders,
  listNotifications,
  listOrders,
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
import ProfileOrderBlock from '../components/profile/ProfileOrderBlock.vue'
import ProfileCartBlock from '../components/profile/ProfileCartBlock.vue'
import ProfileFavoritesBlock from '../components/profile/ProfileFavoritesBlock.vue'
import ProfileVoucherBlock from '../components/profile/ProfileVoucherBlock.vue'
import ProfileMessageBlock from '../components/profile/ProfileMessageBlock.vue'
import { useChatStore } from '../composables/useChatStore'

const activeProfileTab = ref('orders')
const chatStore = useChatStore()
const chatConversations = ref([])
const merchantCache = ref({})
const chatLoading = ref(false)
const loading = ref(false)
const loadError = ref('')
const orders = ref([])
const reviewedOrderIds = ref(new Set())
const cartItems = ref([])
const favorites = ref([])
const dealOrders = ref([])
const coupons = ref([])
const notifications = ref([])
const favoriteActionId = ref(null)
const notificationActionId = ref(null)
const markingAllRead = ref(false)
const deletingAllNotifications = ref(false)
const profileForm = reactive({ nickname: '', avatar: '' })
const avatarUploading = ref(false)
const nicknameSaving = ref(false)

const currentUser = computed(() => sessionUser.value || {})
const displayName = computed(() => profileForm.nickname || currentUser.value?.username || currentUser.value?.phone || '未命名用户')
const unreadCount = computed(() => notifications.value.filter((item) => !item.readFlag).length)
const pendingPaymentOrders = computed(() => orders.value.filter((item) => item.order.status === 'PENDING_PAYMENT'))
const pendingDealOrders = computed(() => dealOrders.value.filter((item) => item.status === 'PENDING_PAYMENT'))
const waitingReceiveOrders = computed(() => orders.value.filter((item) => {
  const { status, deliveryStatus, refundStatus } = item.order
  if (['REFUNDED', 'REFUND_PENDING', 'CANCELED', 'REJECTED'].includes(status)) return false
  if (refundStatus && refundStatus !== 'NONE') return false
  if (status === 'ACCEPTED' && ['DELIVERING', 'DELIVERED'].includes(deliveryStatus)) return true
  return false
}))
const pendingReviewOrders = computed(() => orders.value.filter((item) => item.order.status === 'COMPLETED' && !reviewedOrderIds.value.has(item.order.id)))
const afterSaleOrders = computed(() => orders.value.filter((item) => item.order.status === 'REFUND_PENDING' || item.order.status === 'REFUNDED' || (item.order.refundStatus && item.order.refundStatus !== 'NONE')))
const unusedDealOrders = computed(() => dealOrders.value.filter((item) => item.status === 'UNUSED'))
const voucherCount = computed(() => dealOrders.value.length + coupons.value.length)

const summaryCards = computed(() => [
  { label: '订单', value: orders.value.length, targetTab: 'orders' },
  { label: '购物车', value: cartItems.value.length + pendingPaymentOrders.value.length + pendingDealOrders.value.length, targetTab: 'cart' },
  { label: '收藏', value: favorites.value.length, targetTab: 'favorites' },
  { label: '券包', value: voucherCount.value, targetTab: 'vouchers' },
  { label: '消息', value: unreadCount.value, targetTab: 'messages' }
])

const orderModules = computed(() => [
  { label: '全部订单', count: orders.value.length, to: '/orders', description: '查看所有外卖订单' },
  { label: '待支付', count: pendingPaymentOrders.value.length + pendingDealOrders.value.length, to: '/cart', description: '继续完成支付' },
  { label: '待收货/使用', count: waitingReceiveOrders.value.length + unusedDealOrders.value.length, to: '/orders?tab=receiving', description: '外卖配送与团购到店履约' },
  { label: '待评价', count: pendingReviewOrders.value.length, to: '/orders?tab=review', description: '给已完成订单评价' },
  { label: '退款/售后', count: afterSaleOrders.value.length, to: '/orders?tab=after-sale', description: '查看退款与售后进度' }
])

function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function openSummaryCard(item) {
  activeProfileTab.value = item.targetTab
}

async function loadProfile() {
  try {
    const profile = await getProfile({ silent: true })
    profileForm.nickname = profile.nickname || profile.username || ''
    profileForm.avatar = profile.avatar || ''
    if (profile && sessionUser.value) {
      setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    }
  } catch {
    // 资料加载失败不阻塞用户中心主体。
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

async function loadReviewState(orderList) {
  const reviewed = await Promise.all(
    orderList
      .filter((entry) => entry.order.status === 'COMPLETED')
      .map(async (entry) => {
        try {
          const review = await getReviewByOrder(entry.order.id)
          return review ? entry.order.id : null
        } catch {
          return null
        }
      })
  )
  reviewedOrderIds.value = new Set(reviewed.filter(Boolean))
}

async function loadChatConversations() {
  chatLoading.value = true
  try {
    chatConversations.value = await getConversations()
    await Promise.all(
      chatConversations.value.map(async (conv) => {
        if (!merchantCache.value[conv.merchantId]) {
          try {
            merchantCache.value[conv.merchantId] = await getMerchant(conv.merchantId)
          } catch {
            merchantCache.value[conv.merchantId] = { id: conv.merchantId, merchantName: `商家 #${conv.merchantId}` }
          }
        }
      })
    )
  } catch {
    chatConversations.value = []
  } finally {
    chatLoading.value = false
  }
}

function handleOpenChat(merchantId) {
  chatStore.openMerchantChat(merchantId)
}

async function load() {
  loading.value = true
  loadError.value = ''
  const results = await Promise.allSettled([
    listOrders(),
    getCart(),
    listFavorites({ silent: true }),
    listMyDealOrders({ silent: true }),
    listMyCoupons({ silent: true }),
    listNotifications({ silent: true })
  ])
  orders.value = results[0].status === 'fulfilled' ? results[0].value : []
  cartItems.value = results[1].status === 'fulfilled' ? results[1].value : []
  favorites.value = results[2].status === 'fulfilled' ? results[2].value : []
  dealOrders.value = results[3].status === 'fulfilled' ? results[3].value : []
  coupons.value = results[4].status === 'fulfilled' ? results[4].value : []
  notifications.value = results[5].status === 'fulfilled' ? results[5].value : []
  await loadReviewState(orders.value)
  loadChatConversations()
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
        <el-tab-pane label="订单" name="orders">
          <ProfileOrderBlock :modules="orderModules" />
        </el-tab-pane>

        <el-tab-pane label="购物车" name="cart">
          <ProfileCartBlock
            :cart-items="cartItems"
            :pending-payment-orders="pendingPaymentOrders"
            :pending-deal-orders="pendingDealOrders"
          />
        </el-tab-pane>

        <el-tab-pane label="收藏" name="favorites">
          <ProfileFavoritesBlock
            :favorites="favorites"
            :action-id="favoriteActionId"
            @remove="removeFavoriteMerchant"
          />
        </el-tab-pane>

        <el-tab-pane label="券包" name="vouchers">
          <ProfileVoucherBlock :deal-orders="dealOrders" :coupons="coupons" />
        </el-tab-pane>

        <el-tab-pane label="消息" name="messages">
          <ProfileMessageBlock
            :notifications="notifications"
            :unread-count="unreadCount"
            :marking-all-read="markingAllRead"
            :deleting-all="deletingAllNotifications"
            :action-id="notificationActionId"
            :conversations="chatConversations"
            :merchant-cache="merchantCache"
            :chat-loading="chatLoading"
            @read="readNotification"
            @read-all="readAllNotifications"
            @remove="removeNotification"
            @clear="clearAllNotifications"
            @open-chat="handleOpenChat"
          />
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.profile-workspace { overflow: hidden; }
.profile-tabs :deep(.el-tabs__header) { margin-bottom: 18px; }
.profile-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; }
.state-panel { display: grid; gap: 16px; }
</style>
