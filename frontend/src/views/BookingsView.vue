<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelBooking, createBooking, listMerchants, listMyBookings } from '../api/clas'

const merchants = ref([])
const bookings = ref([])
const saving = ref(false)
const form = ref({
  merchantId: null,
  serviceName: '',
  appointmentTime: '',
  contactPhone: '',
  note: ''
})

const statusMap = {
  PENDING: { text: '待确认', type: 'warning' },
  CONFIRMED: { text: '已确认', type: 'success' },
  CANCELED: { text: '已取消', type: 'info' },
  COMPLETED: { text: '已完成', type: 'primary' }
}

const merchantOptions = computed(() => merchants.value.map((item) => ({
  label: `${item.merchantName} · ${item.category || '生活服务'}`,
  value: item.id
})))

function merchantName(id) {
  return merchants.value.find((item) => item.id === id)?.merchantName || `商家 #${id}`
}

function formatTime(value) {
  if (!value) return '未设置'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function resetForm() {
  form.value = {
    merchantId: null,
    serviceName: '',
    appointmentTime: '',
    contactPhone: '',
    note: ''
  }
}

async function load() {
  const [merchantList, bookingList] = await Promise.all([
    listMerchants(),
    listMyBookings()
  ])
  merchants.value = merchantList
  bookings.value = bookingList
}

async function submit() {
  saving.value = true
  try {
    await createBooking(form.value)
    ElMessage.success('预约已提交，请等待商家确认')
    resetForm()
    await load()
  } finally {
    saving.value = false
  }
}

async function cancel(id) {
  await cancelBooking(id)
  ElMessage.success('预约已取消')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="booking-page">
    <div class="booking-workbench">
      <section class="panel booking-form">
        <div class="section-heading">
          <p>生活服务预约</p>
          <h1>安排一次到店服务</h1>
        </div>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="预约商家">
            <el-select v-model="form.merchantId" filterable placeholder="选择已营业商家">
              <el-option v-for="item in merchantOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务项目">
            <el-input v-model="form.serviceName" maxlength="100" placeholder="例如：家政清洁、门店咨询、设备维修" />
          </el-form-item>
          <el-form-item label="预约时间">
            <el-input v-model="form.appointmentTime" type="datetime-local" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" maxlength="20" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.note" type="textarea" maxlength="255" show-word-limit placeholder="补充地址、偏好或特殊要求" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">提交预约</el-button>
        </el-form>
      </section>

      <section class="panel booking-list-panel">
        <div class="section-heading">
          <p>我的预约</p>
          <h1>跟踪确认与履约进度</h1>
        </div>
        <div class="list booking-list">
          <article class="row booking-row" v-for="booking in bookings" :key="booking.id">
            <div>
              <div class="booking-title">
                <h2>{{ booking.serviceName }}</h2>
                <el-tag :type="statusMap[booking.status]?.type || 'info'">
                  {{ statusMap[booking.status]?.text || booking.status }}
                </el-tag>
              </div>
              <p>{{ merchantName(booking.merchantId) }} · {{ formatTime(booking.appointmentTime) }}</p>
              <p>联系电话：{{ booking.contactPhone }}</p>
              <p v-if="booking.note">备注：{{ booking.note }}</p>
            </div>
            <div class="row-actions">
              <el-button
                v-if="booking.status !== 'CANCELED' && booking.status !== 'COMPLETED'"
                type="danger"
                plain
                @click="cancel(booking.id)"
              >
                取消预约
              </el-button>
            </div>
          </article>
          <el-empty v-if="bookings.length === 0" description="暂无预约记录" />
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.booking-workbench {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(300px, 380px) 1fr;
}
.booking-form :deep(.el-select),
.booking-form :deep(.el-input),
.booking-form :deep(.el-textarea) {
  width: 100%;
}
.section-heading {
  margin-bottom: 18px;
}
.section-heading p {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  margin: 0 0 6px;
}
.section-heading h1 {
  font-size: 24px;
  margin: 0;
}
.booking-title {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.booking-list {
  max-height: 680px;
  overflow: auto;
  padding-right: 4px;
}
.booking-row {
  align-items: flex-start;
}
@media (max-width: 900px) {
  .booking-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
