<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  createAddress,
  deleteAddress,
  listFavorites,
  listAddresses,
  listMyDealOrders,
  listNotifications,
  markNotificationRead,
  setDefaultAddress,
  sessionUser
} from '../api/clas'
import { ElMessage } from 'element-plus'

const addresses = ref([])
const dealOrders = ref([])
const favorites = ref([])
const notifications = ref([])
const form = reactive({
  contactName: '',
  phone: '',
  address: '',
  isDefault: false
})

async function load() {
  const [addressList, orderList, favoriteList, notificationList] = await Promise.all([
    listAddresses(),
    listMyDealOrders(),
    listFavorites(),
    listNotifications()
  ])
  addresses.value = addressList
  dealOrders.value = orderList
  favorites.value = favoriteList
  notifications.value = notificationList
}

async function submitAddress() {
  await createAddress(form)
  ElMessage.success('地址已保存')
  Object.assign(form, { contactName: '', phone: '', address: '', isDefault: false })
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

onMounted(load)
</script>

<template>
  <section class="hero">
    <div>
      <h1>个人中心</h1>
      <p>{{ sessionUser?.username }} · {{ sessionUser?.phone }}</p>
    </div>
  </section>

  <section class="profile-grid">
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
        <el-form-item label="地址">
          <el-input v-model="form.address" />
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
      <div class="section-head">
        <h2>我的收藏</h2>
      </div>
      <article class="list-row" v-for="item in favorites" :key="item.id">
        <div>
          <strong>{{ item.merchantName }}</strong>
          <p>{{ item.category }} · {{ item.address }}</p>
        </div>
        <RouterLink class="button secondary" :to="`/merchant/${item.id}`">进入</RouterLink>
      </article>
    </div>

    <div class="panel">
      <div class="section-head">
        <h2>我的团购券</h2>
      </div>
      <article class="list-row" v-for="item in dealOrders" :key="item.id">
        <div>
          <strong>{{ item.voucherCode }}</strong>
          <p>¥{{ (item.payAmount / 100).toFixed(2) }} · {{ item.status }}</p>
        </div>
        <el-tag :type="item.status === 'USED' ? 'info' : 'warning'">{{ item.status }}</el-tag>
      </article>
    </div>

    <div class="panel">
      <div class="section-head">
        <h2>通知中心</h2>
      </div>
      <article class="list-row" v-for="item in notifications" :key="item.id">
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
        </div>
        <el-button v-if="!item.readFlag" text type="primary" @click="readNotification(item.id)">标记已读</el-button>
        <el-tag v-else type="info">已读</el-tag>
      </article>
    </div>
  </section>
</template>

<style scoped>
.profile-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}
.section-head h2 {
  margin: 0 0 16px;
}
.address-form {
  margin-bottom: 18px;
}
.address-form .el-button {
  margin-top: 12px;
}
.list-row {
  align-items: center;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
}
.list-row p {
  color: var(--text-secondary);
  margin: 6px 0 0;
}
.row-actions {
  align-items: center;
  display: flex;
  gap: 8px;
}
@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
