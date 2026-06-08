<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createAddress,
  deleteAddress,
  deleteNotification,
  getProfile,
  listMyAppeals,
  listMyPenalties,
  listFavorites,
  listAddresses,
  listMyDealOrders,
  listMyReviews,
  listNotifications,
  listOrders,
  markNotificationRead,
  setDefaultAddress,
  sessionUser,
  setSessionUser,
  submitAppeal,
  updateProfile,
  uploadAvatar
} from '../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'
import LocationSelector from '../components/LocationSelector.vue'

const addresses = ref([])
const dealOrders = ref([])
const favorites = ref([])
const notifications = ref([])
const orders = ref([])
const reviews = ref([])
const appeals = ref([])
const penalties = ref([])
const profileForm = reactive({ nickname: '', avatar: '' })
const appealForm = reactive({ penaltyId: null, content: '' })
const avatarInputRef = ref(null)
const avatarUploading = ref(false)
const nicknameSaving = ref(false)

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

const dealStatusLabel = {
  PENDING_PAYMENT: '待支付',
  UNUSED: '待使用',
  USED: '已核销'
}

const penaltyTypeLabel = {
  MUTE: '禁言',
  BAN: '封禁',
  SERVICE_STOP: '停止服务'
}

const appealablePenalties = computed(() => penalties.value.filter((item) => item.active))

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

const displayName = computed(() => profileForm.nickname || sessionUser.value?.username || sessionUser.value?.phone)

const NOTIFICATION_PREVIEW_LIMIT = 5
const previewNotifications = computed(() => notifications.value.slice(0, NOTIFICATION_PREVIEW_LIMIT))
const hiddenNotificationCount = computed(() =>
  Math.max(notifications.value.length - NOTIFICATION_PREVIEW_LIMIT, 0)
)

function avatarText() {
  return (displayName.value || '?').slice(0, 1).toUpperCase()
}

function avatarStyle() {
  if (profileForm.avatar) {
    return { backgroundImage: `url(${profileForm.avatar})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
}

function onLocationConfirm(loc) {
  form.address = loc.address
  form.longitude = loc.longitude
  form.latitude = loc.latitude
  ElMessage.success('收货位置已确认')
}

async function loadProfile() {
  const profile = await getProfile()
  profileForm.nickname = profile.nickname || profile.username || ''
  profileForm.avatar = profile.avatar || ''
  setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
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
    ElMessage.error(error.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function load() {
  const results = await Promise.allSettled([
    listAddresses(),
    listMyDealOrders(),
    listFavorites(),
    listNotifications(),
    listOrders(),
    listMyReviews(),
    listMyAppeals(),
    listMyPenalties()
  ])
  const pick = (index, fallback = []) => (results[index].status === 'fulfilled' ? results[index].value : fallback)
  addresses.value = pick(0)
  dealOrders.value = pick(1)
  favorites.value = pick(2)
  notifications.value = pick(3)
  orders.value = pick(4)
  reviews.value = pick(5)
  appeals.value = pick(6)
  penalties.value = pick(7)
  if (results.some((item) => item.status === 'rejected')) {
    ElMessage.warning('部分资料加载失败，请稍后刷新')
  }
}

async function submitAddress() {
  if (!form.longitude || !form.latitude) {
    ElMessage.warning('请在地图中选择收货位置')
    return
  }
  await createAddress(form)
  ElMessage.success('地址已保存')
  Object.assign(form, { contactName: '', phone: '', address: '', longitude: null, latitude: null, isDefault: false })
  await load()
}

async function markDefault(id) {
  await setDefaultAddress(id)
  await load()
}

async function removeAddress(id) {
  await deleteAddress(id)
  await load()
}

async function readNotification(id) {
  await markNotificationRead(id)
  await load()
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
      ElMessage.error('删除失败')
    }
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
    ElMessage.error(error.response?.data?.message || '申诉提交失败')
  }
}

onMounted(async () => {
  await loadProfile()
  await load()
})
</script>

<template>
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
        <p>{{ displayName }} · {{ sessionUser?.phone }}</p>
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

  <section class="profile-grid">
    <div class="panel">
      <div class="section-head"><h2>历史订单</h2></div>
      <article class="list-row" v-for="item in orders" :key="item.order.id">
        <div>
          <strong>订单 #{{ item.order.id }}</strong>
          <p>
            ¥{{ ((item.order.totalPrice || 0) / 100).toFixed(2) }}
            · {{ orderStatusLabel[item.order.status] || item.order.status }}
            · {{ item.items?.length || 0 }} 件商品
          </p>
        </div>
        <RouterLink class="button secondary" to="/orders">查看</RouterLink>
      </article>
      <el-empty v-if="!orders.length" description="暂无历史订单" />
    </div>

    <div class="panel">
      <div class="section-head"><h2>历史评价</h2></div>
      <article class="list-row" v-for="item in reviews" :key="item.id">
        <div>
          <strong>{{ item.score }} 星</strong>
          <p>{{ item.content || '（无文字评价）' }}</p>
        </div>
      </article>
      <el-empty v-if="!reviews.length" description="暂无历史评价" />
    </div>

    <div class="panel">
      <div class="section-head"><h2>处罚记录</h2></div>
      <article class="list-row" v-for="item in penalties" :key="item.id">
        <div>
          <strong>{{ penaltyTypeLabel[item.penaltyType] || item.penaltyType }}</strong>
          <p>{{ item.reason }}</p>
          <p class="muted">
            {{ item.active ? '生效中' : '已失效' }}
            <span v-if="item.endTime"> · 至 {{ new Date(item.endTime).toLocaleString('zh-CN', { hour12: false }) }}</span>
          </p>
        </div>
        <el-tag :type="item.active ? 'danger' : 'info'">{{ item.active ? '生效中' : '已结束' }}</el-tag>
      </article>
      <el-empty v-if="!penalties.length" description="暂无处罚记录" />
    </div>

    <div class="panel">
      <div class="section-head"><h2>处罚申诉</h2></div>
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
      <el-input v-model="appealForm.content" type="textarea" rows="3" placeholder="如对禁言、封禁等处理有异议，请向客服申诉" />
      <el-button type="primary" style="margin-top: 10px" @click="sendAppeal">提交申诉</el-button>
      <article class="list-row" v-for="item in appeals" :key="item.id">
        <div>
          <strong>{{ item.status }}</strong>
          <p>{{ item.content }}</p>
          <p v-if="item.adminReply" class="muted">管理员回复：{{ item.adminReply }}</p>
        </div>
      </article>
    </div>

    <div class="panel">
      <div class="section-head">
        <h2>收货地址</h2>
      </div>
      <el-form class="address-form" :model="form" label-position="top">
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="收货位置">
          <LocationSelector
            v-model="locationData"
            @confirm="onLocationConfirm"
          />
          <div v-if="form.address" class="address-preview">
            已选位置: {{ form.address }}
          </div>
        </el-form-item>
        <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
        <el-button type="primary" @click="submitAddress">保存地址</el-button>
      </el-form>

      <article class="list-row" v-for="item in addresses" :key="item.id">
        <div>
          <strong>{{ item.contactName }}</strong>
          <p>{{ item.phone }} · {{ item.address }}</p>
        </div>
        <div class="row-actions">
          <el-tag v-if="item.isDefault" type="success">默认</el-tag>
          <el-button v-else text @click="markDefault(item.id)">设默认</el-button>
          <el-button text type="danger" @click="removeAddress(item.id)">删除</el-button>
        </div>
      </article>
    </div>

    <div class="panel">
      <div class="section-head"><h2>我的收藏</h2></div>
      <article class="list-row" v-for="item in favorites" :key="item.id">
        <div>
          <strong>{{ item.merchantName }}</strong>
          <p>{{ item.category }} · {{ item.address }}</p>
        </div>
        <RouterLink class="button secondary" :to="`/merchant/${item.id}`">进入</RouterLink>
      </article>
    </div>

    <div class="panel">
      <div class="section-head"><h2>我的团购券</h2></div>
      <article class="list-row" v-for="item in dealOrders" :key="item.id">
        <div>
          <strong>{{ item.status === 'PENDING_PAYMENT' ? `团购订单 #${item.id}` : item.voucherCode }}</strong>
          <p>¥{{ (item.payAmount / 100).toFixed(2) }} · {{ dealStatusLabel[item.status] || item.status }}</p>
        </div>
        <RouterLink
          v-if="item.status === 'PENDING_PAYMENT'"
          class="button secondary"
          :to="`/payment/deal/${item.id}`"
        >
          去支付
        </RouterLink>
        <el-tag v-else :type="item.status === 'USED' ? 'info' : 'warning'">
          {{ dealStatusLabel[item.status] || item.status }}
        </el-tag>
      </article>
    </div>

    <div class="panel">
      <div class="section-head"><h2>通知中心</h2></div>
      <article class="list-row" v-for="item in previewNotifications" :key="item.id">
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
        </div>
        <div class="row-actions">
          <el-button v-if="!item.readFlag" text type="primary" @click="readNotification(item.id)">标记已读</el-button>
          <el-tag v-else type="info">已读</el-tag>
          <el-button class="btn-delete-soft" size="small" type="danger" @click="removeNotification(item.id)">删除</el-button>
        </div>
      </article>
      <div v-if="hiddenNotificationCount" class="notification-fold">
        <p>还有 {{ hiddenNotificationCount }} 条通知已折叠</p>
        <RouterLink class="button secondary compact-btn" to="/profile/notifications">查看全部通知</RouterLink>
      </div>
      <el-empty v-if="!notifications.length" description="暂无通知" />
    </div>
  </section>
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
.profile-grid { display: grid; gap: 18px; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
.section-head h2 { margin: 0 0 16px; }
.compact-btn { font-size: 13px; padding: 8px 14px; }
.notification-fold {
  align-items: center;
  border-top: 1px dashed var(--border-light);
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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
.list-row p, .muted { color: var(--text-secondary); margin: 6px 0 0; }
.row-actions { align-items: center; display: flex; gap: 8px; }
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
