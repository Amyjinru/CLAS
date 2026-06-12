<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMyMerchant,
  sendMerchantPhoneCode,
  sendMerchantBankCode,
  sendPhoneChangeCode,
  setSessionUser,
  updateBoundPhone,
  updateMyMerchantProfile,
  uploadMerchantLogo
} from '../../api/clas'
import LocationSelector from '../LocationSelector.vue'
import {
  buildMerchantProfilePayload,
  isBankAccountReadyForSave,
  isVerificationReady,
  shouldResetVerification,
  trimValue
} from './merchantProfileSecurity'

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
  accountPhone: '',
  accountCode: '',
  merchantName: '',
  phone: '',
  bankAccount: '',
  address: '',
  longitude: null,
  latitude: null,
  deliveryRadiusM: 3000,
  businessHours: '09:00-21:00',
  phoneCode: '',
  bankCode: ''
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
const accountPhoneSaving = ref(false)
const phoneSaving = ref(false)
const bankSaving = ref(false)

const phonePattern = /^1[3-9]\d{9}$/
const bankPattern = /^\d{9,25}$/

const accountPhoneChanged = computed(() => Boolean(props.merchant && trimValue(form.accountPhone) !== trimValue(props.merchant.userId)))
const accountPhoneValid = computed(() => phonePattern.test(form.accountPhone.trim()))
const phoneChanged = computed(() => Boolean(props.merchant && trimValue(form.phone) !== trimValue(props.merchant.phone)))
const bankChanged = computed(() => Boolean(props.merchant && trimValue(form.bankAccount) !== trimValue(props.merchant.bankAccount)))
const phoneValid = computed(() => phonePattern.test(form.phone.trim()))
const bankValid = computed(() => bankPattern.test(form.bankAccount.trim()))
function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function createVerificationControl({ codeKey, valueGetter }) {
  const sending = ref(false)
  const cooldown = ref(0)
  const sent = ref(false)
  const lastSentValue = ref('')
  const sendText = computed(() => cooldown.value ? `${cooldown.value}秒后重发` : '发送验证码')
  let timer = null

  function clearTimer() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function startCooldown(seconds = 60) {
    cooldown.value = seconds
    clearTimer()
    timer = setInterval(() => {
      cooldown.value--
      if (cooldown.value <= 0) {
        cooldown.value = 0
        clearTimer()
      }
    }, 1000)
  }

  function markSent() {
    sent.value = true
    lastSentValue.value = trimValue(valueGetter())
    startCooldown()
  }

  function reset({ clearCooldown = false } = {}) {
    form[codeKey] = ''
    sent.value = false
    lastSentValue.value = ''
    if (clearCooldown) {
      cooldown.value = 0
      clearTimer()
    }
  }

  function resetIfValueChanged(value) {
    if (shouldResetVerification(value, lastSentValue.value)) {
      form[codeKey] = ''
      sent.value = false
    }
  }

  return {
    sending,
    cooldown,
    sent,
    lastSentValue,
    sendText,
    markSent,
    reset,
    resetIfValueChanged,
    clearTimer
  }
}

const accountVerification = createVerificationControl({
  codeKey: 'accountCode',
  valueGetter: () => form.accountPhone
})
const phoneVerification = createVerificationControl({
  codeKey: 'phoneCode',
  valueGetter: () => form.phone
})
const bankVerification = createVerificationControl({
  codeKey: 'bankCode',
  valueGetter: () => form.bankAccount
})
const canSaveAccountPhoneChange = computed(() => accountPhoneChanged.value && isVerificationReady({
  changed: true,
  sent: accountVerification.sent.value,
  sentValue: accountVerification.lastSentValue.value,
  currentValue: form.accountPhone,
  code: form.accountCode
}))
const canSavePhoneChange = computed(() => phoneChanged.value && isVerificationReady({
  changed: true,
  sent: phoneVerification.sent.value,
  sentValue: phoneVerification.lastSentValue.value,
  currentValue: form.phone,
  code: form.phoneCode
}))
const canSaveBankChange = computed(() => bankChanged.value && isVerificationReady({
  changed: true,
  sent: bankVerification.sent.value,
  sentValue: bankVerification.lastSentValue.value,
  currentValue: form.bankAccount,
  code: form.bankCode
}))

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

watch(() => props.visible, (visible) => {
  if (visible) resetForm()
})

function resetForm() {
  const merchant = props.merchant || {}
  form.accountPhone = merchant.userId || ''
  form.accountCode = ''
  form.merchantName = merchant.merchantName || ''
  form.phone = merchant.phone || ''
  form.bankAccount = merchant.bankAccount || ''
  form.address = merchant.address || ''
  form.longitude = merchant.longitude ?? null
  form.latitude = merchant.latitude ?? null
  form.deliveryRadiusM = merchant.deliveryRadiusM || 3000
  form.businessHours = merchant.businessHours || '09:00-21:00'
  accountVerification.reset({ clearCooldown: true })
  phoneVerification.reset({ clearCooldown: true })
  bankVerification.reset({ clearCooldown: true })
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

watch(() => form.phone, (newPhone) => {
  phoneVerification.resetIfValueChanged(newPhone)
})

watch(() => form.bankAccount, (newBank) => {
  bankVerification.resetIfValueChanged(newBank)
})

watch(() => form.accountPhone, (phone) => {
  accountVerification.resetIfValueChanged(phone)
})

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

async function sendPhoneCode() {
  if (!phoneChanged.value) return
  if (!phoneValid.value) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  phoneVerification.sending.value = true
  try {
    await sendMerchantPhoneCode(trimValue(form.phone))
    phoneVerification.markSent()
    ElMessage.success('验证码已发送，请查看后端控制台输出')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码发送失败'))
  } finally {
    phoneVerification.sending.value = false
  }
}

async function sendBankCode() {
  if (!bankChanged.value) return
  if (!bankValid.value) {
    ElMessage.warning('请输入 9 到 25 位银行卡号')
    return
  }
  bankVerification.sending.value = true
  try {
    await sendMerchantBankCode()
    bankVerification.markSent()
    ElMessage.success('验证码已发送，请查看后端控制台输出')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码发送失败'))
  } finally {
    bankVerification.sending.value = false
  }
}

async function sendAccountCode() {
  if (!accountPhoneChanged.value) return
  if (!accountPhoneValid.value) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  accountVerification.sending.value = true
  try {
    const phone = trimValue(form.accountPhone)
    await sendPhoneChangeCode({ phone })
    accountVerification.markSent()
    ElMessage.success('验证码已发送，请查看后端控制台输出')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码发送失败'))
  } finally {
    accountVerification.sending.value = false
  }
}

function validateBasicProfile() {
  if (!trimValue(form.merchantName)) {
    ElMessage.warning('请输入店铺名称')
    return false
  }
  if (!trimValue(form.address) || form.longitude == null || form.latitude == null) {
    ElMessage.warning('请确认店铺地图地址')
    return false
  }
  if (!/^\d{2}:\d{2}-\d{2}:\d{2}$/.test(trimValue(form.businessHours))) {
    ElMessage.warning('营业时间格式应为 HH:mm-HH:mm')
    return false
  }
  if (!Number.isInteger(form.deliveryRadiusM) || form.deliveryRadiusM < 500 || form.deliveryRadiusM > 10000) {
    ElMessage.warning('配送半径应为 500 到 10000 米')
    return false
  }
  if (!phonePattern.test(trimValue(form.phone))) {
    ElMessage.warning('请输入正确手机号')
    return false
  }
  if (!isBankAccountReadyForSave(form.bankAccount, bankChanged.value)) {
    ElMessage.warning('请输入 9 到 25 位银行卡号')
    return false
  }
  return true
}

function validateAccountPhoneChange() {
  if (!phonePattern.test(trimValue(form.accountPhone))) {
    ElMessage.warning('请输入正确注册绑定手机号')
    return false
  }
  if (!isVerificationReady({
    changed: accountPhoneChanged.value,
    sent: accountVerification.sent.value,
    sentValue: accountVerification.lastSentValue.value,
    currentValue: form.accountPhone,
    code: form.accountCode
  })) {
    ElMessage.warning('请先发送并填写注册绑定手机号验证码')
    return false
  }
  return true
}

function validatePhoneChange() {
  if (!phonePattern.test(trimValue(form.phone))) {
    ElMessage.warning('请输入正确手机号')
    return false
  }
  if (!isVerificationReady({
    changed: phoneChanged.value,
    sent: phoneVerification.sent.value,
    sentValue: phoneVerification.lastSentValue.value,
    currentValue: form.phone,
    code: form.phoneCode
  })) {
    ElMessage.warning('请先发送并填写联系电话验证码')
    return false
  }
  return true
}

function validateBankChange() {
  if (!isBankAccountReadyForSave(form.bankAccount, bankChanged.value)) {
    ElMessage.warning('请输入 9 到 25 位银行卡号')
    return false
  }
  if (!isVerificationReady({
    changed: bankChanged.value,
    sent: bankVerification.sent.value,
    sentValue: bankVerification.lastSentValue.value,
    currentValue: form.bankAccount,
    code: form.bankCode
  })) {
    ElMessage.warning('请先发送并填写银行卡号验证码')
    return false
  }
  return true
}

async function updateAccountPhone() {
  const result = await updateBoundPhone({
    phone: trimValue(form.accountPhone),
    code: trimValue(form.accountCode)
  })
  setSessionUser({ ...result.user, token: result.token, password: undefined })
  accountVerification.reset({ clearCooldown: true })
  const merchant = await getMyMerchant()
  emit('saved', merchant)
  return merchant
}

async function saveAccountPhone() {
  if (!validateAccountPhoneChange()) return
  accountPhoneSaving.value = true
  try {
    await updateAccountPhone()
    ElMessage.success('注册绑定手机号已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '注册绑定手机号更新失败'))
  } finally {
    accountPhoneSaving.value = false
  }
}

async function saveMerchantProfileChanges(successMessage = '店铺资料已更新') {
  const merchant = await updateMyMerchantProfile(buildMerchantProfilePayload(form, {
    phoneChanged: phoneChanged.value,
    bankChanged: bankChanged.value
  }))
  emit('saved', merchant)
  ElMessage.success(successMessage)
  return merchant
}

async function savePhone() {
  if (!validatePhoneChange()) return
  if (!validateBasicProfile()) return
  phoneSaving.value = true
  try {
    await saveMerchantProfileChanges('联系电话已更新')
    phoneVerification.reset({ clearCooldown: true })
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '联系电话更新失败'))
  } finally {
    phoneSaving.value = false
  }
}

async function saveBank() {
  if (!validateBankChange()) return
  if (!validateBasicProfile()) return
  bankSaving.value = true
  try {
    await saveMerchantProfileChanges('银行卡号已更新')
    bankVerification.reset({ clearCooldown: true })
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '银行卡号更新失败'))
  } finally {
    bankSaving.value = false
  }
}

async function save() {
  if (!phonePattern.test(trimValue(form.accountPhone))) return ElMessage.warning('请输入正确注册绑定手机号')
  if (!validateBasicProfile()) return
  if (accountPhoneChanged.value && !validateAccountPhoneChange()) return
  if (phoneChanged.value && !validatePhoneChange()) return
  if (bankChanged.value && !validateBankChange()) return
  saving.value = true
  try {
    if (accountPhoneChanged.value) await updateAccountPhone()
    const anythingChanged = phoneChanged.value || bankChanged.value || hasBasicProfileChanged.value
    const merchant = anythingChanged ? await saveMerchantProfileChanges('店铺资料已更新') : await getMyMerchant()
    if (!anythingChanged) emit('saved', merchant)
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '店铺资料更新失败'))
  } finally {
    saving.value = false
  }
}

const hasBasicProfileChanged = computed(() => {
  const merchant = props.merchant
  if (!merchant) return false
  return form.merchantName !== (merchant.merchantName || '')
    || form.address !== (merchant.address || '')
    || form.longitude !== (merchant.longitude ?? null)
    || form.latitude !== (merchant.latitude ?? null)
    || form.deliveryRadiusM !== (merchant.deliveryRadiusM || 3000)
    || form.businessHours !== (merchant.businessHours || '09:00-21:00')
})

onUnmounted(() => {
  accountVerification.clearTimer()
  phoneVerification.clearTimer()
  bankVerification.clearTimer()
})
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
        <el-form-item label="注册绑定手机号">
          <div class="code-row">
            <el-input v-model="form.accountPhone" maxlength="11" />
            <el-button
              class="code-button"
              :type="accountPhoneChanged && accountPhoneValid ? 'success' : 'info'"
              :loading="accountVerification.sending.value"
              :disabled="!accountPhoneChanged || !accountPhoneValid || accountVerification.cooldown.value > 0"
              @click="sendAccountCode"
            >
              {{ accountVerification.sendText.value }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="accountVerification.sent.value" label="账号验证码">
          <div class="code-row">
            <el-input v-model="form.accountCode" maxlength="6" placeholder="请输入验证码" />
            <el-button
              class="code-button"
              type="primary"
              :loading="accountPhoneSaving"
              :disabled="!canSaveAccountPhoneChange"
              @click="saveAccountPhone"
            >
              保存手机号
            </el-button>
          </div>
        </el-form-item>
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
        <el-form-item label="配送半径">
          <el-input-number
            v-model="form.deliveryRadiusM"
            :min="500"
            :max="10000"
            :precision="0"
            :step="500"
            step-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <div class="code-row">
            <el-input v-model="form.phone" maxlength="11" />
            <el-button
              class="code-button"
              :type="phoneChanged && phoneValid ? 'success' : 'info'"
              :loading="phoneVerification.sending.value"
              :disabled="!phoneChanged || !phoneValid || phoneVerification.cooldown.value > 0"
              @click="sendPhoneCode"
            >
              {{ phoneVerification.sendText.value }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="phoneVerification.sent.value" label="电话验证码">
          <div class="code-row">
            <el-input v-model="form.phoneCode" maxlength="6" placeholder="请输入电话验证码" />
            <el-button
              class="code-button"
              type="primary"
              :loading="phoneSaving"
              :disabled="!canSavePhoneChange"
              @click="savePhone"
            >
              保存电话
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="银行卡号">
          <div class="code-row">
            <el-input v-model="form.bankAccount" maxlength="25" />
            <el-button
              class="code-button"
              :type="bankChanged && bankValid ? 'success' : 'info'"
              :loading="bankVerification.sending.value"
              :disabled="!bankChanged || !bankValid || bankVerification.cooldown.value > 0"
              @click="sendBankCode"
            >
              {{ bankVerification.sendText.value }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="bankVerification.sent.value" label="银行卡验证码">
          <div class="code-row">
            <el-input v-model="form.bankCode" maxlength="6" placeholder="请输入银行卡验证码" />
            <el-button
              class="code-button"
              type="primary"
              :loading="bankSaving"
              :disabled="!canSaveBankChange"
              @click="saveBank"
            >
              保存银行卡
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <div class="dialog-actions">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
      </div>
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
  border: 1px solid var(--border-color);
  border-radius: 50%;
  height: 64px;
  object-fit: cover;
  width: 64px;
}

.logo-placeholder {
  align-items: center;
  background: var(--color-accent);
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
  align-items: center;
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr) 112px;
  width: 100%;
}

.code-button {
  min-width: 112px;
  width: 112px;
}

.dialog-actions {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  width: 100%;
}

@media (max-width: 720px) {
  .logo-row {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .code-row {
    grid-template-columns: 1fr;
  }

  .code-button {
    width: 100%;
  }

  .dialog-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .dialog-actions :deep(.el-button) {
    margin-left: 0;
    width: 100%;
  }
}
</style>
