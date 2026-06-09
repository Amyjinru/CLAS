<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  createAddress,
  deleteAddress,
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
  setDefaultAddress,
  sessionUser,
  setSessionUser,
  submitAppeal,
  updateAddress,
  updateProfile,
  uploadAvatar
} from '../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'
import LocationSelector from '../components/LocationSelector.vue'

const addresses = ref([])
const dealOrders = ref([])
const favorites = ref([])
const notifications = ref([])
const penalties = ref([])
const appeals = ref([])
const loading = ref(false)
const loadError = ref('')
const savingAddress = ref(false)
const addressActionId = ref('')
const favoriteActionId = ref(null)
const notificationActionId = ref(null)
const markingAllRead = ref(false)
const deletingAllNotifications = ref(false)
const editingAddressId = ref(null)
const locationKey = ref(0)
const addressFormRef = ref(null)
const activeProfileTab = ref('transactions')
const profileForm = reactive({ nickname: '', avatar: '' })
const appealForm = reactive({ penaltyId: null, content: '' })
const avatarInputRef = ref(null)
const avatarUploading = ref(false)
const nicknameSaving = ref(false)

const form = reactive({
  contactName: '',
  phone: '',
  address: '',
  longitude: null,
  latitude: null,
  isDefault: false
})

const locationData = reactive({
  province: '',
  city: '',
  district: '',
  street: '',
  address: '',
  longitude: null,
  latitude: null
})

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
const penaltyTypeLabel = {
  MUTE: '禁言',
  BAN: '封禁',
  SERVICE_STOP: '停止服务'
}
const appealStatusLabel = {
  PENDING: '待处理',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}
const appealablePenalties = computed(() => penalties.value.filter((item) => item.active))
const displayName = computed(() => profileForm.nickname || currentUser.value?.username || currentUser.value?.phone || '未命名用户')
const addressRules = {
  contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请填写联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请选择或定位收货地址', trigger: 'change' }]
}

function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function hasCoordinate(longitude, latitude) {
  return longitude !== null && longitude !== undefined && longitude !== ''
    && latitude !== null && latitude !== undefined && latitude !== ''
}

function resetLocationData() {
  Object.assign(locationData, {
    province: '',
    city: '',
    district: '',
    street: '',
    address: '',
    longitude: null,
    latitude: null
  })
  locationKey.value += 1
}

function resetForm() {
  Object.assign(form, {
    contactName: '',
    phone: '',
    address: '',
    longitude: null,
    latitude: null,
    isDefault: false
  })
  editingAddressId.value = null
  resetLocationData()
}

function onLocationConfirm(loc) {
  syncLocationDraft(loc)
  ElMessage.success('收货位置已确认')
}

function syncLocationDraft(loc) {
  Object.assign(form, {
    address: loc.address,
    longitude: loc.longitude,
    latitude: loc.latitude
  })
  addressFormRef.value?.clearValidate?.('address')
}

function openSummaryCard(item) {
  activeProfileTab.value = item.targetTab
}

function validateAddressForm() {
  const contactName = form.contactName.trim()
  const phone = form.phone.trim()
  const address = form.address.trim()
  if (!contactName) {
    ElMessage.warning('请填写联系人')
    return false
  }
  if (!phone) {
    ElMessage.warning('请填写联系电话')
    return false
  }
  if (!address || !hasCoordinate(form.longitude, form.latitude)) {
    ElMessage.warning('请选择或定位收货地址')
    return false
  }
  Object.assign(form, {
    contactName,
    phone,
    address
  })
  return true
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

function openAvatarPicker() {
  avatarInputRef.value?.click()
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

function avatarStyle() {
  if (profileForm.avatar) {
    return { backgroundImage: `url(${profileForm.avatar})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
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
  const failedCount = results.filter((item) => item.status === 'rejected').length
  if (failedCount === results.length) {
    loadError.value = '个人中心数据加载失败，请确认已登录后重试'
  } else if (failedCount > 0) {
    ElMessage.warning('部分个人中心数据加载失败')
  }
  loading.value = false
}

async function submitAddress() {
  if (!validateAddressForm()) {
    return
  }
  savingAddress.value = true
  try {
    if (editingAddressId.value) {
      await updateAddress(editingAddressId.value, form)
      ElMessage.success('地址已更新')
    } else {
      await createAddress(form)
      ElMessage.success('地址已保存')
    }
    resetForm()
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '地址保存失败'))
  } finally {
    savingAddress.value = false
  }
}

function editAddress(item) {
  editingAddressId.value = item.id
  Object.assign(form, {
    contactName: item.contactName,
    phone: item.phone,
    address: item.address,
    longitude: item.longitude,
    latitude: item.latitude,
    isDefault: Boolean(item.isDefault)
  })
  Object.assign(locationData, {
    province: '',
    city: '',
    district: '',
    street: item.address || '',
    address: item.address || '',
    longitude: item.longitude,
    latitude: item.latitude
  })
  locationKey.value += 1
}

async function markDefault(id) {
  addressActionId.value = `default-${id}`
  try {
    await setDefaultAddress(id)
    ElMessage.success('默认地址已更新')
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '设置默认地址失败'))
  } finally {
    addressActionId.value = ''
  }
}

async function removeAddress(id) {
  try {
    await ElMessageBox.confirm('删除后需要重新添加该收货地址，确定删除吗？', '删除地址', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  addressActionId.value = `delete-${id}`
  try {
    await deleteAddress(id)
    if (editingAddressId.value === id) {
      resetForm()
    }
    ElMessage.success('地址已删除')
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除地址失败'))
  } finally {
    addressActionId.value = ''
  }
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

async function sendAppeal() {
  if (!appealForm.content.trim()) {
    ElMessage.warning('请填写申诉内容')
    return
  }
  try {
    await submitAppeal({
      penaltyId: appealForm.penaltyId || undefined,
      content: appealForm.content.trim()
    })
    appealForm.content = ''
    appealForm.penaltyId = null
    ElMessage.success('申诉已提交')
    await load()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申诉提交失败'))
  }
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
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
  <section class="hero profile-hero">
    <div class="profile-head">
      <button
        type="button"
        class="avatar-btn"
        :class="{ uploading: avatarUploading }"
        :disabled="avatarUploading"
        @click="openAvatarPicker"
      >
        <div class="avatar" :style="avatarStyle()">{{ profileForm.avatar ? '' : avatarText() }}</div>
        <span class="avatar-tip">{{ avatarUploading ? '上传中...' : '点击更换头像' }}</span>
      </button>
      <input
        ref="avatarInputRef"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp"
        class="avatar-input"
        @change="onAvatarSelected"
      />
      <div class="profile-meta">
        <h1>个人中心</h1>
        <p>{{ displayName }} · {{ currentUser.phone || '未绑定手机号' }}</p>
        <div class="nickname-row">
          <el-input
            v-model="profileForm.nickname"
            maxlength="50"
            show-word-limit
            placeholder="设置昵称"
            @keyup.enter="saveProfile"
          />
          <el-button type="primary" :loading="nicknameSaving" @click="saveProfile">保存昵称</el-button>
        </div>
      </div>
    </div>
  </section>

  <section class="profile-summary">
    <button
      v-for="item in summaryCards"
      :key="item.label"
      type="button"
      class="summary-item"
      :class="{ active: activeProfileTab === item.targetTab }"
      @click="openSummaryCard(item)"
    >
      <strong>{{ item.value }}</strong>
      <span>{{ item.label }}</span>
    </button>
  </section>

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
        <div class="section-head">
          <div>
            <h2>处罚记录</h2>
            <p>查看平台对账号的限制状态</p>
          </div>
        </div>
        <el-empty v-if="!penalties.length" description="暂无处罚记录" />
        <article v-for="item in penalties" v-else :key="item.id" class="list-row">
          <div>
            <strong>{{ penaltyTypeLabel[item.penaltyType] || item.penaltyType }}</strong>
            <p>{{ item.reason }}</p>
            <p class="muted">
              {{ item.active ? '生效中' : '已失效' }}
              <span v-if="item.endTime"> · 至 {{ formatDateTime(item.endTime) }}</span>
            </p>
          </div>
          <el-tag :type="item.active ? 'danger' : 'info'">{{ item.active ? '生效中' : '已结束' }}</el-tag>
        </article>

        <div class="section-head" style="margin-top: 20px">
          <div>
            <h2>处罚申诉</h2>
            <p>如对禁言、封禁等处理有异议，可提交申诉</p>
          </div>
        </div>
        <div v-if="appealablePenalties.length" class="appeal-penalty-select">
          <span>关联处罚（可选）</span>
          <el-select v-model="appealForm.penaltyId" clearable placeholder="选择要申诉的处罚">
            <el-option
              v-for="item in appealablePenalties"
              :key="item.id"
              :label="`#${item.id} ${penaltyTypeLabel[item.penaltyType] || item.penaltyType}：${item.reason}`"
              :value="item.id"
            />
          </el-select>
        </div>
        <el-input v-model="appealForm.content" type="textarea" rows="3" placeholder="请描述申诉理由" />
        <el-button type="primary" style="margin-top: 10px" @click="sendAppeal">提交申诉</el-button>
        <article v-for="item in appeals" :key="item.id" class="list-row">
          <div>
            <strong>{{ appealStatusLabel[item.status] || item.status }}</strong>
            <p>{{ item.content }}</p>
            <p v-if="item.adminReply" class="muted">管理员回复：{{ item.adminReply }}</p>
          </div>
        </article>
      </el-tab-pane>

      <el-tab-pane label="地址与资料" name="addresses" lazy>
        <div class="section-head">
          <div>
            <h2>收货地址</h2>
            <p>{{ editingAddressId ? '正在编辑已保存地址' : '新增常用收货地址' }}</p>
          </div>
          <el-tag type="success">{{ addresses.length }} 个地址</el-tag>
        </div>

        <el-form
          ref="addressFormRef"
          class="address-form"
          :model="form"
          :rules="addressRules"
          label-position="top"
        >
          <div class="form-row">
            <el-form-item label="联系人" prop="contactName" required>
              <el-input v-model="form.contactName" placeholder="收货人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" prop="phone" required>
              <el-input v-model="form.phone" placeholder="收货人手机号" />
            </el-form-item>
          </div>
          <el-form-item label="收货位置" prop="address" required>
            <LocationSelector
              :key="locationKey"
              v-model="locationData"
              @update:modelValue="syncLocationDraft"
              @confirm="onLocationConfirm"
            />
          </el-form-item>
          <div class="form-actions">
            <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
            <div>
              <el-button @click="resetForm">
                {{ editingAddressId ? '取消编辑' : '重置' }}
              </el-button>
              <el-button type="primary" :loading="savingAddress" @click="submitAddress">
                {{ editingAddressId ? '保存修改' : '保存地址' }}
              </el-button>
            </div>
          </div>
        </el-form>

        <el-empty v-if="!addresses.length" description="暂无收货地址">
          <el-button type="primary" plain @click="resetForm">添加地址</el-button>
        </el-empty>

        <article v-for="item in addresses" v-else :key="item.id" class="list-row address-row">
          <div>
            <div class="row-title">
              <strong>{{ item.contactName }}</strong>
              <el-tag v-if="item.isDefault" type="success" size="small">默认</el-tag>
            </div>
            <p>{{ item.phone }} · {{ item.address }}</p>
            <p v-if="hasCoordinate(item.longitude, item.latitude)" class="coord-line">
              {{ Number(item.longitude).toFixed(6) }}, {{ Number(item.latitude).toFixed(6) }}
            </p>
          </div>
          <div class="row-actions">
            <el-button text type="primary" @click="editAddress(item)">编辑</el-button>
            <el-button
              v-if="!item.isDefault"
              text
              :loading="addressActionId === `default-${item.id}`"
              @click="markDefault(item.id)"
            >
              设默认
            </el-button>
            <el-button
              text
              type="danger"
              :loading="addressActionId === `delete-${item.id}`"
              @click="removeAddress(item.id)"
            >
              删除
            </el-button>
          </div>
        </article>
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
.profile-hero { display: grid; gap: 16px; }
.profile-head { align-items: center; display: flex; gap: 20px; flex-wrap: wrap; }
.avatar-btn {
  align-items: center; background: transparent; border: 0; cursor: pointer;
  display: flex; flex-direction: column; gap: 8px; padding: 0;
}
.avatar-btn.uploading { cursor: wait; opacity: 0.75; }
.avatar-input { display: none; }
.avatar {
  align-items: center; background: #dbeafe; border: 2px solid #bfdbfe; border-radius: 50%;
  color: #1d4ed8; display: flex; height: 88px; justify-content: center;
  width: 88px; font-size: 28px; font-weight: 700; transition: border-color 0.2s, box-shadow 0.2s;
}
.avatar-btn:hover .avatar { border-color: #2563eb; box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12); }
.avatar-tip { color: var(--text-secondary); font-size: 12px; }
.profile-meta { display: grid; gap: 10px; min-width: 240px; }
.profile-meta h1 { margin: 0; }
.profile-meta p { color: var(--text-secondary); margin: 0; }
.nickname-row { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; max-width: 420px; }
.nickname-row .el-input { flex: 1; min-width: 180px; }
.profile-summary {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 18px;
}
.summary-item {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  color: var(--text-primary);
  cursor: pointer;
  display: block;
  padding: 16px;
  text-align: left;
  width: 100%;
}
.summary-item:hover,
.summary-item:focus-visible {
  background: #fff;
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
  outline: none;
  transform: none;
}
.summary-item.active {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}
.summary-item strong {
  color: var(--text-primary);
  display: block;
  font-size: 24px;
  line-height: 1.1;
}
.summary-item span {
  color: var(--text-secondary);
  display: block;
  font-size: 13px;
  margin-top: 8px;
}
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
.section-head h2 {
  margin: 0;
}
.section-head p {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 6px 0 0;
}
.address-form {
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 6px;
  padding-bottom: 16px;
}
.form-row {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.form-actions {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.list-row {
  align-items: center;
  border-top: 1px dashed var(--border-light);
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 14px;
}
.notification-fold p {
  color: var(--text-secondary);
  margin: 0;
}
.address-form { margin-bottom: 18px; }
.list-row {
  align-items: center; border-top: 1px solid var(--border-light);
  display: flex; justify-content: space-between; padding: 14px 0;
}
.row-title {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.row-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.address-preview {
  background: #f0f9eb;
  border: 1px solid #d1edc4;
  border-radius: 8px;
  color: #529b2e;
  display: grid;
  font-size: 13px;
  gap: 4px;
  line-height: 1.4;
  margin-top: 10px;
  padding: 10px 12px;
  width: 100%;
}
.address-preview small {
  color: #67c23a;
}
.notification-row.unread {
  background: #fff7f0;
  margin-left: -12px;
  margin-right: -12px;
  padding-left: 12px;
  padding-right: 12px;
}
.voucher-row strong {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
@media (max-width: 900px) {
  .shortcut-grid,
  .profile-summary {
    grid-template-columns: 1fr;
  }
  .form-row,
  .form-actions {
    grid-template-columns: 1fr;
  }
  .form-actions {
    align-items: stretch;
    display: grid;
  }
  .list-row {
    align-items: flex-start;
    flex-direction: column;
  }
  .row-actions {
    justify-content: flex-start;
  }
}
.appeal-penalty-select {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}
.appeal-penalty-select .el-select { min-width: 280px; }
.address-preview { margin-top: 8px; font-size: 13px; color: #67c23a; }
@media (max-width: 900px) { .profile-grid { grid-template-columns: 1fr; } }
</style>
