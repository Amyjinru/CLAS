<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMerchantProfileCode, updateMyMerchantProfile, uploadMerchantLogo } from '../../api/clas'
import LocationSelector from '../LocationSelector.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  merchant: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'saved'])

const form = reactive({
  merchantName: '',
  phone: '',
  bankAccount: '',
  address: '',
  longitude: null,
  latitude: null,
  deliveryRadiusM: 3000,
  businessHours: '09:00-21:00',
  code: ''
})
const locationData = reactive({
  province: '',
  city: '',
  district: '',
  street: '',
  address: '',
  longitude: null,
  latitude: null,
  source: ''
})
const fileInputRef = ref(null)
const logoUploading = ref(false)
const saving = ref(false)
const codeSending = ref(false)
const codeCooldown = ref(0)
let cooldownTimer = null

const sensitiveChanged = computed(() => {
  const merchant = props.merchant
  return Boolean(merchant && (form.phone !== merchant.phone || form.bankAccount !== merchant.bankAccount))
})

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

watch(() => props.visible, (visible) => {
  if (visible) resetForm()
})

function resetForm() {
  const merchant = props.merchant || {}
  form.merchantName = merchant.merchantName || ''
  form.phone = merchant.phone || ''
  form.bankAccount = merchant.bankAccount || ''
  form.address = merchant.address || ''
  form.longitude = merchant.longitude ?? null
  form.latitude = merchant.latitude ?? null
  form.deliveryRadiusM = merchant.deliveryRadiusM || 3000
  form.businessHours = merchant.businessHours || '09:00-21:00'
  form.code = ''
  Object.assign(locationData, {
    province: '',
    city: '',
    district: '',
    street: merchant.address || '',
    address: merchant.address || '',
    longitude: merchant.longitude ?? null,
    latitude: merchant.latitude ?? null,
    source: 'manual'
  })
}

function syncLocationDraft(loc) {
  form.address = loc.address
  form.longitude = loc.longitude
  form.latitude = loc.latitude
  Object.assign(locationData, loc)
}

function onLocationConfirm(loc) {
  syncLocationDraft(loc)
  ElMessage.success('店铺地址已确认')
}

function openLogoPicker() {
  fileInputRef.value?.click()
}

async function onLogoSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.warning('仅支持 jpg/png 图片')
    return
  }
  logoUploading.value = true
  try {
    const merchant = await uploadMerchantLogo(file)
    emit('saved', merchant)
    ElMessage.success('店铺头像已更新')
  } finally {
    logoUploading.value = false
  }
}

function payload() {
  return {
    merchantName: form.merchantName.trim(),
    phone: form.phone.trim(),
    bankAccount: form.bankAccount.trim(),
    address: form.address.trim(),
    longitude: form.longitude,
    latitude: form.latitude,
    deliveryRadiusM: form.deliveryRadiusM,
    businessHours: form.businessHours.trim(),
    code: form.code.trim()
  }
}

async function sendCode() {
  codeSending.value = true
  try {
    await sendMerchantProfileCode(payload())
    ElMessage.success('验证码已发送，请查看后端控制台输出')
    codeCooldown.value = 60
    cooldownTimer && clearInterval(cooldownTimer)
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
  } finally {
    codeSending.value = false
  }
}

async function save() {
  if (!form.merchantName.trim()) return ElMessage.warning('请输入店铺名称')
  if (!form.address.trim() || !form.longitude || !form.latitude) return ElMessage.warning('请确认店铺地图地址')
  if (!/^\d{2}:\d{2}-\d{2}:\d{2}$/.test(form.businessHours.trim())) return ElMessage.warning('营业时间格式应为 HH:mm-HH:mm')
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) return ElMessage.warning('请输入正确手机号')
  if (!/^\d{9,25}$/.test(form.bankAccount.trim())) return ElMessage.warning('请输入 9 到 25 位银行卡号')
  if (sensitiveChanged.value && !form.code.trim()) return ElMessage.warning('手机号或银行卡变更需要验证码')
  const phoneChanged = props.merchant && form.phone !== props.merchant.phone
  saving.value = true
  try {
    const merchant = await updateMyMerchantProfile(payload())
    emit('saved', merchant)
    dialogVisible.value = false
    if (phoneChanged) {
      ElMessage.success({
        message: `手机号已变更为 ${form.phone}，请使用新号码重新登录`,
        duration: 8000
      })
    } else {
      ElMessage.success('店铺资料已更新')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" title="信息修改" width="720px" destroy-on-close>
    <div class="profile-dialog">
      <div class="logo-row">
        <img v-if="merchant?.logo" :src="merchant.logo" alt="店铺头像" loading="lazy" />
        <div v-else class="logo-placeholder">{{ merchant?.merchantName?.slice(0, 1) || '店' }}</div>
        <input ref="fileInputRef" type="file" accept="image/jpeg,image/png" class="file-input" @change="onLogoSelected" />
        <el-button type="primary" :loading="logoUploading" @click="openLogoPicker">上传店铺头像</el-button>
      </div>
      <el-form label-width="96px">
        <el-form-item label="店铺名称">
          <el-input v-model="form.merchantName" maxlength="100" />
        </el-form-item>
        <el-form-item label="店铺地址">
          <LocationSelector
            v-model="locationData"
            @update:modelValue="syncLocationDraft"
            @confirm="onLocationConfirm"
          />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.businessHours" placeholder="09:00-21:00" maxlength="11" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="银行卡号">
          <el-input v-model="form.bankAccount" maxlength="25" />
        </el-form-item>
        <el-form-item v-if="sensitiveChanged" label="验证码">
          <div class="code-row">
            <el-input v-model="form.code" maxlength="6" placeholder="请输入验证码" />
            <el-button :loading="codeSending" :disabled="codeCooldown > 0" @click="sendCode">
              {{ codeCooldown ? `${codeCooldown}秒后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.profile-dialog {
  display: grid;
  gap: 18px;
}

.logo-row {
  align-items: center;
  display: flex;
  gap: 14px;
}

.logo-row img,
.logo-placeholder {
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  height: 64px;
  object-fit: cover;
  width: 64px;
}

.logo-placeholder {
  align-items: center;
  background: #409eff;
  color: #fff;
  display: flex;
  font-size: 26px;
  font-weight: 700;
  justify-content: center;
}

.file-input {
  display: none;
}

.code-row {
  display: grid;
  gap: 10px;
  grid-template-columns: 1fr auto;
  width: 100%;
}
</style>
