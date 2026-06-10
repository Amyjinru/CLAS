<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAddress, updateAddress, deleteAddress, setDefaultAddress } from '../../api/clas'
import LocationSelector from '../LocationSelector.vue'

const props = defineProps({
  addresses: { type: Array, default: () => [] }
})

const emit = defineEmits(['reload'])

const savingAddress = ref(false)
const addressActionId = ref('')
const editingAddressId = ref(null)
const locationKey = ref(0)
const addressFormRef = ref(null)

const form = reactive({
  contactName: '', phone: '', address: '',
  longitude: null, latitude: null, isDefault: false
})

const locationData = reactive({
  province: '', city: '', district: '', street: '',
  address: '', longitude: null, latitude: null
})

const addressRules = {
  contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请填写联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请选择或定位收货地址', trigger: 'change' }]
}

function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function hasCoordinate(lon, lat) {
  return lon !== null && lon !== undefined && lon !== ''
    && lat !== null && lat !== undefined && lat !== ''
}

function resetForm() {
  Object.assign(form, { contactName: '', phone: '', address: '', longitude: null, latitude: null, isDefault: false })
  editingAddressId.value = null
  resetLocationData()
}

function resetLocationData() {
  Object.assign(locationData, { province: '', city: '', district: '', street: '', address: '', longitude: null, latitude: null })
  locationKey.value += 1
}

function onLocationConfirm(loc) {
  syncLocationDraft(loc)
  ElMessage.success('收货位置已确认')
}

function syncLocationDraft(loc) {
  Object.assign(form, { address: loc.address, longitude: loc.longitude, latitude: loc.latitude })
  addressFormRef.value?.clearValidate?.('address')
}

function validateForm() {
  const cn = form.contactName.trim()
  const ph = form.phone.trim()
  const ad = form.address.trim()
  if (!cn) { ElMessage.warning('请填写联系人'); return false }
  if (!ph) { ElMessage.warning('请填写联系电话'); return false }
  if (!ad || !hasCoordinate(form.longitude, form.latitude)) { ElMessage.warning('请选择或定位收货地址'); return false }
  Object.assign(form, { contactName: cn, phone: ph, address: ad })
  return true
}

async function submitAddress() {
  if (!validateForm()) return
  savingAddress.value = true
  try {
    if (editingAddressId.value) {
      await updateAddress(editingAddressId.value, { ...form })
      ElMessage.success('地址已更新')
    } else {
      await createAddress({ ...form })
      ElMessage.success('地址已保存')
    }
    resetForm()
    emit('reload')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '地址保存失败'))
  } finally { savingAddress.value = false }
}

function editAddress(item) {
  editingAddressId.value = item.id
  Object.assign(form, {
    contactName: item.contactName, phone: item.phone, address: item.address,
    longitude: item.longitude, latitude: item.latitude, isDefault: Boolean(item.isDefault)
  })
  Object.assign(locationData, {
    province: '', city: '', district: '', street: item.address || '',
    address: item.address || '', longitude: item.longitude, latitude: item.latitude
  })
  locationKey.value += 1
}

async function markDefault(id) {
  addressActionId.value = `default-${id}`
  try {
    await setDefaultAddress(id)
    ElMessage.success('默认地址已更新')
    emit('reload')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '设置默认地址失败'))
  } finally { addressActionId.value = '' }
}

async function removeAddr(id) {
  try {
    await ElMessageBox.confirm('删除后需要重新添加该收货地址，确定删除吗？', '删除地址', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  addressActionId.value = `delete-${id}`
  try {
    await deleteAddress(id)
    if (editingAddressId.value === id) resetForm()
    ElMessage.success('地址已删除')
    emit('reload')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除地址失败'))
  } finally { addressActionId.value = '' }
}
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>收货地址</h2>
        <p>{{ editingAddressId ? '正在编辑已保存地址' : '新增常用收货地址' }}</p>
      </div>
      <el-tag type="success">{{ addresses.length }} 个地址</el-tag>
    </div>

    <el-form ref="addressFormRef" class="address-form" :model="form" :rules="addressRules" label-position="top">
      <div class="form-row">
        <el-form-item label="联系人" prop="contactName" required>
          <el-input v-model="form.contactName" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone" required>
          <el-input v-model="form.phone" placeholder="收货人手机号" />
        </el-form-item>
      </div>
      <el-form-item label="收货位置" prop="address" required>
        <LocationSelector :key="locationKey" v-model="locationData" @update:modelValue="syncLocationDraft" @confirm="onLocationConfirm" />
      </el-form-item>
      <div class="form-actions">
        <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
        <div>
          <el-button @click="resetForm">{{ editingAddressId ? '取消编辑' : '重置' }}</el-button>
          <el-button type="primary" :loading="savingAddress" @click="submitAddress">{{ editingAddressId ? '保存修改' : '保存地址' }}</el-button>
        </div>
      </div>
    </el-form>

    <el-empty v-if="!addresses.length" description="暂无收货地址">
      <el-button type="primary" plain @click="resetForm">添加地址</el-button>
    </el-empty>

    <article v-for="item in addresses" :key="item.id" class="list-row">
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
        <el-button v-if="!item.isDefault" text :loading="addressActionId === `default-${item.id}`" @click="markDefault(item.id)">设默认</el-button>
        <el-button text type="danger" :loading="addressActionId === `delete-${item.id}`" @click="removeAddr(item.id)">删除</el-button>
      </div>
    </article>
  </div>
</template>

<style scoped>
.section-head { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 16px; }
.section-head h2 { margin: 0; }
.section-head p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }
.address-form { border-bottom: 1px solid var(--border-light); margin-bottom: 6px; padding-bottom: 16px; }
.form-row { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.form-actions { align-items: center; display: flex; gap: 12px; justify-content: space-between; }
.list-row { align-items: center; border-top: 1px solid var(--border-light); display: flex; justify-content: space-between; padding: 14px 0; }
.row-title { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; }
.row-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
.coord-line { color: var(--text-secondary); font-size: 12px; }
@media (max-width: 900px) {
  .form-row, .form-actions { grid-template-columns: 1fr; }
  .form-actions { display: grid; align-items: stretch; }
  .list-row { align-items: flex-start; flex-direction: column; }
  .row-actions { justify-content: flex-start; }
}
</style>
